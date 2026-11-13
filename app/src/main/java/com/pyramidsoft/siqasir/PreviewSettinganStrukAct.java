package com.pyramidsoft.siqasir;

import static com.pyramidsoft.siqasir.db.DataBaseHelper.database_name;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.pyramidsoft.siqasir.databinding.ActivityPreviewSettinganStrukBinding;
import com.pyramidsoft.siqasir.db.DataBaseHelper;
import com.pyramidsoft.siqasir.utils.SessionManager;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class PreviewSettinganStrukAct extends AppCompatActivity {

    private ActivityPreviewSettinganStrukBinding binding;
    SessionManager sessionManager;
    DataBaseHelper dbHelper;
    SQLiteDatabase mDatabase;
    Calendar c = Calendar.getInstance();
    String invoice, formattedDate;
    File file;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityPreviewSettinganStrukBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setSupportActionBar(binding.toolbar);

        sessionManager = new SessionManager(PreviewSettinganStrukAct.this);
        dbHelper = new DataBaseHelper(PreviewSettinganStrukAct.this);
        mDatabase = openOrCreateDatabase(database_name, MODE_PRIVATE, null);
        SimpleDateFormat df = new SimpleDateFormat("ddMMMMyyyy", new Locale("id", "ID"));
        formattedDate = df.format(c.getTime());

        binding.IvClose.setOnClickListener(this::onViewClicked);

        getDataToko();
        getDataStruk();

    }

    private void getDataToko() {
        String kodePegawai = sessionManager.getKodeUsers();
        String idToko = dbHelper.getIdTokoByKodePegawai(kodePegawai);
        Cursor cursor = dbHelper.dataToko(idToko);

        if (cursor.moveToFirst()) {
            String namaToko = cursor.getString(cursor.getColumnIndexOrThrow(DataBaseHelper.tb_toko_nama_toko));

            byte[] ivLogoStrukByte = cursor.getBlob(6);
            if (ivLogoStrukByte == null) {
            } else {
                Bitmap bitmap2 = BitmapFactory.decodeByteArray(ivLogoStrukByte, 0, ivLogoStrukByte.length);
                binding.ivLogoStruk.setImageBitmap(bitmap2);
            }

            String noTelp = cursor.getString(cursor.getColumnIndexOrThrow(DataBaseHelper.tb_toko_no_telepon));

            binding.tvNamaOutlet.setText(namaToko);
            binding.tvNoHp.setText(noTelp);
        }

    }

    private void setTextOrHide(TextView tv, String value) {
        if (value != null && !value.trim().isEmpty()) {
            tv.setText(value);
            tv.setVisibility(View.VISIBLE);
        } else {
            tv.setVisibility(View.GONE);
        }
    }

    private void getDataStruk() {
        String kodePegawai = sessionManager.getKodeUsers();
        String idToko = dbHelper.getIdTokoByKodePegawai(kodePegawai);
        Cursor cursor = dbHelper.detailStruk(idToko);

        if (cursor != null && cursor.moveToFirst()) {

            String social_media = cursor.getString(
                    cursor.getColumnIndexOrThrow(DataBaseHelper.socialmedia_struk));

            String footer = cursor.getString(
                    cursor.getColumnIndexOrThrow(DataBaseHelper.footer));

            String alamat_toko = cursor.getString(
                    cursor.getColumnIndexOrThrow(DataBaseHelper.alamat_struk));

            setTextOrHide(binding.tvSocialMedia, social_media);
            setTextOrHide(binding.tvFooter, footer);
            setTextOrHide(binding.tvAlamat, alamat_toko);
        }

    }

    public void onViewClicked(View view) {
        if (view.getId() == R.id.IvClose) {
            finish();
        } else {
        }
    }

}
