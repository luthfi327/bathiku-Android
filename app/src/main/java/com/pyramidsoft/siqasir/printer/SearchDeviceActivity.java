package com.pyramidsoft.siqasir.printer;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.pyramidsoft.siqasir.R;
import com.pyramidsoft.siqasir.databinding.ActivityDeviceListBinding;

import java.util.HashSet;
import java.util.Set;

public class SearchDeviceActivity extends AppCompatActivity {
    private ActivityDeviceListBinding binding;

    private static final String TAG = "SearchDeviceActivity";
    public static final String EXTRA_DEVICE_ADDRESS = "device_address";
    public static final String EXTRA_DEVICE_NAME = "device_name";

    private BluetoothAdapter mBtAdapter;
    private ArrayAdapter<String> mPairedDevicesArrayAdapter;
    private ArrayAdapter<String> mNewDevicesArrayAdapter;

    // Untuk mencegah duplikat device di daftar
    private final Set<String> discoveredAddresses = new HashSet<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityDeviceListBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setResult(Activity.RESULT_CANCELED);

        mPairedDevicesArrayAdapter = new ArrayAdapter<>(this, R.layout.device_name);
        mNewDevicesArrayAdapter = new ArrayAdapter<>(this, R.layout.device_name);

        binding.pairedDevices.setAdapter(mPairedDevicesArrayAdapter);
        binding.pairedDevices.setOnItemClickListener(mDeviceClickListener);

        binding.newDevices.setAdapter(mNewDevicesArrayAdapter);
        binding.newDevices.setOnItemClickListener(mDeviceClickListener);

        // Register BroadcastReceiver
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        registerReceiver(mReceiver, filter);

        mBtAdapter = BluetoothAdapter.getDefaultAdapter();
        loadPairedDevices();

        binding.buttonScan.setOnClickListener(v -> {
            doDiscovery();
            binding.buttonScan.setVisibility(View.GONE);
            binding.titleNewDevices.setVisibility(View.VISIBLE);
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mBtAdapter != null) {
            mBtAdapter.cancelDiscovery();
        }
        unregisterReceiver(mReceiver);
    }

    private void loadPairedDevices() {
        Set<BluetoothDevice> pairedDevices = mBtAdapter.getBondedDevices();

        mPairedDevicesArrayAdapter.clear();
        if (pairedDevices != null && pairedDevices.size() > 0) {
            binding.titlePairedDevices.setVisibility(View.VISIBLE);
            for (BluetoothDevice device : pairedDevices) {
                String name = (device.getName() != null) ? device.getName() : "Unknown Device";
                mPairedDevicesArrayAdapter.add(name + "\n" + device.getAddress());
            }
        } else {
            mPairedDevicesArrayAdapter.add(getString(R.string.none_paired));
        }
    }

    private void doDiscovery() {
        Log.d(TAG, "doDiscovery()");
        if (mBtAdapter.isDiscovering()) {
            mBtAdapter.cancelDiscovery();
        }
        discoveredAddresses.clear();
        mNewDevicesArrayAdapter.clear();
        mBtAdapter.startDiscovery();
    }

    private final AdapterView.OnItemClickListener mDeviceClickListener = (av, v, position, id) -> {
        mBtAdapter.cancelDiscovery();

        String info = ((TextView) v).getText().toString();
        String noDevices = getString(R.string.none_paired);
        String noNewDevice = getString(R.string.none_found);

        if (!info.equals(noDevices) && !info.equals(noNewDevice)) {
            String address = info.substring(info.length() - 17);
            String name = info.substring(0, info.length() - 17).trim();

            Intent intent = new Intent();
            intent.putExtra(EXTRA_DEVICE_ADDRESS, address);
            intent.putExtra(EXTRA_DEVICE_NAME, name);
            setResult(Activity.RESULT_OK, intent);
            finish();
        }
    };

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (device != null && device.getBondState() != BluetoothDevice.BOND_BONDED) {
                    String address = device.getAddress();
                    if (!discoveredAddresses.contains(address)) {
                        discoveredAddresses.add(address);
                        String name = (device.getName() != null) ? device.getName() : "Unknown Device";
                        mNewDevicesArrayAdapter.add(name + "\n" + address);
                    }
                }
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                if (mNewDevicesArrayAdapter.getCount() == 0) {
                    mNewDevicesArrayAdapter.add(getString(R.string.none_found));
                }
            }
        }
    };
}
