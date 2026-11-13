package com.pyramidsoft.siqasir;

import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;

import androidx.appcompat.app.AppCompatActivity;

import com.chivorn.smartmaterialspinner.SmartMaterialSpinner;
import com.pyramidsoft.siqasir.databinding.ActivityPilihKaryawanBinding;
import com.pyramidsoft.siqasir.db.DataBaseHelper;
import com.pyramidsoft.siqasir.model.KategoriModels;
import com.pyramidsoft.siqasir.utils.AndLog;
import com.pyramidsoft.siqasir.utils.GlobalToast;
import com.pyramidsoft.siqasir.utils.ProgressDialog;
import com.pyramidsoft.siqasir.utils.SessionManager;

import java.util.ArrayList;

public class PilihKaryawan extends AppCompatActivity {

    private ActivityPilihKaryawanBinding binding;
    private SmartMaterialSpinner<KategoriModels> spKaryawan;
    ProgressDialog pDialog;
    SessionManager sessionManager;
    DataBaseHelper dbHelper;

    // FIX: inisialisasi vKaryawan dengan string kosong agar tidak null
    String vKaryawan = "";
    String pinText = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityPilihKaryawanBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // ActionBar tidak dipakai, dihapus agar bersih
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        pDialog     = new ProgressDialog(PilihKaryawan.this);
        sessionManager = new SessionManager(PilihKaryawan.this);
        dbHelper    = new DataBaseHelper(PilihKaryawan.this);

        getDataToko();
        initSpinview();

        // Cek sesi aktif — redirect ke Home jika sudah login sebelumnya
        if (sessionManager.isLoggedIn()) {
            GlobalToast.ShowToast(this, "Selamat Datang Kembali, " + sessionManager.getNama());
            startActivity(new Intent(PilihKaryawan.this, HomeMainActivity.class));
            finish();
            return;
        }

        binding.btKonfirmasi.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                validated();
            }
        });
    }

    // ---------------------------------------------------------------
    // Ambil data toko untuk ditampilkan di header layar
    // ---------------------------------------------------------------
    private void getDataToko() {
        String kodePegawai = sessionManager.getKodeUsers();

        if (kodePegawai == null || kodePegawai.isEmpty()) {
            GlobalToast.ShowToast(this, "Kode pegawai belum tersedia. Silakan login ulang.");
            return;
        }

        String idToko = dbHelper.getIdTokoByKodePegawai(kodePegawai);

        if (idToko == null || idToko.isEmpty()) {
            GlobalToast.ShowToast(this, "Toko tidak ditemukan untuk pegawai ini.");
            return;
        }

        Cursor cursor = dbHelper.dataToko(idToko);

        if (cursor != null && cursor.moveToFirst()) {
            String namaToko = cursor.getString(cursor.getColumnIndexOrThrow(DataBaseHelper.tb_toko_nama_toko));
            String email    = cursor.getString(cursor.getColumnIndexOrThrow(DataBaseHelper.tb_toko_email));

            byte[] logoBytes = cursor.getBlob(cursor.getColumnIndexOrThrow(DataBaseHelper.tb_toko_logo));
            if (logoBytes != null) {
                Bitmap bitmap = BitmapFactory.decodeByteArray(logoBytes, 0, logoBytes.length);
                binding.IvLogo.setImageBitmap(bitmap);
            }

            binding.tvNamaOutlet.setText(namaToko);
            binding.tvEmail.setText(email);
        }

        if (cursor != null) cursor.close();
    }

    // ---------------------------------------------------------------
    // Inisialisasi spinner daftar karyawan
    // ---------------------------------------------------------------
    private void initSpinview() {
        spKaryawan = findViewById(R.id.spinPilihKaryawan);

        ArrayList<String> listKaryawan = dbHelper.spinPegawai();

        ArrayAdapter<String> dataAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                listKaryawan
        );
        dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spKaryawan.setAdapter(dataAdapter);

        AndLog.ShowLog("listKaryawan", listKaryawan.toString());

        spKaryawan.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int position, long id) {
                vKaryawan = adapterView.getItemAtPosition(position).toString();
                AndLog.ShowLog("vKaryawan", vKaryawan);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // FIX: reset ke string kosong jika tidak ada yang dipilih
                vKaryawan = "";
            }
        });
    }

    // ---------------------------------------------------------------
    // Validasi input sebelum login — INI TEMPAT BUG DIPERBAIKI
    // ---------------------------------------------------------------
    private void validated() {

        // FIX 1: Cek karyawan sudah dipilih sebelum menyentuh database
        if (vKaryawan == null || vKaryawan.trim().isEmpty()) {
            GlobalToast.ShowToast(this, "Silakan pilih karyawan terlebih dahulu.");
            return;
        }

        // FIX 2: Cek PIN tidak kosong sebelum menyentuh database
        pinText = binding.etPin.getText().toString().trim();
        if (pinText.isEmpty()) {
            GlobalToast.ShowToast(this, "Silakan masukkan PIN.");
            return;
        }

        // Baru panggil database setelah kedua input dipastikan tidak kosong
        Cursor cursor = dbHelper.loginPegawai(vKaryawan, pinText);

        if (cursor == null || !cursor.moveToFirst()) {
            GlobalToast.ShowToast(this, "Pegawai tidak ditemukan.");
            if (cursor != null) cursor.close();
            return;
        }

        String pinDB = cursor.getString(cursor.getColumnIndexOrThrow(DataBaseHelper.pin));

        // FIX 3: Pastikan pinDB dari DB juga tidak null sebelum dibandingkan
        if (pinDB == null || !pinText.equals(pinDB)) {
            GlobalToast.ShowToast(this, "PIN Salah! Ulangi lagi.");
            cursor.close();
            return;
        }

        String kodePegawai = cursor.getString(cursor.getColumnIndexOrThrow(DataBaseHelper.kode_pegawai));
        String namaPegawai = cursor.getString(cursor.getColumnIndexOrThrow(DataBaseHelper.nama_pegawai));
        String hakAkses    = cursor.getString(cursor.getColumnIndexOrThrow(DataBaseHelper.hak_akses));
        cursor.close();

        // Simpan sesi login
        sessionManager.createLoginSession(kodePegawai, namaPegawai, hakAkses);

        // Ambil data toko milik karyawan yang berhasil login
        String idToko = dbHelper.getIdTokoByKodePegawai(kodePegawai);

        if (idToko == null || idToko.isEmpty()) {
            GlobalToast.ShowToast(this, "Toko tidak ditemukan untuk pegawai ini.");
            return;
        }

        Cursor tokoCursor = dbHelper.getTokoById(idToko);

        if (tokoCursor != null && tokoCursor.moveToFirst()) {
            String namaToko  = tokoCursor.getString(tokoCursor.getColumnIndexOrThrow(DataBaseHelper.tb_toko_nama_toko));
            String emailToko = tokoCursor.getString(tokoCursor.getColumnIndexOrThrow(DataBaseHelper.tb_toko_email));
            sessionManager.setNamatoko(namaToko);
            sessionManager.setEmailToko(emailToko);
        }

        if (tokoCursor != null) tokoCursor.close();

        GlobalToast.ShowToast(this, "Login Berhasil!");
        startActivity(new Intent(this, HomeMainActivity.class));
        finish();
    }
}