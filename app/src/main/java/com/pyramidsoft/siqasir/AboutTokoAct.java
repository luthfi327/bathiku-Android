package com.pyramidsoft.siqasir;

import static com.pyramidsoft.siqasir.db.DataBaseHelper.database_name;
import static com.pyramidsoft.siqasir.utils.AppConf.URL_UPDATE_TOKO;

import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.util.Base64;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.pyramidsoft.siqasir.databinding.ActivityAboutTokoBinding;
import com.pyramidsoft.siqasir.db.DataBaseHelper;
import com.pyramidsoft.siqasir.utils.AndLog;
import com.pyramidsoft.siqasir.utils.GlobalToast;
import com.pyramidsoft.siqasir.utils.ProgressDialog;
import com.pyramidsoft.siqasir.utils.SessionManager;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class AboutTokoAct extends AppCompatActivity {

    private ActivityAboutTokoBinding binding;
    SessionManager sessionManager;
    ProgressDialog pDialog;
    DataBaseHelper dbHelper;
    SQLiteDatabase mDatabase;

    private RequestQueue requestQueue;
    private StringRequest stringRequest;
    private static final int RESULT_LOAD_IMG = 1002, RESULT_LOAD_IMG_2 = 1003;

    Bitmap bitmap, decodedLogo, decodedLogoStruk;
    int bitmap_size = 50;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAboutTokoBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        Window window = getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);

        setSupportActionBar(binding.toolbar);
        ActionBar actionBar = getSupportActionBar();

        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        pDialog = new ProgressDialog(AboutTokoAct.this);
        sessionManager = new SessionManager(AboutTokoAct.this);
        dbHelper = new DataBaseHelper(AboutTokoAct.this);
        mDatabase = openOrCreateDatabase(database_name, MODE_PRIVATE, null);
        requestQueue = Volley.newRequestQueue(AboutTokoAct.this);

        getDataToko();

        modeActivity(false);

        binding.btUpdate.setVisibility(View.VISIBLE);
        binding.btSimpan.setVisibility(View.GONE);

        binding.tvUploadUlangFotoLogo.setOnClickListener(this::onViewClicked);
        binding.ivUploadLogo.setOnClickListener(this::onViewClicked);
        binding.tvUploadUlangLogoStruk.setOnClickListener(this::onViewClicked);
        binding.ivLogoStruk.setOnClickListener(this::onViewClicked);
        binding.btSimpan.setOnClickListener(this::onViewClicked);
        binding.btUpdate.setOnClickListener(this::onViewClicked);
    }

    private void modeActivity(boolean status) {
        binding.etKota.setEnabled(status);
        binding.etNamaToko.setEnabled(status);
        binding.etKecamatan.setEnabled(status);
        binding.etProvinsi.setEnabled(status);
        binding.etEmail.setEnabled(status);
        binding.etNoHp.setEnabled(status);
    }

    private void getDataToko() {
        String kodePegawai = sessionManager.getKodeUsers();
        String idToko = dbHelper.getIdTokoByKodePegawai(kodePegawai);
        Cursor cursor = dbHelper.dataToko(idToko);

        if (cursor.moveToFirst()) {
            String kodeToko = cursor.getString(cursor.getColumnIndexOrThrow(DataBaseHelper.tb_toko_kode_toko));
            String namaToko = cursor.getString(cursor.getColumnIndexOrThrow(DataBaseHelper.tb_toko_nama_toko));
            String kota = cursor.getString(cursor.getColumnIndexOrThrow(DataBaseHelper.tb_toko_kota));
            String provinsi = cursor.getString(cursor.getColumnIndexOrThrow(DataBaseHelper.tb_toko_provinsi));

            byte[] ivLogoByte = cursor.getBlob(5);
            if (ivLogoByte != null) {
                Bitmap bitmap = BitmapFactory.decodeByteArray(ivLogoByte, 0, ivLogoByte.length);
                binding.ivUploadLogo.setImageBitmap(bitmap);
                binding.icIconCamera.setVisibility(View.GONE);
                binding.ivUploadLogo.setBackgroundResource(R.drawable.rounded_imgview);
            }

            byte[] ivLogoStrukByte = cursor.getBlob(6);
            if (ivLogoStrukByte == null) {
                binding.icIconCamera.setVisibility(View.VISIBLE);
                binding.tvLogoStruk.setVisibility(View.VISIBLE);
                binding.tvUploadUlangLogoStruk.setVisibility(View.GONE);
            } else {
                binding.tvUploadUlangLogoStruk.setVisibility(View.VISIBLE);
                binding.tvLogoStruk.setVisibility(View.GONE);
                Bitmap bitmap2 = BitmapFactory.decodeByteArray(ivLogoStrukByte, 0, ivLogoStrukByte.length);
                binding.ivLogoStruk.setImageBitmap(bitmap2);
                binding.icIconCamera2.setVisibility(View.GONE);
                binding.ivLogoStruk.setBackgroundResource(R.drawable.rounded_imgview);
            }

            String email = cursor.getString(cursor.getColumnIndexOrThrow(DataBaseHelper.tb_toko_email));
            String noTelp = cursor.getString(cursor.getColumnIndexOrThrow(DataBaseHelper.tb_toko_no_telepon));

            binding.etKodeToko.setText(kodeToko);
            binding.etNamaToko.setText(namaToko);
            binding.etKota.setText(kota);
            binding.etProvinsi.setText(provinsi);
            binding.etEmail.setText(email);
            binding.etNoHp.setText(noTelp);
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

        binding.ivUploadLogo.setImageBitmap(decodedLogo);
        binding.ivUploadLogo.setBackgroundResource(R.drawable.rounded_imgview);
        binding.icIconCamera.setVisibility(View.GONE);
    }

    private void setToImageViewLogoStruk(Bitmap bmp) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        bmp.compress(Bitmap.CompressFormat.PNG, 60, bytes);
        decodedLogoStruk = BitmapFactory.decodeStream(new ByteArrayInputStream(bytes.toByteArray()));

        binding.ivLogoStruk.setImageBitmap(decodedLogoStruk);
        binding.ivLogoStruk.setBackgroundResource(R.drawable.rounded_imgview);
        binding.icIconCamera2.setVisibility(View.GONE);
    }

    public String getStringImage(Bitmap bmp) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bmp.compress(Bitmap.CompressFormat.PNG, bitmap_size, baos);
        byte[] imageBytes = baos.toByteArray();
        String encodedImage = android.util.Base64.encodeToString(imageBytes, Base64.DEFAULT);
        return encodedImage;
    }

    private void updateDataToko() {
        pDialog.show();

        String KodeToko = binding.etKodeToko.getText().toString().trim();
        String NamaToko = binding.etNamaToko.getText().toString().trim();
        String Kota = binding.etKota.getText().toString().trim();
        String Provinsi = binding.etProvinsi.getText().toString().trim();
        String Email = binding.etEmail.getText().toString().trim();
        String NoHp = binding.etNoHp.getText().toString().trim();

        ContentValues values = new ContentValues();
        values.put(DataBaseHelper.tb_toko_kode_toko, KodeToko);
        values.put(DataBaseHelper.tb_toko_nama_toko, NamaToko);
        values.put(DataBaseHelper.tb_toko_kota, Kota);
        values.put(DataBaseHelper.tb_toko_provinsi, Provinsi);
        values.put(DataBaseHelper.tb_toko_email, Email);
        values.put(DataBaseHelper.tb_toko_no_telepon, NoHp);

        if (!NamaToko.isEmpty() && !Kota.isEmpty() && !Provinsi.isEmpty() && !Email.isEmpty() && !NoHp.isEmpty()) {
            try {
                // pakai kode_toko untuk whereClause
                dbHelper.updateToko(values, KodeToko);

                GlobalToast.ShowToast(this, "Data Berhasil Ter-update");
                pDialog.dismiss();
                modeActivity(false);
            } catch (Exception e) {
                e.printStackTrace();
                pDialog.dismiss();
            }
        } else {
            pDialog.dismiss();
            GlobalToast.ShowToast(this, "Isi semua data terlebih dahulu");
        }
    }

    private void updateDataTokoMySQL() {
        StringRequest strReq = new StringRequest(Request.Method.POST, URL_UPDATE_TOKO, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                AndLog.ShowLog("req-toko: ", response);
                pDialog.dismiss();
                modeActivity(false);
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                GlobalToast.ShowToast(AboutTokoAct.this, "Ada Kesalahan Insert Cloud");

                AndLog.ShowLog("VolleyError:", String.valueOf(error));
                modeActivity(false);
                pDialog.dismiss();
            }
        }) {

            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<String, String>();

                params.put("kode_toko", binding.etKodeToko.getText().toString());
                params.put("nama_toko", binding.etNamaToko.getText().toString());
                params.put("kota", binding.etKota.getText().toString());
                params.put("provinsi", binding.etProvinsi.getText().toString());
                params.put("email", binding.etEmail.getText().toString());
                params.put("no_telepon", binding.etNoHp.getText().toString());

                if (decodedLogo != null) {
                    params.put("logo", getStringImage(decodedLogo));
                }

                if (decodedLogoStruk != null) {
                    params.put("logo_struk", getStringImage(decodedLogoStruk));
                }

                AndLog.ShowLog("paramsRegToko", String.valueOf(params));
                return params;
            }

        };
        requestQueue.add(strReq);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK & requestCode == RESULT_LOAD_IMG && data != null && data.getData() != null) {
            try {
                final Uri imageUri = data.getData();
                final InputStream imageStream = getContentResolver().openInputStream(imageUri);
                bitmap = BitmapFactory.decodeStream(imageStream);
                setToImageViewLogo(getResizedBitmap(bitmap, 512));
                ContentValues values = new ContentValues();
                values.put(DataBaseHelper.tb_toko_logo, imageViewToByte(binding.ivUploadLogo));
                dbHelper.updateToko(values, binding.etKodeToko.getText().toString());
                GlobalToast.ShowToast(this, "Foto Berhasil di Edit");
            } catch (FileNotFoundException e) {
                e.printStackTrace();
                GlobalToast.ShowToast(AboutTokoAct.this, "Kesalahan Saat Reload Gambar");
            }

        }

        if (resultCode == RESULT_OK & requestCode == RESULT_LOAD_IMG_2 && data != null && data.getData() != null) {
            try {
                final Uri imageUri = data.getData();
                final InputStream imageStream = getContentResolver().openInputStream(imageUri);
                bitmap = BitmapFactory.decodeStream(imageStream);
                setToImageViewLogoStruk(getResizedBitmap(bitmap, 512));
                ContentValues values = new ContentValues();
                values.put(DataBaseHelper.tb_struk_logo, imageViewToByte(binding.ivLogoStruk));
                dbHelper.updateToko(values, binding.etKodeToko.getText().toString());
            } catch (FileNotFoundException e) {
                e.printStackTrace();
                GlobalToast.ShowToast(AboutTokoAct.this, "Kesalahan Saat Memuat Ulang Gambar");
            }

        }
    }

    private static byte[] imageViewToByte(ImageView image) {

        Bitmap bitmap = ((BitmapDrawable) image.getDrawable()).getBitmap();
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
        byte[] byteArray = stream.toByteArray();
        return byteArray;
    }

    private void showFileChooser() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select Picture"), RESULT_LOAD_IMG);
    }

    private void showFileChooserLogoStruk() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select Picture"), RESULT_LOAD_IMG_2);
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
        if (view.getId() == R.id.tvUploadUlangFotoLogo || view.getId() == R.id.ivUploadLogo) {
            showFileChooser();
        } else if (view.getId() == R.id.tvUploadUlangLogoStruk || view.getId() == R.id.ivLogoStruk) {
            showFileChooserLogoStruk();
        } else if (view.getId() == R.id.btSimpan) {
            updateDataToko();
        } else if (view.getId() == R.id.btUpdate) {
            binding.btUpdate.setVisibility(View.GONE);
            binding.btSimpan.setVisibility(View.VISIBLE);
            modeActivity(true);
        }
    }


}
