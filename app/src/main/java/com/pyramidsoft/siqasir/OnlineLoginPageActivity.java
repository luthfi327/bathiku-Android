package com.pyramidsoft.siqasir;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.pyramidsoft.siqasir.databinding.ActivityOnlineLoginPageBinding;
import com.pyramidsoft.siqasir.db.DataBaseHelper;
import com.pyramidsoft.siqasir.utils.AndLog;
import com.pyramidsoft.siqasir.utils.GlobalToast;
import com.pyramidsoft.siqasir.utils.SessionManager;

import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class OnlineLoginPageActivity extends AppCompatActivity {

    private ActivityOnlineLoginPageBinding binding;
    private SessionManager sessionManager;
    private DataBaseHelper dbHelper;
    private SQLiteDatabase mDatabase;

    private static final String BASE_URL              = "https://siqasir.awandigital.id/api";
    private static final String LOGIN_ENDPOINT        = BASE_URL + "/download-db-login";
    private static final String DB_NAME               = "kasir.db";
    private static final String RESET_PASSWORD_ENDPOINT = BASE_URL + "/otp/getusers";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityOnlineLoginPageBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Login Online");
        }

        sessionManager = new SessionManager(this);
        dbHelper       = new DataBaseHelper(this);
        mDatabase      = openOrCreateDatabase(DB_NAME, MODE_PRIVATE, null);

        binding.btLanjutkan.setOnClickListener(v -> {
            String username = binding.etUsernameAdmin.getText().toString().trim();
            String password = binding.etPasswordAdmin.getText().toString().trim();
            String pin      = binding.etPin.getText().toString().trim();

            if (username.isEmpty() || password.isEmpty() || pin.isEmpty()) {
                Toast.makeText(this, "Semua kolom wajib diisi!", Toast.LENGTH_SHORT).show();
                return;
            }

            sessionManager.setBukaKasir("close");
            downloadDatabaseThenLogin(username, password, pin);
        });

        binding.tvLupaSandi.setOnClickListener(v -> {
            if (binding.layoutForgotPassword.getVisibility() == View.GONE) {
                binding.layoutForgotPassword.setVisibility(View.VISIBLE);
            } else {
                binding.layoutForgotPassword.setVisibility(View.GONE);
            }
        });

        binding.btKirimReset.setOnClickListener(v -> {
            String email = binding.etEmailReset.getText().toString().trim();

            if (email.isEmpty()) {
                Toast.makeText(this, "Email wajib diisi!", Toast.LENGTH_SHORT).show();
                return;
            }

            kirimResetPassword(email);
        });
    }

    @Override
    public boolean onOptionsItemSelected(android.view.MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void kirimResetPassword(String email) {
        ExecutorService executor  = Executors.newSingleThreadExecutor();
        Handler        mainHandler = new Handler(Looper.getMainLooper());

        Toast.makeText(this, "Mengirim permintaan reset...", Toast.LENGTH_SHORT).show();

        executor.execute(() -> {
            boolean success  = false;
            String  message  = "";
            HttpURLConnection connection = null;

            try {
                URL url = new URL(RESET_PASSWORD_ENDPOINT);

                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                connection.setRequestProperty("X-Android-Key", BuildConfig.ANDROID_API_KEY);
                connection.setDoOutput(true);
                connection.setConnectTimeout(15000);
                connection.setReadTimeout(15000);

                JSONObject body = new JSONObject();
                body.put("email", email);

                try (OutputStreamWriter writer =
                             new OutputStreamWriter(connection.getOutputStream(), "UTF-8")) {
                    writer.write(body.toString());
                    writer.flush();
                }

                int code = connection.getResponseCode();

                InputStream stream;
                if (code >= 200 && code < 300) {
                    stream  = connection.getInputStream();
                    success = true;
                } else {
                    stream = connection.getErrorStream();
                }

                StringBuilder responseBuilder = new StringBuilder();
                if (stream != null) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
                    String line;
                    while ((line = reader.readLine()) != null) {
                        responseBuilder.append(line);
                    }
                    reader.close();
                }

                String response = responseBuilder.toString();
                Log.d("RESET_PASSWORD", "Response: " + response);

                try {
                    JSONObject json = new JSONObject(response);
                    message = json.optString("message",
                            success ? "Berhasil mengirim email" : "Terjadi kesalahan");
                } catch (Exception e) {
                    message = success ? "Berhasil mengirim email" : "Server error (" + code + ")";
                }

            } catch (Exception e) {
                message = e.getMessage();
                Log.e("ResetPassword", "Exception:", e);
            } finally {
                if (connection != null) connection.disconnect();
            }

            boolean finalSuccess = success;
            String  finalMessage = message;

            mainHandler.post(() -> {
                Toast.makeText(this, finalMessage, Toast.LENGTH_LONG).show();
                if (finalSuccess) {
                    binding.layoutForgotPassword.setVisibility(View.GONE);
                }
            });
        });
    }

    private void downloadDatabaseThenLogin(String username, String password, String pin) {
        ExecutorService executor   = Executors.newSingleThreadExecutor();
        Handler         mainHandler = new Handler(Looper.getMainLooper());

        Toast.makeText(this, "Mulai mengunduh database...", Toast.LENGTH_SHORT).show();

        executor.execute(() -> {
            boolean success      = false;
            String  errorMessage = "";

            HttpURLConnection connection = null;
            try {
                URL url = new URL(LOGIN_ENDPOINT);

                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                connection.setRequestProperty("Accept", "application/octet-stream");
                connection.setRequestProperty("X-Android-Key", BuildConfig.ANDROID_API_KEY);
                connection.setDoOutput(true);
                connection.setConnectTimeout(15000);
                connection.setReadTimeout(15000);

                JSONObject body = new JSONObject();
                body.put("username", username);
                body.put("password", password);
                body.put("pin", pin);

                try (OutputStreamWriter writer =
                             new OutputStreamWriter(connection.getOutputStream(), "UTF-8")) {
                    writer.write(body.toString());
                    writer.flush();
                }

                int code = connection.getResponseCode();
                Log.d("OnlineLogin", "Response code: " + code);

                if (code == HttpURLConnection.HTTP_OK) {
                    File dbFile = getDatabasePath(DB_NAME);
                    File parent = dbFile.getParentFile();
                    if (parent != null && !parent.exists()) parent.mkdirs();

                    try (InputStream    input  = new BufferedInputStream(connection.getInputStream());
                         FileOutputStream output = new FileOutputStream(dbFile)) {
                        byte[] buffer = new byte[8192];
                        int    bytesRead;
                        while ((bytesRead = input.read(buffer)) != -1) {
                            output.write(buffer, 0, bytesRead);
                        }
                    }

                    if (!dbFile.exists() || dbFile.length() < 1024) {
                        errorMessage = "File database tidak valid atau kosong.";
                    } else {
                        success = true;
                    }
                } else {
                    InputStream errStream = connection.getErrorStream();
                    if (errStream != null) {
                        BufferedReader reader = new BufferedReader(new InputStreamReader(errStream));
                        StringBuilder  sb     = new StringBuilder();
                        String line;
                        while ((line = reader.readLine()) != null) sb.append(line);
                        reader.close();
                        Log.e("OnlineLogin", "Error body: " + sb.toString());
                    }
                    errorMessage = "Login gagal (kode: " + code + ")";
                }

            } catch (Exception e) {
                errorMessage = e.getMessage();
                Log.e("OnlineLogin", "Exception: ", e);
            } finally {
                if (connection != null) connection.disconnect();
            }

            boolean finalSuccess      = success;
            String  finalErrorMessage = errorMessage;

            mainHandler.post(() -> {
                if (finalSuccess) {
                    Toast.makeText(this, "Database berhasil diperbarui.", Toast.LENGTH_SHORT).show();
                    setLogin();
                } else {
                    Toast.makeText(this,
                            "Gagal login/download: " + finalErrorMessage,
                            Toast.LENGTH_LONG).show();
                }
            });
        });
    }

    private void setLogin() {
        String username = binding.etUsernameAdmin.getText().toString().trim();
        String pwd      = binding.etPasswordAdmin.getText().toString().trim();

        Cursor cursor = null;
        try {
            SQLiteDatabase localDb = getDatabasePath(DB_NAME).exists()
                    ? SQLiteDatabase.openDatabase(getDatabasePath(DB_NAME).getPath(), null, SQLiteDatabase.OPEN_READONLY)
                    : null;

            if (localDb == null) {
                GlobalToast.ShowToast(this, "Database tidak ditemukan. Coba login ulang.");
                return;
            }

            cursor = localDb.rawQuery(
                    "SELECT * FROM tb_pegawai WHERE username = ? LIMIT 1",
                    new String[]{username}
            );

            if (cursor != null && cursor.moveToFirst()) {
                @SuppressLint("Range")
                String storedPassword = cursor.getString(cursor.getColumnIndexOrThrow("password"));
                Log.d("DEBUG_LOGIN", "Berhasil Login Online. Data lokal di DB: [" + storedPassword + "]");
                Log.d("DEBUG_LOGIN", "Username from cursor: [" + cursor.getString(cursor.getColumnIndexOrThrow("username")) + "]");

                // [BYPASS] Karena server sudah memverifikasi password saat proses download-db-login,
                // kita tidak perlu melakukan pengecekan ulang di SQLite lokal yang datanya mungkin tidak sinkron.
                // Kita langsung ambil data profil untuk membuat session.

                String kodePegawai = cursor.getString(cursor.getColumnIndexOrThrow(DataBaseHelper.kode_pegawai));
                String namaPegawai = cursor.getString(cursor.getColumnIndexOrThrow(DataBaseHelper.nama_pegawai));
                String hakAkses    = cursor.getString(cursor.getColumnIndexOrThrow(DataBaseHelper.hak_akses));

                if (kodePegawai == null) kodePegawai = "";
                if (namaPegawai == null) namaPegawai = "";
                if (hakAkses    == null) hakAkses    = "";

                sessionManager.createLoginSession(kodePegawai, namaPegawai, hakAkses);
                sessionManager.setLogin();
                sessionManager.setKodeUsers(kodePegawai);
                sessionManager.setRegistrasi("Complete");

                String idToko     = dbHelper.getIdTokoByKodePegawai(kodePegawai);
                Cursor tokoCursor = dbHelper.dataToko(idToko);
                if (tokoCursor != null && tokoCursor.moveToFirst()) {
                    String emailToko = tokoCursor.getString(
                            tokoCursor.getColumnIndexOrThrow(DataBaseHelper.tb_toko_email));
                    String namaToko  = tokoCursor.getString(
                            tokoCursor.getColumnIndexOrThrow(DataBaseHelper.tb_toko_nama_toko));
                    sessionManager.setEmailToko(emailToko);
                    sessionManager.setNamatoko(namaToko);
                    tokoCursor.close();
                }

                Cursor ownerCursor = dbHelper.getOwnerUtama();
                if (ownerCursor != null && ownerCursor.moveToFirst()) {
                    String ownerUtama = ownerCursor.getString(
                            ownerCursor.getColumnIndexOrThrow(DataBaseHelper.kode_pegawai));
                    sessionManager.setKodeOwnerutama(ownerUtama);
                    ownerCursor.close();
                }

                localDb.close();

                AndLog.ShowLog("loginSuccesfully:", kodePegawai + " : " + namaPegawai + " : " + hakAkses);
                GlobalToast.ShowToast(OnlineLoginPageActivity.this, "Login berhasil. Selamat datang!");

                Intent intent = new Intent(OnlineLoginPageActivity.this, PilihKaryawan.class);
                startActivity(intent);
                finish();

            } else {
                GlobalToast.ShowToast(this, "Username tidak ditemukan atau database kosong.");
            }

        } catch (Exception e) {
            GlobalToast.ShowToast(this, "Gagal login: " + e.getMessage());
        } finally {
            if (cursor != null) cursor.close();
        }
    }
}
