package com.pyramidsoft.siqasir;

import static com.pyramidsoft.siqasir.db.DataBaseHelper.database_name;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ImageView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.pyramidsoft.siqasir.databinding.ActivityRegistrasiTokoBinding;
import com.pyramidsoft.siqasir.db.DataBaseHelper;
import com.pyramidsoft.siqasir.model.JenisUsahaModel;
import com.pyramidsoft.siqasir.utils.ApiKeyHttpClient;
import com.pyramidsoft.siqasir.utils.GlobalToast;
import com.pyramidsoft.siqasir.utils.ProgressDialog;
import com.pyramidsoft.siqasir.utils.SessionManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Response;

public class RegistrasiToko extends AppCompatActivity {

    private ActivityRegistrasiTokoBinding binding;
    DataBaseHelper dbHelper;
    SQLiteDatabase mDatabase;
    SessionManager sessionManager;
    ProgressDialog pDialog;

    private String expDateFromApi = null;
    private RequestQueue requestQueue;
    private OkHttpClient okHttpClient = ApiKeyHttpClient.get();
    private static final int RESULT_LOAD_IMG = 1002, RESULT_LOAD_IMG_2 = 1003;
    private boolean isSendingOtp = false;
    private CountDownTimer otpTimer;

    Bitmap bitmap, decodedLogo, decodedLogoStruk;

    String prov_id, provinsi;

    JenisUsahaModel selectedJenisUsaha = null;

    private android.os.Handler emailCheckHandler = new android.os.Handler();
    private Runnable emailCheckRunnable;
    private boolean isEmailVerified = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRegistrasiTokoBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        ActionBar actionBar = getSupportActionBar();

        setSupportActionBar(binding.toolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        List<JenisUsahaModel> jenisUsahaList = loadJenisUsahaFromAssets();

        ArrayAdapter<JenisUsahaModel> adapter =
                new ArrayAdapter<>(
                        this,
                        android.R.layout.simple_dropdown_item_1line,
                        jenisUsahaList
                );

        binding.etJenisUsaha.setAdapter(adapter);
        binding.etJenisUsaha.setThreshold(1);

        binding.etJenisUsaha.setOnItemClickListener((parent, view, position, id) -> {
            selectedJenisUsaha =
                    (JenisUsahaModel) parent.getItemAtPosition(position);

            Log.d("JENIS_USAHA",
                    "Dipilih: " + selectedJenisUsaha.nama +
                            " | id=" + selectedJenisUsaha.id);
        });

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            binding.etProvinsi.setImportantForAutofill(View.IMPORTANT_FOR_AUTOFILL_NO);
            binding.etProvinsi.setAutofillHints((String) null);

            binding.etKota.setImportantForAutofill(View.IMPORTANT_FOR_AUTOFILL_NO);
            binding.etKota.setAutofillHints((String) null);

            binding.etEmail.setImportantForAutofill(View.IMPORTANT_FOR_AUTOFILL_NO);
            binding.etEmail.setAutofillHints((String) null);
        }

        binding.btnKirimOtp.setOnClickListener(v -> {

            if (isSendingOtp) return;

            binding.btnKirimOtp.setEnabled(false);

            if (!isConnected()) {
                binding.btnKirimOtp.setEnabled(true);
                GlobalToast.ShowToast(this, "Tidak ada koneksi internet");
                return;
            }

            String email = binding.etEmail.getText().toString().trim();

            if (email.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                binding.btnKirimOtp.setEnabled(true);
                GlobalToast.ShowToast(this, "Format email tidak valid");
                return;
            }

            sendOtp(email);
        });

        binding.tvUploadLogo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
            }
        });

        binding.btSimpan.setOnClickListener(v -> {
            if (!isConnected()) {
                GlobalToast.ShowToast(this, "Tidak ada koneksi internet");
                return;
            }

            String email = binding.etEmail.getText().toString().trim();
            checkEmailVerified(email);
        });

        binding.tvUploadUlangFotoLogo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
            }
        });

        binding.btNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(RegistrasiToko.this, RegistrasiUser.class);
                startActivity(intent);
                finish();
            }
        });

        binding.relImg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showFileChooser();
            }
        });

        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        pDialog = new ProgressDialog(RegistrasiToko.this);
        sessionManager = new SessionManager(RegistrasiToko.this);
        dbHelper = new DataBaseHelper(this);
        mDatabase = openOrCreateDatabase(database_name, MODE_PRIVATE, null);
        requestQueue = Volley.newRequestQueue(RegistrasiToko.this);

        binding.btSimpan.setVisibility(View.VISIBLE);
        binding.btNext.setVisibility(View.GONE);

        if (isConnected()) {
            getProvinsi();
        } else {
            GlobalToast.ShowToast(RegistrasiToko.this, "Sinyal Lemah / Tidak Ada Koneksi Internet ");
        }

        binding.ivUploadLogo.setTag("kosong");
        binding.ivUploadLogo.setImageResource(R.drawable.outlet_kosong);

    }

    @Override
    public boolean onOptionsItemSelected(android.view.MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void getProvinsi() {
        String url = "https://www.emsifa.com/api-wilayah-indonesia/api/provinces.json";

        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                response -> {
                    try {
                        JSONArray jsonArray = new JSONArray(response);
                        final List<String> provinsiList = new ArrayList<>();
                        final Map<String, String> provinsiMap = new HashMap<>();

                        for (int i = 0; i < jsonArray.length(); i++) {
                            JSONObject json = jsonArray.getJSONObject(i);
                            String id = json.getString("id");
                            String name = json.getString("name");

                            provinsiList.add(name);
                            provinsiMap.put(name, id);
                        }

                        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                                RegistrasiToko.this,
                                android.R.layout.simple_list_item_1,
                                provinsiList
                        );

                        binding.etProvinsi.setAdapter(adapter);
                        binding.etProvinsi.setThreshold(1);

                        binding.etProvinsi.setOnItemClickListener((parent, view, position, id) -> {
                            String selectedName = adapter.getItem(position);
                            prov_id = provinsiMap.get(selectedName);
                            provinsi = selectedName;

                            Log.d("Provinsi", "Dipilih: " + provinsi + " | ID: " + prov_id);

                            getKota(prov_id);
                        });

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                },
                error -> Log.e("API", "Error: " + error.getMessage())
        );
        // getProvinsi ke API eksternal (bukan siqasir) — tidak perlu API key
        requestQueue.add(stringRequest);
    }

    private void getKota(String provId) {
        String url = "https://www.emsifa.com/api-wilayah-indonesia/api/regencies/" + provId + ".json";

        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                response -> {
                    try {
                        JSONArray jsonArray = new JSONArray(response);
                        final List<String> kotaList = new ArrayList<>();

                        for (int i = 0; i < jsonArray.length(); i++) {
                            JSONObject json = jsonArray.getJSONObject(i);
                            kotaList.add(json.getString("name"));
                        }

                        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                                RegistrasiToko.this,
                                android.R.layout.simple_list_item_1,
                                kotaList
                        );

                        binding.etKota.setAdapter(adapter);
                        binding.etKota.setThreshold(1);

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                },
                error -> Log.e("API", "Error: " + error.getMessage())
        );
        // getKota ke API eksternal — tidak perlu API key
        requestQueue.add(stringRequest);
    }

    public boolean isConnected() {
        try {
            ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
                return activeNetwork != null && activeNetwork.isConnected();
            } else {
                NetworkInfo nInfo = cm.getActiveNetworkInfo();
                return nInfo != null && nInfo.isConnected();
            }

        } catch (Exception e) {
            Log.e("Connectivity Exception", e.getMessage());
            return false;
        }
    }

    private void sendOtp(String email) {

        isSendingOtp = true;

        binding.btnKirimOtp.setEnabled(false);
        binding.btnKirimOtp.setText("Mengirim...");

        String url = "https://siqasir.awandigital.id/api/otp/send";

        Log.d("OTP_SEND", "REQUEST -> email: " + email);

        try {
            okhttp3.FormBody body = new okhttp3.FormBody.Builder()
                    .add("email", email)
                    .build();

            okhttp3.Request request = new okhttp3.Request.Builder()
                    .url(url)
                    .post(body)
                    .build();

            okHttpClient.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Log.e("OTP_SEND", "ERROR: " + e.toString());
                    runOnUiThread(() -> {
                        isSendingOtp = false;
                        binding.btnKirimOtp.setEnabled(true);
                        binding.btnKirimOtp.setText("Kirim OTP");
                        GlobalToast.ShowToast(RegistrasiToko.this, "Gagal mengirim OTP (Koneksi)");
                    });
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    final String responseBody = response.body() != null ? response.body().string() : "";
                    Log.d("OTP_SEND", "RESPONSE: " + responseBody);

                    runOnUiThread(() -> {
                        try {
                            JSONObject json = new JSONObject(responseBody);
                            String message = json.optString("message", "OTP berhasil dikirim");

                            if (response.isSuccessful()) {
                                GlobalToast.ShowToast(RegistrasiToko.this, message);
                                startOtpCountdown();
                                startAutoCheckEmail(email);
                            } else {
                                isSendingOtp = false;
                                binding.btnKirimOtp.setEnabled(true);
                                binding.btnKirimOtp.setText("Kirim OTP");
                                GlobalToast.ShowToast(RegistrasiToko.this, message);
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                            isSendingOtp = false;
                            binding.btnKirimOtp.setEnabled(true);
                            binding.btnKirimOtp.setText("Kirim OTP");
                            GlobalToast.ShowToast(RegistrasiToko.this, "Response tidak valid");
                        }
                    });
                }
            });

        } catch (Exception e) {
            e.printStackTrace();
            isSendingOtp = false;
            binding.btnKirimOtp.setEnabled(true);
            binding.btnKirimOtp.setText("Kirim OTP");
        }
    }

    private void startAutoCheckEmail(String email) {

        emailCheckRunnable = new Runnable() {
            @Override
            public void run() {

                if (isEmailVerified) return;

                String url = "https://siqasir.awandigital.id/api/otp/check-email";

                try {
                    okhttp3.FormBody body = new okhttp3.FormBody.Builder()
                            .add("email", email)
                            .build();

                    okhttp3.Request request = new okhttp3.Request.Builder()
                            .url(url)
                            .post(body)
                            .build();

                    okHttpClient.newCall(request).enqueue(new Callback() {
                        @Override
                        public void onFailure(Call call, IOException e) {
                            Log.e("AUTO_CHECK", e.toString());
                        }

                        @Override
                        public void onResponse(Call call, Response response) throws IOException {
                            final String responseBody = response.body() != null ? response.body().string() : "";
                            Log.d("AUTO_CHECK", responseBody);

                            try {
                                JSONObject json = new JSONObject(responseBody);
                                boolean success = json.optBoolean("success", false);
                                String status = json.optString("status");

                                if (success && "verified".equalsIgnoreCase(status)) {
                                    isEmailVerified = true;
                                    runOnUiThread(() -> {
                                        binding.btnKirimOtp.setEnabled(false);
                                        binding.btnKirimOtp.setText("Terverifikasi");
                                        binding.btnKirimOtp.setBackgroundTintList(
                                                android.content.res.ColorStateList.valueOf(
                                                        getResources().getColor(android.R.color.holo_green_dark)
                                                )
                                        );
                                        binding.btnKirimOtp.setTextColor(
                                                getResources().getColor(android.R.color.white)
                                        );
                                    });
                                    emailCheckHandler.removeCallbacks(emailCheckRunnable);
                                }
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }
                    });

                } catch (Exception e) {
                    e.printStackTrace();
                }

                emailCheckHandler.postDelayed(this, 5000);
            }
        };

        emailCheckHandler.post(emailCheckRunnable);
    }

    private void startOtpCountdown() {

        otpTimer = new CountDownTimer(30000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                if (isEmailVerified) {
                    cancel();
                    return;
                }

                int detik = (int) (millisUntilFinished / 1000);
                binding.btnKirimOtp.setText("Tunggu " + detik + " detik");
            }

            @Override
            public void onFinish() {
                isSendingOtp = false;

                if (!isEmailVerified) {
                    binding.btnKirimOtp.setEnabled(true);
                    binding.btnKirimOtp.setText("Kirim OTP");
                }
            }
        }.start();
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (!isConnected()) {
            GlobalToast.ShowToast(this, "Tidak ada koneksi internet");
            return;
        }

        if (!isEmailVerified && isSendingOtp) {
            startAutoCheckEmail(binding.etEmail.getText().toString().trim());
        }

        if (sessionManager.getRegistrasi() == null) {

        } else if (sessionManager.getRegistrasi().equals("OutletDone")) {
        } else {
            Intent intent = new Intent(RegistrasiToko.this, RegistrasiUser.class);
            startActivity(intent);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (emailCheckHandler != null && emailCheckRunnable != null) {
            emailCheckHandler.removeCallbacks(emailCheckRunnable);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (otpTimer != null) {
            otpTimer.cancel();
        }

        if (emailCheckHandler != null && emailCheckRunnable != null) {
            emailCheckHandler.removeCallbacks(emailCheckRunnable);
        }
    }

    public Bitmap getResizedBitmap(Bitmap image, int maxSize) {
        int width = image.getWidth();
        int height = image.getHeight();

        float bitmapRatio = (float) width / (float) height;
        if (bitmapRatio > 1) {
            width = maxSize;
            height = (int) (width / bitmapRatio);
        } else {
            height = maxSize;
            width = (int) (height * bitmapRatio);
        }
        return Bitmap.createScaledBitmap(image, width, height, true);
    }

    private void setToImageViewLogo(Bitmap bmp) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        bmp.compress(Bitmap.CompressFormat.PNG, 60, bytes);
        decodedLogo = BitmapFactory.decodeStream(new ByteArrayInputStream(bytes.toByteArray()));

        binding.ivUploadLogo.setTag("done");
        binding.ivUploadLogo.setImageBitmap(decodedLogo);
        binding.ivUploadLogo.setBackgroundResource(R.drawable.rounded_imageview);
        binding.icIconCamera.setVisibility(View.GONE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK && requestCode == RESULT_LOAD_IMG && data != null && data.getData() != null) {
            try {
                final Uri imageUri = data.getData();
                final InputStream imageStream = getContentResolver().openInputStream(imageUri);
                bitmap = BitmapFactory.decodeStream(imageStream);
                setToImageViewLogo(getResizedBitmap(bitmap, 512));
                binding.tvUploadLogo.setVisibility(View.GONE);
                binding.tvUploadUlangFotoLogo.setVisibility(View.VISIBLE);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
                GlobalToast.ShowToast(RegistrasiToko.this, "Kesalahan Saat Reload Gambar");
            }

        }

    }

    private void checkEmailVerified(String email) {
        pDialog.show();

        String url = "https://siqasir.awandigital.id/api/otp/check-email";

        try {
            okhttp3.FormBody body = new okhttp3.FormBody.Builder()
                    .add("email", email)
                    .build();

            okhttp3.Request request = new okhttp3.Request.Builder()
                    .url(url)
                    .post(body)
                    .build();

            okHttpClient.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Log.e("OTP_CHECK", e.toString());
                    runOnUiThread(() -> {
                        pDialog.dismiss();
                        GlobalToast.ShowToast(RegistrasiToko.this, "Gagal cek status email (Koneksi)");
                    });
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    final String responseBody = response.body() != null ? response.body().string() : "";
                    Log.d("OTP_CHECK", responseBody);

                    runOnUiThread(() -> {
                        pDialog.dismiss();
                        try {
                            JSONObject json = new JSONObject(responseBody);
                            boolean success = json.optBoolean("success", false);
                            String status = json.optString("status");
                            String message = json.optString("message");

                            GlobalToast.ShowToast(RegistrasiToko.this, message);

                            if (success && "verified".equalsIgnoreCase(status)) {
                                if (json.has("exp_date") && !json.isNull("exp_date")) {
                                    expDateFromApi = json.getString("exp_date");
                                } else {
                                    expDateFromApi = null;
                                }
                                Log.d("EXP_DEBUG", "expDateFromApi: " + expDateFromApi);
                                InsertRegistrasiTokoSQLite();
                            }

                        } catch (JSONException e) {
                            e.printStackTrace();
                            GlobalToast.ShowToast(RegistrasiToko.this, "Response tidak valid");
                        }
                    });
                }
            });

        } catch (Exception e) {
            e.printStackTrace();
            pDialog.dismiss();
        }
    }

    private void activateEmailApi(String email) {

        String url = "https://siqasir.awandigital.id/api/otp/activate";

        try {
            okhttp3.FormBody body = new okhttp3.FormBody.Builder()
                    .add("email", email)
                    .build();

            okhttp3.Request request = new okhttp3.Request.Builder()
                    .url(url)
                    .post(body)
                    .build();

            okHttpClient.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Log.e("OTP_ACTIVATE", e.toString());
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    final String responseBody = response.body() != null ? response.body().string() : "";
                    Log.d("OTP_ACTIVATE", responseBody);

                    runOnUiThread(() -> {
                        try {
                            JSONObject json = new JSONObject(responseBody);
                            String message = json.optString("message");
                            GlobalToast.ShowToast(RegistrasiToko.this, message);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    });
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void InsertRegistrasiTokoSQLite() {

        pDialog.show();

        String KodeToko = dbHelper.generateKodeToko();
        String NamaToko = binding.etNamaToko.getText().toString().trim();
        String Kota     = binding.etKota.getText().toString().trim();
        String Provinsi = binding.etProvinsi.getText().toString().trim();
        String Email    = binding.etEmail.getText().toString().trim();
        String NoHp     = binding.etNoHp.getText().toString().trim();
        String Alamat   = binding.etAlamat.getText().toString().trim();
        String inputJenisUsaha = binding.etJenisUsaha.getText().toString().trim();

        if (NamaToko.equals("") ||
                Kota.equals("") ||
                Provinsi.equals("") ||
                Email.equals("") ||
                NoHp.equals("")) {

            GlobalToast.ShowToast(this, "Data Tidak Boleh Kosong");
            pDialog.dismiss();
            return;
        }

        if (inputJenisUsaha.equals("")) {
            GlobalToast.ShowToast(this, "Jenis usaha tidak boleh kosong");
            pDialog.dismiss();
            return;
        }

        ContentValues values = new ContentValues();
        values.put(DataBaseHelper.tb_toko_kode_toko, KodeToko);
        values.put(DataBaseHelper.tb_toko_nama_toko, NamaToko);
        values.put(DataBaseHelper.tb_toko_kota, Kota);
        values.put(DataBaseHelper.tb_toko_provinsi, Provinsi);
        values.put(DataBaseHelper.tb_toko_email, Email);
        values.put(DataBaseHelper.tb_toko_no_telepon, NoHp);

        if (expDateFromApi != null
                && !expDateFromApi.trim().isEmpty()
                && !expDateFromApi.equalsIgnoreCase("null")) {

            values.put("exp_date", expDateFromApi);
        }

        String idJenisUsaha;
        String namaJenisUsaha;

        if (selectedJenisUsaha != null &&
                selectedJenisUsaha.nama != null &&
                selectedJenisUsaha.nama.equalsIgnoreCase(inputJenisUsaha)) {

            idJenisUsaha = selectedJenisUsaha.id;
            namaJenisUsaha = selectedJenisUsaha.nama;

        } else {
            idJenisUsaha = "100";
            namaJenisUsaha = inputJenisUsaha;
        }

        values.put("id_jenis_usaha", idJenisUsaha);
        values.put("nama_jenis_usaha", namaJenisUsaha);

        if (binding.ivUploadLogo.getTag() == null ||
                binding.ivUploadLogo.getTag().toString().equalsIgnoreCase("kosong")) {
            binding.ivUploadLogo.setImageResource(R.drawable.outlet_kosong);
        }

        values.put(DataBaseHelper.tb_toko_logo, imageViewToByte(binding.ivUploadLogo));

        try {

            long idToko = dbHelper.insertDataToko(values);

            if (!Alamat.equals("")) {
                ContentValues cvStruk = new ContentValues();
                cvStruk.put("id_toko", idToko);
                cvStruk.put("alamat_toko", Alamat);
                dbHelper.insertSettingStruk(cvStruk);
            }

            sessionManager.setNamatoko(NamaToko);
            sessionManager.setEmailToko(Email);
            sessionManager.setRegistrasi("OutletDone");

            activateEmailApi(Email);

            GlobalToast.ShowToast(this, "Registrasi Toko Berhasil Tersimpan.");
            pDialog.dismiss();

            Intent intent = new Intent(RegistrasiToko.this, RegistrasiUser.class);
            startActivity(intent);
            finishAffinity();

        } catch (Exception e) {
            e.printStackTrace();
            pDialog.dismiss();
        }
    }

    private static byte[] imageViewToByte(ImageView image) {

        if (image.getDrawable() == null) {
            image.setImageResource(R.drawable.outlet_kosong);
        }

        Bitmap bitmap = ((BitmapDrawable) image.getDrawable()).getBitmap();
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
        return stream.toByteArray();
    }

    private void showFileChooser() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select Picture"), RESULT_LOAD_IMG);
    }

    private List<JenisUsahaModel> loadJenisUsahaFromAssets() {
        List<JenisUsahaModel> list = new ArrayList<>();
        try {
            InputStream is = getAssets().open("jenis-usaha.json");
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();

            String json = new String(buffer, "UTF-8");
            JSONArray array = new JSONArray(json);

            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);

                JenisUsahaModel m = new JenisUsahaModel();
                m.id = obj.getString("id");
                m.nama = obj.getString("nama");

                list.add(m);
            }

            Log.d("JENIS_USAHA", "Total data: " + list.size());

        } catch (Exception e) {
            Log.e("JENIS_USAHA", "Gagal load jenis usaha", e);
        }
        return list;
    }
}