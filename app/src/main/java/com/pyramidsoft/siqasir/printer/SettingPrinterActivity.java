package com.pyramidsoft.siqasir.printer;

import static com.pyramidsoft.siqasir.db.DataBaseHelper.database_name;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.pyramidsoft.siqasir.Command.PrinterCommand;
import com.pyramidsoft.siqasir.databinding.ActivitySettingPrinterBinding;
import com.pyramidsoft.siqasir.databinding.ProgressnewBinding;
import com.pyramidsoft.siqasir.db.DataBaseHelper;
import com.pyramidsoft.siqasir.printer.adapter.PrintAdapter;
import com.pyramidsoft.siqasir.printer.model.PrintModel;
import com.pyramidsoft.siqasir.utils.AndLog;
import com.pyramidsoft.siqasir.utils.GlobalToast;
import com.pyramidsoft.siqasir.utils.ProgressDialog;
import com.pyramidsoft.siqasir.utils.SessionManager;
import com.zj.btsdk.BluetoothService;

import java.util.ArrayList;

public class SettingPrinterActivity extends AppCompatActivity implements PrintAdapter.PrintAdapterCallback {

    private static final String TAG = "SettingPrinterActivity";
    private static final boolean DEBUG = true;

    private static final int REQUEST_CONNECT_DEVICE = 1;
    private static final int REQUEST_ENABLE_BT = 2;
    private static final int REQUEST_BT_PERMISSIONS = 2001;

    private static final String CHARSET_CHINESE = "GBK";

    private ActivitySettingPrinterBinding binding;
    private ProgressnewBinding newbinding;

    private BluetoothAdapter bluetoothAdapter;
    private BluetoothService bluetoothService;

    private SessionManager sessionManager;
    private DataBaseHelper dbHelper;
    private SQLiteDatabase database;
    private ProgressDialog progressDialog;

    private ArrayList<PrintModel> printerList = new ArrayList<>();

    private String namaPrinter, alamatPrinter, statusStruk, banyakStruk, ukuranStruk, tipePrinter;
    private boolean hasShownConnectedToast = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySettingPrinterBinding.inflate(getLayoutInflater());
        newbinding = ProgressnewBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setupActionBar();
        initComponents();
        initBluetooth();
        setupRecyclerView();
        setupListeners();
    }

    private void setupActionBar() {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) actionBar.setDisplayHomeAsUpEnabled(true);
    }

    private void initComponents() {
        progressDialog = new ProgressDialog(this);
        sessionManager = new SessionManager(this);
        dbHelper = new DataBaseHelper(this);
        database = openOrCreateDatabase(database_name, MODE_PRIVATE, null);
        newbinding.progressView.setVisibility(View.GONE);
    }

    private void initBluetooth() {
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth tidak tersedia", Toast.LENGTH_LONG).show();
            return;
        }

        if (bluetoothService == null) {
            bluetoothService = new BluetoothService(this, new Handler(msg -> {

                switch (msg.what) {

                    case BluetoothService.MESSAGE_STATE_CHANGE:

                        switch (msg.arg1) {

                            case BluetoothService.STATE_CONNECTED:

                                if (!hasShownConnectedToast) {
                                    GlobalToast.ShowToast(this, "Terhubung ke printer");
                                    hasShownConnectedToast = true;
                                }

                                sessionManager.setPRINTER("1");

                                runOnUiThread(() -> {
                                    if (binding.listDevice.getAdapter() != null) {
                                        binding.listDevice.getAdapter().notifyDataSetChanged();
                                    }
                                });

                                break;

                            case BluetoothService.STATE_CONNECTING:
                                GlobalToast.ShowToast(this, "Menyambungkan...");
                                break;

                            case BluetoothService.STATE_NONE:
                                hasShownConnectedToast = false;

                                runOnUiThread(() -> {
                                    if (binding.listDevice.getAdapter() != null) {
                                        binding.listDevice.getAdapter().notifyDataSetChanged();
                                    }
                                });

                                break;
                        }
                        break;
                }

                return true;
            }));
        }
    }

    private void setupRecyclerView() {
        binding.listDevice.setHasFixedSize(true);
        GridLayoutManager layoutManager = new GridLayoutManager(this, 1, LinearLayoutManager.VERTICAL, false);
        binding.listDevice.setLayoutManager(layoutManager);
    }

    private void setupListeners() {
        binding.btScanDevice.setOnClickListener(v -> {
            if (!hasBluetoothPermissions()) {
                requestBluetoothPermissions();
                return;
            }
            Intent intent = new Intent(this, SearchDeviceActivity.class);
            startActivityForResult(intent, REQUEST_CONNECT_DEVICE);
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        loadPrinterList();

        if (!bluetoothAdapter.isEnabled()) {
            startActivityForResult(new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE), REQUEST_ENABLE_BT);
        } else {
            if (hasBluetoothPermissions()) {
                autoReconnectPrinter();
            } else {
                requestBluetoothPermissions();
            }
        }
    }

    private void autoReconnectPrinter() {

        if (bluetoothService == null) return;

        int state = bluetoothService.getState();

        if (state == BluetoothService.STATE_CONNECTED ||
                state == BluetoothService.STATE_CONNECTING) {
            Log.d(TAG, "Sudah terkoneksi / sedang connecting, tidak reconnect");
            return;
        }

        String lastAddress = sessionManager.getAlamatPrinter();

        if (lastAddress != null && !lastAddress.isEmpty()) {

            try {
                BluetoothDevice device =
                        bluetoothAdapter.getRemoteDevice(lastAddress);

                bluetoothService.connect(device);

                Log.d(TAG, "Reconnect ke printer " + lastAddress);

            } catch (Exception e) {
                Log.e(TAG, "Gagal reconnect", e);
            }
        }
    }

    private boolean hasBluetoothPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT)
                    == PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN)
                            == PackageManager.PERMISSION_GRANTED;
        }
        return true;
    }

    private void requestBluetoothPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ActivityCompat.requestPermissions(this,
                    new String[]{
                            Manifest.permission.BLUETOOTH_CONNECT,
                            Manifest.permission.BLUETOOTH_SCAN
                    }, REQUEST_BT_PERMISSIONS);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_BT_PERMISSIONS) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                GlobalToast.ShowToast(this, "Izin Bluetooth diberikan");
                autoReconnectPrinter();
            } else {
                GlobalToast.ShowToast(this, "Izin Bluetooth ditolak");
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (bluetoothService != null) {
            if (bluetoothService.getState() == BluetoothService.STATE_CONNECTED) {
                bluetoothService.stop();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (DEBUG) AndLog.ShowLog(TAG, "onActivityResult " + resultCode);

        if (requestCode == REQUEST_CONNECT_DEVICE && resultCode == Activity.RESULT_OK && data != null) {
            String address = data.getStringExtra(SearchDeviceActivity.EXTRA_DEVICE_ADDRESS);
            String name = data.getStringExtra(SearchDeviceActivity.EXTRA_DEVICE_NAME);
            AndLog.ShowLog("hasil", address + " : " + name);
            savePrinterIfNotExist(name, address);
        }
    }

    private void savePrinterIfNotExist(String nama, String alamat) {
        String kodePegawai = sessionManager.getKodeUsers();
        String idToko = dbHelper.getIdTokoByKodePegawai(kodePegawai);

        Cursor cursor = dbHelper.checkPrinter(alamat, idToko);
        boolean exists = cursor.moveToFirst();
        cursor.close();

        if (!exists) {
            ContentValues values = new ContentValues();
            values.put(DataBaseHelper.nama_printer, nama);
            values.put(DataBaseHelper.alamat_printer, alamat);
            values.put(DataBaseHelper.banyak_struk, "1");
            values.put(DataBaseHelper.ukuran_struk, "58");
            values.put(DataBaseHelper.tipe_printer, "1");
            values.put(DataBaseHelper.status_cetak_struk, "1");
            values.put("id_toko", idToko);

            dbHelper.insertPrinter(values);

            sessionManager.setNAMA_PRINTER(nama);
            sessionManager.setALAMAT_PRINTER(alamat);
            sessionManager.setTOTAL_STRUK("1");
            sessionManager.setSTS_STRUK("1");
            sessionManager.setUKURAN_STRUK("58");
            sessionManager.setTIPE_PRINTER("bt");
            sessionManager.setPRINTER("1");

            loadPrinterList();
        } else {
            GlobalToast.ShowToast(this, "Printer sudah ada untuk toko ini");
        }
    }

    private void loadPrinterList() {
        newbinding.progressView.setVisibility(View.VISIBLE);
        printerList.clear();
        String kodePegawai = sessionManager.getKodeUsers();
        String idToko = dbHelper.getIdTokoByKodePegawai(kodePegawai);

        Cursor cursor = dbHelper.listPrinter(idToko);
        if (cursor.moveToFirst()) {
            do {
                PrintModel model = new PrintModel();
                model.setId(cursor.getString(0));
                model.setNama_printer(cursor.getString(2));
                model.setAlamat_printer(cursor.getString(3));
                printerList.add(model);
            } while (cursor.moveToNext());
        }
        cursor.close();

        if (printerList.isEmpty()) {
            Toast.makeText(this, "Tidak ada printer ditemukan", Toast.LENGTH_SHORT).show();
        }

        PrintAdapter adapter = new PrintAdapter(this, printerList, this, database);
        binding.listDevice.setAdapter(adapter);
        newbinding.progressView.setVisibility(View.GONE);
    }

    private void printTest() {
        String msg = "Selamat\n\n";
        String data = "Anda berhasil terhubung dengan printer\n";
        sendData(PrinterCommand.POS_Print_Text(msg, CHARSET_CHINESE, 0, 1, 1, 0));
        sendData(PrinterCommand.POS_Print_Text(data, CHARSET_CHINESE, 0, 0, 0, 0));
        sendData(PrinterCommand.POS_Set_Cut(1));
        sendData(PrinterCommand.POS_Set_PrtInit());
    }

    private void sendData(byte[] data) {
        if (bluetoothService == null || bluetoothService.getState() != BluetoothService.STATE_CONNECTED) {
            GlobalToast.ShowToast(this, "Tidak Terhubung");
            return;
        }
        bluetoothService.write(data);
    }

    @Override
    public void onButtontesAdapterClicked(int position) {
        printTest();
    }

    @Override
    public void Buttonsettingimg(int pos) {
        if (!bluetoothAdapter.isEnabled()) {
            GlobalToast.ShowToast(this, "Hidupkan Bluetooth");
            return;
        }

        String alamat = printerList.get(pos).getAlamat_printer();
        boolean isSamePrinter = alamat.equals(sessionManager.getAlamatPrinter());
        boolean isConnected = bluetoothService != null &&
                bluetoothService.getState() == BluetoothService.STATE_CONNECTED;

        AlertDialog.Builder alert = new AlertDialog.Builder(this);
        String action;

        if (isConnected && isSamePrinter) {
            alert.setTitle("Putuskan koneksi dengan printer ini?");
            action = "putus";
        } else {
            alert.setTitle("Hubungkan koneksi dengan printer ini?");
            action = "konek";
        }

        String finalAction = action;
        alert.setPositiveButton("Ya", (dialog, which) -> {
            if ("putus".equals(finalAction) && isConnected) {
                bluetoothService.stop();
            } else {
                getPrinterDetail(printerList.get(pos).getId(), alamat);
            }
        });
        alert.setNegativeButton("Tidak", (d, w) -> sessionManager.setPRINTER("0"));
        alert.show();
    }

    private void getPrinterDetail(String id, String alamat) {
        newbinding.progressView.setVisibility(View.VISIBLE);

        Cursor cursor = dbHelper.getDetailPrinter(id);
        if (cursor.moveToFirst()) {
            namaPrinter = cursor.getString(cursor.getColumnIndex(DataBaseHelper.nama_printer));
            alamatPrinter = cursor.getString(cursor.getColumnIndex(DataBaseHelper.alamat_printer));
            tipePrinter = cursor.getString(cursor.getColumnIndex(DataBaseHelper.tipe_printer));
            banyakStruk = cursor.getString(cursor.getColumnIndex(DataBaseHelper.banyak_struk));
            ukuranStruk = cursor.getString(cursor.getColumnIndex(DataBaseHelper.ukuran_struk));
            sessionManager.setTIPE_PRINTER(tipePrinter);

            sessionManager.setNAMA_PRINTER(namaPrinter);
            sessionManager.setALAMAT_PRINTER(alamatPrinter);
            sessionManager.setTIPE_PRINTER("bt");
            sessionManager.setPRINTER("1");

            if (!hasBluetoothPermissions()) {
                requestBluetoothPermissions();
                cursor.close();
                newbinding.progressView.setVisibility(View.GONE);
                return;
            }

            BluetoothDevice device = bluetoothAdapter.getRemoteDevice(alamatPrinter);

            new Handler().postDelayed(() -> {
                bluetoothService.connect(device);
            }, 500);
        }
        cursor.close();
        newbinding.progressView.setVisibility(View.GONE);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
