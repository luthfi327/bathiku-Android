package com.pyramidsoft.siqasir;

import static com.pyramidsoft.siqasir.db.DataBaseHelper.database_name;

import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import com.pyramidsoft.siqasir.databinding.ActivitySettingHalamanStrukBinding;
import com.pyramidsoft.siqasir.db.DataBaseHelper;
import com.pyramidsoft.siqasir.utils.AndLog;
import com.pyramidsoft.siqasir.utils.GlobalToast;
import com.pyramidsoft.siqasir.utils.ProgressDialog;
import com.pyramidsoft.siqasir.utils.SessionManager;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

public class SettingHalamanStrukAct extends AppCompatActivity {

    private ActivitySettingHalamanStrukBinding binding;
    ProgressDialog pDialog;
    SessionManager sessionManager;
    DataBaseHelper dbHelper;
    SQLiteDatabase mDatabase;
    private static final int RESULT_LOAD_IMG_2 = 1003;
    Bitmap bitmap, decodedLogo, decodedLogoStruk;
    int bitmap_size = 50;
    private String id_setting_struk;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySettingHalamanStrukBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setSupportActionBar(binding.toolbar);
        ActionBar actionBar = getSupportActionBar();

        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        pDialog = new ProgressDialog(SettingHalamanStrukAct.this);
        sessionManager = new SessionManager(SettingHalamanStrukAct.this);
        dbHelper = new DataBaseHelper(SettingHalamanStrukAct.this);
        mDatabase = openOrCreateDatabase(database_name, MODE_PRIVATE, null);

        binding.tvLihatTampilan.setOnClickListener(this::onViewClicked);
        binding.tvLogoStruk.setOnClickListener(this::onViewClicked);
        binding.tvUploadUlangLogoStruk.setOnClickListener(this::onViewClicked);
        binding.btSimpan.setOnClickListener(this::onViewClicked);
        binding.btBukaAkses.setOnClickListener(this::onViewClicked);
        binding.relImg.setOnClickListener(this::onViewClicked);
        binding.btUpdate.setOnClickListener(this::onViewClicked);

        getDataToko();
        getDataStruk();

        setDefaultActivity(false);
        binding.btBukaAkses.setVisibility(View.VISIBLE);
        binding.btSimpan.setVisibility(View.GONE);
    }

    private void setDefaultActivity(boolean status) {
        binding.etNamaToko.setEnabled(status);
        binding.etAlamat.setEnabled(status);
        binding.etNoHp.setEnabled(status);
        binding.etSocialMedia.setEnabled(status);
        binding.etFooter.setEnabled(status);
        binding.tvLogoStruk.setEnabled(status);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK & requestCode == RESULT_LOAD_IMG_2 && data != null && data.getData() != null) {
            try {
                final Uri imageUri = data.getData();
                final InputStream imageStream = getContentResolver().openInputStream(imageUri);
                String kodeToko = binding.etKodeToko.getText().toString().trim();
                bitmap = BitmapFactory.decodeStream(imageStream);
                setToImageViewLogoStruk(getResizedBitmap(bitmap, 512));
                ContentValues values = new ContentValues();
                values.put(DataBaseHelper.tb_struk_logo, imageViewToByte(binding.ivLogoStruk));
                dbHelper.updateToko(values, kodeToko);

                binding.tvLogoStruk.setVisibility(View.GONE);
                binding.tvUploadUlangLogoStruk.setVisibility(View.VISIBLE);

                GlobalToast.ShowToast(this, "Foto Berhasil di Edit");
            } catch (FileNotFoundException e) {
                e.printStackTrace();
                GlobalToast.ShowToast(SettingHalamanStrukAct.this, "Kesalahan Saat Reload Gambar");
            }
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

    private void setToImageViewLogoStruk(Bitmap bmp) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        bmp.compress(Bitmap.CompressFormat.PNG, 60, bytes);
        decodedLogoStruk = BitmapFactory.decodeStream(new ByteArrayInputStream(bytes.toByteArray()));

        binding.ivLogoStruk.setImageBitmap(decodedLogoStruk);
        binding.ivLogoStruk.setBackgroundResource(R.drawable.rounded_imgview);
        binding.icIconCamera.setVisibility(View.GONE);
    }

    private void getDataToko() {
        String kodePegawai = sessionManager.getKodeUsers();
        String idToko = dbHelper.getIdTokoByKodePegawai(kodePegawai);
        Cursor cursor = dbHelper.dataToko(idToko);
        if (cursor.moveToFirst()) {
            String namaToko = cursor.getString(cursor.getColumnIndexOrThrow(DataBaseHelper.tb_toko_nama_toko));

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
                binding.icIconCamera.setVisibility(View.GONE);
                binding.ivLogoStruk.setBackgroundResource(R.drawable.rounded_imgview);
            }

            String noTelp = cursor.getString(cursor.getColumnIndexOrThrow(DataBaseHelper.tb_toko_no_telepon));
            String kodeToko = cursor.getString(cursor.getColumnIndexOrThrow(DataBaseHelper.tb_toko_kode_toko));

            binding.etNamaToko.setText(namaToko);
            binding.etNoHp.setText(noTelp);
            binding.etKodeToko.setText(kodeToko);
        }

    }

    private void getDataStruk() {
        String kodePegawai = sessionManager.getKodeUsers();
        String idToko = dbHelper.getIdTokoByKodePegawai(kodePegawai);
        Cursor cursor = dbHelper.detailStruk(idToko);

        if (cursor.moveToFirst()) {
            id_setting_struk = cursor.getString(cursor.getColumnIndexOrThrow("id_setting_struk"));
            String social_media = cursor.getString(cursor.getColumnIndexOrThrow(DataBaseHelper.socialmedia_struk));
            String footer = cursor.getString(cursor.getColumnIndexOrThrow(DataBaseHelper.footer));
            String alamat_toko = cursor.getString(cursor.getColumnIndexOrThrow(DataBaseHelper.alamat_struk));

            binding.etSocialMedia.setText(social_media);
            binding.etFooter.setText(footer);
            binding.etAlamat.setText(alamat_toko);
        }

    }

    private void simpanData() {

        pDialog.show();

        String socialmedia = binding.etSocialMedia.getText().toString().trim();
        String alamatToko = binding.etAlamat.getText().toString().trim();
        String footer = binding.etFooter.getText().toString().trim();

        String Email = sessionManager.getEmailToko();
        String NamaToko = binding.etNamaToko.getText().toString().trim();
        String NoHp = binding.etNoHp.getText().toString().trim();

        String kodePegawai = sessionManager.getKodeUsers();
        String idToko = dbHelper.getIdTokoByKodePegawai(kodePegawai);

        ContentValues vStruk = new ContentValues();
        vStruk.put(DataBaseHelper.socialmedia_struk, socialmedia);
        vStruk.put(DataBaseHelper.alamat_struk, alamatToko);
        vStruk.put(DataBaseHelper.footer, footer);
        vStruk.put(DataBaseHelper.id_toko, idToko);

        ContentValues vToko = new ContentValues();
        vToko.put(DataBaseHelper.tb_toko_email, Email);
        vToko.put(DataBaseHelper.tb_toko_nama_toko, NamaToko);
        vToko.put(DataBaseHelper.tb_toko_no_telepon, NoHp);

        if (
                socialmedia.equals("") ||
                        alamatToko.equals("") ||
                        footer.equals("") ||
                        Email.equals("") ||
                        NoHp.equals("")) {

            GlobalToast.ShowToast(this, "Data Tidak Boleh Kosong");

        } else {
            try {
                dbHelper.insertSettingStruk(vStruk);
                AndLog.ShowLog("vStruk", String.valueOf(vStruk));

                dbHelper.updateToko(vToko, Email);

                GlobalToast.ShowToast(this, "Data Berhasil Tersimpan");
                pDialog.dismiss();

                setDefaultActivity(false);
                binding.btBukaAkses.setVisibility(View.VISIBLE);
                binding.btSimpan.setVisibility(View.GONE);
            } catch (Exception e) {
                e.printStackTrace();
            }

        }
    }

    private void updateData() {

        pDialog.show();

        String socialmedia = binding.etSocialMedia.getText().toString().trim();
        String alamatToko = binding.etAlamat.getText().toString().trim();
        String footer = binding.etFooter.getText().toString().trim();

        String email = sessionManager.getEmailToko();
        String kodeToko = binding.etKodeToko.getText().toString().trim();
        String NamaToko = binding.etNamaToko.getText().toString().trim();
        String NoHp = binding.etNoHp.getText().toString().trim();

        String kodePegawai = sessionManager.getKodeUsers();
        String idToko = dbHelper.getIdTokoByKodePegawai(kodePegawai);

        ContentValues vStruk = new ContentValues();
        vStruk.put(DataBaseHelper.socialmedia_struk, socialmedia);
        vStruk.put(DataBaseHelper.alamat_struk, alamatToko);
        vStruk.put(DataBaseHelper.footer, footer);
        vStruk.put(DataBaseHelper.id_toko, idToko);

        ContentValues vToko = new ContentValues();
        vToko.put(DataBaseHelper.tb_toko_email, email);
        vToko.put(DataBaseHelper.tb_toko_nama_toko, NamaToko);
        vToko.put(DataBaseHelper.tb_toko_no_telepon, NoHp);
        vToko.put(DataBaseHelper.tb_struk_logo, imageViewToByte(binding.ivLogoStruk));

        if (socialmedia.equals("") || alamatToko.equals("") || footer.equals("")) {
            GlobalToast.ShowToast(this, "Data Tidak Boleh Kosong");
        } else {
            try {

                dbHelper.updateStruk(vStruk, id_setting_struk);
                dbHelper.updateToko(vToko, kodeToko);

                GlobalToast.ShowToast(this, "Data Berhasil Ter-Update.");
                pDialog.dismiss();

                setDefaultActivity(false);
                binding.btBukaAkses.setVisibility(View.VISIBLE);
                binding.btUpdate.setVisibility(View.GONE);

            } catch (Exception e) {
                e.printStackTrace();
            }

        }
    }

    private static byte[] imageViewToByte(ImageView image) {
        if (image.getDrawable() == null) {
            return null;
        }

        Bitmap bitmap = ((BitmapDrawable) image.getDrawable()).getBitmap();
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
        return stream.toByteArray();
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

    private void showFileChooserLogoStruk() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select Picture"), RESULT_LOAD_IMG_2);
    }

    public void onViewClicked(View view) {
        if (view.getId() == R.id.tvLihatTampilan) {
            Intent intent = new Intent(SettingHalamanStrukAct.this, PreviewSettinganStrukAct.class);
            startActivity(intent);
        } else if (view.getId() == R.id.tvUploadUlangLogoStruk || view.getId() == R.id.tvLogoStruk || view.getId() == R.id.rel_img) {
            showFileChooserLogoStruk();
        } else if (view.getId() == R.id.btSimpan) {
            simpanData();
        } else if (view.getId() == R.id.btUpdate) {
            if (id_setting_struk != null) {
                updateData();
            }
        } else if (view.getId() == R.id.btBukaAkses) {
            if (id_setting_struk != null) {
                setDefaultActivity(true);
                binding.btBukaAkses.setVisibility(View.GONE);
                binding.btUpdate.setVisibility(View.VISIBLE);
                binding.btSimpan.setVisibility(View.GONE);
                binding.btUpdate.setText("Update Data");
            } else {
                setDefaultActivity(true);
                binding.btBukaAkses.setVisibility(View.GONE);
                binding.btUpdate.setVisibility(View.GONE);
                binding.btSimpan.setVisibility(View.VISIBLE);
                binding.btSimpan.setText("Simpan Data");
            }
        }
    }

}
