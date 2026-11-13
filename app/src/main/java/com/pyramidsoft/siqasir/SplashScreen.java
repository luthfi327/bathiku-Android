package com.pyramidsoft.siqasir;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import com.pyramidsoft.siqasir.utils.ApiKeyHttpClient;
import androidx.appcompat.app.AppCompatActivity;

import com.pyramidsoft.siqasir.db.DataBaseHelper;
import com.pyramidsoft.siqasir.utils.SessionManager;

import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class SplashScreen extends AppCompatActivity {

    SessionManager sessionManager;
    DataBaseHelper dbHelper;

    private boolean isNavigated = false;
    private boolean isCheckingExp = false;

    private OkHttpClient client = ApiKeyHttpClient.get();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.d("SPLASH_DEBUG", "Splash jalan: " + System.currentTimeMillis());

        setContentView(R.layout.activity_splash_screen);

        dbHelper = new DataBaseHelper(this);
        try {
            dbHelper.createDatabase();
        } catch (IOException e) {
            throw new RuntimeException("Unable to create database");
        }

        sessionManager = new SessionManager(this);

        String kodeOwner = sessionManager.getKodeOwnerutama();

        if (kodeOwner != null && !kodeOwner.isEmpty()) {
            checkExpDateFromServer();
        } else {
            lanjutFlow();
        }
    }

    private void checkExpDateFromServer() {
        if (isCheckingExp) return;
        isCheckingExp = true;

        String kodeOwner = sessionManager.getKodeOwnerutama();

        if (kodeOwner == null || kodeOwner.isEmpty()) {
            lanjutFlow();
            return;
        }

        MediaType JSON = MediaType.parse("application/json; charset=utf-8");

        JSONObject json = new JSONObject();
        try {
            json.put("kode_owner", kodeOwner);
        } catch (Exception e) {
            e.printStackTrace();
        }

        RequestBody body = RequestBody.create(json.toString(), JSON);

        Request request = new Request.Builder()
                .url("https://siqasir.awandigital.id/api/check-exp-date")
                .post(body)
                .addHeader("Accept", "application/json")
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> lanjutFlow());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    runOnUiThread(() -> lanjutFlow());
                    return;
                }

                try {
                    String res = response.body().string();
                    JSONObject obj = new JSONObject(res);

                    boolean status = obj.optBoolean("status", false);

                    JSONObject data = obj.optJSONObject("data");

                    if (data != null) {
                        String expServer = data.optString("new_exp_date", null);
                        int maxToko = data.optInt("max_toko", 1);

                        syncExpDate(expServer, maxToko);
                    } else {
                        runOnUiThread(() -> lanjutFlow());
                    }

                } catch (Exception e) {
                    runOnUiThread(() -> lanjutFlow());
                }
            }
        });
    }

    private void syncExpDate(String expServer, int maxToko) {
        try {
            dbHelper.syncAndCheckExpDate(expServer, maxToko);
        } catch (Exception e) {
            e.printStackTrace();
        }

        runOnUiThread(this::lanjutFlow);
    }

    private void lanjutFlow() {
        if (isNavigated) return;
        isNavigated = true;

        Intent intent;

        if (sessionManager.getRegistrasi() == null) {
            intent = new Intent(this, LoginOrRegisterActivity.class);

        } else if ("OutletDone".equals(sessionManager.getRegistrasi())) {
            intent = new Intent(this, RegistrasiUser.class);

        } else if ("Complete".equals(sessionManager.getRegistrasi()) && sessionManager.isLoggedIn()) {
            intent = new Intent(this, PilihKaryawan.class);

        } else {
            intent = new Intent(this, PilihKaryawan.class);
        }

        startActivity(intent);
        finish();
    }
}