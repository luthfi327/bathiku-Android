package com.pyramidsoft.siqasir;

import static com.pyramidsoft.siqasir.db.DataBaseHelper.database_name;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.GridLayoutManager;

import com.pyramidsoft.siqasir.Command.Command;
import com.pyramidsoft.siqasir.Command.PrintPicture;
import com.pyramidsoft.siqasir.Command.PrinterCommand;
import com.pyramidsoft.siqasir.adapter.DataPembelianRPAdapter;
import com.pyramidsoft.siqasir.databinding.ActivityCetakUlangRiwayatPenjualanBinding;
import com.pyramidsoft.siqasir.db.DataBaseHelper;
import com.pyramidsoft.siqasir.model.ListPembelianModels;
import com.pyramidsoft.siqasir.model.ProdukPesananModel;
import com.pyramidsoft.siqasir.printer.service.BTServices;
import com.pyramidsoft.siqasir.printer.service.PerintahPrinter;
import com.pyramidsoft.siqasir.utils.AndLog;
import com.pyramidsoft.siqasir.utils.DecimalsFormat;
import com.pyramidsoft.siqasir.utils.GlobalToast;
import com.pyramidsoft.siqasir.utils.ProgressDialog;
import com.pyramidsoft.siqasir.utils.SessionManager;
import com.zj.btsdk.BluetoothService;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

public class CetakUlangRiwayatPenjualan extends AppCompatActivity {

    private ActivityCetakUlangRiwayatPenjualanBinding binding;
    ProgressDialog pDialog;
    SessionManager sessionManager;
    DataBaseHelper dbHelper;
    SQLiteDatabase mDatabase;
    DataPembelianRPAdapter adapter;
    private ArrayList<ProdukPesananModel> arrayList =new ArrayList<>();

    private static final String TAG = "PembayaranSuksesAct";
    private static final boolean DEBUG = true;
    boolean mBounded;
    private BluetoothService mBluetoothService = null;
    private BluetoothAdapter mBluetoothAdapter = null;
    private static final String CHINESE = "GBK";
    Calendar c = Calendar.getInstance();
    String invoice, formattedDate, jenis_kedatangan, invoices, nama_pegawai, nama_pelanggan, kode_pelanggan,
            jenis_pembayaran, tanggal, total_jml_transaksi, dibayar, kembali, total_qty, diskon, mPath;

    private Uri lastSavedImageUri;
    private String idToko;
    private boolean hasAutoConnected = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCetakUlangRiwayatPenjualanBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        pDialog = new ProgressDialog(this);
        sessionManager = new SessionManager(this);
        dbHelper = new DataBaseHelper(this);
        mDatabase = openOrCreateDatabase(database_name, MODE_PRIVATE, null);
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        if (mBluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth tidak tersedia", Toast.LENGTH_LONG).show();
        } else {
            mBluetoothService = new BluetoothService(this, new Handler(msg -> {
                switch (msg.what) {
                    case BluetoothService.MESSAGE_STATE_CHANGE:
                        switch (msg.arg1) {
                            case BluetoothService.STATE_CONNECTED:
                                GlobalToast.ShowToast(this, "Printer terhubung");
                                break;
                        }
                        break;
                }
                return true;
            }));
        }

        binding.IvClose.setOnClickListener(this::onViewClicked);
        binding.btCetakUlangStruk.setOnClickListener(this::onViewClicked);
        binding.btBagikan.setOnClickListener(this::onViewClicked);

        if (getIntent().getStringExtra("invoice") != null) {
            invoice = getIntent().getStringExtra("invoice");
        }

        binding.rvListPenjualan.setHasFixedSize(true);
        GridLayoutManager layoutManager = new GridLayoutManager(this, 1, GridLayoutManager.VERTICAL, false);
        binding.rvListPenjualan.setLayoutManager(layoutManager);

        getDetailInvoice(invoice);

        listPembelian(invoice);

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        if (mBluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth is not available", Toast.LENGTH_LONG).show();
        }

        getDataToko();
        getDataStruk();
    }

    @Override
    public void onResume() {
        super.onResume();

        if (!hasAutoConnected) {
            autoReconnectPrinter();   // ✅ hanya sekali
            hasAutoConnected = true;  // tandai sudah pernah reconnect
        }
    }

    private void autoReconnectPrinter() {

        if (mBluetoothService == null) return;
        if (mBluetoothAdapter == null) return;
        if (!mBluetoothAdapter.isEnabled()) return;

        String alamatPrinter = sessionManager.getAlamatPrinter();
        if (alamatPrinter == null || alamatPrinter.isEmpty()) {
            Log.d("PRINTER_DEBUG", "Alamat kosong");
            return;
        }

        try {
            BluetoothDevice device =
                    mBluetoothAdapter.getRemoteDevice(alamatPrinter);

            mBluetoothService.connect(device);

        } catch (Exception e) {
            Log.e("PRINTER_DEBUG", "ERROR CONNECT", e);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            saveImage();
        } else {
            GlobalToast.ShowToast(this, "Izin tidak diberikan");
        }

        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

    }

    public Bitmap getScreenShot() {
        View v = findViewById(R.id.card_transaksi);
        Bitmap bitmap = Bitmap.createBitmap(v.getWidth(), v.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        v.draw(canvas);
        return bitmap;
    }

    private void saveImage() {
        Bitmap bitmap = getScreenShot();
        OutputStream outputStream;

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Android 10+
                ContentResolver resolver = getContentResolver();
                ContentValues values = new ContentValues();
                values.put(MediaStore.Images.Media.DISPLAY_NAME, "ScreenShot_" + new Date().getTime() + ".png");
                values.put(MediaStore.Images.Media.MIME_TYPE, "image/png");
                values.put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/Struk CashierApp");

                lastSavedImageUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
                outputStream = resolver.openOutputStream(lastSavedImageUri);
            } else {
                // Android 9 ke bawah → Simpan di folder private app (tidak perlu permission)
                File fileDir = new File(getExternalFilesDir(Environment.DIRECTORY_PICTURES), "Struk CashierApp");
                if (!fileDir.exists()) fileDir.mkdirs();

                File file = new File(fileDir, "ScreenShot_" + new Date().getTime() + ".png");
                mPath = file.getAbsolutePath();
                outputStream = new FileOutputStream(file);
            }

            bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
            outputStream.flush();
            outputStream.close();

            GlobalToast.ShowToast(this, "Tangkapan layar berhasil disimpan.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void shareImage() {
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("image/*");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        Uri shareUri = null;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (lastSavedImageUri != null) {
                shareUri = lastSavedImageUri;
            }
        } else {
            if (mPath != null && !mPath.isEmpty()) {
                File file = new File(mPath);
                if (file.exists()) {
                    shareUri = FileProvider.getUriForFile(
                            this,
                            getApplicationContext().getPackageName() + ".provider",
                            file
                    );
                }
            }
        }

        if (shareUri != null) {
            intent.putExtra(Intent.EXTRA_STREAM, shareUri);
            try {
                startActivity(Intent.createChooser(intent, "Share Screenshot"));
            } catch (ActivityNotFoundException e) {
                Toast.makeText(this, "No App Available", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "Gambar tidak ditemukan", Toast.LENGTH_SHORT).show();
        }
    }

    private void getDataToko() {
        Cursor cursor = dbHelper.dataToko(idToko);

        if (cursor.moveToFirst()) {
            String namaToko = cursor.getString(cursor.getColumnIndexOrThrow(DataBaseHelper.tb_toko_nama_toko));

            byte[] ivLogoStrukByte = cursor.getBlob(cursor.getColumnIndexOrThrow("logo_struk"));
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

        if (cursor != null) cursor.close();
    }

    private void SendDataByte(byte[] data) {
        if (mBluetoothService == null || mBluetoothService.getState() != BluetoothService.STATE_CONNECTED) {
            GlobalToast.ShowToast(this, "Printer belum terhubung");
            return;
        }
        mBluetoothService.write(data);
    }

    private void SendDataString(String data) {
        if (mBluetoothService == null || mBluetoothService.getState() != BluetoothService.STATE_CONNECTED) {
            GlobalToast.ShowToast(this, "Printer belum terhubung");
            return;
        }
        try {
            mBluetoothService.write(data.getBytes("GBK"));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

    private boolean hasBluetoothPermission() {

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            return this.checkSelfPermission(
                    android.Manifest.permission.BLUETOOTH_CONNECT
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED;
        }

        return true; // android < 12 tidak perlu runtime permission
    }

    private void requestBluetoothPermission() {

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            requestPermissions(
                    new String[]{
                            android.Manifest.permission.BLUETOOTH_CONNECT,
                            android.Manifest.permission.BLUETOOTH_SCAN
                    },
                    1001
            );
        }
    }

    private void setBtCetakStruk() {
        Cursor cursor;
        Bitmap bmLogoStruk = null;
        String namaToko = "", noTelp = "", social_media = "", footer = "", alamat_toko = "";

        String kodePegawai = sessionManager.getKodeUsers();
        String idToko = dbHelper.getIdTokoByKodePegawai(kodePegawai);
        cursor = dbHelper.dataToko(idToko);
        SendDataByte(Command.ESC_NORMAL);

        // ===== CEK BLUETOOTH ADAPTER =====
        if (mBluetoothAdapter == null) {
            new AlertDialog.Builder(this)
                    .setTitle("Bluetooth Tidak Tersedia")
                    .setMessage("Perangkat ini tidak mendukung Bluetooth.")
                    .setPositiveButton("OK", null)
                    .show();
            return;
        }

        // ===== CEK BLUETOOTH AKTIF =====
        if (!mBluetoothAdapter.isEnabled()) {

            new AlertDialog.Builder(this)
                    .setTitle("Bluetooth Mati")
                    .setMessage("Bluetooth akan diaktifkan dan mencoba menghubungkan printer.")
                    .setCancelable(false)
                    .setPositiveButton("OK", (dialog, which) -> {

                        if (!hasBluetoothPermission()) {
                            requestBluetoothPermission();
                            return;
                        }

                        try {
                            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                            startActivity(enableBtIntent);
                        } catch (SecurityException e) {
                            e.printStackTrace();
                            Toast.makeText(this,
                                    "Permission Bluetooth ditolak",
                                    Toast.LENGTH_SHORT).show();
                        }

                        Toast.makeText(this,
                                "Mengaktifkan Bluetooth...",
                                Toast.LENGTH_SHORT).show();

                        // tunggu bluetooth aktif lalu reconnect saja
                        new Handler().postDelayed(() -> {

                            if (mBluetoothAdapter.isEnabled()) {

                                autoReconnectPrinter();

                                Toast.makeText(this,
                                        "Mencoba menghubungkan printer...",
                                        Toast.LENGTH_SHORT).show();

                            } else {

                                new AlertDialog.Builder(this)
                                        .setTitle("Gagal Mengaktifkan Bluetooth")
                                        .setMessage("Bluetooth tidak dapat diaktifkan.")
                                        .setPositiveButton("OK", null)
                                        .show();
                            }

                        }, 1000); // tunggu bluetooth aktif

                    })
                    .show();

            return;
        }

        // ===== CEK PRINTER TERSIMPAN =====
        String alamatPrinter = sessionManager.getAlamatPrinter();
        if (alamatPrinter == null || alamatPrinter.isEmpty()) {
            new AlertDialog.Builder(this)
                    .setTitle("Printer Belum Dipilih")
                    .setMessage("Silakan pilih dan hubungkan printer terlebih dahulu di menu pengaturan.")
                    .setPositiveButton("OK", null)
                    .show();
            return;
        }

        // ===== CEK STATUS KONEKSI =====
        if (mBluetoothService == null ||
                mBluetoothService.getState() != BluetoothService.STATE_CONNECTED) {

            new AlertDialog.Builder(this)
                    .setTitle("Printer Belum Terhubung")
                    .setMessage("Printer belum siap.\n\nTunggu beberapa saat atau buka ulang halaman ini.")
                    .setPositiveButton("OK", null)
                    .show();

            return;
        }

        if (cursor.moveToFirst()) {

            namaToko = cursor.getString(cursor.getColumnIndexOrThrow(DataBaseHelper.tb_toko_nama_toko));
            noTelp   = cursor.getString(cursor.getColumnIndexOrThrow(DataBaseHelper.tb_toko_no_telepon));
            byte[] ivLogoStrukByte = cursor.getBlob(cursor.getColumnIndexOrThrow("logo_struk"));

            if (ivLogoStrukByte != null && ivLogoStrukByte.length > 0) {
                bmLogoStruk = BitmapFactory.decodeByteArray(
                        ivLogoStrukByte,
                        0,
                        ivLogoStrukByte.length
                );
                Log.d("LOGO_DEBUG", "Logo diambil dari database");
            } else {
                Drawable drawable = ContextCompat.getDrawable(this, R.drawable.no_image_logo);
                if (drawable != null) {
                    Bitmap bitmap = Bitmap.createBitmap(
                            drawable.getIntrinsicWidth(),
                            drawable.getIntrinsicHeight(),
                            Bitmap.Config.ARGB_8888
                    );
                    Canvas canvas = new Canvas(bitmap);
                    drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
                    drawable.draw(canvas);
                    bmLogoStruk = bitmap;

                    Log.d("LOGO_DEBUG", "Logo kosong, pakai vector no_image_logo");
                } else {
                    Log.e("LOGO_DEBUG", "Drawable no_image_logo NULL!");
                }
            }
        }
        cursor.close();

        if (bmLogoStruk != null) {

            try {

                int logoSize = 90;
                int printerWidth = 384;

                // Resize
                Bitmap scaledLogo = Bitmap.createScaledBitmap(
                        bmLogoStruk,
                        logoSize,
                        logoSize,
                        true
                );

                int paddingLeftRight = (printerWidth - logoSize) / 2;

                Bitmap paddedLogo = Bitmap.createBitmap(
                        printerWidth,
                        logoSize,
                        Bitmap.Config.ARGB_8888
                );

                Canvas canvas = new Canvas(paddedLogo);
                canvas.drawColor(Color.WHITE);
                canvas.drawBitmap(scaledLogo, paddingLeftRight, 0, null);

                Bitmap monoLogo = convertTo1Bit(paddedLogo);

                // Print
                SendDataByte(PrinterCommand.POS_Set_PrtInit());
                SendDataByte(PerintahPrinter.ESC_ALIGN_CENTER);
                SendDataByte(PrintPicture.POS_PrintBMP(monoLogo, printerWidth, 0));
                SendDataString("\n");

            } catch (Exception e) {
                e.printStackTrace();
            }

        } else {

            Log.e("LOGO_DEBUG", "Bitmap LOGO NULL sebelum diproses!");
        }

        // Ambil data struk
        cursor = dbHelper.detailStruk(idToko);
        if (cursor.moveToFirst()) {
            social_media = cursor.getString(cursor.getColumnIndexOrThrow(DataBaseHelper.socialmedia_struk));
            footer = cursor.getString(cursor.getColumnIndexOrThrow(DataBaseHelper.footer));
            alamat_toko = cursor.getString(cursor.getColumnIndexOrThrow(DataBaseHelper.alamat_struk));
        }
        cursor.close();

        // Cetak header struk
        SendDataByte(PerintahPrinter.ESC_ALIGN_CENTER);
        SendDataString(namaToko + "\n");
        SendDataString(noTelp + "\n");
        SendDataString(social_media + "\n");
        SendDataString(alamat_toko + "\n\n");

        // Cetak detail transaksi
        SendDataByte(PerintahPrinter.ESC_ALIGN_LEFT);
        SendDataString(leftRightAlign("Tanggal Pesanan", tanggal));
        SendDataString(leftRightAlign("Kode Counter", invoices));
        SendDataString(leftRightAlign("Nama Pegawai", nama_pegawai));
        SendDataString(leftRightAlign("Nama Pelanggan", nama_pelanggan));
        SendDataString(leftRightAlign("Jenis Kedatangan", jenis_kedatangan));
        SendDataString(leftRightAlign("Jenis Pembayaran", jenis_pembayaran));
        SendDataString("\n");
        SendDataString("Pesanan (" + total_qty + ")\n");

        // Cetak produk: nama kiri, harga kanan
        for (int a = 0; a < DataPembelianRPAdapter.arrayList.size(); a++) {
            int hrg = Integer.parseInt(DataPembelianRPAdapter.arrayList.get(a).getHargaproduk());
            int qty = Integer.parseInt(DataPembelianRPAdapter.arrayList.get(a).getQty());
            int total_harga = hrg * qty;

            String productName = DataPembelianRPAdapter.arrayList.get(a).getNamaproduk();
            String qtyText = DataPembelianRPAdapter.arrayList.get(a).getQty();
            String totalPrice = "Rp. " + DecimalsFormat.priceWithoutDecimal(String.valueOf(total_harga));

            SendDataString(leftRightAlign(qtyText + "X " + productName, totalPrice));
        }

        // Cetak summary
        SendDataString("\n");
        SendDataString(leftRightAlign("Subtotal", "Rp. " + DecimalsFormat.priceWithoutDecimal(total_jml_transaksi)));
        SendDataString(leftRightAlign("Diskon", "-Rp. " + DecimalsFormat.priceWithoutDecimal(diskon)) + "\n");
        SendDataString(leftRightAlign("Total Pembayaran", "Rp. " + DecimalsFormat.priceWithoutDecimal(total_jml_transaksi)));
        SendDataString(leftRightAlign("Dibayar", "Rp. " + DecimalsFormat.priceWithoutDecimal(dibayar)));
        SendDataString(leftRightAlign("Kembali", "Rp. " + DecimalsFormat.priceWithoutDecimal(kembali)) + "\n\n");

        // Footer
        SendDataByte(PerintahPrinter.ESC_ALIGN_CENTER);
        SendDataString(footer + "\n\n");
        SendDataString("Bathiku by Pyramid\n\n");

        try {
            Bitmap bmPlaystore = BitmapFactory.decodeResource(getResources(), R.drawable.get_playstore);
            int targetWidth = 300;
            int printerWidth = 384;

            // Skala agar lebar 150 px
            int targetHeight = (int) ((double) bmPlaystore.getHeight() / bmPlaystore.getWidth() * targetWidth);
            Bitmap scaledPlaystore = Bitmap.createScaledBitmap(bmPlaystore, targetWidth, targetHeight, true);

            // Tambahkan padding kiri-kanan agar center
            int paddingLeftRight = (printerWidth - targetWidth) / 2;
            Bitmap paddedPlaystore = Bitmap.createBitmap(printerWidth, targetHeight, Bitmap.Config.ARGB_8888);
            Canvas canvasPlay = new Canvas(paddedPlaystore);
            canvasPlay.drawColor(Color.WHITE);
            canvasPlay.drawBitmap(scaledPlaystore, paddingLeftRight, 0, null);

            // Convert ke 1-bit dan cetak
            Bitmap monoPlaystore = convertTo1Bit(paddedPlaystore);
            SendDataByte(PerintahPrinter.ESC_ALIGN_CENTER);
            SendDataByte(PrintPicture.POS_PrintBMP(monoPlaystore, printerWidth, 0));
            SendDataString("\n\n");
        } catch (Exception e) {
            e.printStackTrace();
            Log.e("PrintDebug", "Gagal print logo playstore");
        }

        SendDataString("** Salinan Struk **\n\n\n\n\n");

        // Potong kertas
        SendDataByte(PrinterCommand.POS_Set_Cut(1));
        SendDataByte(PrinterCommand.POS_Set_PrtInit());
    }

    private Bitmap convertTo1Bit(Bitmap bmp) {
        Bitmap bmpMonochrome = Bitmap.createBitmap(bmp.getWidth(), bmp.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bmpMonochrome);
        canvas.drawColor(Color.WHITE);

        for (int y = 0; y < bmp.getHeight(); y++) {
            for (int x = 0; x < bmp.getWidth(); x++) {
                int pixel = bmp.getPixel(x, y);
                int gray = (Color.red(pixel) + Color.green(pixel) + Color.blue(pixel)) / 3;
                bmpMonochrome.setPixel(x, y, gray < 128 ? Color.BLACK : Color.WHITE);
            }
        }
        return bmpMonochrome;
    }

    private void getDetailInvoice(String invoice) {
        pDialog.show();
        Cursor cursor = dbHelper.getDetailInvoice(invoice);

        if (cursor.moveToFirst()) {

            invoices = cursor.getString(cursor.getColumnIndexOrThrow("invoice"));
            jenis_kedatangan = cursor.getString(cursor.getColumnIndexOrThrow(DataBaseHelper.jenis_kedatangan));
            nama_pegawai = cursor.getString(cursor.getColumnIndexOrThrow(DataBaseHelper.nama_pegawai));
            nama_pelanggan = cursor.getString(cursor.getColumnIndexOrThrow("nama_pelanggan"));
            kode_pelanggan = cursor.getString(cursor.getColumnIndexOrThrow("kode_pelanggan"));
            jenis_pembayaran = cursor.getString(cursor.getColumnIndexOrThrow(DataBaseHelper.jenis_pembayaran));
            tanggal = cursor.getString(cursor.getColumnIndexOrThrow("tanggal"));
            total_jml_transaksi = cursor.getString(cursor.getColumnIndexOrThrow("total_jml_transaksi"));
            dibayar = cursor.getString(cursor.getColumnIndexOrThrow("dibayar"));
            kembali = cursor.getString(cursor.getColumnIndexOrThrow("kembali"));
            total_qty = cursor.getString(cursor.getColumnIndexOrThrow("total_qty"));
            diskon = cursor.getString(cursor.getColumnIndexOrThrow("nominal_disc"));
            String id_toko = cursor.getString(cursor.getColumnIndexOrThrow("id_toko"));
            Log.d("INVOICE_DETAIL", "ID Toko: " + id_toko);
            this.idToko = id_toko;

            // Set data lain ke view
            binding.tvJenisKedatangan.setText(jenis_kedatangan);
            binding.tvJenisPembayaran.setText(jenis_pembayaran);
            binding.tvkodeCounter.setText(invoices);
            binding.tvTglOrder.setText(tanggal);
            binding.tvNamaPegawai.setText(nama_pegawai);
            binding.tvNamaCust.setText(nama_pelanggan);
            binding.tvIdMember.setText(kode_pelanggan);
            binding.tvLblPesanan.setText("Pesanan(" + total_qty + ")");

            long subtotal = Long.parseLong(total_jml_transaksi);
            long totalSetelahDiskon = subtotal;
            long nilaiDiskonRupiah = 0;

            if (diskon != null && !diskon.trim().isEmpty()) {
                try {
                    long nilaiDiskon = Long.parseLong(diskon);

                    if (nilaiDiskon <= 0) {
                        // Tidak ada diskon
                        binding.tvDiskon.setText("-");

                    } else if (nilaiDiskon > 0 && nilaiDiskon <= 100 && diskon.length() <= 3) {
                        // Diskon persen
                        binding.tvDiskon.setText("-" + nilaiDiskon + "%");
                        nilaiDiskonRupiah = (subtotal * nilaiDiskon) / 100;

                    } else {
                        // Diskon nominal
                        binding.tvDiskon.setText("-Rp" + DecimalsFormat.priceWithoutDecimal(diskon));
                        nilaiDiskonRupiah = nilaiDiskon;
                    }

                    totalSetelahDiskon = subtotal - nilaiDiskonRupiah;

                    // Hindari minus
                    if (totalSetelahDiskon < 0) {
                        totalSetelahDiskon = 0;
                    }

                } catch (NumberFormatException e) {
                    Log.e("DISKON_CHECK", "Format diskon tidak valid: " + diskon);
                    binding.tvDiskon.setText("-");
                    totalSetelahDiskon = subtotal;
                }
            } else {
                binding.tvDiskon.setText("-");
            }

            binding.tvSubTotal.setText("Rp" + DecimalsFormat.priceWithoutDecimal(String.valueOf(subtotal)));
            binding.tvTotalBayar.setText("Rp" + DecimalsFormat.priceWithoutDecimal(String.valueOf(totalSetelahDiskon)));
            binding.tvDibayar.setText("Rp" + DecimalsFormat.priceWithoutDecimal(dibayar));
            binding.tvKembali.setText("Rp" + DecimalsFormat.priceWithoutDecimal(kembali));

            pDialog.dismiss();
            formattedDate = tanggal;
        }

        //cursor.close();
    }

    public void listPembelian(String invoice) {

        Cursor cursorproduct = dbHelper.listPembelian(invoice);

        ArrayList<ListPembelianModels> data = new ArrayList<>();

        if (cursorproduct.moveToFirst()) {
            do {
                data.add(new ListPembelianModels(
                        cursorproduct.getString(0), //KdPegawai
                        cursorproduct.getString(1), //nmPegawai
                        cursorproduct.getString(2) // posisi

                ));
            } while (cursorproduct.moveToNext());
        }
        if (data.isEmpty()) {
            Toast.makeText(this, "No items Found in database", Toast.LENGTH_SHORT).show();

        }

        cursorproduct.close();

        adapter = new DataPembelianRPAdapter(data, this, mDatabase);

        binding.rvListPenjualan.setAdapter(adapter);

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(mConnection);
        if (mBluetoothService != null) {
            if (mBluetoothService.getState() == BluetoothService.STATE_CONNECTED) {
                mBluetoothService.stop();
            }
        }
    }

    ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceDisconnected(ComponentName name) {
            AndLog.ShowLog("SERVICE CHECK", "Service is disconnected");
            mBounded = false;
        }

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            AndLog.ShowLog("SERVICE CHECK", "Service is connected");
            mBounded = true;
            BTServices.LocalBinder mLocalBinder = (BTServices.LocalBinder) service;
        }
    };

    @Override
    protected void onStart() {
        super.onStart();
        Intent mIntent = new Intent(this, BTServices.class);
        bindService(mIntent, mConnection, BIND_AUTO_CREATE);
        Log.e(TAG, "--- ON START ---");
    }

    public void onViewClicked(View view) {
        if (view.getId() == R.id.IvClose) {
            finish();
        } else if (view.getId() == R.id.btCetakUlangStruk) {
            setBtCetakStruk();
        } else if (view.getId() == R.id.btBagikan) {
            saveImage();
            shareImage();
        }
    }

    private String leftRightAlign(String str1, String str2) {
        String ans = str1 + str2;
        if (ans.length() < 32) {
            int a = str1.length() + str2.length();
            int n = (32 - a);
            ans = str1 + new String(new char[n]).replace("\0", " ") + str2 + "\n";
        }
        return ans;
    }

}
