package com.pyramidsoft.siqasir;

import static com.pyramidsoft.siqasir.db.DataBaseHelper.database_name;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.pyramidsoft.siqasir.databinding.ActivityLoginPageBinding;
import com.pyramidsoft.siqasir.db.DataBaseHelper;
import com.pyramidsoft.siqasir.utils.AndLog;
import com.pyramidsoft.siqasir.utils.GlobalToast;
import com.pyramidsoft.siqasir.utils.ProgressDialog;
import com.pyramidsoft.siqasir.utils.SessionManager;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

public class LoginPageActivity extends AppCompatActivity {

    private ActivityLoginPageBinding binding;

    SessionManager sessionManager;
    ProgressDialog pDialog;
    DataBaseHelper dbHelper;
    SQLiteDatabase mDatabase;

    private static final int IMPORT_DB_REQUEST_CODE = 1001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLoginPageBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Login Offline");
        }

        sessionManager = new SessionManager(LoginPageActivity.this);
        pDialog = new ProgressDialog(LoginPageActivity.this);
        dbHelper = new DataBaseHelper(this);
        mDatabase = openOrCreateDatabase(database_name, MODE_PRIVATE, null);

        // Auto-login check
        if (sessionManager.isLoggedIn()) {
            String nama = sessionManager.getNama();

            if (nama != null && !nama.isEmpty()) {
                GlobalToast.ShowToast(LoginPageActivity.this, "Hai, " + nama);
                startActivity(new Intent(LoginPageActivity.this, HomeMainActivity.class));
                finish();
            } else {
                sessionManager.logoutUser();
            }
        }

        binding.btLanjutkan.setOnClickListener(v -> {
            sessionManager.setBukaKasir("close");
            setLogin();
        });

        binding.tvBuatAkun.setOnClickListener(v -> {
            Intent intent = new Intent(this, RegistrasiToko.class);
            startActivity(intent);
        });

        binding.layoutImportDb.setOnClickListener(v -> openFilePicker());
    }

    @Override
    public boolean onOptionsItemSelected(android.view.MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void setLogin() {
        String username = binding.etUsernameAdmin.getText().toString().trim();
        String pwd = binding.etPasswordAdmin.getText().toString().trim();

        Cursor cursor = null;
        try {
            cursor = dbHelper.login(username, pwd);

            if (cursor != null && cursor.moveToFirst()) {
                String kodePegawai = cursor.getString(cursor.getColumnIndex(DataBaseHelper.kode_pegawai));
                String namaPegawai = cursor.getString(cursor.getColumnIndex(DataBaseHelper.nama_pegawai));
                String hakAkses   = cursor.getString(cursor.getColumnIndex(DataBaseHelper.hak_akses));

                if (kodePegawai == null) kodePegawai = "";
                if (namaPegawai == null) namaPegawai = "";
                if (hakAkses == null) hakAkses = "";

                sessionManager.createLoginSession(kodePegawai, namaPegawai, hakAkses);
                sessionManager.setLogin();
                sessionManager.setKodeUsers(kodePegawai);
                sessionManager.setRegistrasi("Complete");

                String idToko = dbHelper.getIdTokoByKodePegawai(kodePegawai);
                Cursor tokoCursor = dbHelper.dataToko(idToko);
                if (tokoCursor != null && tokoCursor.moveToFirst()) {
                    String emailToko  = tokoCursor.getString(tokoCursor.getColumnIndex(DataBaseHelper.tb_toko_email));
                    String namaToko   = tokoCursor.getString(tokoCursor.getColumnIndex(DataBaseHelper.tb_toko_nama_toko));

                    sessionManager.setEmailToko(emailToko);
                    sessionManager.setNamatoko(namaToko);

                    tokoCursor.close();
                }

                Cursor ownerCursor = dbHelper.getOwnerUtama();
                if (ownerCursor != null && ownerCursor.moveToFirst()) {
                    String ownerUtama = ownerCursor.getString(ownerCursor.getColumnIndex(DataBaseHelper.kode_pegawai));
                    sessionManager.setKodeOwnerutama(ownerUtama);
                    ownerCursor.close();
                }


                AndLog.ShowLog("loginSuccesfully:", kodePegawai + " : " + namaPegawai + " : " + hakAkses);
                GlobalToast.ShowToast(LoginPageActivity.this, "Login berhasil. Selamat datang!");

                Intent intent = new Intent(LoginPageActivity.this, PilihKaryawan.class);
                startActivity(intent);
                finish();

            } else {
                GlobalToast.ShowToast(LoginPageActivity.this,
                        "Username/Password salah atau database kosong.\nPastikan database sudah diimport.");
            }

        } catch (Exception e) {
            GlobalToast.ShowToast(LoginPageActivity.this,
                    "Gagal login: " + e.getMessage() + "\nPastikan database sudah diimport.");
        } finally {
            if (cursor != null) cursor.close();
        }
    }

    // ================= IMPORT DATABASE =================

    private void openFilePicker() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.setType("*/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(intent, IMPORT_DB_REQUEST_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == IMPORT_DB_REQUEST_CODE && resultCode == RESULT_OK && data != null) {
            Uri uri = data.getData();
            if (uri != null) {
                importDatabase(uri);
            }
        }
    }

    private void importDatabase(Uri uri) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(uri);

            File dbFile = getDatabasePath(database_name);
            File parent = dbFile.getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }

            OutputStream outputStream = new FileOutputStream(dbFile, false);

            byte[] buffer = new byte[1024];
            int length;
            while ((length = inputStream.read(buffer)) > 0) {
                outputStream.write(buffer, 0, length);
            }

            outputStream.flush();
            outputStream.close();
            inputStream.close();

            GlobalToast.ShowToast(this, "Database berhasil diimport!");
            AndLog.ShowLog("ImportDB", "Database path: " + dbFile.getAbsolutePath());

        } catch (Exception e) {
            e.printStackTrace();
            GlobalToast.ShowToast(this, "Gagal import database: " + e.getMessage());
        }
    }
}
