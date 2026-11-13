package com.pyramidsoft.siqasir;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.appcompat.widget.Toolbar;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.material.textfield.TextInputEditText;
import com.pyramidsoft.siqasir.db.DataBaseHelper;
import com.pyramidsoft.siqasir.utils.SessionManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TambahOutletActivity extends AppCompatActivity {

    private static final int RESULT_LOAD_IMG = 1002;

    private TextInputEditText etNamaToko, etEmail, etNoHp;
    private AutoCompleteTextView etKota, etProvinsi;
    private AppCompatButton btUpdate;
    private ImageView ivUploadLogo;
    private DataBaseHelper dbHelper;
    private SessionManager sessionManager;
    private RequestQueue requestQueue;
    private Bitmap bitmap, decodedLogo;
    private String prov_id, provinsi;
    private int editOutletId = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tambah_outlet);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("Tambah Outlet");
        toolbar.setNavigationOnClickListener(v -> finish());

        dbHelper = new DataBaseHelper(this);
        sessionManager = new SessionManager(this);
        requestQueue = Volley.newRequestQueue(this);

        ivUploadLogo = findViewById(R.id.ivUploadLogo);
        etNamaToko = findViewById(R.id.etNamaToko);
        etKota = findViewById(R.id.etKota);
        etProvinsi = findViewById(R.id.etProvinsi);
        etEmail = findViewById(R.id.etEmail);
        etNoHp = findViewById(R.id.etNoHp);
        btUpdate = findViewById(R.id.btUpdate);

        ivUploadLogo.setTag("kosong");
        ivUploadLogo.setOnClickListener(v -> showFileChooser());

        btUpdate.setOnClickListener(v -> simpanOutlet());

        if (getIntent().hasExtra("edit_outlet_id")) {
            editOutletId = getIntent().getIntExtra("edit_outlet_id", -1);
            if (editOutletId != -1) {
                loadOutlet(editOutletId);
                getSupportActionBar().setTitle("Edit Outlet");
                btUpdate.setText("Update");
            }
        }

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            etProvinsi.setImportantForAutofill(View.IMPORTANT_FOR_AUTOFILL_NO);
            etProvinsi.setAutofillHints((String) null);
            etKota.setImportantForAutofill(View.IMPORTANT_FOR_AUTOFILL_NO);
            etKota.setAutofillHints((String) null);
            etEmail.setImportantForAutofill(View.IMPORTANT_FOR_AUTOFILL_NO);
            etEmail.setAutofillHints((String) null);
        }

        setEmailFromOwner();

        if (isConnected()) {
            getProvinsi();
        } else {
            Toast.makeText(this, "Sinyal Lemah / Tidak Ada Koneksi Internet", Toast.LENGTH_SHORT).show();
        }
    }

    private void setEmailFromOwner() {

        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT email FROM tb_toko WHERE id_toko = 1 LIMIT 1",
                null
        );

        if (cursor != null && cursor.moveToFirst()) {

            String emailOwner = cursor.getString(
                    cursor.getColumnIndexOrThrow("email")
            );
            etEmail.setText(emailOwner);
            cursor.close();
        }
        db.close();
    }

    private void loadOutlet(int id) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM tb_toko WHERE id_toko = ?", new String[]{String.valueOf(id)});
        if (cursor.moveToFirst()) {
            etNamaToko.setText(cursor.getString(cursor.getColumnIndexOrThrow("nama_toko")));
            etKota.setText(cursor.getString(cursor.getColumnIndexOrThrow("kota")));
            etProvinsi.setText(cursor.getString(cursor.getColumnIndexOrThrow("provinsi")));
            etEmail.setText(cursor.getString(cursor.getColumnIndexOrThrow("email")));
            etNoHp.setText(cursor.getString(cursor.getColumnIndexOrThrow("no_telepon")));

            byte[] logoBytes = cursor.getBlob(cursor.getColumnIndexOrThrow("logo"));
            if (logoBytes != null && logoBytes.length > 0) {
                Bitmap logoBmp = BitmapFactory.decodeByteArray(logoBytes, 0, logoBytes.length);
                ivUploadLogo.setImageBitmap(logoBmp);
                ivUploadLogo.setTag("done");
            }
        }
        cursor.close();
        db.close();
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

                        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                                android.R.layout.simple_list_item_1, provinsiList);
                        etProvinsi.setAdapter(adapter);
                        etProvinsi.setThreshold(1);

                        etProvinsi.setOnItemClickListener((parent, view, position, id) -> {
                            String selectedName = adapter.getItem(position);
                            prov_id = provinsiMap.get(selectedName);
                            provinsi = selectedName;
                            getKota(prov_id);
                        });

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                },
                error -> Log.e("API", "Error: " + error.getMessage())
        );

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

                        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                                android.R.layout.simple_list_item_1, kotaList);
                        etKota.setAdapter(adapter);
                        etKota.setThreshold(1);

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                },
                error -> Log.e("API", "Error: " + error.getMessage())
        );

        requestQueue.add(stringRequest);
    }

    private boolean isConnected() {
        try {
            ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo nInfo = cm.getActiveNetworkInfo();
            return nInfo != null && nInfo.isAvailable() && nInfo.isConnected();
        } catch (Exception e) {
            Log.e("Connectivity Exception", e.getMessage());
        }
        return false;
    }

    private void showFileChooser() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select Picture"), RESULT_LOAD_IMG);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK && requestCode == RESULT_LOAD_IMG && data != null && data.getData() != null) {
            try {
                Uri imageUri = data.getData();
                InputStream imageStream = getContentResolver().openInputStream(imageUri);
                bitmap = BitmapFactory.decodeStream(imageStream);
                setToImageViewLogo(getResizedBitmap(bitmap, 512));
            } catch (FileNotFoundException e) {
                e.printStackTrace();
                Toast.makeText(this, "Kesalahan Saat Memuat Gambar", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void setToImageViewLogo(Bitmap bmp) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        bmp.compress(Bitmap.CompressFormat.PNG, 60, bytes);
        decodedLogo = BitmapFactory.decodeStream(new ByteArrayInputStream(bytes.toByteArray()));
        ivUploadLogo.setTag("done");
        ivUploadLogo.setImageBitmap(decodedLogo);
    }

    public Bitmap getResizedBitmap(Bitmap image, int maxSize) {
        int width = image.getWidth();
        int height = image.getHeight();
        float bitmapRatio = (float) width / height;
        if (bitmapRatio > 1) {
            width = maxSize;
            height = (int) (width / bitmapRatio);
        } else {
            height = maxSize;
            width = (int) (height * bitmapRatio);
        }
        return Bitmap.createScaledBitmap(image, width, height, true);
    }

    private void simpanOutlet() {

        String nama = etNamaToko.getText().toString().trim();
        String kota = etKota.getText().toString().trim();
        String prov = etProvinsi.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String noHp = etNoHp.getText().toString().trim();

        if (nama.isEmpty() || kota.isEmpty() || prov.isEmpty() || email.isEmpty() || noHp.isEmpty()) {
            Toast.makeText(this, "Data tidak boleh kosong", Toast.LENGTH_SHORT).show();
            return;
        }

        ContentValues values = new ContentValues();
        values.put("nama_toko", nama);
        values.put("kota", kota);
        values.put("provinsi", prov);
        values.put("email", email);
        values.put("no_telepon", noHp);

        if (ivUploadLogo.getTag() == null || ivUploadLogo.getTag().toString().equalsIgnoreCase("kosong")) {
            values.put("logo", new byte[]{});
        } else {
            values.put("logo", imageViewToByte(ivUploadLogo));
        }

        boolean result;

        if (editOutletId == -1) {

            SQLiteDatabase db = dbHelper.getReadableDatabase();
            Cursor cursor = db.rawQuery("SELECT id_jenis_usaha, nama_jenis_usaha, exp_date FROM tb_toko ORDER BY id_toko ASC LIMIT 1", null);

            if (cursor != null && cursor.moveToFirst()) {

                String idJenisUsaha = cursor.getString(
                        cursor.getColumnIndexOrThrow("id_jenis_usaha")
                );

                String namaJenisUsaha = cursor.getString(
                        cursor.getColumnIndexOrThrow("nama_jenis_usaha")
                );

                String expDate = cursor.getString(
                        cursor.getColumnIndexOrThrow("exp_date")
                );

                String maxToko = cursor.getString(
                        cursor.getColumnIndexOrThrow("max_toko")
                );

                values.put("id_jenis_usaha", idJenisUsaha);
                values.put("nama_jenis_usaha", namaJenisUsaha);
                values.put("exp_date", expDate);
                values.put("max_toko", maxToko);

                cursor.close();
            }

            values.put("kode_toko", dbHelper.generateKodeToko());

            result = dbHelper.getWritableDatabase().insert("tb_toko", null, values) > 0;

        } else {

            result = dbHelper.updateOutlet(editOutletId, values);
        }

        if (result) {
            Toast.makeText(this,
                    (editOutletId == -1 ? "Outlet berhasil ditambahkan" : "Outlet berhasil diupdate"),
                    Toast.LENGTH_SHORT).show();
            finish();
        } else {
            Toast.makeText(this, "Gagal menyimpan outlet", Toast.LENGTH_SHORT).show();
        }
    }

    private static byte[] imageViewToByte(ImageView image) {
        Bitmap bitmap = ((BitmapDrawable) image.getDrawable()).getBitmap();
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
        return stream.toByteArray();
    }
}
