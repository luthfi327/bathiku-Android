package com.pyramidsoft.siqasir;

import static com.pyramidsoft.siqasir.db.DataBaseHelper.database_name;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import com.pyramidsoft.siqasir.databinding.ActivityDetailPegawaiBinding;
import com.pyramidsoft.siqasir.db.DataBaseHelper;
import com.pyramidsoft.siqasir.utils.ProgressDialog;
import com.pyramidsoft.siqasir.utils.SessionManager;

public class DetailPegawaiActivity extends AppCompatActivity {

    private ActivityDetailPegawaiBinding binding;
    ProgressDialog pDialog;
    SessionManager sessionManager;
    DataBaseHelper dbHelper;
    SQLiteDatabase mDatabase;

    private String id_toko;

    String vJk, vPosisi, vHakAkses, UOV, statusUsers, datauser, kode_pegawai;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityDetailPegawaiBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setSupportActionBar(binding.toolbar);
        ActionBar actionBar = getSupportActionBar();

        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        pDialog = new ProgressDialog(DetailPegawaiActivity.this);
        sessionManager = new SessionManager(DetailPegawaiActivity.this);
        dbHelper = new DataBaseHelper(DetailPegawaiActivity.this);
        mDatabase = openOrCreateDatabase(database_name, MODE_PRIVATE, null);

        setDefaultActivity(false);

        kode_pegawai = getIntent().getStringExtra("kode_pegawai");
        if (kode_pegawai != null) {
            getDetailPegawai(kode_pegawai);
        }
    }

    private void setDefaultActivity(boolean status) {
        binding.etnamaPegawai.setEnabled(status);
        binding.spinJK.setEnabled(status);
        binding.etNoHp.setEnabled(status);
        binding.etUsername.setEnabled(status);
        binding.etPassword.setEnabled(status);
        binding.etPin.setEnabled(status);
        binding.etHakAkses.setEnabled(status);
        binding.etPosisi.setEnabled(status);
        binding.switchStatus.setClickable(status);
        binding.switchStatus.setChecked(status);
    }

    private void getDetailPegawai(String kode_pegawai) {
        pDialog.show();
        Cursor cursor = dbHelper.getDetailPegawai(kode_pegawai);
        if (cursor.moveToFirst()) {
            String namaPegawai = cursor.getString(cursor.getColumnIndexOrThrow(DataBaseHelper.nama_pegawai));
            String gender = cursor.getString(cursor.getColumnIndexOrThrow(DataBaseHelper.gender));
            String noHp = cursor.getString(cursor.getColumnIndexOrThrow(DataBaseHelper.no_hp));
            String hakAkses = cursor.getString(cursor.getColumnIndexOrThrow(DataBaseHelper.hak_akses));
            String posisi = cursor.getString(cursor.getColumnIndexOrThrow(DataBaseHelper.posisi));
            String status = cursor.getString(cursor.getColumnIndexOrThrow(DataBaseHelper.status));
            String username = cursor.getString(cursor.getColumnIndexOrThrow(DataBaseHelper.username));
            String password = cursor.getString(cursor.getColumnIndexOrThrow(DataBaseHelper.password));
            String pin = cursor.getString(cursor.getColumnIndexOrThrow(DataBaseHelper.pin));
            id_toko = cursor.getString(cursor.getColumnIndexOrThrow(DataBaseHelper.id_toko));

            binding.etKodePegawai.setText(kode_pegawai);
            binding.etnamaPegawai.setText(namaPegawai);
            binding.tvJk.setText(gender);
            binding.etNoHp.setText(noHp);
            binding.etHakAkses.setText(hakAkses);
            binding.etPosisi.setText(posisi);
            if (status.equals("Aktif")) {
                binding.switchStatus.setChecked(true);
            } else {
                binding.switchStatus.setChecked(false);
            }
            binding.etUsername.setText(username);
            binding.etPassword.setText(password);
            binding.etPin.setText(pin);

            Cursor tokoCursor = dbHelper.getTokoById(id_toko);
            if (tokoCursor.moveToFirst()) {
                String namaToko = tokoCursor.getString(tokoCursor.getColumnIndexOrThrow("nama_toko"));
                binding.etNamaOutlet.setText(namaToko);
            }
            tokoCursor.close();

            pDialog.dismiss();
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

}
