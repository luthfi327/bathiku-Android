package com.pyramidsoft.siqasir;

import static com.pyramidsoft.siqasir.db.DataBaseHelper.database_name;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Toast;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import com.chivorn.smartmaterialspinner.SmartMaterialSpinner;
import com.google.android.material.snackbar.Snackbar;
import com.pyramidsoft.siqasir.databinding.ActivityRegistrasiUserBinding;
import com.pyramidsoft.siqasir.db.DataBaseHelper;
import com.pyramidsoft.siqasir.utils.AndLog;
import com.pyramidsoft.siqasir.utils.ApiKeyHttpClient;
import com.pyramidsoft.siqasir.utils.GlobalToast;
import com.pyramidsoft.siqasir.utils.ProgressDialog;
import com.pyramidsoft.siqasir.utils.SessionManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Random;

import okhttp3.Call;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class RegistrasiUser extends AppCompatActivity {

    private ActivityRegistrasiUserBinding binding;
    ProgressDialog pDialog;
    SessionManager sessionManager;
    DataBaseHelper dbHelper;
    SQLiteDatabase mDatabase;
    private SmartMaterialSpinner<String> spJK;
    private SmartMaterialSpinner<String> spPosisi;
    private SmartMaterialSpinner<String> spHakAkses;

    private List<String> genderList;
    private List<String> posisiList;
    private List<String> hakAksesList;

    private boolean isProcessing = false;
    private android.os.Handler handler = new android.os.Handler();
    private Runnable usernameRunnable;

    private Call currentUsernameCall;

    String vJk, vPosisi, vHakAkses, UOV, statusUsers, dataUser, kode_pegawai;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRegistrasiUserBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);
        ActionBar actionBar = getSupportActionBar();

        binding.etUsername.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (usernameRunnable != null) {
                    handler.removeCallbacks(usernameRunnable);
                }
                usernameRunnable = () -> cekUsernameRealtime(s.toString());
                handler.postDelayed(usernameRunnable, 500);
            }

            @Override
            public void afterTextChanged(android.text.Editable s) {}
        });

        binding.btSimpan.setOnClickListener(view -> {
            if (isProcessing) return;
            isProcessing = true;
            binding.btSimpan.setEnabled(false);
            sessionManager.setBukaKasir("close");
            cekUsernameDulu(view);
        });

        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        pDialog = new ProgressDialog(RegistrasiUser.this);
        sessionManager = new SessionManager(RegistrasiUser.this);
        dbHelper = new DataBaseHelper(RegistrasiUser.this);
        mDatabase = openOrCreateDatabase(database_name, MODE_PRIVATE, null);

        initSpinner();
        UOV = generateUOV();

        AndLog.ShowLog("UserOutletVerification", UOV);

        binding.etKodePegawai.setText(UOV);
    }

    private boolean validated(boolean temp) {
        temp = true;

        if (!binding.etPassword.getText().toString().equals(binding.etUlangiPassword.getText().toString())) {
            binding.etUlangiPassword.setFocusable(true);
            binding.tilPwdBaru.setError("Password Tidak Sama!");
            dataUser = "0";
            temp = false;
        } else if (!binding.etPin.getText().toString().equals(binding.etUlangiPin.getText().toString())) {
            binding.etUlangiPin.setFocusable(true);
            binding.tilPinBaru.setError("PIN Tidak Sama!");
            dataUser = "0";
            temp = false;
        }

        return temp;
    }

    private void showUsernameLoading(boolean isLoading) {
        if (isLoading) {
            binding.tilUsername.setEndIconDrawable(R.drawable.loading_small);
            binding.tilUsername.setEndIconVisible(true);
        } else {
            binding.tilUsername.setEndIconVisible(false);
        }
    }

    private void initSpinner() {

        spJK = findViewById(R.id.spinJK);
        spHakAkses = findViewById(R.id.spinHakAkses);
        spPosisi = findViewById(R.id.spinPosisi);

        genderList = new ArrayList<>();
        hakAksesList = new ArrayList<>();
        posisiList = new ArrayList<>();

        genderList.add("Pria");
        genderList.add("Wanita");

        hakAksesList.add("Kasir");

        posisiList.add("Pegawai");

        spJK.setItem(genderList);
        spHakAkses.setItem(hakAksesList);
        spPosisi.setItem(posisiList);

        spJK.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int position, long id) {
                vJk = genderList.get(position);
                AndLog.ShowLog("vJk", vJk);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {}
        });

        spHakAkses.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int position, long id) {
                vHakAkses = hakAksesList.get(position);
                AndLog.ShowLog("vHA", vHakAkses);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {}
        });

        spPosisi.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int position, long id) {
                vPosisi = posisiList.get(position);
                AndLog.ShowLog("vPss", vPosisi);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {}
        });
    }

    private static String getRandomString(int i) {
        final String character = "1234567890ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        StringBuilder result = new StringBuilder();
        while (i > 0) {
            Random random = new Random();
            result.append(character.charAt(random.nextInt(character.length())));
            i--;
        }
        return result.toString();
    }

    private String generateUOV() {
        SimpleDateFormat sdf = new SimpleDateFormat("ddMMyyyy");
        String date = sdf.format(new Date());
        return "UOV-" + date + getRandomString(6);
    }

    private boolean isInternetAvailable() {
        ConnectivityManager connectivityManager =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

        if (connectivityManager == null) return false;

        Network network = null;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            network = connectivityManager.getActiveNetwork();
        }
        if (network == null) return false;

        NetworkCapabilities capabilities =
                connectivityManager.getNetworkCapabilities(network);

        return capabilities != null &&
                (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
                        || capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
                        || capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET));
    }

    private void flushWalToDb() {
        DataBaseHelper helper = new DataBaseHelper(this);
        SQLiteDatabase db = helper.getWritableDatabase();

        Cursor cursor = null;
        try {
            cursor = db.rawQuery("PRAGMA wal_checkpoint(FULL);", null);
            if (cursor.moveToFirst()) {
                int checkpointed = cursor.getInt(0);
                int logSize = cursor.getInt(1);
                int frames = cursor.getInt(2);

                Log.d("WAL", "checkpointed=" + checkpointed +
                        " logSize=" + logSize +
                        " frames=" + frames);
            }
        } finally {
            if (cursor != null) cursor.close();
            db.close();
        }
    }

    private Object mapToJson(Object data) throws JSONException {
        if (data instanceof Map) {
            JSONObject json = new JSONObject();
            Map<?, ?> map = (Map<?, ?>) data;

            for (Map.Entry<?, ?> entry : map.entrySet()) {
                json.put(
                        String.valueOf(entry.getKey()),
                        mapToJson(entry.getValue())
                );
            }
            return json;

        } else if (data instanceof List) {
            JSONArray array = new JSONArray();
            for (Object item : (List<?>) data) {
                array.put(mapToJson(item));
            }
            return array;

        } else {
            return data == null ? JSONObject.NULL : data;
        }
    }

    private void cekUsernameRealtime(String username) {

        if (username.isEmpty()) {
            runOnUiThread(() -> {
                showUsernameLoading(false);
                binding.etUsername.setError(null);
            });
            return;
        }

        if (!isInternetAvailable()) return;

        OkHttpClient client = ApiKeyHttpClient.get();

        try {
            JSONObject json = new JSONObject();
            json.put("username", username);

            RequestBody body = RequestBody.create(
                    json.toString(),
                    MediaType.parse("application/json; charset=utf-8")
            );

            Request request = new Request.Builder()
                    .url("https://siqasir.awandigital.id/api/check-username")
                    .post(body)
                    .build();

            if (currentUsernameCall != null && !currentUsernameCall.isCanceled()) {
                currentUsernameCall.cancel();
            }

            currentUsernameCall = client.newCall(request);

            runOnUiThread(() -> showUsernameLoading(true));

            new Thread(() -> {
                try (Response response = currentUsernameCall.execute()) {

                    String resBody = response.body() != null ? response.body().string() : "";

                    if (!response.isSuccessful()) {
                        runOnUiThread(() -> showUsernameLoading(false));
                        return;
                    }

                    JSONObject resJson = new JSONObject(resBody);
                    boolean isUsed = resJson.optBoolean("is_used", true);

                    runOnUiThread(() -> {
                        showUsernameLoading(false);

                        if (isUsed) {
                            binding.etUsername.setError("Username sudah digunakan");
                        } else {
                            binding.etUsername.setError(null);
                            binding.tilUsername.setEndIconDrawable(R.drawable.ic_24_px_check);
                            binding.tilUsername.setEndIconVisible(true);
                        }
                    });

                } catch (Exception e) {
                    runOnUiThread(() -> showUsernameLoading(false));
                }
            }).start();

        } catch (Exception e) {
            runOnUiThread(() -> showUsernameLoading(false));
        }
    }

    private void cekUsernameDulu(View view) {

        String NamaPegawai = binding.etnamaPegawai.getText().toString().trim();
        String NoHP = binding.etNoHp.getText().toString().trim();
        String Username = binding.etUsername.getText().toString().trim();
        String Pwd = binding.etPassword.getText().toString().trim();
        String Pin = binding.etPin.getText().toString().trim();

        if (NamaPegawai.isEmpty() ||
                NoHP.isEmpty() ||
                Username.isEmpty() ||
                Pwd.isEmpty() ||
                Pin.isEmpty()) {

            isProcessing = false;
            binding.btSimpan.setEnabled(true);
            Snackbar.make(view, "Data Tidak Boleh Kosong", Snackbar.LENGTH_SHORT).show();
            return;
        }

        if (!isInternetAvailable()) {
            isProcessing = false;
            binding.btSimpan.setEnabled(true);
            Snackbar.make(view, "Butuh internet untuk cek username", Snackbar.LENGTH_SHORT).show();
            return;
        }

        pDialog.show();

        OkHttpClient client = ApiKeyHttpClient.get();

        try {
            JSONObject json = new JSONObject();
            json.put("username", Username);

            RequestBody body = RequestBody.create(
                    json.toString(),
                    MediaType.parse("application/json; charset=utf-8")
            );

            Request request = new Request.Builder()
                    .url("https://siqasir.awandigital.id/api/check-username")
                    .post(body)
                    .build();

            new Thread(() -> {
                try (Response response = client.newCall(request).execute()) {

                    String resBody = response.body() != null ? response.body().string() : "";

                    if (!response.isSuccessful()) {
                        runOnUiThread(() -> {
                            isProcessing = false;
                            binding.btSimpan.setEnabled(true);
                            pDialog.dismiss();
                            Snackbar.make(view, "Gagal cek username", Snackbar.LENGTH_LONG).show();
                        });
                        return;
                    }

                    JSONObject resJson = new JSONObject(resBody);
                    boolean isUsed = resJson.optBoolean("is_used", true);

                    runOnUiThread(() -> {
                        if (isUsed) {
                            isProcessing = false;
                            binding.btSimpan.setEnabled(true);
                            pDialog.dismiss();
                            Snackbar.make(view, "Username sudah digunakan!", Snackbar.LENGTH_LONG).show();
                        } else {
                            registrasiUsers(view);
                        }
                    });

                } catch (Exception e) {
                    runOnUiThread(() -> {
                        isProcessing = false;
                        binding.btSimpan.setEnabled(true);
                        pDialog.dismiss();
                        Snackbar.make(view, "Error: " + e.getMessage(), Snackbar.LENGTH_LONG).show();
                    });
                }
            }).start();

        } catch (Exception e) {
            isProcessing = false;
            binding.btSimpan.setEnabled(true);
            pDialog.dismiss();
            Snackbar.make(view, "Error: " + e.getMessage(), Snackbar.LENGTH_LONG).show();
        }
    }

    private void registrasiUsers(View view) {

        String NamaPegawai = binding.etnamaPegawai.getText().toString().trim();
        String NoHP = binding.etNoHp.getText().toString().trim();
        String Username = binding.etUsername.getText().toString().trim();
        String Pwd = binding.etPassword.getText().toString().trim();
        String Pin = binding.etPin.getText().toString().trim();

        try {
            if (validated(true)) {

                // ============================================================
                // FASE 1 — PERUBAHAN D: Hash password sebelum simpan ke SQLite
                //
                // SEBELUM:
                //   values.put(DataBaseHelper.password, Pwd);
                //   → password "rahasia123" tersimpan plaintext di SQLite
                //
                // SESUDAH:
                //   values.put(DataBaseHelper.password, dbHelper.hashPassword(Pwd));
                //   → password tersimpan sebagai SHA-256 hex (64 karakter)
                //   → login offline membandingkan SHA-256 vs SHA-256 ✓
                //   → server tetap menerima SHA-256 ini dan akan di-hash
                //     lagi menjadi bcrypt via hashPasswordIfNeeded() ✓
                //
                // PIN tidak di-hash di sini — akan dihandle di Fase 4-5
                // setelah migrasi data PIN di server selesai.
                // ============================================================
                String hashedPwd = dbHelper.hashPassword(Pwd);

                ContentValues values = new ContentValues();
                values.put(DataBaseHelper.nama_pegawai, NamaPegawai);
                values.put(DataBaseHelper.kode_pegawai, UOV);
                values.put(DataBaseHelper.no_hp, NoHP);
                values.put(DataBaseHelper.hak_akses, "Owner");
                values.put(DataBaseHelper.posisi, "Owner");
                values.put(DataBaseHelper.status, "Aktif");
                values.put(DataBaseHelper.username, Username);
                values.put(DataBaseHelper.password, hashedPwd); // ← SHA-256, bukan plaintext
                values.put(DataBaseHelper.pin, Pin);            // ← PIN masih plaintext (Fase 4-5)

                int idTokoUtama = dbHelper.getFirstTokoId();
                if (idTokoUtama != -1) {
                    values.put("id_toko", idTokoUtama);
                }

                dbHelper.registrasiUsers(values);

                GlobalToast.ShowToast(this, "Data Berhasil Tersimpan");

                sessionManager.setKodeOwnerutama(UOV);
                sessionManager.setKodeUsers(UOV);
                sessionManager.setNama(NamaPegawai);
                sessionManager.setHakAkses("Owner");
                sessionManager.setRegistrasi("Complete");
                sessionManager.setLogin();

                Toast.makeText(this,
                        "Data tersimpan. Mengupload ke server...",
                        Toast.LENGTH_SHORT).show();

                uploadToServer();

                pDialog.dismiss();

                Intent intent = new Intent(RegistrasiUser.this, HomeMainActivity.class);
                startActivity(intent);
                finish();

                AndLog.ShowLog("dataUsers: ", String.valueOf(values));

            } else {
                isProcessing = false;
                binding.btSimpan.setEnabled(true);
                pDialog.dismiss();
            }
        } catch (Exception e) {
            isProcessing = false;
            binding.btSimpan.setEnabled(true);
            pDialog.dismiss();
            e.printStackTrace();
        }
    }

    private void uploadToServer() {

        flushWalToDb();

        try {
            File dbFile = getDatabasePath("kasir.db");
            if (!dbFile.exists()) {
                runOnUiThread(() ->
                        Toast.makeText(this,
                                "Upload gagal: database tidak ditemukan",
                                Toast.LENGTH_LONG).show()
                );
                return;
            }

            String kodePegawai = sessionManager.getKodeUsers();
            String namaToko = sessionManager.getNamatoko();
            String kodeOwner = sessionManager.getKodeOwnerutama();

            DataBaseHelper dbHelper = new DataBaseHelper(this);
            String idTokoStr = dbHelper.getIdTokoByKodePegawai(kodePegawai);

            // FIX: konversi idToko ke int agar dikirim sebagai JSON integer, bukan string.
            // Sebelumnya: jsonPayload.put("id_toko", idToko) → "id_toko": "1" (string)
            // Server validasi 'required|integer' menolak string "1" → upload gagal 422.
            // Sesudah: jsonPayload.put("id_toko", idTokoInt) → "id_toko": 1 (integer) ✓
            int idTokoInt = 1; // default fallback
            if (idTokoStr != null && !idTokoStr.isEmpty()) {
                try {
                    idTokoInt = Integer.parseInt(idTokoStr);
                } catch (NumberFormatException e) {
                    AndLog.ShowLog("UPLOAD_DB", "idToko parse error: " + e.getMessage());
                }
            }

            // FIX: fallback nama_toko jika session belum di-set (edge case btNext skip)
            if (namaToko == null || namaToko.isEmpty()) {
                namaToko = dbHelper.getNamaTokoUtama();
            }
            if (namaToko == null || namaToko.isEmpty()) {
                namaToko = "Toko Saya"; // fallback terakhir agar upload tidak gagal
            }

            String pegawaiDataJson = dbHelper.getPegawaiDataJson(kodePegawai);

            String kodeFranchise = "FR-" + kodePegawai + "-" + kodeOwner;

            byte[] dbBytes = new byte[(int) dbFile.length()];
            try (FileInputStream fis = new FileInputStream(dbFile)) {
                fis.read(dbBytes);
            }

            String dbBase64 = Base64.encodeToString(dbBytes, Base64.NO_WRAP);

            JSONObject jsonPayload = new JSONObject();
            jsonPayload.put("kode_franchise", kodeFranchise);
            jsonPayload.put("id_toko", idTokoInt);  // ← int, bukan String
            jsonPayload.put("nama_toko", namaToko);
            jsonPayload.put("kode_owner", kodeOwner);
            jsonPayload.put("pegawai_data", new JSONObject(pegawaiDataJson));
            jsonPayload.put("file_name", kodeFranchise + ".db");
            jsonPayload.put("file_data", dbBase64);

            OkHttpClient client = ApiKeyHttpClient.get();

            RequestBody body = RequestBody.create(
                    jsonPayload.toString(),
                    MediaType.parse("application/json; charset=utf-8")
            );

            Request request = new Request.Builder()
                    .url("https://siqasir.awandigital.id/api/upload-db")
                    .post(body)
                    .build();

            new Thread(() -> {
                try (Response response = client.newCall(request).execute()) {

                    if (!response.isSuccessful()) {
                        runOnUiThread(() ->
                                Toast.makeText(this,
                                        "Upload gagal ke server",
                                        Toast.LENGTH_LONG).show()
                        );
                        return;
                    }

                    doMergeAndSyncSilent(kodeOwner);

                } catch (Exception e) {
                    runOnUiThread(() ->
                            Toast.makeText(this,
                                    "Upload error: " + e.getMessage(),
                                    Toast.LENGTH_LONG).show()
                    );
                }
            }).start();

        } catch (Exception e) {
            Toast.makeText(this,
                    "Upload error: " + e.getMessage(),
                    Toast.LENGTH_LONG).show();
        }
    }

    private void doMergeAndSyncSilent(String kodeOwner) {

        new Thread(() -> {
            try {
                OkHttpClient client = ApiKeyHttpClient.get();

                Response mergeResponse = client.newCall(
                        new Request.Builder()
                                .url("https://siqasir.awandigital.id/api/merge/" + kodeOwner)
                                .get()
                                .build()
                ).execute();

                if (!mergeResponse.isSuccessful()) {
                    runOnUiThread(() ->
                            Toast.makeText(this,
                                    "Merge gagal",
                                    Toast.LENGTH_LONG).show()
                    );
                    return;
                }

                DataBaseHelper dbHelper = new DataBaseHelper(this);
                Map<String, Object> syncData = dbHelper.getSyncData(kodeOwner);
                JSONObject syncJson = (JSONObject) mapToJson(syncData);

                RequestBody syncBody = RequestBody.create(
                        syncJson.toString(),
                        MediaType.parse("application/json; charset=utf-8")
                );

                Request syncRequest = new Request.Builder()
                        .url("https://siqasir.awandigital.id/api/sync/data-compare")
                        .post(syncBody)
                        .build();

                Response syncResponse = client.newCall(syncRequest).execute();

                if (!syncResponse.isSuccessful()) {
                    runOnUiThread(() ->
                            Toast.makeText(this,
                                    "Sync gagal",
                                    Toast.LENGTH_LONG).show()
                    );
                }

            } catch (Exception e) {
                runOnUiThread(() ->
                        Toast.makeText(this,
                                "Sync error: " + e.getMessage(),
                                Toast.LENGTH_LONG).show()
                );
            }
        }).start();
    }
}