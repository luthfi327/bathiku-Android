package com.pyramidsoft.siqasir.ui.pengaturan;
import com.pyramidsoft.siqasir.utils.ApiKeyHttpClient;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.pyramidsoft.siqasir.AboutTokoAct;
import com.pyramidsoft.siqasir.BackupDatabaseActivity;
import com.pyramidsoft.siqasir.BantuanActivity;
import com.pyramidsoft.siqasir.KategoriActivity;
import com.pyramidsoft.siqasir.ListOutletActivity;
import com.pyramidsoft.siqasir.ListPegawaiAct;
import com.pyramidsoft.siqasir.ListProdukActivity;
import com.pyramidsoft.siqasir.MasterJenisKedatanganActivity;
import com.pyramidsoft.siqasir.PaketLanggananActivity;
import com.pyramidsoft.siqasir.PengaturanProfile;
import com.pyramidsoft.siqasir.R;
import com.pyramidsoft.siqasir.SettingHalamanStrukAct;
import com.pyramidsoft.siqasir.databinding.FragmentPengaturanBinding;
import com.pyramidsoft.siqasir.db.DataBaseHelper;
import com.pyramidsoft.siqasir.printer.SettingPrinterActivity;
import com.pyramidsoft.siqasir.utils.SessionManager;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class PengaturanFragment extends Fragment {

    private FragmentPengaturanBinding binding;
    Activity activity;
    SessionManager sessionManager;
    DataBaseHelper dbHelper;

    private static final String DB_NAME = "kasir.db";
    private static final String API_URL = "https://siqasir.awandigital.id/api/download-owner-db";

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentPengaturanBinding.inflate(inflater, container, false);
        View root = binding.getRoot();
        activity = getActivity();
        sessionManager = new SessionManager(activity);
        dbHelper = new DataBaseHelper(activity);

        binding.tvNamaOutlet.setOnClickListener(this::onViewClicked);
        binding.tvListOutlet.setOnClickListener(this::onViewClicked);
        binding.tvPerpanjangMAO.setOnClickListener(this::onViewClicked);
        binding.tvListBarang.setOnClickListener(this::onViewClicked);
        binding.tvListKategori.setOnClickListener(this::onViewClicked);
        binding.tvJenisKedatangan.setOnClickListener(this::onViewClicked);
        binding.tvPemilikToko.setOnClickListener(this::onViewClicked);
        binding.tvPegawai.setOnClickListener(this::onViewClicked);
        binding.tvHapusAkun.setOnClickListener(this::onViewClicked);
        binding.tvPengaturanStruk.setOnClickListener(this::onViewClicked);
        binding.tvPengaturanPrinter.setOnClickListener(this::onViewClicked);
        binding.tvBantuan.setOnClickListener(this::onViewClicked);
        binding.tvBackupDatabase.setOnClickListener(this::onViewClicked);
        binding.tvSinkronisasiDatabase.setOnClickListener(this::onViewClicked);

        if (sessionManager.getHakAkses().equals("Kasir")) {
            binding.masterOutlet.setVisibility(View.GONE);
            binding.masterUser.setVisibility(View.VISIBLE);
            binding.masterStruk.setVisibility(View.GONE);
            binding.masterDatabase.setVisibility(View.VISIBLE);
            binding.tvPegawai.setVisibility(View.GONE);
            binding.tvHapusAkun.setVisibility(View.GONE);
            binding.tvPerpanjangMAO.setVisibility(View.GONE);
        } else if (sessionManager.getHakAkses().equals("Owner")) {
            binding.masterOutlet.setVisibility(View.VISIBLE);
            binding.masterUser.setVisibility(View.VISIBLE);
            binding.masterStruk.setVisibility(View.VISIBLE);
            binding.masterDatabase.setVisibility(View.VISIBLE);
            binding.tvPerpanjangMAO.setVisibility(View.VISIBLE);
        } else if (sessionManager.getHakAkses().equals("Kepala Cabang")) {
            binding.tvListOutlet.setVisibility(View.GONE);
            binding.masterUser.setVisibility(View.VISIBLE);
            binding.masterStruk.setVisibility(View.VISIBLE);
            binding.masterDatabase.setVisibility(View.VISIBLE);
            binding.tvPegawai.setVisibility(View.GONE);
            binding.tvHapusAkun.setVisibility(View.GONE);
            binding.tvPerpanjangMAO.setVisibility(View.GONE);
        }

        return root;
    }

    public void onViewClicked(View view) {
        Intent intent;
        if (view.getId() == R.id.tvNamaOutlet) {
            intent = new Intent(activity, AboutTokoAct.class);
            startActivity(intent);
        } else if (view.getId() == R.id.tvListOutlet) {
            intent = new Intent(activity, ListOutletActivity.class);
            startActivity(intent);
        } else if (view.getId() == R.id.tvPerpanjangMAO) {
            intent = new Intent(activity, PaketLanggananActivity.class);
            startActivity(intent);
        } else if (view.getId() == R.id.tvListBarang) {
            intent = new Intent(activity, ListProdukActivity.class);
            startActivity(intent);
        } else if (view.getId() == R.id.tvListKategori) {
            intent = new Intent(activity, KategoriActivity.class);
            startActivity(intent);
        } else if (view.getId() == R.id.tvJenisKedatangan) {
            intent = new Intent(activity, MasterJenisKedatanganActivity.class);
            startActivity(intent);
        } else if (view.getId() == R.id.tvPemilikToko) {
            intent = new Intent(activity, PengaturanProfile.class);
            startActivity(intent);
        } else if (view.getId() == R.id.tvPegawai) {
            intent = new Intent(activity, ListPegawaiAct.class);
            startActivity(intent);
        } else if (view.getId() == R.id.tvHapusAkun) {
            showDeleteAccountDialog();
        }
        else if (view.getId() == R.id.tvPengaturanStruk) {
            intent = new Intent(activity, SettingHalamanStrukAct.class);
            startActivity(intent);
        } else if (view.getId() == R.id.tvPengaturanPrinter) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (ContextCompat.checkSelfPermission(activity, android.Manifest.permission.BLUETOOTH_CONNECT)
                        != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(activity,
                            new String[]{android.Manifest.permission.BLUETOOTH_CONNECT}, 1001);
                    return;
                }
            }
            intent = new Intent(activity, SettingPrinterActivity.class);
            startActivity(intent);
        } else if (view.getId() == R.id.tvBantuan) {
            intent = new Intent(activity, BantuanActivity.class);
            startActivity(intent);
        } else if (view.getId() == R.id.tvBackupDatabase) {
            intent = new Intent(activity, BackupDatabaseActivity.class);
            startActivity(intent); }
        else if (view.getId() == R.id.tvSinkronisasiDatabase) {
            showConfirmSyncDialog();
        }
    }

    private void showDeleteAccountDialog() {

        androidx.appcompat.app.AlertDialog dialog = new MaterialAlertDialogBuilder(activity)
                .setTitle("Hapus Akun")
                .setMessage(
                        "Pilih jenis penghapusan:\n\n" +
                                "• Hapus di HP saja → hanya data di perangkat ini (Pastikan sudah upload data dulu) masih bisa login lagi\n" +
                                "• Hapus dengan server → semua data ikut terhapus (tidak bisa dipulihkan)\n\n" +
                                "Lanjutkan?"
                )
                .setPositiveButton("HP Saja", (d, which) -> performDeleteLocal())
                .setNeutralButton("Server + HP", (d, which) -> performDeleteWithServer())
                .setNegativeButton("Batal", null)
                .create();

        dialog.show();

        Button btnHP = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
        Button btnServer = dialog.getButton(AlertDialog.BUTTON_NEUTRAL);

        btnHP.setBackgroundTintList(null);
        btnHP.setBackgroundResource(R.drawable.bg_button_orange_circle);
        btnHP.setTextColor(Color.WHITE);

        btnServer.setBackgroundTintList(null);
        btnServer.setBackgroundResource(R.drawable.bg_button_red_circle);
        btnServer.setTextColor(Color.WHITE);
    }

    private void performDeleteLocal() {

        try {
            if (sessionManager != null) {
                sessionManager.clearAllSession();
            }

            if (dbHelper != null) {
                dbHelper.close();
            }

            File dbFile = activity.getDatabasePath("kasir.db");
            if (dbFile.exists()) {
                dbFile.delete();
            }

            activity.deleteDatabase("kasir.db");

            deleteDir(activity.getCacheDir());
            deleteDir(activity.getFilesDir());

            Intent intent = new Intent(activity, com.pyramidsoft.siqasir.LoginOrRegisterActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);

            Toast.makeText(activity, "Data lokal berhasil dihapus", Toast.LENGTH_SHORT).show();

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(activity, "Gagal menghapus data lokal", Toast.LENGTH_SHORT).show();
        }
    }

    private static final String TAG = "DELETE_ACCOUNT";

    private void performDeleteWithServer() {

        String kodeOwner = sessionManager.getKodeOwnerutama();

        Log.d(TAG, "Kode Owner: " + kodeOwner);

        if (kodeOwner == null || kodeOwner.isEmpty()) {
            Toast.makeText(activity, "Kode owner tidak ditemukan", Toast.LENGTH_SHORT).show();
            return;
        }

        OkHttpClient client = ApiKeyHttpClient.get();

        JSONObject json = new JSONObject();
        try {
            json.put("kode_owner", kodeOwner);
        } catch (JSONException e) {
            e.printStackTrace();
            Log.e(TAG, "JSON Error: " + e.getMessage());
        }

        String jsonString = json.toString();
        Log.d(TAG, "Request Body: " + jsonString);

        RequestBody body = RequestBody.create(
                jsonString,
                MediaType.parse("application/json")
        );

        Request request = new Request.Builder()
                .url("https://siqasir.awandigital.id/api/delete-by-kode-owner")
                .post(body)
                .build();

        Log.d(TAG, "Request URL: " + request.url());

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {

                Log.e(TAG, "Request Failed: ", e);

                activity.runOnUiThread(() ->
                        Toast.makeText(activity, "Gagal koneksi ke server", Toast.LENGTH_SHORT).show()
                );
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {

                String responseBody = response.body() != null ? response.body().string() : "null";

                Log.d(TAG, "Response Code: " + response.code());
                Log.d(TAG, "Response Body: " + responseBody);

                activity.runOnUiThread(() -> {

                    if (response.isSuccessful()) {
                        Toast.makeText(activity, "Data server berhasil dihapus", Toast.LENGTH_SHORT).show();

                        performDeleteLocal();

                    } else {
                        Toast.makeText(activity, "Gagal hapus data server", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }

    private void deleteDir(File dir) {
        if (dir != null && dir.isDirectory()) {
            String[] children = dir.list();
            if (children != null) {
                for (String child : children) {
                    deleteDir(new File(dir, child));
                }
            }
        }
        if (dir != null) {
            dir.delete();
        }
    }

    private void showConfirmSyncDialog() {
        new AlertDialog.Builder(activity)
                .setTitle("Sinkronisasi Database")
                .setMessage("Apakah kamu yakin ingin mendownload dan mengganti database lokal?")
                .setPositiveButton("Ya", (dialog, which) -> {
                    if (isNetworkAvailable()) {
                        new DownloadDatabaseTask().execute(API_URL);
                    } else {
                        Toast.makeText(activity,
                                "Tidak ada koneksi internet",
                                Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Batal", null)
                .show();
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager cm = (ConnectivityManager)
                activity.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
    }

    private class DownloadDatabaseTask extends AsyncTask<String, Void, Integer> {

        private static final int RESULT_SUCCESS = 0;
        private static final int RESULT_ERROR_NETWORK = 1;
        private static final int RESULT_ERROR_SERVER = 2;
        private static final int RESULT_ERROR_PERMISSION = 3;
        private static final int RESULT_ERROR_DATABASE = 4;
        private static final int RESULT_ERROR_VALIDATION = 5;
        private static final int RESULT_ERROR_NO_OWNER_CODE = 6;

        private AlertDialog loadingDialog;
        private String errorMessage = "";
        private File downloadedDbFile;

        @Override
        protected void onPreExecute() {
            AlertDialog.Builder builder = new AlertDialog.Builder(activity);
            builder.setCancelable(false);
            builder.setView(R.layout.dialogue_loading);
            loadingDialog = builder.create();
            loadingDialog.show();

            if (dbHelper != null) {
                dbHelper.close();
            }
        }

        @Override
        protected Integer doInBackground(String... urls) {
            String apiUrl = urls[0];
            HttpURLConnection connection = null;
            InputStream inputStream = null;
            FileOutputStream outputStream = null;

            try {
                String kodeOwner = sessionManager.getKodeOwnerutama();
                if (kodeOwner == null || kodeOwner.isEmpty()) {
                    errorMessage = "Kode owner tidak ditemukan. Silakan login ulang.";
                    return RESULT_ERROR_NO_OWNER_CODE;
                }

                if (!backupCurrentDatabase()) {
                    Log.w("DownloadDB", "Gagal backup database, tetap melanjutkan...");
                }

                URL url = new URL(apiUrl);
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                connection.setRequestProperty("Accept", "application/octet-stream");
                connection.setDoOutput(true);
                connection.setConnectTimeout(30000);
                connection.setReadTimeout(60000);

                String jsonBody = "{\"kode_owner\": \"" + kodeOwner + "\"}";
                try (DataOutputStream wr = new DataOutputStream(connection.getOutputStream())) {
                    wr.write(jsonBody.getBytes("UTF-8"));
                    wr.flush();
                }

                int responseCode = connection.getResponseCode();
                Log.d("DownloadDB", "Response Code: " + responseCode);

                if (responseCode != HttpURLConnection.HTTP_OK) {
                    InputStream errorStream = connection.getErrorStream();
                    if (errorStream != null) {
                        BufferedReader reader = new BufferedReader(new InputStreamReader(errorStream));
                        StringBuilder response = new StringBuilder();
                        String line;
                        while ((line = reader.readLine()) != null) {
                            response.append(line);
                        }
                        reader.close();
                        errorMessage = "Server error: " + responseCode + " - " + response.toString();
                    } else {
                        errorMessage = "Server error: " + responseCode;
                    }
                    return RESULT_ERROR_SERVER;
                }

                inputStream = new BufferedInputStream(connection.getInputStream());

                File tempDbFile = new File(activity.getCacheDir(), "temp_download.db");

                outputStream = new FileOutputStream(tempDbFile);
                byte[] buffer = new byte[8192];
                int bytesRead;
                long totalBytes = 0;

                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                    totalBytes += bytesRead;

                    if (isCancelled()) {
                        return RESULT_ERROR_NETWORK;
                    }
                }
                outputStream.flush();

                Log.d("DownloadDB", "Downloaded " + totalBytes + " bytes");

                if (totalBytes == 0) {
                    errorMessage = "File database kosong";
                    return RESULT_ERROR_DATABASE;
                }

                if (!validateDatabaseFile(tempDbFile.getAbsolutePath())) {
                    errorMessage = "File database tidak valid atau corrupt";
                    return RESULT_ERROR_VALIDATION;
                }

                File finalDbFile = activity.getDatabasePath(DB_NAME);
                File parentDir = finalDbFile.getParentFile();

                if (parentDir != null && !parentDir.exists()) {
                    if (!parentDir.mkdirs()) {
                        errorMessage = "Gagal membuat direktori database";
                        return RESULT_ERROR_PERMISSION;
                    }
                }

                if (!parentDir.canWrite()) {
                    errorMessage = "Tidak ada izin menulis ke direktori database";
                    return RESULT_ERROR_PERMISSION;
                }

                Files.copy(tempDbFile.toPath(), finalDbFile.toPath(),
                        StandardCopyOption.REPLACE_EXISTING);

                tempDbFile.delete();

                downloadedDbFile = finalDbFile;

                return RESULT_SUCCESS;

            } catch (java.net.SocketTimeoutException e) {
                errorMessage = "Timeout koneksi ke server";
                return RESULT_ERROR_NETWORK;
            } catch (java.net.ConnectException e) {
                errorMessage = "Tidak dapat terhubung ke server";
                return RESULT_ERROR_NETWORK;
            } catch (IOException e) {
                errorMessage = "Error IO: " + e.getMessage();
                Log.e("DownloadDB", "IO Exception: ", e);
                return RESULT_ERROR_NETWORK;
            } catch (Exception e) {
                errorMessage = "Error: " + e.getMessage();
                Log.e("DownloadDB", "Exception: ", e);
                return RESULT_ERROR_DATABASE;
            } finally {
                try {
                    if (outputStream != null) outputStream.close();
                    if (inputStream != null) inputStream.close();
                } catch (IOException e) {
                    Log.e("DownloadDB", "Error closing streams: ", e);
                }
                if (connection != null) connection.disconnect();
            }
        }

        @Override
        protected void onPostExecute(Integer resultCode) {
            if (loadingDialog != null && loadingDialog.isShowing()) {
                loadingDialog.dismiss();
            }

            switch (resultCode) {
                case RESULT_SUCCESS:
                    handleSyncSuccess();
                    break;

                case RESULT_ERROR_NO_OWNER_CODE:
                    showErrorDialog("Kode Owner Hilang",
                            errorMessage + "\n\nSilakan logout dan login kembali.");
                    break;

                case RESULT_ERROR_SERVER:
                    showErrorDialog("Server Error",
                            errorMessage + "\n\nPeriksa koneksi internet dan coba lagi.");
                    break;

                case RESULT_ERROR_NETWORK:
                    showErrorDialog("Koneksi Error",
                            errorMessage + "\n\nPeriksa koneksi internet Anda.");
                    break;

                case RESULT_ERROR_PERMISSION:
                    showErrorDialog("Izin Error",
                            errorMessage + "\n\nAplikasi memerlukan izin penyimpanan.");
                    break;

                case RESULT_ERROR_VALIDATION:
                    showErrorDialog("Database Tidak Valid",
                            errorMessage + "\n\nFile database dari server corrupt.");
                    restoreFromBackup();
                    break;

                case RESULT_ERROR_DATABASE:
                default:
                    showErrorDialog("Error Sinkronisasi",
                            errorMessage + "\n\nSilakan coba lagi nanti.");
                    restoreFromBackup();
                    break;
            }
        }

        @Override
        protected void onCancelled() {
            if (loadingDialog != null && loadingDialog.isShowing()) {
                loadingDialog.dismiss();
            }
            Toast.makeText(activity, "Sinkronisasi dibatalkan", Toast.LENGTH_SHORT).show();
        }

        private boolean backupCurrentDatabase() {
            try {
                File currentDb = activity.getDatabasePath(DB_NAME);
                if (!currentDb.exists()) {
                    return true;
                }

                File backupDir = new File(activity.getExternalFilesDir(null), "backup");
                if (!backupDir.exists() && !backupDir.mkdirs()) {
                    Log.e("DownloadDB", "Gagal membuat direktori backup");
                    return false;
                }

                String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
                        .format(new Date());
                File backupFile = new File(backupDir, DB_NAME + "_backup_" + timestamp);

                Files.copy(currentDb.toPath(), backupFile.toPath(),
                        StandardCopyOption.REPLACE_EXISTING);

                Log.d("DownloadDB", "Backup created: " + backupFile.getAbsolutePath());
                return true;

            } catch (Exception e) {
                Log.e("DownloadDB", "Backup failed: ", e);
                return false;
            }
        }

        private boolean validateDatabaseFile(String dbPath) {
            SQLiteDatabase db = null;
            try {
                db = SQLiteDatabase.openDatabase(dbPath, null,
                        SQLiteDatabase.OPEN_READONLY | SQLiteDatabase.NO_LOCALIZED_COLLATORS);

                Cursor cursor = db.rawQuery(
                        "SELECT name FROM sqlite_master WHERE type='table' AND name NOT LIKE 'sqlite_%'",
                        null
                );

                boolean isValid = cursor != null && cursor.getCount() > 0;

                if (cursor != null) {
                    cursor.close();
                }

                Log.d("DownloadDB", "Database validation: " + isValid +
                        " (tables found: " + (cursor != null ? cursor.getCount() : 0) + ")");

                return isValid;

            } catch (Exception e) {
                Log.e("DownloadDB", "Database validation failed: ", e);
                return false;
            } finally {
                if (db != null && db.isOpen()) {
                    db.close();
                }
            }
        }

        private void restoreFromBackup() {
            Toast.makeText(activity, "Mengembalikan database dari backup...", Toast.LENGTH_SHORT).show();
        }

        private void handleSyncSuccess() {
            try {
                if (dbHelper != null) {
                    dbHelper.close();
                }

                Thread.sleep(100);

                dbHelper = new DataBaseHelper(activity);

                SQLiteDatabase db = dbHelper.getReadableDatabase();
                if (db != null && db.isOpen()) {
                    db.close();
                }

                new AlertDialog.Builder(activity)
                        .setTitle("Sinkronisasi Berhasil")
                        .setMessage("Database telah berhasil diperbarui dari server.")
                        .setPositiveButton("OK", (dialog, which) -> {
                            restartFragment();
                        })
                        .setCancelable(false)
                        .show();

            } catch (Exception e) {
                Log.e("DownloadDB", "Error handling sync success: ", e);
                Toast.makeText(activity,
                        "Database diperbarui tetapi ada error: " + e.getMessage(),
                        Toast.LENGTH_LONG).show();
            }
        }

        private void restartFragment() {
            if (getFragmentManager() != null) {
                getFragmentManager().beginTransaction()
                        .detach(PengaturanFragment.this)
                        .attach(PengaturanFragment.this)
                        .commit();
            }

        }

        private void showErrorDialog(String title, String message) {
            new AlertDialog.Builder(activity)
                    .setTitle(title)
                    .setMessage(message)
                    .setPositiveButton("OK", null)
                    .show();
        }
    }
}
