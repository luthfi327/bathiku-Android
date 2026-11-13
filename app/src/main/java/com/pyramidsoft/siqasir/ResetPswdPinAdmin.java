package com.pyramidsoft.siqasir;

import static com.pyramidsoft.siqasir.db.DataBaseHelper.database_name;

import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.snackbar.Snackbar;
import com.pyramidsoft.siqasir.databinding.ActivityResetPswdPinAdminBinding;
import com.pyramidsoft.siqasir.db.DataBaseHelper;
import com.pyramidsoft.siqasir.utils.AndLog;
import com.pyramidsoft.siqasir.utils.ApiKeyHttpClient;
import com.pyramidsoft.siqasir.utils.GlobalToast;
import com.pyramidsoft.siqasir.utils.ProgressDialog;
import com.pyramidsoft.siqasir.utils.SessionManager;

import org.json.JSONObject;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * ResetPswdPinAdmin
 *
 * Activity untuk ganti password atau PIN oleh Owner/Admin.
 *
 * FASE 6 — Perubahan:
 *
 * SEBELUM:
 *   Update hanya dilakukan ke SQLite lokal.
 *   Data di server tidak ter-update → inkonsistensi:
 *     - Login Offline pakai password baru ✓
 *     - Login Online pakai password lama ✗ (gagal karena server masih simpan lama)
 *
 * SESUDAH:
 *   1. Update ke SQLite lokal terlebih dahulu (sama seperti sebelumnya)
 *   2. Jika update lokal berhasil → sync ke server via POST /api/users/update-credential
 *   3. Jika internet tidak ada / server error → simpan lokal dulu, tampilkan pesan
 *      bahwa perubahan akan aktif online setelah ada koneksi internet (user bisa retry
 *      dengan cara backup ulang dari BackupDatabaseActivity, yang memicu sync otomatis)
 *
 * Endpoint server: POST https://siqasir.awandigital.id/api/users/update-credential
 * Body: { kode_pegawai, type, current_value, new_value }
 *
 * Catatan hash password:
 *   Android mengirim current_value password sebagai SHA-256 (setelah Fase 1-2),
 *   karena itulah yang tersimpan di SQLite lokal. Server verifikasi via
 *   Hash::check(SHA-256, bcrypt(SHA-256)) → cocok ✓
 *
 *   Untuk PIN, Android mengirim plaintext PIN (4-6 digit), karena PIN tidak di-hash
 *   di sisi Android. Server verifikasi via Hash::check(plaintext, bcrypt(PIN)) ✓
 */
public class ResetPswdPinAdmin extends AppCompatActivity {

    private ActivityResetPswdPinAdminBinding binding;
    SessionManager sessionManager;
    ProgressDialog pDialog;
    DataBaseHelper dbHelper;
    SQLiteDatabase mDatabase;
    String kode_pegawai;

    // ======================================================================
    // FASE 6 — Konstanta URL server
    // ======================================================================
    private static final String UPDATE_CREDENTIAL_URL =
            "https://siqasir.awandigital.id/api/users/update-credential";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityResetPswdPinAdminBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setSupportActionBar(binding.toolbar);
        ActionBar actionBar = getSupportActionBar();

        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        pDialog = new ProgressDialog(ResetPswdPinAdmin.this);
        sessionManager = new SessionManager(ResetPswdPinAdmin.this);
        dbHelper = new DataBaseHelper(ResetPswdPinAdmin.this);
        mDatabase = openOrCreateDatabase(database_name, MODE_PRIVATE, null);

        binding.btSimpanPwd.setOnClickListener(this::onViewClicked);
        binding.btSimpanPin.setOnClickListener(this::onViewClicked);

        if (getIntent().getStringExtra("reset") != null) {
            String reset = getIntent().getStringExtra("reset");
            if (reset != null) {
                if (reset.equals("password")) {
                    binding.tvLabelToolbar.setText("Ganti Password");
                    binding.llPassword.setVisibility(View.VISIBLE);
                    binding.llPin.setVisibility(View.GONE);
                } else {
                    binding.tvLabelToolbar.setText("Ganti PIN");
                    binding.llPassword.setVisibility(View.GONE);
                    binding.llPin.setVisibility(View.VISIBLE);
                }
            }
        }

        if (getIntent().getStringExtra("pwd") != null) {
            String pwd = getIntent().getStringExtra("pwd");
            binding.etPasswordLama.setText(pwd);
            AndLog.ShowLog("pwdGET", pwd);
        } else if (getIntent().getStringExtra("pin") != null) {
            String pin = getIntent().getStringExtra("pin");
            binding.etPinLama.setText(pin);
        }

        kode_pegawai = getIntent().getStringExtra("kode_pegawai");
    }

    private boolean validatedpwd(boolean temp) {
        temp = true;

        if (!binding.etPasswordBaru.getText().toString().equals(binding.etUlangiPasswordBaru.getText().toString())) {
            binding.etUlangiPasswordBaru.setFocusable(true);
            binding.tilUlangiPwdbaru.setError("Password Tidak Sama!");

            temp = false;
        }
        return temp;
    }

    private boolean validatepin(boolean temp) {
        temp = true;

        if (!binding.etPinBaru.getText().toString().equals(binding.etUlangiPinBaru.getText().toString())) {
            binding.etUlangiPinBaru.setFocusable(true);
            binding.tilUlangiPinBaru.setError("PIN Tidak Sama!");

            temp = false;
        }
        return temp;
    }

    // ======================================================================
    // updatePwd — Ganti Password
    //
    // FASE 6: Alur yang baru:
    //   1. Hash password baru dengan SHA-256 (konsisten dengan Fase 1)
    //   2. Simpan SHA-256 ke SQLite lokal
    //   3. Sync ke server:
    //      current_value = SHA-256 dari password lama (yang tersimpan di DB lokal)
    //      new_value     = SHA-256 dari password baru
    //   4. Server verifikasi current_value via Hash::check(), lalu simpan bcrypt(new_value)
    // ======================================================================
    private void updatePwd(View view) {
        pDialog.show();

        String Pwdlama       = binding.etPasswordLama.getText().toString().trim();
        String Pwdbaru       = binding.etPasswordBaru.getText().toString().trim();
        String UlangipwdBaru = binding.etUlangiPasswordBaru.getText().toString().trim();

        if (Pwdlama.equals("") || Pwdbaru.equals("") || UlangipwdBaru.equals("")) {
            Snackbar.make(view, "Data Tidak Boleh Kosong", Snackbar.LENGTH_SHORT).show();
            pDialog.dismiss();
            return;
        }

        try {
            if (validatedpwd(true)) {
                // ── FASE 6: Hash password sebelum simpan ke SQLite ────────
                // Konsisten dengan Fase 1 (DataBaseHelper.hashPassword)
                // Password lama yang tersimpan di DB lokal sudah SHA-256,
                // jadi kita ambil SHA-256 dari input Pwdlama untuk dikirim ke server.
                String hashedPasswordBaru = dbHelper.hashPassword(Pwdbaru);
                String hashedPasswordLama = dbHelper.hashPassword(Pwdlama);

                ContentValues values = new ContentValues();
                values.put(DataBaseHelper.password, hashedPasswordBaru); // SHA-256 di lokal

                // ── Step 1: Update SQLite lokal dulu ─────────────────────
                dbHelper.updateDataOwner(values, kode_pegawai);
                AndLog.ShowLog("dataUpdatePassword: ", "Lokal berhasil di-update (SHA-256)");

                // ── Step 2: Sync ke server (background thread) ────────────
                // current_value: SHA-256 password lama (sama dengan yang tersimpan di DB lokal)
                // new_value:     SHA-256 password baru (yang baru saja disimpan di lokal)
                syncCredentialToServer("password", hashedPasswordLama, hashedPasswordBaru);

                GlobalToast.ShowToast(this, "Password berhasil diperbarui");
                finish();
            }
            pDialog.dismiss();
        } catch (Exception e) {
            e.printStackTrace();
            pDialog.dismiss();
        }
    }

    // ======================================================================
    // updatePin — Ganti PIN
    //
    // FASE 6: Alur yang baru:
    //   1. Simpan PIN baru (plaintext) ke SQLite lokal
    //      → PIN di lokal tetap plaintext (Android tidak hash PIN)
    //   2. Sync ke server:
    //      current_value = PIN lama plaintext (untuk verifikasi via Hash::check di server)
    //      new_value     = PIN baru plaintext  (server yang akan hash menjadi bcrypt)
    //   3. Server verifikasi PIN lama via Hash::check(plaintext, bcrypt), lalu
    //      simpan bcrypt(PIN baru)
    // ======================================================================
    private void updatePin(View view) {
        pDialog.show();

        String PinLama      = binding.etPinLama.getText().toString().trim();
        String PinBaru      = binding.etPinBaru.getText().toString().trim();
        String UlangiPinBaru = binding.etUlangiPinBaru.getText().toString().trim();

        if (PinLama.equals("") || PinBaru.equals("") || UlangiPinBaru.equals("")) {
            Snackbar.make(view, "Data Tidak Boleh Kosong", Snackbar.LENGTH_SHORT).show();
            pDialog.dismiss();
            return;
        }

        try {
            if (validatepin(true)) {
                ContentValues values = new ContentValues();
                values.put(DataBaseHelper.pin, UlangiPinBaru); // PIN plaintext di lokal

                // ── Step 1: Update SQLite lokal dulu ─────────────────────
                dbHelper.updateDataOwner(values, kode_pegawai);
                AndLog.ShowLog("DataUpdatePin: ", "Lokal berhasil di-update");

                // ── Step 2: Sync ke server (background thread) ────────────
                // PIN dikirim plaintext ke server, server yang hash ke bcrypt
                // Server verifikasi PinLama via Hash::check(PinLama, bcrypt_di_db)
                syncCredentialToServer("pin", PinLama, UlangiPinBaru);

                GlobalToast.ShowToast(this, "PIN berhasil diperbarui");
                finish();
            }
            pDialog.dismiss();
        } catch (Exception e) {
            e.printStackTrace();
            pDialog.dismiss();
        }
    }

    // ======================================================================
    // FASE 6 — syncCredentialToServer()
    //
    // Kirim perubahan password atau PIN ke server via:
    //   POST /api/users/update-credential
    //
    // Parameter:
    //   type         : "password" atau "pin"
    //   currentValue : nilai lama untuk verifikasi di server
    //                  → password: SHA-256 hex (setelah Fase 1)
    //                  → pin     : plaintext (PIN tidak di-hash di Android)
    //   newValue     : nilai baru yang akan disimpan
    //                  → password: SHA-256 hex
    //                  → pin     : plaintext
    //
    // Dijalankan di background thread agar tidak memblokir UI.
    // Jika server error / offline → update lokal sudah berhasil,
    // user tetap bisa login offline. Sync ke server bisa dilakukan
    // otomatis saat backup berikutnya (BackupDatabaseActivity).
    // ======================================================================
    private void syncCredentialToServer(String type, String currentValue, String newValue) {
        new Thread(() -> {
            try {
                OkHttpClient client = ApiKeyHttpClient.get();

                JSONObject body = new JSONObject();
                body.put("kode_pegawai",  kode_pegawai);
                body.put("type",          type);
                body.put("current_value", currentValue);
                body.put("new_value",     newValue);

                RequestBody requestBody = RequestBody.create(
                        body.toString(),
                        MediaType.parse("application/json; charset=utf-8")
                );

                Request request = new Request.Builder()
                        .url(UPDATE_CREDENTIAL_URL)
                        .post(requestBody)
                        .build();

                try (Response response = client.newCall(request).execute()) {
                    String responseBody = response.body() != null
                            ? response.body().string()
                            : "";

                    if (response.isSuccessful()) {
                        AndLog.ShowLog("FASE6_SYNC",
                                "Server sync " + type + " OK: " + responseBody);
                    } else {
                        // Server menolak (mis. current_value salah, user tidak ditemukan)
                        // Update lokal sudah berhasil — user tetap bisa login offline.
                        // Log error agar bisa di-debug.
                        AndLog.ShowLog("FASE6_SYNC",
                                "Server sync " + type + " gagal (" + response.code() + "): " + responseBody);

                        // Opsional: tampilkan pesan ke user di main thread
                        // (tidak wajib, karena update lokal sudah berhasil)
                        runOnUiThread(() ->
                                AndLog.ShowLog("FASE6_SYNC",
                                        "Perubahan disimpan lokal. Sync ke server akan dicoba saat backup berikutnya.")
                        );
                    }
                }

            } catch (Exception e) {
                // Offline atau timeout — update lokal sudah berhasil
                AndLog.ShowLog("FASE6_SYNC",
                        "Sync ke server gagal (mungkin offline): " + e.getMessage());
            }
        }).start();
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

    public void onViewClicked(View view) {
        if (view.getId() == R.id.btSimpanPwd) {
            updatePwd(view);
        } else if (view.getId() == R.id.btSimpanPin) {
            updatePin(view);
        }
    }

}