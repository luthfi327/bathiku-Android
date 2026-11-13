package com.pyramidsoft.siqasir.printer.service;

import android.annotation.SuppressLint;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;

import com.pyramidsoft.siqasir.printer.BluetoothHandler;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

public class BTServices extends Service {
    private static final String TAG = "BTServices";
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private static final String NAME = "BTPrinter";

    // State
    public static final int STATE_NONE = 0;
    public static final int STATE_LISTEN = 1;
    public static final int STATE_CONNECTING = 2;
    public static final int STATE_CONNECTED = 3;

    // Message codes
    public static final int MESSAGE_STATE_CHANGE = 1;
    public static final int MESSAGE_READ = 2;
    public static final int MESSAGE_WRITE = 3;
    public static final int MESSAGE_DEVICE_NAME = 4;
    public static final int MESSAGE_CONNECTION_LOST = 5;
    public static final int MESSAGE_UNABLE_CONNECT = 6;

    private final BluetoothAdapter mAdapter = BluetoothAdapter.getDefaultAdapter();
    private final IBinder mBinder = new LocalBinder();
    private Handler mHandler;

    private int mState = STATE_NONE;

    private ConnectThread mConnectThread;
    private ConnectedThread mConnectedThread;

    public class LocalBinder extends Binder {
        public BTServices getService() {
            return BTServices.this;
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        // Gunakan handler khusus untuk mengirim pesan ke activity
        mHandler = new BluetoothHandler(this);
        return mBinder;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String mac = intent.getStringExtra("bluetooth_device");
        if (mac != null) {
            BluetoothDevice device = mAdapter.getRemoteDevice(mac);
            connect(device);
        }
        return START_STICKY;
    }

    private synchronized void setState(int state) {
        mState = state;
        if (mHandler != null)
            mHandler.obtainMessage(MESSAGE_STATE_CHANGE, state, -1).sendToTarget();
    }

    public synchronized int getState() {
        return mState;
    }

    public synchronized void connect(BluetoothDevice device) {
        Log.d(TAG, "connect to: " + device);

        if (mState == STATE_CONNECTING && mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }

        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        mConnectThread = new ConnectThread(device);
        mConnectThread.start();
        setState(STATE_CONNECTING);
    }

    public synchronized void connected(BluetoothSocket socket, BluetoothDevice device) {
        Log.d(TAG, "connected to: " + device);

        if (mConnectThread != null) { mConnectThread.cancel(); mConnectThread = null; }
        if (mConnectedThread != null) { mConnectedThread.cancel(); mConnectedThread = null; }

        mConnectedThread = new ConnectedThread(socket);
        mConnectedThread.start();

        if (mHandler != null)
            mHandler.obtainMessage(MESSAGE_DEVICE_NAME, device.getName()).sendToTarget();

        setState(STATE_CONNECTED);
    }

    public synchronized void stop() {
        Log.d(TAG, "stop service");
        setState(STATE_NONE);
        if (mConnectThread != null) { mConnectThread.cancel(); mConnectThread = null; }
        if (mConnectedThread != null) { mConnectedThread.cancel(); mConnectedThread = null; }
    }

    public void write(byte[] data) {
        ConnectedThread r;
        synchronized (this) {
            if (mState != STATE_CONNECTED) return;
            r = mConnectedThread;
        }
        r.write(data);
    }

    private void connectionFailed() {
        setState(STATE_LISTEN);
        if (mHandler != null)
            mHandler.obtainMessage(MESSAGE_UNABLE_CONNECT).sendToTarget();
    }

    private void connectionLost() {
        setState(STATE_LISTEN);
        if (mHandler != null)
            mHandler.obtainMessage(MESSAGE_CONNECTION_LOST).sendToTarget();
    }

    private class ConnectThread extends Thread {
        private final BluetoothDevice mmDevice;
        private BluetoothSocket mmSocket;

        @SuppressLint("MissingPermission")
        ConnectThread(BluetoothDevice device) {
            mmDevice = device;
            try {
                mmSocket = device.createRfcommSocketToServiceRecord(MY_UUID);
            } catch (IOException e) {
                Log.e(TAG, "Socket create failed", e);
            }
        }

        public void run() {
            mAdapter.cancelDiscovery();
            try {
                mmSocket.connect();
            } catch (IOException e) {
                Log.e(TAG, "connect failed", e);
                connectionFailed();
                try { mmSocket.close(); } catch (IOException ignore) {}
                return;
            }
            synchronized (BTServices.this) {
                mConnectThread = null;
            }
            connected(mmSocket, mmDevice);
        }

        void cancel() {
            try { mmSocket.close(); } catch (IOException ignore) {}
        }
    }

    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        ConnectedThread(BluetoothSocket socket) {
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                Log.e(TAG, "Temp sockets not created", e);
            }
            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            byte[] buffer = new byte[256];
            int bytes;

            while (true) {
                try {
                    bytes = mmInStream.read(buffer);
                    if (bytes > 0 && mHandler != null)
                        mHandler.obtainMessage(MESSAGE_READ, bytes, -1, buffer.clone()).sendToTarget();
                } catch (IOException e) {
                    Log.e(TAG, "disconnected", e);
                    connectionLost();
                    break;
                }
            }
        }

        void write(byte[] buffer) {
            try {
                mmOutStream.write(buffer);
                if (mHandler != null)
                    mHandler.obtainMessage(MESSAGE_WRITE, -1, -1, buffer.clone()).sendToTarget();
            } catch (IOException e) {
                Log.e(TAG, "Exception during write", e);
            }
        }

        void cancel() {
            try { mmSocket.close(); } catch (IOException ignore) {}
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stop();
    }
}
