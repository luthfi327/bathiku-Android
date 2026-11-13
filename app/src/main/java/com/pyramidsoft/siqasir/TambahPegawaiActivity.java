package com.pyramidsoft.siqasir;

import static com.pyramidsoft.siqasir.db.DataBaseHelper.database_name;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import com.chivorn.smartmaterialspinner.SmartMaterialSpinner;
import com.google.android.material.snackbar.Snackbar;
import com.pyramidsoft.siqasir.databinding.ActivityTambahPegawaiBinding;
import com.pyramidsoft.siqasir.db.DataBaseHelper;
import com.pyramidsoft.siqasir.utils.AndLog;
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

public class TambahPegawaiActivity extends AppCompatActivity {

    private ActivityTambahPegawaiBinding binding;
    ProgressDialog pDialog;
    SessionManager sessionManager;
    DataBaseHelper dbHelper;
    SQLiteDatabase mDatabase;
    private SmartMaterialSpinner<String> spJk;
    private SmartMaterialSpinner<String> spPosisi;
    private SmartMaterialSpinner<String> spHakAkses;
    private SmartMaterialSpinner<String> spOutlet;
    private List<String> outletList;
    String vOutlet;
    private Runnable usernameRunnable;
    private Call currentUsernameCall;

    private android.os.Handler handler = new android.os.Handler();

    private List<String> genderList;
    private List<String> posisiList;
    private List<String> hakAksesList;
    String vJk, vPosisi, vHakAkses, UOV, statusUsers, dataUser, kode_pegawai;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityTambahPegawaiBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);
        ActionBar actionBar = getSupportActionBar();

        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        pDialog = new ProgressDialog(TambahPegawaiActivity.this);
        sessionManager = new SessionManager(TambahPegawaiActivity.this);
        dbHelper = new DataBaseHelper(this);
        mDatabase = openOrCreateDatabase(database_name, MODE_PRIVATE, null);

        binding.btTambahPegawaiBaru.setOnClickListener(this::onViewClicked);

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

        initSpinner();
        UOV = generateUOV();

        AndLog.ShowLog("UserOutletVerification", UOV);
    }

    private boolean validated(boolean temp) {
        temp = true;

        if (!binding.etPassword.getText().toString().equals(binding.etUlangiPassword.getText().toString())) {
            binding.etUlangiPassword.setFocusable(true);
            binding.tilPwdBaru.setError("Password Tidak Sama!");
            dataUser = "0";
            temp = false;
        } else if (!binding.etPin.getText().toString().equals(binding.etUlangiPin.getText().toString())) {
            binding.etPin.setFocusable(true);
            binding.tilPinBaru.setError("PIN Tidak Sama!");
            dataUser = "0";
            temp = false;
        }

        return temp;
    }

    private void initSpinner() {

        spJk = findViewById(R.id.spinJK);
        spHakAkses = findViewById(R.id.spinHakAkses);
        spPosisi = findViewById(R.id.spinPosisi);
        spOutlet = findViewById(R.id.spinOutlet);

        genderList = new ArrayList<>();
        hakAksesList = new ArrayList<>();
        posisiList = new ArrayList<>();
        outletList = new ArrayList<>();

        genderList.add("Pria");
        genderList.add("Wanita");

        hakAksesList.add("Kasir");
        hakAksesList.add("Kepala Cabang");

        posisiList.add("Pegawai");

        spJk.setItem(genderList);
        spHakAkses.setItem(hakAksesList);
        spPosisi.setItem(posisiList);

        spHakAkses.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int position, long id) {
                vHakAkses = hakAksesList.get(position);
                AndLog.ShowLog("vHA", vHakAkses);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) { }
        });

        spPosisi.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int position, long id) {
                vPosisi = posisiList.get(position);
                AndLog.ShowLog("vPss", vPosisi);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) { }
        });

        List<String[]> outlets = dbHelper.getAllOutletWithId();

        if (outlets.size() == 1) {
            vOutlet = outlets.get(0)[0];
            spOutlet.setVisibility(View.GONE);
            AndLog.ShowLog("vOutlet", vOutlet);
        } else if (outlets.size() > 1) {
            List<String> outletNames = new ArrayList<>();
            for (String[] o : outlets) {
                outletNames.add(o[1]);
            }
            spOutlet.setItem(outletNames);
            spOutlet.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> adapterView, View view, int position, long id) {
                    vOutlet = outlets.get(position)[0];
                    AndLog.ShowLog("vOutlet", vOutlet);
                }

                @Override
                public void onNothingSelected(AdapterView<?> adapterView) { }
            });
        }
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

    private void setBtTambahPegawaiBaru(View view) {
        pDialog.show();
        String KodeToko, NamaPegawai, NoHP, Username, Pwd, Pin;

        NamaPegawai = binding.etnamaPegawai.getText().toString().trim();
        NoHP = binding.etNoHp.getText().toString().trim();
        Username = binding.etUsername.getText().toString().trim();
        Pwd = binding.etPassword.getText().toString().trim();
        Pin = binding.etPin.getText().toString().trim();

        ContentValues values = new ContentValues();
        if(vOutlet == null || vOutlet.isEmpty()){
            Snackbar.make(view, "Pilih outlet terlebih dahulu", Snackbar.LENGTH_SHORT).show();
            pDialog.dismiss();
            return;
        }

        values.put(DataBaseHelper.id_toko, Integer.parseInt(vOutlet));
        values.put("gender", vJk);
        values.put(DataBaseHelper.nama_pegawai, NamaPegawai);
        values.put(DataBaseHelper.kode_pegawai, UOV);
        values.put(DataBaseHelper.no_hp, NoHP);
        values.put(DataBaseHelper.hak_akses, vHakAkses);
        values.put(DataBaseHelper.posisi, vPosisi);

        if (binding.switchStatus.isChecked()) {
            values.put(DataBaseHelper.status, "Aktif");
        } else {
            values.put(DataBaseHelper.status, "Tidak Aktif");
        }

        values.put(DataBaseHelper.username, Username);
        values.put(DataBaseHelper.password, Pwd);
        values.put(DataBaseHelper.pin, Pin);

        if (NamaPegawai.equals("") ||
                NoHP.equals("") ||
                Username.equals("") ||
                Pwd.equals("") ||
                Pin.equals("")) {
            Snackbar.make(view, "Data Tidak Boleh Kosong", Snackbar.LENGTH_SHORT).show();
            pDialog.dismiss();
        } else {
            try {
                if (validated(true)) {
                    dbHelper.registrasiUsers(values);
                    Toast.makeText(this,
                            "Data tersimpan. Mengupload ke server...",
                            Toast.LENGTH_SHORT).show();

                    uploadToServer();
                    finish();
                    AndLog.ShowLog("dataUsers: ", String.valueOf(values));
                } else {
                    pDialog.dismiss();
                }

            } catch (Exception e) {
                e.printStackTrace();
            }

        }

    }

    private void showUsernameLoading(boolean isLoading) {
        if (isLoading) {
            binding.tilUsername.setEndIconDrawable(R.drawable.loading_small);
            binding.tilUsername.setEndIconVisible(true);
        } else {
            binding.tilUsername.setEndIconVisible(false);
        }
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

    private void cekUsernameRealtime(String username) {

        if (username.isEmpty()) {
            runOnUiThread(() -> {
                showUsernameLoading(false);
                binding.etUsername.setError(null);
            });
            return;
        }

        if (!isInternetAvailable()) return;

        OkHttpClient client = com.pyramidsoft.siqasir.utils.ApiKeyHttpClient.get();

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
            String idToko = dbHelper.getIdTokoByKodePegawai(kodePegawai);
            String pegawaiDataJson = dbHelper.getPegawaiDataJson(kodePegawai);

            String kodeFranchise = "FR-" + kodePegawai + "-" + kodeOwner;

            byte[] dbBytes = new byte[(int) dbFile.length()];
            try (FileInputStream fis = new FileInputStream(dbFile)) {
                fis.read(dbBytes);
            }

            String dbBase64 = Base64.encodeToString(dbBytes, Base64.NO_WRAP);

            JSONObject jsonPayload = new JSONObject();
            jsonPayload.put("kode_franchise", kodeFranchise);
            jsonPayload.put("id_toko", idToko);
            jsonPayload.put("nama_toko", namaToko);
            jsonPayload.put("kode_owner", kodeOwner);
            jsonPayload.put("pegawai_data", new JSONObject(pegawaiDataJson));
            jsonPayload.put("file_name", kodeFranchise + ".db");
            jsonPayload.put("file_data", dbBase64);

            OkHttpClient client = com.pyramidsoft.siqasir.utils.ApiKeyHttpClient.get();
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
                OkHttpClient client = com.pyramidsoft.siqasir.utils.ApiKeyHttpClient.get();

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
        setBtTambahPegawaiBaru(view);
    }

}