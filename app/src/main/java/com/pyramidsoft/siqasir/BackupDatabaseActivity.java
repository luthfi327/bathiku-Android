package com.pyramidsoft.siqasir;

import android.Manifest;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.pyramidsoft.siqasir.db.DataBaseHelper;
import com.pyramidsoft.siqasir.utils.ApiKeyHttpClient;
import com.pyramidsoft.siqasir.utils.SessionManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class BackupDatabaseActivity extends AppCompatActivity {

    private Button btnDownload, btnUpload;
    private TextView tvStatus;
    private static final int REQUEST_WRITE_STORAGE = 2001;
    DataBaseHelper dbHelper;
    private AlertDialog loadingDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_backup_database);

        setSupportActionBar(findViewById(R.id.toolbar));

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        btnDownload = findViewById(R.id.btnDownload);
        btnUpload = findViewById(R.id.btnUpload);
        tvStatus = findViewById(R.id.tvStatus);

        btnDownload.setOnClickListener(v -> {
            showAccountReminder(() -> {
                if (checkPermission()) {
                    backupToLocal();
                } else {
                    requestPermission();
                }
            });
        });

        dbHelper = new DataBaseHelper(BackupDatabaseActivity.this);

        btnUpload.setOnClickListener(v -> {
            showAccountReminder(this::uploadToServer);
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

    private void showAccountReminder(Runnable onConfirm) {
        SessionManager sessionManager = new SessionManager(this);
        String kodeUser = sessionManager.getKodeUsers();
        String username = "-";
        String password = "-";

        if (kodeUser != null && !kodeUser.trim().isEmpty()) {
            DataBaseHelper dbHelper = new DataBaseHelper(this);
            try (Cursor cursor = dbHelper.getPegawaiByKode(kodeUser)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int idxUser = cursor.getColumnIndex("username");
                    int idxPass = cursor.getColumnIndex("password");
                    if (idxUser >= 0) username = cursor.getString(idxUser);
                    if (idxPass >= 0) password = cursor.getString(idxPass);
                }
            }
        }

        String message = "Sebelum melanjutkan, harap catat / screenshot akun Anda:\n\n" +
                "Kode User : " + kodeUser + "\n" +
                "Username  : " + username + "\n" +
                "Password  : " + password + "\n\n" +
                "Apakah Anda sudah menyimpannya?";

        new AlertDialog.Builder(this)
                .setTitle("Penting!")
                .setMessage(message)
                .setPositiveButton("Lanjutkan", (dialog, which) -> {
                    if (onConfirm != null) onConfirm.run();
                })
                .setNegativeButton("Batal", null)
                .show();
    }

    private boolean checkPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return true;
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED;
        }
        return true;
    }

    private void requestPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            backupToLocal();
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_WRITE_STORAGE);
        }
    }

    private void backupToLocal() {
        try {
            File dbFile = getDatabasePath("kasir.db");

            if (!dbFile.exists()) {
                Toast.makeText(this, "Database tidak ditemukan!", Toast.LENGTH_LONG).show();
                tvStatus.setText("Status: Database tidak ada");
                return;
            }

            SessionManager sessionManager = new SessionManager(this);
            String namaToko = sessionManager.getNamatoko();
            if (namaToko == null || namaToko.trim().isEmpty()) {
                namaToko = "kasir";
            }
            namaToko = namaToko.replaceAll("[^a-zA-Z0-9_-]", "_");

            String timeStamp = new java.text.SimpleDateFormat(
                    "dd-MM-yyyy_HH-mm", java.util.Locale.getDefault()
            ).format(new java.util.Date());

            String backupName = namaToko + "_" + timeStamp + ".db";

            ContentValues values = new ContentValues();
            values.put(MediaStore.MediaColumns.DISPLAY_NAME, backupName);
            values.put(MediaStore.MediaColumns.MIME_TYPE, "application/octet-stream");
            values.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);

            ContentResolver resolver = getContentResolver();
            Uri uri = resolver.insert(MediaStore.Files.getContentUri("external"), values);

            if (uri != null) {
                try (FileInputStream inputStream = new FileInputStream(dbFile);
                     OutputStream outputStream = resolver.openOutputStream(uri)) {

                    if (outputStream == null) {
                        throw new Exception("OutputStream null, gagal buka file tujuan");
                    }

                    byte[] buffer = new byte[4096];
                    int length;
                    while ((length = inputStream.read(buffer)) > 0) {
                        outputStream.write(buffer, 0, length);
                    }
                    outputStream.flush();

                    Toast.makeText(this, "Database berhasil disimpan di Download", Toast.LENGTH_LONG).show();
                    tvStatus.setText("Status: Backup → " + backupName);
                }
            } else {
                Toast.makeText(this, "Gagal membuat file backup (uri null)", Toast.LENGTH_LONG).show();
                tvStatus.setText("Status: Gagal insert ke MediaStore");
            }

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Gagal backup: " + e.getMessage(), Toast.LENGTH_LONG).show();
            tvStatus.setText("Status: Gagal backup → " + e.getMessage());
        }
    }

    private void showLoading(String message) {
        if (loadingDialog != null && loadingDialog.isShowing()) return;

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = getLayoutInflater().inflate(R.layout.dialog_loading, null);

        TextView tvLabel = view.findViewById(R.id.loading_text);
        tvLabel.setText(message);

        builder.setView(view);
        builder.setCancelable(false);

        loadingDialog = builder.create();

        loadingDialog.show();
    }

    private void hideLoading() {
        if (loadingDialog != null && loadingDialog.isShowing()) {
            loadingDialog.dismiss();
        }
    }

    private void flushWalToDb() {
        DataBaseHelper helper = new DataBaseHelper(this);
        SQLiteDatabase db = helper.getWritableDatabase();

        Cursor cursor = null;
        try {
            // WAJIB pakai rawQuery
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

    private void uploadToServer() {

        Log.d("UPLOAD_DB", "=== START UPLOAD ===");

        showLoading("Mengupload database...");
        flushWalToDb();

        try {
            File dbFile = getDatabasePath("kasir.db");
            Log.d("UPLOAD_DB", "DB Path: " + dbFile.getAbsolutePath());

            if (!dbFile.exists()) {
                Log.e("UPLOAD_DB", "Database tidak ditemukan");
                hideLoading();
                Toast.makeText(this, "Database tidak ditemukan!", Toast.LENGTH_LONG).show();
                return;
            }

            SessionManager sessionManager = new SessionManager(this);
            String kodePegawai = sessionManager.getKodeUsers();
            String namaToko = sessionManager.getNamatoko();

            Log.d("UPLOAD_DB", "kodePegawai: " + kodePegawai);
            Log.d("UPLOAD_DB", "namaToko: " + namaToko);

            DataBaseHelper dbHelper = new DataBaseHelper(this);
            String idToko = dbHelper.getIdTokoByKodePegawai(kodePegawai);
            String pegawaiDataJson = dbHelper.getPegawaiDataJson(kodePegawai);
            String kodeOwner = sessionManager.getKodeOwnerutama();

            Log.d("UPLOAD_DB", "idToko: " + idToko);
            Log.d("UPLOAD_DB", "kodeOwner: " + kodeOwner);

            if (idToko == null) {
                Log.e("UPLOAD_DB", "idToko NULL");
                hideLoading();
                Toast.makeText(this, "Data toko tidak ditemukan", Toast.LENGTH_SHORT).show();
                return;
            }

            String kodeFranchise = "FR-" + kodePegawai + "-" + kodeOwner;
            Log.d("UPLOAD_DB", "kodeFranchise: " + kodeFranchise);

            byte[] dbBytes = new byte[(int) dbFile.length()];
            try (FileInputStream fis = new FileInputStream(dbFile)) {
                fis.read(dbBytes);
            }

            Log.d("UPLOAD_DB", "DB Size (bytes): " + dbBytes.length);

            String dbBase64 = android.util.Base64.encodeToString(dbBytes, android.util.Base64.NO_WRAP);

            JSONObject jsonPayload = new JSONObject();
            jsonPayload.put("kode_franchise", kodeFranchise);
            jsonPayload.put("id_toko", idToko);
            jsonPayload.put("nama_toko", namaToko);
            jsonPayload.put("kode_owner", kodeOwner);
            jsonPayload.put("pegawai_data", new JSONObject(pegawaiDataJson));
            jsonPayload.put("file_name", kodeFranchise + ".db");
            jsonPayload.put("file_data", dbBase64);

            Log.d("UPLOAD_DB", "Payload siap dikirim");

            String uploadUrl = "https://siqasir.awandigital.id/api/upload-db";
            Log.d("UPLOAD_DB", "UPLOAD URL: " + uploadUrl);

            OkHttpClient client = ApiKeyHttpClient.get();

            RequestBody requestBody = RequestBody.create(
                    jsonPayload.toString(),
                    MediaType.parse("application/json; charset=utf-8")
            );

            Request request = new Request.Builder()
                    .url(uploadUrl)
                    .post(requestBody)
                    .build();

            new Thread(() -> {
                try (Response response = client.newCall(request).execute()) {

                    String resBody = response.body() != null ? response.body().string() : "null";

                    Log.d("UPLOAD_DB", "UPLOAD RESPONSE CODE: " + response.code());
                    Log.d("UPLOAD_DB", "UPLOAD RESPONSE BODY: " + resBody);

                    runOnUiThread(() -> {
                        if (!response.isSuccessful()) {
                            Log.e("UPLOAD_DB", "Upload gagal");
                            hideLoading();
                            Toast.makeText(this, "Upload gagal: " + resBody, Toast.LENGTH_LONG).show();
                            tvStatus.setText("Status: Upload gagal");
                            return;
                        }

                        tvStatus.setText("Status: Upload berhasil → merge...");
                        showLoading("Menggabungkan database...");

                        new Thread(() -> {
                            try {
                                String mergeUrl = "https://siqasir.awandigital.id/api/merge/" + kodeOwner;
                                Log.d("UPLOAD_DB", "MERGE URL: " + mergeUrl);

                                OkHttpClient mergeClient = ApiKeyHttpClient.get();

                                Request mergeRequest = new Request.Builder()
                                        .url(mergeUrl)
                                        .get()
                                        .build();

                                try (Response mergeResponse = mergeClient.newCall(mergeRequest).execute()) {

                                    String mergeResBody = mergeResponse.body() != null ? mergeResponse.body().string() : "null";

                                    Log.d("UPLOAD_DB", "MERGE RESPONSE CODE: " + mergeResponse.code());
                                    Log.d("UPLOAD_DB", "MERGE RESPONSE BODY: " + mergeResBody);

                                    runOnUiThread(() -> {
                                        hideLoading();

                                        if (mergeResponse.isSuccessful()) {

                                            tvStatus.setText("Status: Merge berhasil → Sync data");

                                            new Thread(() -> {
                                                try {
                                                    DataBaseHelper dbHelperSync = new DataBaseHelper(this);
                                                    Map<String, Object> syncData = dbHelperSync.getSyncData(kodeOwner);

                                                    Object jsonObj = mapToJson(syncData);

                                                    if (!(jsonObj instanceof JSONObject)) {
                                                        throw new IllegalStateException("Root JSON bukan JSONObject");
                                                    }

                                                    JSONObject syncJson = (JSONObject) jsonObj;

                                                    Log.d("UPLOAD_DB", "SYNC JSON: " + syncJson.toString());

                                                    String syncUrl = "https://siqasir.awandigital.id/api/sync/data-compare";
                                                    Log.d("UPLOAD_DB", "SYNC URL: " + syncUrl);

                                                    OkHttpClient syncClient = ApiKeyHttpClient.get();

                                                    RequestBody syncBody = RequestBody.create(
                                                            syncJson.toString(),
                                                            MediaType.parse("application/json; charset=utf-8")
                                                    );

                                                    Request syncRequest = new Request.Builder()
                                                            .url(syncUrl)
                                                            .post(syncBody)
                                                            .build();

                                                    try (Response syncResponse = syncClient.newCall(syncRequest).execute()) {

                                                        String syncResBody = syncResponse.body() != null
                                                                ? syncResponse.body().string()
                                                                : "null";

                                                        Log.d("UPLOAD_DB", "SYNC RESPONSE CODE: " + syncResponse.code());
                                                        Log.d("UPLOAD_DB", "SYNC RESPONSE BODY: " + syncResBody);

                                                        runOnUiThread(() -> {
                                                            hideLoading();

                                                            if (syncResponse.isSuccessful()) {
                                                                Toast.makeText(this, "Sync compare berhasil", Toast.LENGTH_SHORT).show();
                                                                tvStatus.setText("Status: Sync compare berhasil");
                                                            } else {
                                                                Toast.makeText(this, "Sync compare gagal: " + syncResBody, Toast.LENGTH_LONG).show();
                                                                tvStatus.setText("Status: Sync compare gagal");
                                                            }
                                                        });
                                                    }

                                                } catch (Exception e) {
                                                    Log.e("UPLOAD_DB", "ERROR SYNC", e);
                                                    runOnUiThread(() -> {
                                                        hideLoading();
                                                        Toast.makeText(this, "Error sync compare: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                                        tvStatus.setText("Status: Error sync compare");
                                                    });
                                                }
                                            }).start();

                                        } else {
                                            Log.e("UPLOAD_DB", "Merge gagal");
                                            Toast.makeText(this, "Merge gagal: " + mergeResBody, Toast.LENGTH_LONG).show();
                                            tvStatus.setText("Status: Merge gagal");
                                        }
                                    });
                                }

                            } catch (Exception ex) {
                                Log.e("UPLOAD_DB", "ERROR MERGE", ex);
                                runOnUiThread(() -> {
                                    hideLoading();
                                    Toast.makeText(this, "Error merge: " + ex.getMessage(), Toast.LENGTH_LONG).show();
                                    tvStatus.setText("Status: Error merge → " + ex.getMessage());
                                });
                            }
                        }).start();
                    });

                } catch (Exception e) {
                    Log.e("UPLOAD_DB", "ERROR UPLOAD", e);
                    runOnUiThread(() -> {
                        hideLoading();
                        Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        tvStatus.setText("Status: Error → " + e.getMessage());
                    });
                }
            }).start();

        } catch (Exception e) {
            Log.e("UPLOAD_DB", "FATAL ERROR", e);
            hideLoading();
            Toast.makeText(this, "Upload gagal: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
}
