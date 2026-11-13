package com.pyramidsoft.siqasir.printer;


import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

import com.pyramidsoft.siqasir.printer.service.BTServices;


public class BluetoothHandler extends Handler {

    // Message types sent from the BTService Handler
    public static final String TAG = BluetoothHandler.class.getSimpleName();
    public Context context;
    public static final boolean DEBUG = true;
    public static final String TOAST = "toast";


    public BluetoothHandler(Context context) {
        this.context = context;
    }


    public void handleMessage(Message msg) {
        switch (msg.what) {
            case BTServices.MESSAGE_STATE_CHANGE:
                if (DEBUG)
                    Log.i(TAG, "MESSAGE_STATE_CHANGE: " + msg.arg1);
                switch (msg.arg1) {
                    case BTServices.STATE_CONNECTED:
                        Toast.makeText(context, "Tersambung",
                                Toast.LENGTH_SHORT).show();
                        break;
                    case BTServices.STATE_CONNECTING:
                        Toast.makeText(context, "Sedang menghubungkan...", Toast.LENGTH_SHORT).show();
                        break;
                    case BTServices.STATE_LISTEN:
                    case BTServices.STATE_NONE:
                        Log.i(TAG, "Bluetooth state listen or none");
                        break;
                }
                break;
            case BTServices.MESSAGE_WRITE:

                break;
            case BTServices.MESSAGE_READ:

                break;
            case BTServices.MESSAGE_DEVICE_NAME:
                // save the connected device's name

                break;
            case BTServices.MESSAGE_CONNECTION_LOST:    //蓝牙已断开连接
                Toast.makeText(context, "Koneksi terputus",
                        Toast.LENGTH_SHORT).show();

                break;
            case BTServices.MESSAGE_UNABLE_CONNECT:     //无法连接设备
                Toast.makeText(context, "Tidak dapat terhubung ke perangkat",
                        Toast.LENGTH_SHORT).show();
                break;
        }
    }

}
