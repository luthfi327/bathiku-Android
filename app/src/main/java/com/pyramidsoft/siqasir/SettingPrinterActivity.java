package com.pyramidsoft.siqasir;

import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.pyramidsoft.siqasir.Command.PrinterCommand;
import com.pyramidsoft.siqasir.R;
import com.pyramidsoft.siqasir.databinding.ActivitySettingPrinterBinding;
import com.pyramidsoft.siqasir.db.DataBaseHelper;
import com.pyramidsoft.siqasir.printer.SearchDeviceActivity;
import com.pyramidsoft.siqasir.printer.adapter.PrintAdapter;
import com.pyramidsoft.siqasir.printer.model.PrintModel;
import com.pyramidsoft.siqasir.printer.service.BTServices;
import com.pyramidsoft.siqasir.utils.AndLog;
import com.pyramidsoft.siqasir.utils.GlobalToast;
import com.pyramidsoft.siqasir.utils.ProgressDialog;
import com.pyramidsoft.siqasir.utils.SessionManager;
import com.pyramidsoft.siqasir.databinding.ProgressnewBinding;
import com.zj.btsdk.BluetoothService;

import java.util.ArrayList;

import static com.pyramidsoft.siqasir.db.DataBaseHelper.database_name;

public class SettingPrinterActivity extends AppCompatActivity implements PrintAdapter.PrintAdapterCallback {

    private ActivitySettingPrinterBinding binding;
    private ProgressnewBinding newbinding;

    private static final String TAG = "SettingPrinterActivity";
    private static final boolean DEBUG = true;
    /******************************************************************************************************/

    /*******************************************************************************************************/
    public static final String DEVICE_NAME = "device_name";
    public static final String TOAST = "toast";

    private static final int REQUEST_CONNECT_DEVICE = 1;
    private static final int REQUEST_ENABLE_BT = 2;
    private static final int REQUEST_CHOSE_BMP = 3;
    private static final int REQUEST_CAMER = 4;
    public static final int RC_BLUETOOTH = 0;
    private static final int QR_WIDTH = 350;
    private static final int QR_HEIGHT = 350;
    /*******************************************************************************************************/
    private static final String CHINESE = "GBK";
    private static final String THAI = "CP874";
    private static final String KOREAN = "EUC-KR";
    private static final String BIG5 = "BIG5";


    /******************************************************************************************************/
    private String mConnectedDeviceName = null;
    private BluetoothAdapter mBluetoothAdapter = null;
    private BTServices mService = null;

    private BluetoothService bluetoothService = null;
    /******************************************************************************************************/
    SessionManager sessionManager;
    DataBaseHelper dbHelper;
    SQLiteDatabase mDatabase;
    ProgressDialog dialog;
    ArrayList<PrintModel> arrayList = new ArrayList<>();
    String hasil;
    String nmprinter, almtprinter, stsstruk, stsdapur, banyakstruk, banyakdapur, ukuran, tipe;
    boolean mBounded;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySettingPrinterBinding.inflate(getLayoutInflater());
        newbinding = ProgressnewBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        ActionBar actionBar = getSupportActionBar();

        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        newbinding.progressView.setVisibility(View.GONE);

        dialog = new ProgressDialog(SettingPrinterActivity.this);
        sessionManager = new SessionManager(SettingPrinterActivity.this);
        dbHelper = new DataBaseHelper(this);
        mDatabase = openOrCreateDatabase(database_name, MODE_PRIVATE, null);

        binding.listDevice.setHasFixedSize(true);

        GridLayoutManager manager2 = new GridLayoutManager(SettingPrinterActivity.this, 1, LinearLayoutManager.VERTICAL, false);
        binding.listDevice.setLayoutManager(manager2);

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        if (mBluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth is not available",
                    Toast.LENGTH_LONG).show();
        }

        binding.btScanDevice.setOnClickListener(view -> {
            Intent serverIntent = new Intent(SettingPrinterActivity.this, SearchDeviceActivity.class);
            startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE);
        });

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (DEBUG)
            AndLog.ShowLog(TAG, "onActivityResult " + resultCode);
        switch (requestCode) {
            case REQUEST_CONNECT_DEVICE: {
                if (resultCode == Activity.RESULT_OK) {
                    String address = data.getExtras().getString(
                            SearchDeviceActivity.EXTRA_DEVICE_ADDRESS);
                    String name = data.getExtras().getString(
                            SearchDeviceActivity.EXTRA_DEVICE_NAME);
                    AndLog.ShowLog("hasil", address + " : " + name);

                    GetConnectSave(name, address);
                }
                break;
            }
            case REQUEST_ENABLE_BT: {
                if (resultCode == Activity.RESULT_OK) {
//                    KeyListenerInit();
                } else {
                    Log.d(TAG, "BT not enabled");
                    Toast.makeText(this, R.string.bt_not_enabled_leaving,
                            Toast.LENGTH_SHORT).show();
                }
                break;
            }
        }
    }

    private void GetConnectSave(String nama_printer, String alamat_printer) {
        String kodePegawai = sessionManager.getKodeUsers();
        String idToko = dbHelper.getIdTokoByKodePegawai(kodePegawai);

        Cursor cursorproduct = dbHelper.checkPrinter(alamat_printer, idToko);

        ArrayList<PrintModel> device = new ArrayList<>();

        if (cursorproduct.moveToFirst()) {
            do {
                PrintModel printModel = new PrintModel();
                printModel.setId(cursorproduct.getString(0));
                printModel.setNama_printer(cursorproduct.getString(1));
                printModel.setAlamat_printer(cursorproduct.getString(2));
                device.add(printModel);


            } while (cursorproduct.moveToNext());
        }
        if (device.isEmpty()) {

            ContentValues values = new ContentValues();
            values.put(DataBaseHelper.nama_printer, nama_printer);
            values.put(DataBaseHelper.alamat_printer, alamat_printer);
            values.put(DataBaseHelper.banyak_struk, "1");
            values.put(DataBaseHelper.ukuran_struk, "58");
            values.put(DataBaseHelper.tipe_printer, "1");
            values.put(DataBaseHelper.status_cetak_struk, "1");
            dbHelper.insertPrinter(values);

            Intent intent = new Intent(SettingPrinterActivity.this, BTServices.class);
            intent.putExtra("bluetooth_device", alamat_printer);
            startService(intent);

            sessionManager.setNAMA_PRINTER(nama_printer);
            sessionManager.setALAMAT_PRINTER(alamat_printer);
            sessionManager.setTOTAL_STRUK("1");
            sessionManager.setSTS_STRUK("1");
            sessionManager.setUKURAN_STRUK("58");
            sessionManager.setTIPE_PRINTER("bt");
            sessionManager.setPRINTER("1");
            setListView();

        } else {
//            setListView();
            GlobalToast.ShowToast(SettingPrinterActivity.this, "Printer Sudah Ada");
        }
        cursorproduct.close();

    }

    private void SendDataByte(byte[] data) {

        if (mService.getState() != BluetoothService.STATE_CONNECTED) {
            GlobalToast.ShowToast(this, "Tidak Terhubung");
            return;
        }
        mService.write(data);
    }

    /****************************************************************************************************/
    private void Print_Test() {
        String msg = "Selamat\n\n";
        String data = "Anda berhasil terhubung dengan printer\n";
        SendDataByte(PrinterCommand.POS_Print_Text(msg, CHINESE, 0, 1, 1, 0));
        SendDataByte(PrinterCommand.POS_Print_Text(data, CHINESE, 0, 0, 0, 0));
        SendDataByte(PrinterCommand.POS_Set_Cut(1));
        SendDataByte(PrinterCommand.POS_Set_PrtInit());
    }

    public void setListView() {
        newbinding.progressView.setVisibility(View.VISIBLE);
        String kodePegawai = sessionManager.getKodeUsers();
        String idToko = dbHelper.getIdTokoByKodePegawai(kodePegawai);
        Cursor cursorproduct = dbHelper.listPrinter(idToko);

//        ArrayList<PrintModel> data = new ArrayList<>();
        arrayList.clear();
        if (cursorproduct.moveToFirst()) {
            do {

                PrintModel printModel = new PrintModel();
                printModel.setId(cursorproduct.getString(0));
                printModel.setNama_printer(cursorproduct.getString(1));
                printModel.setAlamat_printer(cursorproduct.getString(2));
                arrayList.add(printModel);

            } while (cursorproduct.moveToNext());

        }

        if (arrayList.isEmpty()) {

            Toast.makeText(this, "Tidak ada printer ditemukan", Toast.LENGTH_SHORT).show();
            newbinding.progressView.setVisibility(View.GONE);
        }
        cursorproduct.close();

        PrintAdapter adapter = new PrintAdapter(SettingPrinterActivity.this, arrayList, SettingPrinterActivity.this, mDatabase);
        binding.listDevice.setAdapter(adapter);
        newbinding.progressView.setVisibility(View.GONE);

    }

    @Override
    public void onButtontesAdapterClicked(int position) {
        Print_Test();
    }

    @Override
    protected void onStart() {
        super.onStart();
        setListView();
        Intent mIntent = new Intent(this, BTServices.class);
        bindService(mIntent, mConnection, BIND_AUTO_CREATE);
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableIntent = new Intent(
                    BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
            // Otherwise, setup the session
        } else {
//            if (mService == null)
//                KeyListenerInit();
        }
    }

    @Override
    public synchronized void onPause() {
        super.onPause();
        if (DEBUG)
            Log.e(TAG, "- ON PAUSE -");
    }

    @Override
    public void onStop() {
        super.onStop();
        if (DEBUG)
            Log.e(TAG, "-- ON STOP --");

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unbindService(mConnection);
        if (DEBUG)
            Log.e(TAG, "--- ON DESTROY ---");
    }

    ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.w("SERVICE CHECK", "Service is disconnected");
            mBounded = false;
            mService = null;
        }

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.w("SERVICE CHECK", "Service is connected");
            mBounded = true;
            BTServices.LocalBinder mLocalBinder = (BTServices.LocalBinder) service;
            mService = mLocalBinder.getService();
        }
    };

    @Override
    public void Buttonsettingimg(int pos) {
        String cek;
        if (mBluetoothAdapter.isEnabled()) {
            AlertDialog.Builder alert = new AlertDialog.Builder(SettingPrinterActivity.this);
            if (mService.getState() != BTServices.STATE_CONNECTED) {
                alert.setTitle("Hubungkankan koneksi dengan Printer ini?");
                cek = "konek";
            } else {
                String alamatprinter = arrayList.get(pos).getAlamat_printer();
                if (sessionManager.getAlamatPrinter().equals(alamatprinter)) {
                    alert.setTitle("Putuskan koneksi dengan Printer ini?");
                    cek = "putus";

                } else {
                    alert.setTitle("Hubungkankan koneksi dengan Printer ini?");
                    cek = "konek";
                }
            }
            alert.setPositiveButton("Ya", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    String alamatprinter = arrayList.get(pos).getAlamat_printer();
                    if (cek.equals("putus") && mService.getState() == BTServices.STATE_CONNECTED) {
                        mService.stop();
                    } else {
                        getDetailSetting(arrayList.get(pos).getId(), alamatprinter);
                    }
                }
            });
            alert.setNegativeButton("Tidak", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.cancel();
                    sessionManager.setPRINTER("0");
                }
            });
            alert.show();
            AndLog.ShowLog("cek", cek);
        } else {
            GlobalToast.ShowToast(SettingPrinterActivity.this, "Hidupkan Bluetooth");
        }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                this.finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void getDetailSetting(String id, String alamat_printer) {
        newbinding.progressView.setVisibility(View.VISIBLE);
        Cursor cursor = dbHelper.getDetailPrinter(id);

        if (cursor.moveToFirst()) {
            nmprinter = cursor.getString(cursor.getColumnIndex(DataBaseHelper.nama_printer));
            almtprinter = cursor.getString(cursor.getColumnIndex(DataBaseHelper.alamat_printer));
            stsstruk = cursor.getString(cursor.getColumnIndex(DataBaseHelper.status_cetak_struk));
            banyakstruk = cursor.getString(cursor.getColumnIndex(DataBaseHelper.banyak_struk));
            ukuran = cursor.getString(cursor.getColumnIndex(DataBaseHelper.ukuran_struk));
            tipe = cursor.getString(cursor.getColumnIndex(DataBaseHelper.tipe_printer));

//            AndLog.ShowLog("alamat_printer", alamat_printer);

            sessionManager.setNAMA_PRINTER(nmprinter);
            sessionManager.setALAMAT_PRINTER(almtprinter);
            sessionManager.setTOTAL_STRUK(banyakstruk);
            sessionManager.setSTS_STRUK(stsstruk);
            sessionManager.setUKURAN_STRUK(ukuran);
            sessionManager.setTIPE_PRINTER(tipe);

            Intent intent = new Intent(SettingPrinterActivity.this, BTServices.class);
            intent.putExtra("bluetooth_device", alamat_printer);
            startService(intent);
        }

        newbinding.progressView.setVisibility(View.GONE);
//        cursor.close();

    }

    private void requestBluetooth() {
        if (bluetoothService != null) {
            if (!bluetoothService.isBTopen()) {
                Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(intent, REQUEST_ENABLE_BT);
            }
        }
    }
}