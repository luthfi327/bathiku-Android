package com.pyramidsoft.siqasir;

import static com.pyramidsoft.siqasir.db.DataBaseHelper.database_name;

import android.app.Dialog;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import com.chivorn.smartmaterialspinner.SmartMaterialSpinner;
import com.pyramidsoft.siqasir.databinding.ActivityProdukTokoBinding;
import com.pyramidsoft.siqasir.db.DataBaseHelper;
import com.pyramidsoft.siqasir.model.KategoriModels;
import com.pyramidsoft.siqasir.utils.AndLog;
import com.pyramidsoft.siqasir.utils.GlobalToast;
import com.pyramidsoft.siqasir.utils.ProgressDialog;
import com.pyramidsoft.siqasir.utils.SessionManager;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class ProdukTokoActivity extends AppCompatActivity {

    private ActivityProdukTokoBinding binding;
    SessionManager sessionManager;
    ProgressDialog pDialog;
    DataBaseHelper dbHelper;
    SQLiteDatabase mDatabase;
    String kategori;

    private SmartMaterialSpinner<KategoriModels> spKategori;
    private SmartMaterialSpinner<String> spKategoriItem;
    private static final int RESULT_LOAD_IMG = 1002;
    private List<String> ListKategori;
    Bitmap bitmap, decodedLogo;
    private String KodeProduk, IkodeProduk;
    String hrgJual = "0";
    int hrgawal = 0;
    int hrgdiskon = 0;
    private boolean isAddingKategori = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityProdukTokoBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        ActionBar actionBar = getSupportActionBar();

        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        setSupportActionBar(binding.toolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        binding.toolbar.setNavigationOnClickListener(v -> onBackPressed());

        pDialog = new ProgressDialog(ProdukTokoActivity.this);
        sessionManager = new SessionManager(ProdukTokoActivity.this);
        dbHelper = new DataBaseHelper(this);
        mDatabase = openOrCreateDatabase(database_name, MODE_PRIVATE, null);

        binding.btSimpan.setOnClickListener(this::onViewClicked);
        binding.btEdit.setOnClickListener(this::onViewClicked);
        binding.tvUploadProduk.setOnClickListener(this::onViewClicked);
        binding.tvUploadUlangProduk.setOnClickListener(this::onViewClicked);
        binding.relImg.setOnClickListener(this::onViewClicked);

        initSpinView();
        KodeProduk = "KDB-" + getTodayDate() + "-" + getRandomString(6);
        AndLog.ShowLog("generateKdProduk: ", KodeProduk);

        IkodeProduk = getIntent().getStringExtra("kode_produk");

        if (getIntent().getStringExtra("kode_produk") != null) {
            getDetailProduk(IkodeProduk);
            binding.btSimpan.setVisibility(View.GONE);
            binding.btEdit.setVisibility(View.VISIBLE);
            AndLog.ShowLog("Ikode_Produk", IkodeProduk);
            binding.icIconCamera.setVisibility(View.GONE);
        } else {
            binding.tvUploadProduk.setVisibility(View.VISIBLE);
            binding.tvUploadUlangProduk.setVisibility(View.GONE);
            binding.btSimpan.setVisibility(View.VISIBLE);
            binding.btEdit.setVisibility(View.GONE);
        }
        binding.etStok.setEnabled(binding.switchStok.isChecked());

        binding.switchStok.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                binding.etStok.setEnabled(true);
                binding.etStok.setVisibility(View.VISIBLE);
            } else {
                binding.etStok.setText("");
                binding.etStok.setEnabled(false);
                binding.etStok.setVisibility(View.GONE);
            }
        });


        binding.etHrgJual.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

                if (s.length() > 0) {
                    hrgJual = String.valueOf(s);
                } else {
                    hrgJual = "0";
                    GlobalToast.ShowToast(ProdukTokoActivity.this, "Harga Jual Tidak Boleh Kosong!");
                    binding.etHrgJual.setText("0");
                }
                hrgJual = binding.etHrgJual.getText().toString();
                hrgawal = Integer.parseInt(binding.etHrgAwal.getText().toString());
                int laba = Integer.parseInt(hrgJual) - hrgawal;
                binding.etLaba.setText(Integer.toString(laba));
                binding.etLaba.setText(Integer.toString(laba));
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (s.length() < 0){
                    binding.etHrgJual.setText("0");
                }
            }
        });

        binding.etHrgDiskon.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() > 0) {
                    hrgawal = Integer.parseInt(binding.etHrgAwal.getText().toString());
                    hrgdiskon = Integer.parseInt(binding.etHrgDiskon.getText().toString());
                    int laba = hrgdiskon - hrgawal;
                    binding.etLaba.setText(Integer.toString(laba));
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (s.length() < 0) {
                    int laba = Integer.parseInt(hrgJual) - hrgawal;
                    binding.etLaba.setText(Integer.toString(laba));
                }
            }
        });

    }

    private Bitmap generateTextIcon(String namaProduk) {
        if (namaProduk == null || namaProduk.trim().isEmpty()) {
            namaProduk = "?";
        }

        String[] words = namaProduk.trim().split("\\s+");
        StringBuilder initials = new StringBuilder();

        for (int i = 0; i < words.length && initials.length() < 3; i++) {
            if (!words[i].isEmpty()) {
                initials.append(Character.toUpperCase(words[i].charAt(0)));
            }
        }

        if (initials.length() == 0) {
            initials.append("?");
        }

        int size = 200;
        Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);

        Paint paintBg = new Paint();
        paintBg.setColor(Color.parseColor("#c9caf0"));
        paintBg.setStyle(Paint.Style.FILL);
        canvas.drawRect(0, 0, size, size, paintBg);

        Paint paintText = new Paint();
        paintText.setColor(Color.BLACK);
        paintText.setTextSize(56f);
        paintText.setTextAlign(Paint.Align.CENTER);
        paintText.setAntiAlias(true);

        Paint.FontMetrics fontMetrics = paintText.getFontMetrics();
        float x = size / 2f;
        float y = size / 2f - (fontMetrics.ascent + fontMetrics.descent) / 2f;
        canvas.drawText(initials.toString(), x, y, paintText);

        return bitmap;
    }

    private void getDetailProduk(String kodeProduk) {
        Cursor cursor = dbHelper.getDetailProduk(kodeProduk);

        if (cursor.moveToFirst()) {
            String namaProduk = cursor.getString(cursor.getColumnIndexOrThrow(DataBaseHelper.nama_produk));
            String kategoriDb = cursor.getString(cursor.getColumnIndexOrThrow(DataBaseHelper.kategori));
            byte[] ivProdukByte = cursor.getBlob(cursor.getColumnIndexOrThrow(DataBaseHelper.gambar_produk));
            Bitmap bitmap = null;
            if (ivProdukByte != null && ivProdukByte.length > 0) {
                bitmap = BitmapFactory.decodeByteArray(ivProdukByte, 0, ivProdukByte.length);
            } else {
                bitmap = generateTextIcon(namaProduk);
            }
            binding.ivProduk.setImageBitmap(bitmap);

            String sku = cursor.getString(cursor.getColumnIndexOrThrow(DataBaseHelper.sku));
            String hargaBeli = cursor.getString(cursor.getColumnIndexOrThrow(DataBaseHelper.harga_beli));
            String hargaJual = cursor.getString(cursor.getColumnIndexOrThrow(DataBaseHelper.harga_jual));
            String hargaDiskon = cursor.getString(cursor.getColumnIndexOrThrow(DataBaseHelper.harga_diskon));
            Integer stok = null;
            if (!cursor.isNull(cursor.getColumnIndexOrThrow(DataBaseHelper.stok))) {
                stok = cursor.getInt(cursor.getColumnIndexOrThrow(DataBaseHelper.stok));
            }

            binding.etNamaProduk.setText(namaProduk);
            binding.tvKategori.setText(kategoriDb);
            binding.etSKU.setText(sku);
            binding.etHrgAwal.setText(hargaBeli);
            binding.etHrgJual.setText(hargaJual);
            binding.etHrgDiskon.setText(hargaDiskon.equals("0") ? "" : hargaDiskon);

            int spinnerPosition = ((ArrayAdapter) spKategori.getAdapter()).getPosition(kategoriDb);
            if (spinnerPosition >= 0) {
                spKategori.setSelection(spinnerPosition);
            }

            if (stok == null) {
                binding.switchStok.setChecked(false);
                binding.etStok.setText("");
                binding.etStok.setEnabled(false);
            } else {
                binding.switchStok.setChecked(true);
                binding.etStok.setText(String.valueOf(stok));
                binding.etStok.setEnabled(true);
            }
        }
        cursor.close();
    }

    @Override
    protected void onStop() {
        super.onStop();

    }

    public void initSpinView() {
        spKategori = findViewById(R.id.spinKategori);
        String kodePegawai = sessionManager.getKodeUsers();
        String idToko = dbHelper.getIdTokoByKodePegawai(kodePegawai);
        ArrayList<String> listKategori = dbHelper.kategoriSpiner(idToko);

        listKategori.add("Tambah Kategori");

        ArrayAdapter<String> dataAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, listKategori);
        dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spKategori.setAdapter(dataAdapter);

        spKategori.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int position, long id) {
                kategori = adapterView.getItemAtPosition(position).toString();

                if (isAddingKategori) {
                    isAddingKategori = false;
                    return;
                }

                if ("Tambah Kategori".equals(kategori)) {
                    showTambahKategoriPopup();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {}
        });
    }

    private void showTambahKategoriPopup() {
        final Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_kategori_add);

        dialog.setCancelable(false);
        dialog.setCanceledOnTouchOutside(false);

        TextView tvBatal = dialog.findViewById(R.id.tvBatal);
        EditText etNamaKategori = dialog.findViewById(R.id.etNamaKategori);
        Button btSimpan = dialog.findViewById(R.id.btSimpan);

        tvBatal.setOnClickListener(v -> {
            dialog.dismiss();

            spKategori.post(() -> {
                if (spKategori.getAdapter() != null && spKategori.getCount() > 1) {
                    spKategori.setSelection(0);
                }
            });
        });

        btSimpan.setOnClickListener(v -> {
            String namaKategori = etNamaKategori.getText().toString().trim();

            if (namaKategori.isEmpty()) {
                GlobalToast.ShowToast(this, "Nama kategori tidak boleh kosong");
                return;
            }

            String kodePegawai = sessionManager.getKodeUsers();
            String idToko = dbHelper.getIdTokoByKodePegawai(kodePegawai);

            ContentValues values = new ContentValues();
            values.put(DataBaseHelper.kode_pegawai, kodePegawai);
            values.put(DataBaseHelper.kategori, namaKategori);
            if (idToko != null) {
                values.put(DataBaseHelper.id_toko, idToko);
            } else {
                AndLog.ShowLog("TambahKategori", "id_toko null untuk kode pegawai: " + kodePegawai);
            }

            dbHelper.tambahKategori(values);
            GlobalToast.ShowToast(this, "Kategori berhasil ditambahkan");
            AndLog.ShowLog("dataUsers: ", String.valueOf(values));

            dialog.dismiss();

            spKategori.post(() -> {
                isAddingKategori = true;
                initSpinView();

                ArrayAdapter<String> adapter = (ArrayAdapter<String>) spKategori.getAdapter();
                for (int i = 0; i < adapter.getCount(); i++) {
                    if (adapter.getItem(i).equalsIgnoreCase(namaKategori)) {
                        spKategori.setSelection(i);
                        break;
                    }
                }
            });
        });

        dialog.show();
    }

    private void insertProduct() {

        pDialog.show();

        String namaProduk = binding.etNamaProduk.getText().toString().trim();
        String kodeUsers = sessionManager.getKodeUsers();
        String sku = binding.etSKU.getText().toString().trim();
        String hargaBeli = binding.etHrgAwal.getText().toString().trim();
        String hargaJual = binding.etHrgJual.getText().toString().trim();
        String hargaDiskon = binding.etHrgDiskon.getText().toString().trim();
        String laba = binding.etLaba.getText().toString().trim();
        String idToko = dbHelper.getIdTokoByKodePegawai(kodeUsers);

        ContentValues values = new ContentValues();
        values.put(DataBaseHelper.kode_produk, KodeProduk);
        values.put(DataBaseHelper.nama_produk, namaProduk);
        values.put(DataBaseHelper.kode_pegawai, kodeUsers);
        values.put(DataBaseHelper.kategori, kategori);
        values.put(DataBaseHelper.id_toko, idToko);

        if (sku.equals("0") || sku.isEmpty()) {
            values.put(DataBaseHelper.sku, "0");
        } else {
            values.put(DataBaseHelper.sku, sku);
        }
        values.put(DataBaseHelper.harga_beli, hargaBeli);
        values.put(DataBaseHelper.harga_jual, hargaJual);
        if (hargaDiskon.equals("0") || hargaDiskon.isEmpty()) {
            values.put(DataBaseHelper.harga_diskon, "0");
        } else {
            values.put(DataBaseHelper.harga_diskon, hargaDiskon);
        }
        values.put(DataBaseHelper.laba, laba);

        if (binding.ivProduk.getDrawable() == null) {
            Bitmap autoIcon = generateTextIcon(binding.etNamaProduk.getText().toString().trim());
            binding.ivProduk.setImageBitmap(autoIcon);
        }
        values.put(DataBaseHelper.gambar_produk, imageViewToByte(binding.ivProduk));

        if (binding.switchStok.isChecked()) {
            String stok = binding.etStok.getText().toString().trim();
            if (!stok.isEmpty()) {
                values.put(DataBaseHelper.stok, Integer.parseInt(stok));
            } else {
                values.putNull(DataBaseHelper.stok);
            }
        } else {
            values.putNull(DataBaseHelper.stok);
        }

        if (
                namaProduk.equals("") ||
                        kodeUsers.equals("") ||
                        hargaBeli.equals("") ||
                        hargaJual.equals("")) {

            GlobalToast.ShowToast(this, "Data Tidak Boleh Kosong");
            pDialog.dismiss();
        } else {

            try {

                dbHelper.tambahProduk(values);
                GlobalToast.ShowToast(this, "Data Berhasil Tersimpan");
                this.finish();
                pDialog.dismiss();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }

    private void updateProduct() {

        pDialog.show();

        String namaProduk = binding.etNamaProduk.getText().toString().trim();
        String kodeUsers = sessionManager.getKodeUsers();
        String sku = binding.etSKU.getText().toString().trim();
        String hargaBeli = binding.etHrgAwal.getText().toString().trim();
        String hargaJual = binding.etHrgJual.getText().toString().trim();
        String hargaDiskon = binding.etHrgDiskon.getText().toString().trim();
        String cadanganKategori = binding.tvKategori.getText().toString().trim();
        String idToko = dbHelper.getIdTokoByKodePegawai(kodeUsers);

        ContentValues values = new ContentValues();
        values.put(DataBaseHelper.kode_produk, IkodeProduk);
        values.put(DataBaseHelper.nama_produk, namaProduk);
        values.put(DataBaseHelper.kode_pegawai, kodeUsers);
        values.put(DataBaseHelper.id_toko, idToko);
        if (kategori != null) {
            values.put(DataBaseHelper.kategori, kategori);

        } else {
            values.put(DataBaseHelper.kategori, cadanganKategori);
        }
        if (sku.equals("0") || sku.isEmpty()) {
            values.put(DataBaseHelper.sku, "0");
        } else {
            values.put(DataBaseHelper.sku, sku);
        }
        values.put(DataBaseHelper.harga_beli, hargaBeli);
        values.put(DataBaseHelper.harga_jual, hargaJual);
        values.put(DataBaseHelper.gambar_produk, imageViewToByte(binding.ivProduk));
        if (hargaDiskon.equals("0") || hargaDiskon.isEmpty()) {
            values.put(DataBaseHelper.harga_diskon, "0");
        } else {
            values.put(DataBaseHelper.harga_diskon, hargaDiskon);
        }

        if (binding.switchStok.isChecked()) {
            String stok = binding.etStok.getText().toString().trim();
            if (!stok.isEmpty()) {
                values.put(DataBaseHelper.stok, Integer.parseInt(stok));
            } else {
                values.putNull(DataBaseHelper.stok);
            }
        } else {
            values.putNull(DataBaseHelper.stok);
        }

        if (
                namaProduk.equals("") ||
                        kodeUsers.equals("") ||
                        sku.equals("") ||
                        hargaBeli.equals("") ||
                        hargaJual.equals("")) {

            GlobalToast.ShowToast(this, "Data Tidak Boleh Kosong");
            pDialog.dismiss();
        } else {

            try {

                dbHelper.updateProduk(values, IkodeProduk);
                GlobalToast.ShowToast(this, "Data Berhasil Tersimpan");
                finish();
                pDialog.dismiss();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private static byte[] imageViewToByte(ImageView image) {
        Bitmap bitmap;

        if (image.getDrawable() instanceof BitmapDrawable) {
            bitmap = ((BitmapDrawable) image.getDrawable()).getBitmap();
        } else if (image.getDrawable() != null) {
            Drawable drawable = image.getDrawable();
            bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
            drawable.draw(canvas);
        } else {
            bitmap = BitmapFactory.decodeResource(image.getResources(), R.drawable.no_image_logo);
        }

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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK & requestCode == RESULT_LOAD_IMG && data != null && data.getData() != null) {
            try {
                final Uri imageUri = data.getData();
                final InputStream imageStream = getContentResolver().openInputStream(imageUri);
                bitmap = BitmapFactory.decodeStream(imageStream);
                setToImageViewLogo(getResizedBitmap(bitmap, 512));
                if (bitmap != null) {
                    binding.tvUploadProduk.setVisibility(View.GONE);
                    binding.tvUploadUlangProduk.setVisibility(View.VISIBLE);
                    binding.icIconCamera.setVisibility(View.GONE);
                }
            } catch (FileNotFoundException e) {
                e.printStackTrace();
                GlobalToast.ShowToast(ProdukTokoActivity.this, "Kesalahan Saat Reload Gambar");
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

    private void setToImageViewLogo(Bitmap bmp) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        bmp.compress(Bitmap.CompressFormat.JPEG, 60, bytes);
        decodedLogo = BitmapFactory.decodeStream(new ByteArrayInputStream(bytes.toByteArray()));

        binding.ivProduk.setTag("done");
        binding.ivProduk.setImageBitmap(decodedLogo);
    }

    private String getTodayDate() {
        return new java.text.SimpleDateFormat("ddMMyyyy")
                .format(new java.util.Date());
    }

    private static String getRandomString(int i) {
        final String character = "1234567890ABCDEFGHIJKLMNOPQRSTUVYXYZ";
        StringBuilder result = new StringBuilder();
        while (i > 0) {
            Random random = new Random();
            result.append(character.charAt(random.nextInt(character.length())));
            i--;
        }
        return result.toString();
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
        if (view.getId() == R.id.btSimpan) {
            insertProduct();
        } else if (view.getId() == R.id.btEdit) {
            updateProduct();
        } else if (view.getId() == R.id.tvUploadProduk || view.getId() == R.id.tvUploadUlangProduk || view.getId() == R.id.rel_img) {
            showFileChooser();
        }
    }
}
