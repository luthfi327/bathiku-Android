package com.pyramidsoft.siqasir.printer;

import static com.pyramidsoft.siqasir.db.DataBaseHelper.database_name;

import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import com.pyramidsoft.siqasir.R;
import com.pyramidsoft.siqasir.databinding.ActivityPengaturanDetailOutputPrinterBinding;
import com.pyramidsoft.siqasir.db.DataBaseHelper;
import com.pyramidsoft.siqasir.printer.service.BTServices;
import com.pyramidsoft.siqasir.utils.ProgressDialog;
import com.pyramidsoft.siqasir.utils.SessionManager;

public class PengaturanDetailOutputPrinter extends AppCompatActivity {

    private ActivityPengaturanDetailOutputPrinterBinding binding;
    String id, hasil;
    private static final String TAG_SUCCESS = "result";
    SQLiteDatabase mDatabase;
    SessionManager sessionManager;
    ProgressDialog pDialog;
    DataBaseHelper dbHelper;
    String nmprinter, almtprinter, stsstruk, stsdapur, banyakstruk, banyakdapur, ukuran, tipe;
    String swistur, swidapur;

    boolean mBounded;
    private BTServices mService = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityPengaturanDetailOutputPrinterBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        ActionBar actionBar = getSupportActionBar();

        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        pDialog = new ProgressDialog(PengaturanDetailOutputPrinter.this);
        sessionManager = new SessionManager(PengaturanDetailOutputPrinter.this);
        dbHelper = new DataBaseHelper(PengaturanDetailOutputPrinter.this);
        mDatabase = openOrCreateDatabase(database_name, MODE_PRIVATE, null);

        Intent intent = getIntent();
        id = intent.getStringExtra("id");
        getDetailSetting(id);
        binding.switchstruk.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if (compoundButton.isChecked()) {
                    swistur = "1";
                    binding.laystruk.setVisibility(View.VISIBLE);
                } else {
                    swistur = "0";
                    binding.laystruk.setVisibility(View.GONE);
                }

            }
        });

        if (sessionManager.getStsStruk() != null) {
            binding.laystruk.setVisibility(View.VISIBLE);
        } else {
            binding.laystruk.setVisibility(View.GONE);
        }

        binding.switchstruk.setEnabled(false);
        binding.btSimpan.setVisibility(View.VISIBLE);
    }

    private void getDetailSetting(String id) {
        pDialog.show();
        Cursor cursor = dbHelper.getDetailPrinter(id);

        if (cursor.moveToFirst()) {
            nmprinter = cursor.getString(cursor.getColumnIndexOrThrow(DataBaseHelper.nama_printer));
            almtprinter = cursor.getString(cursor.getColumnIndexOrThrow(DataBaseHelper.alamat_printer));
            stsstruk = cursor.getString(cursor.getColumnIndexOrThrow(DataBaseHelper.status_cetak_struk));
            banyakstruk = cursor.getString(cursor.getColumnIndexOrThrow(DataBaseHelper.banyak_struk));
            ukuran = cursor.getString(cursor.getColumnIndexOrThrow(DataBaseHelper.ukuran_struk));
            tipe = cursor.getString(cursor.getColumnIndexOrThrow(DataBaseHelper.tipe_printer));

            binding.namaprinter.setText(nmprinter);
            binding.alamatprinter.setText(almtprinter);
            setPrinterPaperOptions(tipe, ukuran);
            if (stsstruk.equals("1")) {
                binding.switchstruk.setChecked(true);
            } else {
                binding.switchstruk.setChecked(false);
            }
            binding.tvTotalCetakStruk.setText(banyakstruk);
        }

        pDialog.dismiss();

    }

    private void setPrinterPaperOptions(String tipePrinter, String ukuranAktif) {

        binding.rb58.setVisibility(View.GONE);
        binding.rb72.setVisibility(View.GONE);
        binding.rb80.setVisibility(View.GONE);

        /*
         * Mapping:
         * tipe 1 = printer 58mm
         * tipe 2 = printer 72mm
         * tipe 3 = printer 80mm
         */

        if ("1".equals(tipePrinter)) {

            // printer kecil hanya bisa 58
            binding.rb58.setVisibility(View.VISIBLE);

        } else if ("2".equals(tipePrinter)) {

            // printer 72 bisa 58 & 72
            binding.rb58.setVisibility(View.VISIBLE);
            binding.rb72.setVisibility(View.VISIBLE);

        } else {

            // printer besar bisa semua
            binding.rb58.setVisibility(View.VISIBLE);
            binding.rb72.setVisibility(View.VISIBLE);
            binding.rb80.setVisibility(View.VISIBLE);
        }

        if (ukuranAktif == null) {
            ukuranAktif = "58";
        }

        // set checked aktif
        switch (ukuranAktif) {
            case "58":
                binding.rb58.setChecked(true);
                break;

            case "72":
                binding.rb72.setChecked(true);
                break;

            case "80":
                binding.rb80.setChecked(true);
                break;

            default:
                binding.rb58.setChecked(true);
                break;
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        Intent mIntent = new Intent(this, BTServices.class);
        bindService(mIntent, mConnection, BIND_AUTO_CREATE);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (mBounded) {
            unbindService(mConnection);
            mBounded = false;
        }
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
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.hapus_printer, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();

        if (itemId == android.R.id.home) {
            this.finish();
            return true;
        } else if (itemId == R.id.deleteprinter) {
            new AlertDialog.Builder(PengaturanDetailOutputPrinter.this)
                    .setTitle("Anda Ingin Menghapus Printer ini?")
                    .setPositiveButton("Ya", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            if (mService != null &&
                                    mService.getState() == BTServices.STATE_CONNECTED) {

                                mService.stop();
                            }
                            dbHelper.deletePrinter(id);
                            SessionManager session =
                                    new SessionManager(PengaturanDetailOutputPrinter.this);

                            session.setALAMAT_PRINTER("");
                            Toast.makeText(
                                    PengaturanDetailOutputPrinter.this,
                                    "Printer berhasil dihapus",
                                    Toast.LENGTH_SHORT
                            ).show();
                            finish();
                        }
                    })
                    .setNegativeButton("Tidak", (dialog, which) -> dialog.cancel())
                    .show();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void onViewClicked(View view) {
        if (view.getId() == R.id.minIcon) {
            if (binding.tvTotalCetakStruk.getText().toString().equals("1")) {
                // Do nothing
            } else {
                int a = Integer.parseInt(binding.tvTotalCetakStruk.getText().toString());
                int c = a - 1;
                binding.tvTotalCetakStruk.setText(String.valueOf(c));
            }
        } else if (view.getId() == R.id.plusIcon) {
            int a = Integer.parseInt(binding.tvTotalCetakStruk.getText().toString());
            int c = a + 1;
            binding.tvTotalCetakStruk.setText(String.valueOf(c));
        } else if (view.getId() == R.id.btSimpan) {
            String ukuranDipilih = "58";

            if (binding.rb72.isChecked()) {
                ukuranDipilih = "72";
            } else if (binding.rb80.isChecked()) {
                ukuranDipilih = "80";
            }

            dbHelper.updateUkuranPrinter(id, ukuranDipilih);

            sessionManager.setUKURAN_STRUK(ukuranDipilih);

            Toast.makeText(
                    PengaturanDetailOutputPrinter.this,
                    "Ukuran printer berhasil disimpan",
                    Toast.LENGTH_SHORT
            ).show();

            finish();
        }
    }

}
