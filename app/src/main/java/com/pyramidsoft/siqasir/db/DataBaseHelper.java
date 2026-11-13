package com.pyramidsoft.siqasir.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.Log;

import com.pyramidsoft.siqasir.model.DaftarPesananModel;
import com.pyramidsoft.siqasir.model.JenisKedatanganModels;
import com.pyramidsoft.siqasir.model.KategoriModels;
import com.pyramidsoft.siqasir.model.Outlet;
import com.pyramidsoft.siqasir.model.ProdukPesananModel;
import com.pyramidsoft.siqasir.utils.AndLog;
import com.pyramidsoft.siqasir.utils.DataPesanan;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class DataBaseHelper extends SQLiteOpenHelper {

    // import database
    private static String DB_PATH = "/data/user/0/com.pyramidsoft.siqasir/databases/";
    private final Context mContext;

    public static final String database_name = "kasir.db";
    Calendar c = Calendar.getInstance();
    SimpleDateFormat formatlengkap = new SimpleDateFormat("EEEE, dd MMMM yyyy, HH:mm", new Locale("id","ID"));
    SimpleDateFormat formattanggal = new SimpleDateFormat("dd-MM-yy", new Locale("id", "ID"));
    SimpleDateFormat formatbulan = new SimpleDateFormat("MM", new Locale("id", "ID"));
    SimpleDateFormat formattahun = new SimpleDateFormat("yyyy", new Locale("id", "ID"));
    SimpleDateFormat formatwaktu = new SimpleDateFormat("HH:mm", new Locale("id", "ID"));
    String ftanggal = formattanggal.format(c.getTime());
    String fbulan = formatbulan.format(c.getTime());
    String ftahun = formattahun.format(c.getTime());
    String fwaktu = formatwaktu.format(c.getTime());
    String fwaktulengkap = formatlengkap.format(c.getTime());

    //Toko
    public static final String tb_toko_kode_toko = "kode_toko";
    public static final String tb_toko_nama_toko = "nama_toko";
    public static final String tb_toko_kota = "kota";
    public static final String tb_toko_provinsi = "provinsi";
    public static final String tb_toko_logo = "logo";
    public static final String tb_struk_logo = "logo_struk";
    public static final String tb_toko_email = "email";
    public static final String tb_toko_no_telepon = "no_telepon";

    //Users
    public static final String id_toko = "id_toko";
    public static final String nama_pegawai = "nama_pegawai";
    public static final String kode_pegawai = "kode_pegawai";
    public static final String gender = "gender";
    public static final String no_hp = "no_hp";
    public static final String hak_akses = "hak_akses";
    public static final String posisi = "posisi";
    public static final String status = "status";
    public static final String username = "username";
    public static final String password = "password";
    public static final String pin = "pin";

    //Kategori
    public static final String kategori = "kategori";

    //product
    public static final String kode_produk = "kode_produk";
    public static final String nama_produk = "nama_produk";
    public static final String gambar_produk = "gambar";
    public static final String sku = "sku";
    public static final String harga_beli = "harga_beli";
    public static final String harga_jual = "harga_jual";
    public static final String harga_diskon = "harga_diskon";
    public static final String laba = "laba";
    public static final String stok = "stok";

    //setting struk
    public static final String alamat_struk = "alamat_toko";
    public static final String socialmedia_struk = "social_media";
    public static final String footer = "footer";

    //Jenis Kedatangan
    public static final String jenis_kedatangan = "jenis_kedatangan";

    //Rekap Kas
    public static final String kode_rekapkas = "kode_rekapkas";
    public static final String status_rekap_kas = "status";
    public static final String waktu_buka = "waktu_buka";
    public static final String waktu_tutup = "waktu_tutup";
    public static final String tgl_buka = "tgl_buka";
    public static final String tgl_tutup = "tgl_tutup";
    public static final String date_awal = "date_awal";
    public static final String date_tutup = "date_tutup";

    //Rekap Kas Detail
    public static final String total_penjualan = "total_penjualan";
    public static final String total_modalawal = "total_modalawal";
    public static final String total_kaskeluar = "total_kaskeluar";
    public static final String penerimaan_sistem = "penerimaan_sistem";
    public static final String penerimaan_tunai = "penerimaan_tunai";
    public static final String penerimaan_nontunai = "penerimaan_non_tunai";
    public static final String selisih = "selisih";

    public static final String jenis_pembayaran = "jenis_pembayaran";
    public static final String total_jml_transaksi = "total_jml_transaksi";

    //Kas Keluar
    public static final String nominal = "nominal";
    public static final String keperluan_kas = "keperluan";

    //Printer
    public static final String nama_printer = "nama_printer";
    public static final String alamat_printer = "alamat_printer";
    public static final String banyak_struk = "banyak_struk";
    public static final String ukuran_struk = "ukuran_struk";
    public static final String tipe_printer = "tipe_printer";
    public static final String status_cetak_struk = "status_cetak_struk";

    private SQLiteDatabase db;

    public DataBaseHelper(Context context) {
        super(context, database_name, null, 6);
        this.mContext = context;
        db = getWritableDatabase();
    }

    // ============================================================
    // FASE 1 — PERUBAHAN A: hashPassword()
    //
    // Fungsi ini mengubah password plaintext menjadi SHA-256 hex.
    // Dipakai saat:
    //   1. Registrasi → simpan ke SQLite lokal
    //   2. Login offline → hash input sebelum dibandingkan ke DB
    //
    // Mengapa SHA-256 bukan bcrypt?
    //   Android tidak punya library bcrypt bawaan. SHA-256 tersedia
    //   via java.security.MessageDigest tanpa tambahan dependency.
    //   Server (Laravel) tetap menyimpan bcrypt untuk keamanan
    //   di sisi server. SHA-256 ini hanya untuk kebutuhan lokal
    //   offline di SQLite Android.
    //
    // Deteksi hash lama (bcrypt dari server):
    //   Jika password yang tersimpan di DB lokal sudah berbentuk
    //   bcrypt ($2y$...), fungsi login() tidak perlu di-hash lagi
    //   karena proses download DB dari server menyimpan bcrypt.
    //   Untuk kasus ini, login Online yang dipakai (bukan offline).
    // ============================================================
    public String hashPassword(String plainPassword) {
        if (plainPassword == null || plainPassword.isEmpty()) {
            return plainPassword;
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(plainPassword.getBytes("UTF-8"));

            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            Log.e("DataBaseHelper", "hashPassword error: " + e.getMessage());
            // Fallback: kembalikan plaintext agar tidak crash
            // (sangat jarang terjadi karena SHA-256 selalu tersedia di Android)
            return plainPassword;
        }
    }

    // ============================================================
    // FASE 1 — PERUBAHAN B: isPasswordHashed()
    //
    // Helper untuk mendeteksi apakah password di DB lokal sudah
    // berbentuk hash (SHA-256 = 64 karakter hex) atau bcrypt
    // ($2y$... dari server), sehingga tidak di-hash dua kali.
    // ============================================================
    private boolean isPasswordHashed(String pwd) {
        if (pwd == null) return false;
        // Cek bcrypt dari server (Login Online)
        if (pwd.startsWith("$2y$") || pwd.startsWith("$2a$") || pwd.startsWith("$2b$")) {
            return true;
        }
        // Cek SHA-256 lokal (64 karakter hex lowercase)
        return pwd.matches("^[a-f0-9]{64}$");
    }

    public void createDatabase() throws IOException {
        boolean mDataBaseExist = checkDatabase();
        if (!mDataBaseExist) {
            this.getReadableDatabase();
            try {
                copyDatabase();
            } catch (IOException e) {
                throw new Error("Error copying database");
            }
        }
    }

    private boolean checkDatabase() {
        File dbFile = new File(DB_PATH + database_name);
        return dbFile.exists();
    }

    private void copyDatabase() throws IOException {
        InputStream mInput = mContext.getAssets().open(database_name);
        String outFileName = DB_PATH + database_name;
        OutputStream mOutput = new FileOutputStream(outFileName);
        byte[] mBuffer = new byte[1024];
        int mLength;
        while ((mLength = mInput.read(mBuffer)) > 0) {
            mOutput.write(mBuffer, 0, mLength);
        }
        mOutput.flush();
        mOutput.close();
        mInput.close();
    }

    @Override
    public synchronized void close() {
        if (db != null)
            db.close();
        super.close();
    }

    @Override
    public void onCreate(SQLiteDatabase db) {

        String tb_toko = "CREATE TABLE tb_toko (" +
                "id_toko INTEGER PRIMARY KEY AUTOINCREMENT," +
                "kode_toko TEXT(200)," +
                "nama_toko TEXT(200)," +
                "kota TEXT(100)," +
                "provinsi TEXT(100)," +
                "logo BLOB," +
                "logo_struk BLOB," +
                "email TEXT(100)," +
                "no_telepon TEXT(20)," +
                "id_jenis_usaha TEXT(100)," +
                "nama_jenis_usaha TEXT(200)," +
                "created_at TEXT DEFAULT (datetime('now'))," +
                "exp_date TEXT DEFAULT (datetime('now','+3 months'))," +
                "max_toko INTEGER DEFAULT 2," +
                "updated_at TEXT," +
                "deleted_at TEXT DEFAULT NULL" +
                ")";

        String tb_pegawai = "CREATE TABLE tb_pegawai (" +
                "id_pegawai INTEGER PRIMARY KEY AUTOINCREMENT," +
                "id_toko INTEGER," +
                "kode_pegawai TEXT(200)," +
                "nama_pegawai TEXT(200)," +
                "gender TEXT(20)," +
                "no_hp TEXT(20)," +
                "hak_akses TEXT(100)," +
                "posisi TEXT(100)," +
                "status TEXT(11)," +
                "username TEXT(200)," +
                "password TEXT(200)," +
                "pin TEXT(6)," +
                "created_at TEXT DEFAULT (datetime('now'))," +
                "updated_at TEXT," +
                "deleted_at TEXT DEFAULT NULL" +
                ")";

        String tb_kategori = "CREATE TABLE tb_kategori ( " +
                "id_kategori INTEGER PRIMARY KEY AUTOINCREMENT," +
                "id_toko INTEGER," +
                "kode_pegawai TEXT(200)," +
                "kategori TEXT(200), " +
                "created_at TEXT DEFAULT (datetime('now'))," +
                "updated_at TEXT," +
                "deleted_at TEXT DEFAULT NULL" +
                ")";

        String tb_produk = "CREATE TABLE tb_produk ( " +
                "id_produk INTEGER PRIMARY KEY AUTOINCREMENT," +
                "id_toko INTEGER," +
                "kode_produk TEXT(200)," +
                "kode_pegawai TEXT(200)," +
                "nama_produk TEXT(200)," +
                "kategori TEXT(200)," +
                "gambar TEXT(200)," +
                "sku TEXT(200)," +
                "harga_beli TEXT(200)," +
                "harga_jual TEXT(200)," +
                "laba TEXT(200)," +
                "favorit INTEGER DEFAULT 0," +
                "harga_diskon TEXT(200) DEFAULT '0'," +
                "stok INTEGER," +
                "created_at TEXT DEFAULT (datetime('now'))," +
                "updated_at TEXT," +
                "deleted_at TEXT DEFAULT NULL" +
                ")";

        String tb_pelanggan = "CREATE TABLE tb_pelanggan (" +
                "id_pelanggan INTEGER PRIMARY KEY AUTOINCREMENT," +
                "id_toko INTEGER," +
                "kode_pelanggan TEXT(200)," +
                "nama_pelanggan TEXT(200)," +
                "alamat TEXT(250)," +
                "kota TEXT(200)," +
                "no_hp TEXT(200)," +
                "gender TEXT(20)," +
                "email TEXT(100)," +
                "tgl_lahir TEXT(100)," +
                "catatan TEXT(200)," +
                "updated_at TEXT DEFAULT (datetime('now'))," +
                "deleted_at TEXT DEFAULT NULL" +
                ")";

        String tb_whitelist = "CREATE TABLE tb_whitelist (" +
                "id_whitelist INTEGER PRIMARY KEY AUTOINCREMENT," +
                "id_toko INTEGER," +
                "invoice TEXT(200)," +
                "nama_produk TEXT(200)," +
                "hrg_produk TEXT(100)," +
                "qty TEXT," +
                "updated_at TEXT DEFAULT (datetime('now'))," +
                "deleted_at TEXT DEFAULT NULL" +
                ")";

        String tb_printer = "CREATE TABLE tb_printer (" +
                "id_printer INTEGER PRIMARY KEY AUTOINCREMENT," +
                "id_toko INTEGER," +
                "nama_printer TEXT(200)," +
                "alamat_printer TEXT(200)," +
                "status_cetak_struk TEXT(200)," +
                "banyak_struk TEXT(200)," +
                "ukuran_struk TEXT(200)," +
                "tipe_printer TEXT(50)," +
                "created_at TEXT DEFAULT (datetime('now'))," +
                "updated_at TEXT," +
                "deleted_at TEXT DEFAULT NULL" +
                ")";

        String tb_penjualan = "CREATE TABLE tb_penjualan (" +
                "id_penjualan INTEGER PRIMARY KEY AUTOINCREMENT," +
                "id_toko INTEGER," +
                "kode_rekapkas TEXT(200)," +
                "kode_pegawai TEXT(200)," +
                "invoice TEXT(200)," +
                "jenis_pembayaran TEXT(100)," +
                "tanggal TEXT(100)," +
                "waktu TEXT(100)," +
                "bulan TEXT(100)," +
                "tahun TEXT(100)," +
                "status_diskon TEXT(100)," +
                "tipe_diskon TEXT(100)," +
                "nominal_disc TEXT(100)," +
                "kode_pelanggan TEXT(200)," +
                "nama_pelanggan TEXT(200)," +
                "jenis_kedatangan TEXT(100)," +
                "total_jml_transaksi TEXT(200)," +
                "dibayar TEXT(200)," +
                "kembali TEXT(200)," +
                "updated_at TEXT DEFAULT (datetime('now'))," +
                "deleted_at TEXT DEFAULT NULL" +
                ")";

        String tb_penjualan_detail = "CREATE TABLE tb_penjualan_detail (" +
                "id_penjualan_detail INTEGER PRIMARY KEY AUTOINCREMENT," +
                "invoice TEXT(200)," +
                "kode_produk TEXT(200)," +
                "nama_produk TEXT(200)," +
                "harga_produk TEXT(200)," +
                "qty TEXT(100)," +
                "total_harga TEXT(200)," +
                "laba TEXT(200)," +
                "updated_at TEXT DEFAULT (datetime('now'))," +
                "deleted_at TEXT DEFAULT NULL" +
                ")";

        String tb_pesanan = "CREATE TABLE tb_pesanan (" +
                "id_pesanan INTEGER PRIMARY KEY AUTOINCREMENT," +
                "id_toko INTEGER," +
                "kode_toko TEXT(200)," +
                "kode_pegawai TEXT(200)," +
                "invoice TEXT(200)," +
                "jenis_pembayaran TEXT(100)," +
                "tanggal TEXT(100)," +
                "waktu TEXT(100)," +
                "bulan TEXT(100)," +
                "tahun TEXT(100)," +
                "status_diskon TEXT(100)," +
                "tipe_diskon TEXT(100)," +
                "nominal_disc TEXT(100)," +
                "kode_pelanggan TEXT(200)," +
                "nama_pelanggan TEXT(200)," +
                "jenis_kedatangan TEXT(100)," +
                "total_jml_transaksi TEXT(200)," +
                "dibayar TEXT(200)," +
                "kembali TEXT(200)," +
                "created_at TEXT DEFAULT (datetime('now'))," +
                "updated_at TEXT," +
                "deleted_at TEXT DEFAULT NULL" +
                ")";

        String tb_pesanan_detail = "CREATE TABLE tb_pesanan_detail (" +
                "id_pesanan_detail INTEGER PRIMARY KEY AUTOINCREMENT," +
                "kode_toko TEXT(200)," +
                "invoice TEXT(200)," +
                "kode_produk TEXT(200)," +
                "nama_produk TEXT(200)," +
                "harga_produk TEXT(200)," +
                "qty TEXT(100)," +
                "total_harga TEXT(200)," +
                "created_at TEXT DEFAULT (datetime('now'))," +
                "updated_at TEXT," +
                "deleted_at TEXT DEFAULT NULL" +
                ")";

        String tb_setting_struk = "CREATE TABLE tb_setting_struk (" +
                " id_setting_struk INTEGER PRIMARY KEY AUTOINCREMENT," +
                " id_toko INTEGER," +
                " social_media TEXT(200)," +
                " footer TEXT(200)," +
                " alamat_toko TEXT(200)," +
                "created_at TEXT DEFAULT (datetime('now'))," +
                "updated_at TEXT," +
                "deleted_at TEXT DEFAULT NULL" +
                ")";

        String tb_jenis_kedatangan = "CREATE TABLE tb_jenis_kedatangan (" +
                " id_jenis_kedatangan INTEGER PRIMARY KEY AUTOINCREMENT," +
                " id_toko INTEGER," +
                " jenis_kedatangan TEXT(200)," +
                " kode_pegawai TEXT(200)," +
                "created_at TEXT DEFAULT (datetime('now'))," +
                "updated_at TEXT," +
                "deleted_at TEXT DEFAULT NULL" +
                ")";

        String tb_rekapkas = "CREATE TABLE tb_rekapkas (" +
                " id_rekapkas INTEGER PRIMARY KEY AUTOINCREMENT," +
                " id_toko INTEGER," +
                " kode_rekapkas TEXT(200)," +
                " kode_pegawai TEXT(200)," +
                " status TEXT(200)," +
                " waktu_buka TEXT(200)," +
                " waktu_tutup TEXT(200)," +
                " tgl_buka TEXT(200)," +
                " tgl_tutup TEXT(200)," +
                " date_awal TEXT(200)," +
                " date_tutup TEXT(200)," +
                "updated_at TEXT DEFAULT (datetime('now'))," +
                "deleted_at TEXT DEFAULT NULL" +
                ")";

        String tb_rekapkas_detail = "CREATE TABLE tb_rekapkas_detail (" +
                " id_rekapkas_detail INTEGER PRIMARY KEY AUTOINCREMENT," +
                " kode_rekapkas TEXT(200)," +
                " total_penjualan TEXT(200)," +
                " total_modalawal TEXT(200)," +
                " total_kaskeluar TEXT(200)," +
                " penerimaan_sistem TEXT(200)," +
                " penerimaan_tunai TEXT(200)," +
                " penerimaan_non_tunai TEXT(200)," +
                " selisih TEXT(200)," +
                " laba_pendapatan TEXT(200)," +
                "updated_at TEXT DEFAULT (datetime('now'))," +
                "deleted_at TEXT DEFAULT NULL" +
                ")";

        String tb_kas_keluar = "CREATE TABLE tb_kas_keluar (" +
                " id_kaskeluar INTEGER PRIMARY KEY AUTOINCREMENT," +
                " id_toko INTEGER," +
                " kode_rekapkas TEXT(200)," +
                " kode_kaskeluar TEXT(200)," +
                " kode_pegawai TEXT(200)," +
                " nominal TEXT(200)," +
                " keperluan TEXT(200)," +
                " tanggal TEXT(200)," +
                " waktu TEXT(200)," +
                " bulan TEXT(200)," +
                " tahun TEXT(200)," +
                "updated_at TEXT DEFAULT (datetime('now'))," +
                "deleted_at TEXT DEFAULT NULL" +
                ")";

        db.execSQL(tb_toko);
        db.execSQL(tb_pegawai);
        db.execSQL(tb_kategori);
        db.execSQL(tb_produk);
        db.execSQL(tb_pelanggan);
        db.execSQL(tb_whitelist);
        db.execSQL(tb_printer);
        db.execSQL(tb_penjualan);
        db.execSQL(tb_penjualan_detail);
        db.execSQL(tb_pesanan);
        db.execSQL(tb_pesanan_detail);
        db.execSQL(tb_setting_struk);
        db.execSQL(tb_jenis_kedatangan);
        db.execSQL(tb_rekapkas);
        db.execSQL(tb_rekapkas_detail);
        db.execSQL(tb_kas_keluar);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 6) {
            try {
                db.execSQL("ALTER TABLE tb_kas_keluar ADD COLUMN kode_kaskeluar TEXT(200)");
            } catch (Exception e) {
                android.util.Log.w("DBUpgrade", "kode_kaskeluar mungkin sudah ada: " + e.getMessage());
            }

            db.execSQL(
                    "UPDATE tb_kas_keluar " +
                            "SET kode_kaskeluar = 'KK-LAMA-' || CAST(id_kaskeluar AS TEXT) " +
                            "WHERE kode_kaskeluar IS NULL OR kode_kaskeluar = ''"
            );
        }
        // Tambahkan blok if (oldVersion < 7) { ... } di sini untuk migrasi berikutnya
    }

    private String getCurrentDateTime() {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());
    }

    private String generateKodeKasKeluar() {
        String tanggal = new java.text.SimpleDateFormat(
                "ddMMyy", java.util.Locale.getDefault()
        ).format(new java.util.Date());

        String milis = String.valueOf(System.currentTimeMillis() % 10000);
        while (milis.length() < 4) milis = "0" + milis;

        return "KK-" + tanggal + "-" + milis;
    }

    public ArrayList<String> getAllTokoNames() {
        ArrayList<String> tokoNames = new ArrayList<>();
        Cursor cur = db.rawQuery("SELECT nama_toko FROM tb_toko WHERE deleted_at IS NULL", null);
        if (cur.moveToFirst()) {
            do {
                tokoNames.add(cur.getString(0));
            } while (cur.moveToNext());
        }
        cur.close();
        return tokoNames;
    }

    public ArrayList<Integer> getAllTokoIds() {
        ArrayList<Integer> tokoIds = new ArrayList<>();
        Cursor cur = db.rawQuery("SELECT id_toko FROM tb_toko WHERE deleted_at IS NULL", null);
        if (cur.moveToFirst()) {
            do {
                tokoIds.add(cur.getInt(0));
            } while (cur.moveToNext());
        }
        cur.close();
        return tokoIds;
    }

    public Cursor getKodeToko() {
        Cursor cur = db.rawQuery("SELECT * FROM tb_toko WHERE kode_toko = (SELECT MAX(kode_toko) FROM tb_toko)", null);
        return cur;
    }

    public String getIdTokoByKodePegawai(String kodePegawai) {
        String idToko = null;
        Cursor cursor = null;
        try {
            cursor = db.rawQuery(
                    "SELECT id_toko FROM tb_pegawai WHERE kode_pegawai = ? LIMIT 1",
                    new String[]{kodePegawai}
            );
            if (cursor != null && cursor.moveToFirst()) {
                idToko = cursor.getString(0);
            }
        } catch (Exception e) {
            Log.e("DataBaseHelper", "Error getIdTokoByKodePegawai: " + e.getMessage());
        } finally {
            if (cursor != null) cursor.close();
        }
        return idToko;
    }

    /**
     * Ambil nama toko pertama dari tb_toko sebagai fallback
     * saat sessionManager.getNamatoko() belum di-set.
     * Digunakan oleh RegistrasiUser.uploadToServer() untuk memastikan
     * nama_toko tidak null saat dikirim ke server.
     */
    public String getNamaTokoUtama() {
        SQLiteDatabase db = this.getReadableDatabase();
        String nama = null;
        Cursor cursor = null;
        try {
            cursor = db.rawQuery(
                    "SELECT nama_toko FROM tb_toko WHERE deleted_at IS NULL ORDER BY id_toko ASC LIMIT 1",
                    null
            );
            if (cursor != null && cursor.moveToFirst()) {
                nama = cursor.getString(0);
            }
        } catch (Exception e) {
            Log.e("DataBaseHelper", "getNamaTokoUtama error: " + e.getMessage());
        } finally {
            if (cursor != null) cursor.close();
        }
        return nama;
    }

    public List<Outlet> getAllOutlet() {
        List<Outlet> outletList = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.rawQuery("SELECT id_toko, kode_toko, nama_toko, kota, provinsi, email, no_telepon, logo FROM tb_toko WHERE deleted_at IS NULL", null);

        if (cursor.moveToFirst()) {
            do {
                int id = cursor.getInt(0);
                String kode = cursor.getString(1);
                String nama = cursor.getString(2);
                String kota = cursor.getString(3);
                String provinsi = cursor.getString(4);
                String email = cursor.getString(5);
                String nohp = cursor.getString(6);
                byte[] logo = cursor.getBlob(7);

                outletList.add(new Outlet(id, kode, nama, kota, provinsi, email, nohp, logo));
            } while (cursor.moveToNext());
        }

        cursor.close();
        db.close();
        return outletList;
    }

    public List<String[]> getAllOutletWithId() {
        List<String[]> outlets = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT id_toko, nama_toko FROM tb_toko WHERE deleted_at IS NULL", null);
        if (cursor.moveToFirst()) {
            do {
                outlets.add(new String[]{cursor.getString(0), cursor.getString(1)});
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return outlets;
    }

    public boolean updateOutlet(int idToko, ContentValues values) {
        SQLiteDatabase db = this.getWritableDatabase();
        int result = db.update("tb_toko", values, "id_toko = ?", new String[]{String.valueOf(idToko)});
        db.close();
        return result > 0;
    }

    public void deleteOutlet(int idToko) {
        ContentValues values = new ContentValues();
        values.put("deleted_at", getCurrentDateTime());
        values.put("updated_at", getCurrentDateTime());
        int result = db.update("tb_toko", values, "id_toko = ?", new String[]{String.valueOf(idToko)});
    }

    public String generateKodeToko() {
        SQLiteDatabase db = this.getReadableDatabase();
        String kodeBaru;
        boolean exists;

        do {
            String date = new java.text.SimpleDateFormat("ddMMyy", java.util.Locale.getDefault())
                    .format(new java.util.Date());

            int random = new java.util.Random().nextInt(9000) + 1000;

            kodeBaru = "TK" + date + random;

            Cursor cursor = db.rawQuery(
                    "SELECT kode_toko FROM tb_toko WHERE kode_toko = ?",
                    new String[]{kodeBaru}
            );

            exists = cursor.moveToFirst();
            cursor.close();

        } while (exists);

        return kodeBaru;
    }

    public int getMaxToko() {

        int maxToko = 1;

        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.rawQuery(
                "SELECT max_toko FROM tb_toko ORDER BY id_toko ASC LIMIT 1",
                null
        );

        if (cursor != null) {
            if (cursor.moveToFirst()) {
                maxToko = cursor.getInt(0);
            }
            cursor.close();
        }

        return maxToko;
    }

    public String getExpiredDate() {
        String expDate = null;

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT exp_date FROM tb_toko WHERE id_toko = ? LIMIT 1",
                new String[]{"1"}
        );

        if (cursor != null) {
            if (cursor.moveToFirst()) {
                expDate = cursor.getString(0);
            }
            cursor.close();
        }

        return expDate;
    }

    public boolean syncAndCheckExpDate(String expServer, int maxToko) {
        try {
            if (expServer == null || expServer.isEmpty()) return false;

            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());

            Date serverDate = sdf.parse(expServer);

            String expLocalStr = getExpDateToko();
            int maxTokoLocal = getMaxToko();

            boolean isExpDifferent = (expLocalStr == null || !expServer.equals(expLocalStr));
            boolean isMaxTokoDifferent = (maxToko != maxTokoLocal);

            if (isExpDifferent || isMaxTokoDifferent) {

                SQLiteDatabase db = this.getWritableDatabase();

                String nowStr = sdf.format(new Date());

                ContentValues values = new ContentValues();
                values.put("exp_date", expServer);
                values.put("max_toko", maxToko);
                values.put("updated_at", nowStr);

                db.update(
                        "tb_toko",
                        values,
                        "deleted_at IS NULL",
                        null
                );
            }

            Date now = new Date();
            return serverDate.before(now);

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public Cursor getTokoById(String id_toko) {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("SELECT * FROM tb_toko WHERE id_toko = ?", new String[]{id_toko});
    }

    public long insertDataToko(ContentValues values) {
        SQLiteDatabase db = this.getWritableDatabase();
        return db.insert("tb_toko", null, values);
    }

    public Cursor dataToko(String idToko) {
        return db.rawQuery(
                "SELECT * FROM tb_toko WHERE id_toko = ? AND deleted_at IS NULL",
                new String[]{idToko}
        );
    }

    public void updateToko(ContentValues values, String kodeToko) {
        values.put("updated_at", getCurrentDateTime());
        db.update("tb_toko", values, "kode_toko = ?", new String[]{kodeToko});
    }

    public int getFirstTokoId() {
        int id = -1;
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;
        try {
            cursor = db.rawQuery("SELECT id_toko FROM tb_toko ORDER BY id_toko ASC LIMIT 1", null);
            if (cursor.moveToFirst()) {
                id = cursor.getInt(0);
                AndLog.ShowLog("DB", "getFirstTokoId: berhasil, id_toko=" + id);
            } else {
                AndLog.ShowLog("DB", "getFirstTokoId: tidak ada data");
            }
        } catch (Exception e) {
            AndLog.ShowLog("DB", "getFirstTokoId: error -> " + e.getMessage());
        } finally {
            if (cursor != null) cursor.close();
            db.close();
        }
        return id;
    }

    public boolean registrasiUsers(ContentValues values) {
        SQLiteDatabase db = this.getWritableDatabase();
        long result = -1;

        values.put("created_at", getCurrentDateTime());

        try {
            result = db.insert("tb_pegawai", null, values);
            if (result != -1) {
                AndLog.ShowLog("DB", "registrasiUsers: berhasil, values=" + values.toString() + ", rowId=" + result);
            } else {
                AndLog.ShowLog("DB", "registrasiUsers: gagal, values=" + values.toString());
            }
        } catch (Exception e) {
            AndLog.ShowLog("DB", "registrasiUsers: error -> " + e.getMessage());
        } finally {
            db.close();
        }
        return result != -1;
    }

    public Cursor listKategoriOwner() {
        Cursor cur = db.rawQuery("SELECT * FROM tb_kategori", null);
        return cur;
    }

    public Cursor listKategori(String idToko) {
        Cursor cur = db.rawQuery(
                "SELECT * FROM tb_kategori WHERE id_toko = ? AND deleted_at IS NULL",
                new String[]{idToko}
        );
        return cur;
    }

    public void tambahKategori(ContentValues values) {
        values.put("created_at", getCurrentDateTime());
        db.insert("tb_kategori", null, values);
    }

    public void deleteData(String id) {
        ContentValues values = new ContentValues();
        values.put("deleted_at", getCurrentDateTime());
        db.update("tb_kategori", values, "id_kategori = ?", new String[]{id});
    }

    public void updateData(ContentValues values, String id) {
        values.put("updated_at", getCurrentDateTime());
        db.update("tb_kategori", values, "id_kategori = ? AND deleted_at IS NULL", new String[]{id});
    }

    public ArrayList<String> kategoriSpiner(String idToko) {
        ArrayList<String> listKategori = new ArrayList<>();
        String selectQuery = "SELECT * FROM tb_kategori WHERE id_toko = ? AND deleted_at IS NULL";
        Cursor cursor = db.rawQuery(selectQuery, new String[]{idToko});

        if (cursor.moveToFirst()) {
            do {
                listKategori.add(cursor.getString(cursor.getColumnIndexOrThrow("kategori")));
            } while (cursor.moveToNext());
        }

        cursor.close();
        return listKategori;
    }

    public Cursor listProduk(String idToko) {
        return db.rawQuery(
                "SELECT * FROM tb_produk WHERE id_toko = ? AND deleted_at IS NULL",
                new String[]{idToko}
        );
    }

    public Cursor listProdukDiskon(String idToko) {
        return db.rawQuery(
                "SELECT * FROM tb_produk WHERE harga_diskon != '0' AND id_toko = ? AND deleted_at IS NULL",
                new String[]{idToko}
        );
    }

    public Cursor listProdukFavorit(String idToko) {
        return db.rawQuery(
                "SELECT * FROM tb_produk WHERE favorit = 1 AND id_toko = ? AND deleted_at IS NULL",
                new String[]{idToko}
        );
    }

    public String[] getStringKategori(ArrayList<KategoriModels> arrayList) {
        String[] str = new String[arrayList.size()];
        for (int j = 0; j < arrayList.size(); j++) {
            str[j] = "'" + arrayList.get(j).getKategori() + "'";
        }
        return str;
    }

    public Cursor listProdukWhereKategori(ArrayList<KategoriModels> arrayList, String idToko) {

        String[] values = getStringKategori(arrayList);

        String clause = Arrays.toString(values)
                .replace("[", "(")
                .replace("]", ")");

        String sql = "SELECT * FROM tb_produk " +
                "WHERE kategori IN " + clause + " " +
                "AND id_toko = ? " +
                "AND deleted_at IS NULL";

        return db.rawQuery(sql, new String[]{ idToko });
    }

    public Cursor listProdukDiskonWhereKategori(ArrayList<KategoriModels> arrayList, String idToko) {
        String[] values = getStringKategori(arrayList);
        String clause = Arrays.toString(values)
                .replace("[", "(")
                .replace("]", ")");

        String sql = "SELECT * FROM tb_produk " +
                "WHERE harga_diskon != '0' " +
                "AND kategori IN " + clause + " " +
                "AND id_toko = ? " +
                "AND deleted_at IS NULL";

        return db.rawQuery(sql, new String[]{ idToko });
    }

    public Cursor listProdukFavoritWhereKategori(ArrayList<KategoriModels> arrayList, String idToko) {
        String[] values = getStringKategori(arrayList);
        String clause = Arrays.toString(values)
                .replace("[", "(")
                .replace("]", ")");

        String sql = "SELECT * FROM tb_produk " +
                "WHERE favorit = 1 " +
                "AND kategori IN " + clause + " " +
                "AND id_toko = ? " +
                "AND deleted_at IS NULL";

        return db.rawQuery(sql, new String[]{ idToko });
    }

    public void tambahProduk(ContentValues values) {
        values.put("created_at", getCurrentDateTime());
        db.insert("tb_produk", null, values);
    }

    public void insertTemplate(String templateName, int idToko, String kodePegawai) {
        SQLiteDatabase db = this.getWritableDatabase();
        String now = getCurrentDateTime();

        try {
            switch (templateName) {
                case "Barber":
                    db.execSQL("INSERT INTO tb_jenis_kedatangan (id_toko, jenis_kedatangan, kode_pegawai, created_at) VALUES (?, ?, ?, ?)",
                            new Object[]{idToko, "Datang Langsung", kodePegawai, now});
                    db.execSQL("INSERT INTO tb_jenis_kedatangan (id_toko, jenis_kedatangan, kode_pegawai, created_at) VALUES (?, ?, ?, ?)",
                            new Object[]{idToko, "Booking Online", kodePegawai, now});

                    db.execSQL("INSERT INTO tb_kategori (id_toko, kode_pegawai, kategori, created_at) VALUES (?, ?, ?, ?)",
                            new Object[]{idToko, kodePegawai, "Pangkas Rambut", now});
                    db.execSQL("INSERT INTO tb_kategori (id_toko, kode_pegawai, kategori, created_at) VALUES (?, ?, ?, ?)",
                            new Object[]{idToko, kodePegawai, "Perawatan Rambut", now});

                    byte[] imgPR = bitmapToByteArray(generateTextIcon("Pangkas Reguler"));
                    byte[] imgC = bitmapToByteArray(generateTextIcon("Creambath"));

                    ContentValues cv1 = new ContentValues();
                    cv1.put("id_toko", idToko);
                    cv1.put("kode_produk", getRandomKodeProduk());
                    cv1.put("kode_pegawai", kodePegawai);
                    cv1.put("nama_produk", "Pangkas Reguler");
                    cv1.put("kategori", "Pangkas Rambut");
                    cv1.put("gambar", imgPR);
                    cv1.put("sku", "BRB-01");
                    cv1.put("harga_beli", 0);
                    cv1.put("harga_jual", 20000);
                    cv1.put("laba", 20000);
                    cv1.put("favorit", 0);
                    cv1.put("harga_diskon", 0);
                    cv1.putNull("stok");
                    cv1.put("created_at", now);
                    db.insert("tb_produk", null, cv1);

                    ContentValues cv2 = new ContentValues();
                    cv2.put("id_toko", idToko);
                    cv2.put("kode_produk", getRandomKodeProduk());
                    cv2.put("kode_pegawai", kodePegawai);
                    cv2.put("nama_produk", "Creambath");
                    cv2.put("kategori", "Perawatan Rambut");
                    cv2.put("gambar", imgC);
                    cv2.put("sku", "BRB-02");
                    cv2.put("harga_beli", 0);
                    cv2.put("harga_jual", 35000);
                    cv2.put("laba", 35000);
                    cv2.put("favorit", 0);
                    cv2.put("harga_diskon", 0);
                    cv2.putNull("stok");
                    cv2.put("created_at", now);
                    db.insert("tb_produk", null, cv2);
                    break;

                case "Toko Buku":
                    db.execSQL("INSERT INTO tb_jenis_kedatangan (id_toko, jenis_kedatangan, kode_pegawai, created_at) VALUES (?, ?, ?, ?)",
                            new Object[]{idToko, "Datang Langsung", kodePegawai, now});
                    db.execSQL("INSERT INTO tb_jenis_kedatangan (id_toko, jenis_kedatangan, kode_pegawai, created_at) VALUES (?, ?, ?, ?)",
                            new Object[]{idToko, "Pesan Online", kodePegawai, now});

                    db.execSQL("INSERT INTO tb_kategori (id_toko, kode_pegawai, kategori, created_at) VALUES (?, ?, ?, ?)",
                            new Object[]{idToko, kodePegawai, "Buku Pelajaran", now});
                    db.execSQL("INSERT INTO tb_kategori (id_toko, kode_pegawai, kategori, created_at) VALUES (?, ?, ?, ?)",
                            new Object[]{idToko, kodePegawai, "Alat Tulis", now});

                    byte[] imgBS = bitmapToByteArray(generateTextIcon("Buku Matematika"));
                    byte[] imgPS = bitmapToByteArray(generateTextIcon("Pulpen Snowman"));

                    ContentValues cv3 = new ContentValues();
                    cv3.put("id_toko", idToko);
                    cv3.put("kode_produk", getRandomKodeProduk());
                    cv3.put("kode_pegawai", kodePegawai);
                    cv3.put("nama_produk", "Buku Matematika");
                    cv3.put("kategori", "Buku Pelajaran");
                    cv3.put("gambar", imgBS);
                    cv3.put("sku", "BK-01");
                    cv3.put("harga_beli", 10000);
                    cv3.put("harga_jual", 15000);
                    cv3.put("laba", 5000);
                    cv3.put("favorit", 0);
                    cv3.put("harga_diskon", 0);
                    cv3.put("stok", 50);
                    cv3.put("created_at", now);
                    db.insert("tb_produk", null, cv3);

                    ContentValues cv4 = new ContentValues();
                    cv4.put("id_toko", idToko);
                    cv4.put("kode_produk", getRandomKodeProduk());
                    cv4.put("kode_pegawai", kodePegawai);
                    cv4.put("nama_produk", "Pulpen Snowman");
                    cv4.put("kategori", "Alat Tulis");
                    cv4.put("gambar", imgPS);
                    cv4.put("sku", "BK-02");
                    cv4.put("harga_beli", 2000);
                    cv4.put("harga_jual", 3500);
                    cv4.put("laba", 1500);
                    cv4.put("favorit", 0);
                    cv4.put("harga_diskon", 0);
                    cv4.put("stok", 100);
                    cv4.put("created_at", now);
                    db.insert("tb_produk", null, cv4);
                    break;

                case "Warung":
                    db.execSQL("INSERT INTO tb_jenis_kedatangan (id_toko, jenis_kedatangan, kode_pegawai, created_at) VALUES (?, ?, ?, ?)",
                            new Object[]{idToko, "Makan di Tempat", kodePegawai, now});
                    db.execSQL("INSERT INTO tb_jenis_kedatangan (id_toko, jenis_kedatangan, kode_pegawai, created_at) VALUES (?, ?, ?, ?)",
                            new Object[]{idToko, "Bungkus", kodePegawai, now});

                    db.execSQL("INSERT INTO tb_kategori (id_toko, kode_pegawai, kategori, created_at) VALUES (?, ?, ?, ?)",
                            new Object[]{idToko, kodePegawai, "Makanan", now});
                    db.execSQL("INSERT INTO tb_kategori (id_toko, kode_pegawai, kategori, created_at) VALUES (?, ?, ?, ?)",
                            new Object[]{idToko, kodePegawai, "Minuman", now});

                    byte[] imgNG = bitmapToByteArray(generateTextIcon("Nasi Goreng"));
                    byte[] imgETM = bitmapToByteArray(generateTextIcon("Es Teh Manis"));

                    ContentValues cv5 = new ContentValues();
                    cv5.put("id_toko", idToko);
                    cv5.put("kode_produk", getRandomKodeProduk());
                    cv5.put("kode_pegawai", kodePegawai);
                    cv5.put("nama_produk", "Nasi Goreng");
                    cv5.put("kategori", "Makanan");
                    cv5.put("gambar", imgNG);
                    cv5.put("sku", "WRG-01");
                    cv5.put("harga_beli", 8000);
                    cv5.put("harga_jual", 12000);
                    cv5.put("laba", 4000);
                    cv5.put("favorit", 0);
                    cv5.put("harga_diskon", 0);
                    cv5.put("stok", 30);
                    cv5.put("created_at", now);
                    db.insert("tb_produk", null, cv5);

                    ContentValues cv6 = new ContentValues();
                    cv6.put("id_toko", idToko);
                    cv6.put("kode_produk", getRandomKodeProduk());
                    cv6.put("kode_pegawai", kodePegawai);
                    cv6.put("nama_produk", "Es Teh Manis");
                    cv6.put("kategori", "Minuman");
                    cv6.put("gambar", imgETM);
                    cv6.put("sku", "WRG-02");
                    cv6.put("harga_beli", 1000);
                    cv6.put("harga_jual", 3000);
                    cv6.put("laba", 2000);
                    cv6.put("favorit", 0);
                    cv6.put("harga_diskon", 0);
                    cv6.put("stok", 50);
                    cv6.put("created_at", now);
                    db.insert("tb_produk", null, cv6);
                    break;

                case "Toko Biasa":
                    db.execSQL("INSERT INTO tb_jenis_kedatangan (id_toko, jenis_kedatangan, kode_pegawai, created_at) VALUES (?, ?, ?, ?)",
                            new Object[]{idToko, "Datang Langsung", kodePegawai, now});
                    db.execSQL("INSERT INTO tb_jenis_kedatangan (id_toko, jenis_kedatangan, kode_pegawai, created_at) VALUES (?, ?, ?, ?)",
                            new Object[]{idToko, "COD", kodePegawai, now});

                    db.execSQL("INSERT INTO tb_kategori (id_toko, kode_pegawai, kategori, created_at) VALUES (?, ?, ?, ?)",
                            new Object[]{idToko, kodePegawai, "Sembako", now});
                    db.execSQL("INSERT INTO tb_kategori (id_toko, kode_pegawai, kategori, created_at) VALUES (?, ?, ?, ?)",
                            new Object[]{idToko, kodePegawai, "Perlengkapan Rumah", now});

                    byte[] imgTK1 = bitmapToByteArray(generateTextIcon("Minyak Goreng 1L"));
                    byte[] imgSM = bitmapToByteArray(generateTextIcon("Sabun Mandi"));

                    ContentValues cv7 = new ContentValues();
                    cv7.put("id_toko", idToko);
                    cv7.put("kode_produk", getRandomKodeProduk());
                    cv7.put("kode_pegawai", kodePegawai);
                    cv7.put("nama_produk", "Minyak Goreng 1L");
                    cv7.put("kategori", "Sembako");
                    cv7.put("gambar", imgTK1);
                    cv7.put("sku", "TK-01");
                    cv7.put("harga_beli", 13000);
                    cv7.put("harga_jual", 17000);
                    cv7.put("laba", 4000);
                    cv7.put("favorit", 0);
                    cv7.put("harga_diskon", 0);
                    cv7.put("stok", 40);
                    cv7.put("created_at", now);
                    db.insert("tb_produk", null, cv7);

                    ContentValues cv8 = new ContentValues();
                    cv8.put("id_toko", idToko);
                    cv8.put("kode_produk", getRandomKodeProduk());
                    cv8.put("kode_pegawai", kodePegawai);
                    cv8.put("nama_produk", "Sabun Mandi");
                    cv8.put("kategori", "Perlengkapan Rumah");
                    cv8.put("gambar", imgSM);
                    cv8.put("sku", "TK-02");
                    cv8.put("harga_beli", 3000);
                    cv8.put("harga_jual", 5000);
                    cv8.put("laba", 2000);
                    cv8.put("favorit", 0);
                    cv8.put("harga_diskon", 0);
                    cv8.put("stok", 60);
                    cv8.put("created_at", now);
                    db.insert("tb_produk", null, cv8);
                    break;
            }
        } catch (Exception e) {
            Log.e("DBHelper", "Gagal insert template: " + e.getMessage());
        }
    }

    public void copyMasterDataFiltered(
            int idTokoSumber,
            int idTokoTujuan,
            boolean copyJenis,
            boolean copyKategori,
            boolean copyProduk
    ) {
        SQLiteDatabase db = this.getWritableDatabase();

        Log.d("COPY_MASTER", "Mulai proses copy dari toko " + idTokoSumber + " ke " + idTokoTujuan);

        if (copyJenis) {
            Log.d("COPY_MASTER", "Menyalin tb_jenis_kedatangan...");

            Cursor cJenis = db.rawQuery(
                    "SELECT jenis_kedatangan, kode_pegawai FROM tb_jenis_kedatangan " +
                            "WHERE id_toko = ? AND deleted_at IS NULL",
                    new String[]{String.valueOf(idTokoSumber)}
            );

            while (cJenis.moveToNext()) {
                String nama = cJenis.getString(0);
                Log.d("COPY_MASTER", "Cek jenis kedatangan: " + nama);

                Cursor cek = db.rawQuery(
                        "SELECT COUNT(*) FROM tb_jenis_kedatangan " +
                                "WHERE id_toko = ? AND jenis_kedatangan = ? AND deleted_at IS NULL",
                        new String[]{String.valueOf(idTokoTujuan), nama}
                );
                cek.moveToFirst();

                if (cek.getInt(0) == 0) {
                    Log.d("COPY_MASTER", "→ Insert jenis_kedatangan: " + nama);

                    ContentValues cv = new ContentValues();
                    cv.put("id_toko", idTokoTujuan);
                    cv.put("jenis_kedatangan", nama);
                    cv.put("kode_pegawai", cJenis.getString(1));
                    cv.put("created_at", getCurrentDateTime());
                    db.insert("tb_jenis_kedatangan", null, cv);
                } else {
                    Log.d("COPY_MASTER", "→ Lewati (sudah ada): " + nama);
                }

                cek.close();
            }
            cJenis.close();
        }

        if (copyKategori) {
            Log.d("COPY_MASTER", "Menyalin tb_kategori...");

            Cursor cKategori = db.rawQuery(
                    "SELECT kategori, kode_pegawai FROM tb_kategori " +
                            "WHERE id_toko = ? AND deleted_at IS NULL",
                    new String[]{String.valueOf(idTokoSumber)}
            );

            while (cKategori.moveToNext()) {
                String nama = cKategori.getString(0);
                Log.d("COPY_MASTER", "Cek kategori: " + nama);

                Cursor cek = db.rawQuery(
                        "SELECT COUNT(*) FROM tb_kategori " +
                                "WHERE id_toko = ? AND kategori = ? AND deleted_at IS NULL",
                        new String[]{String.valueOf(idTokoTujuan), nama}
                );
                cek.moveToFirst();

                if (cek.getInt(0) == 0) {
                    Log.d("COPY_MASTER", "→ Insert kategori: " + nama);

                    ContentValues cv = new ContentValues();
                    cv.put("id_toko", idTokoTujuan);
                    cv.put("kategori", nama);
                    cv.put("kode_pegawai", cKategori.getString(1));
                    cv.put("created_at", getCurrentDateTime());
                    db.insert("tb_kategori", null, cv);
                } else {
                    Log.d("COPY_MASTER", "→ Lewati (sudah ada): " + nama);
                }

                cek.close();
            }
            cKategori.close();
        }

        if (copyProduk) {
            Log.d("COPY_MASTER", "Menyalin tb_produk...");

            Cursor cProduk = db.rawQuery(
                    "SELECT nama_produk, kategori, gambar, sku, harga_beli, harga_jual, laba, favorit, harga_diskon, stok, kode_pegawai " +
                            "FROM tb_produk WHERE id_toko = ? AND deleted_at IS NULL",
                    new String[]{String.valueOf(idTokoSumber)}
            );

            while (cProduk.moveToNext()) {
                String namaProduk = cProduk.getString(0);
                Log.d("COPY_MASTER", "Cek produk: " + namaProduk);

                Cursor cek = db.rawQuery(
                        "SELECT COUNT(*) FROM tb_produk " +
                                "WHERE id_toko = ? AND nama_produk = ? AND deleted_at IS NULL",
                        new String[]{String.valueOf(idTokoTujuan), namaProduk}
                );
                cek.moveToFirst();

                if (cek.getInt(0) == 0) {
                    Log.d("COPY_MASTER", "→ Insert produk: " + namaProduk);

                    ContentValues cv = new ContentValues();
                    cv.put("id_toko", idTokoTujuan);
                    cv.put("nama_produk", namaProduk);
                    cv.put("kategori", cProduk.getString(1));
                    cv.put("gambar", cProduk.getBlob(2));
                    cv.put("sku", cProduk.getString(3));
                    cv.put("harga_beli", cProduk.getString(4));
                    cv.put("harga_jual", cProduk.getString(5));
                    cv.put("laba", cProduk.getString(6));
                    cv.put("favorit", cProduk.getInt(7));
                    cv.put("harga_diskon", cProduk.getString(8));
                    cv.put("stok", cProduk.getInt(9));
                    cv.put("kode_pegawai", cProduk.getString(10));
                    cv.put("kode_produk", getRandomKodeProduk());
                    cv.put("created_at", getCurrentDateTime());

                    db.insert("tb_produk", null, cv);
                } else {
                    Log.d("COPY_MASTER", "→ Lewati (sudah ada): " + namaProduk);
                }

                cek.close();
            }
            cProduk.close();
        }

        Log.d("COPY_MASTER", "Selesai proses copy master data.");
    }

    private String getRandomKodeProduk() {
        String date = new java.text.SimpleDateFormat("ddMMyyyy").format(new java.util.Date());

        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder sb = new StringBuilder();
        java.util.Random rnd = new java.util.Random();

        for (int i = 0; i < 6; i++) {
            sb.append(chars.charAt(rnd.nextInt(chars.length())));
        }

        return "KDB-" + date + "-" + sb.toString();
    }

    public byte[] bitmapToByteArray(Bitmap bitmap) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
        return stream.toByteArray();
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

    public void deleteProduk(String id) {
        ContentValues values = new ContentValues();
        String now = getCurrentDateTime();
        values.put("deleted_at", now);
        db.update("tb_produk", values, "kode_produk = ?", new String[]{id});
    }

    public void updateProduk(ContentValues values, String id) {
        String now = getCurrentDateTime();
        values.put("updated_at", now);
        db.update("tb_produk", values, "kode_produk = ?", new String[]{id});
    }

    public Cursor getDetailProduk(String kode_produk) {
        return db.rawQuery(
                "SELECT * FROM tb_produk WHERE kode_produk = ? AND deleted_at IS NULL",
                new String[]{kode_produk}
        );
    }

    public Cursor getProfileOwner(String kode_pegawai) {
        return db.rawQuery(
                "SELECT * FROM tb_pegawai WHERE posisi = 'Owner' AND kode_pegawai = ? AND deleted_at IS NULL",
                new String[]{kode_pegawai}
        );
    }

    public Cursor getOwnerUtama() {
        return db.rawQuery(
                "SELECT kode_pegawai FROM tb_pegawai WHERE hak_akses='Owner' AND posisi='Owner' AND deleted_at IS NULL LIMIT 1",
                null
        );
    }

    public Cursor getDetailPegawai(String kode_pegawai) {
        return db.rawQuery(
                "SELECT * FROM tb_pegawai WHERE kode_pegawai = ? AND deleted_at IS NULL",
                new String[]{kode_pegawai}
        );
    }

    public void updateDataOwner(ContentValues values, String id) {
        String now = getCurrentDateTime();
        values.put("updated_at", now);
        db.update("tb_pegawai", values, "kode_pegawai = ?", new String[]{id});
    }

    public Cursor listPegawai() {
        return db.rawQuery(
                "SELECT * FROM tb_pegawai WHERE posisi = 'Pegawai' AND deleted_at IS NULL",
                null
        );
    }

    public int deletePegawai(String kodePegawai) {
        SQLiteDatabase db = this.getWritableDatabase();
        Log.d("DEBUG_DELETE", "Mencoba hapus pegawai dengan kode: " + kodePegawai);

        ContentValues values = new ContentValues();
        String now = getCurrentDateTime();
        values.put("deleted_at", now);
        values.put("updated_at", now);

        int rowsUpdated = db.update(
                "tb_pegawai",
                values,
                "kode_pegawai = ?",
                new String[]{kodePegawai}
        );

        Log.d("DEBUG_DELETE", "Jumlah row yang di-soft-delete: " + rowsUpdated);
        return rowsUpdated;
    }

    public ArrayList<String> spinPegawai() {
        ArrayList<String> listPegawai = new ArrayList<>();

        String selectQuery = "SELECT * FROM tb_pegawai WHERE deleted_at IS NULL";
        Cursor cursor = db.rawQuery(selectQuery, null);

        if (cursor.moveToFirst()) {
            do {
                listPegawai.add(cursor.getString(9));
            } while (cursor.moveToNext());
        }

        cursor.close();
        return listPegawai;
    }

    // ============================================================
    // FASE 1 — PERUBAHAN C: login() diperbarui
    //
    // SEBELUM: membandingkan plaintext vs plaintext
    //   WHERE username = ? AND password = ?
    //   → password dari input langsung dibandingkan ke DB
    //
    // SESUDAH: hash input dulu sebelum dibandingkan
    //   → Jika DB lokal menyimpan SHA-256 (user baru):
    //     hash(inputPassword) == sha256DiDB  ✓
    //   → Jika DB lokal menyimpan bcrypt dari server (setelah Login Online):
    //     tidak bisa dibandingkan di sini — gunakan Login Online.
    //     Untuk antisipasi kasus ini, fungsi tetap mencoba query biasa
    //     sehingga hasil null (tidak cocok), dan user akan diarahkan
    //     ke Login Online oleh Activity pemanggil.
    // ============================================================
    public Cursor login(String username, String password) {
        String hashedPassword = hashPassword(password);
        String query = "SELECT * FROM tb_pegawai WHERE username = ? AND password = ? AND deleted_at IS NULL";
        return db.rawQuery(query, new String[]{username, hashedPassword});
    }

    public Cursor loginPegawai(String username, String pin) {
        // FIX: Defensive guard — tolak parameter null sebelum diteruskan ke SQLite.
        // SQLite Android melempar IllegalArgumentException jika nilai bind adalah null.
        // Validasi utama seharusnya dilakukan di PilihKaryawan.validated(),
        // tetapi lapisan ini mencegah crash jika fungsi dipanggil dari tempat lain.
        if (username == null || username.trim().isEmpty()) {
            return null;
        }
        if (pin == null || pin.trim().isEmpty()) {
            return null;
        }
        // PIN belum di-hash (akan dihandle di Fase 4-5)
        String query = "SELECT * FROM tb_pegawai WHERE username = ? AND pin = ? AND deleted_at IS NULL";
        return db.rawQuery(query, new String[]{username.trim(), pin.trim()});
    }

    public Cursor getPegawaiByKode(String kodePegawai) {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery(
                "SELECT username, password FROM tb_pegawai WHERE kode_pegawai = ? LIMIT 1",
                new String[]{kodePegawai}
        );
    }

    public String getPegawaiDataJson(String kodePegawai) {
        SQLiteDatabase db = this.getReadableDatabase();
        String json = "{}";

        Cursor cursor = null;
        try {
            cursor = db.rawQuery(
                    "SELECT id_toko, kode_pegawai, nama_pegawai, no_hp, hak_akses, posisi, status, username, password, pin " +
                            "FROM tb_pegawai WHERE kode_pegawai = ?",
                    new String[]{kodePegawai}
            );

            if (cursor != null && cursor.moveToFirst()) {
                JSONObject obj = new JSONObject();
                obj.put("id_toko", cursor.getInt(cursor.getColumnIndexOrThrow("id_toko")));
                obj.put("kode_pegawai", cursor.getString(cursor.getColumnIndexOrThrow("kode_pegawai")));
                obj.put("nama_pegawai", cursor.getString(cursor.getColumnIndexOrThrow("nama_pegawai")));
                obj.put("no_hp", cursor.getString(cursor.getColumnIndexOrThrow("no_hp")));
                obj.put("hak_akses", cursor.getString(cursor.getColumnIndexOrThrow("hak_akses")));
                obj.put("posisi", cursor.getString(cursor.getColumnIndexOrThrow("posisi")));
                obj.put("status", cursor.getString(cursor.getColumnIndexOrThrow("status")));
                obj.put("username", cursor.getString(cursor.getColumnIndexOrThrow("username")));
                obj.put("password", cursor.getString(cursor.getColumnIndexOrThrow("password")));
                obj.put("pin", cursor.getString(cursor.getColumnIndexOrThrow("pin")));

                json = obj.toString();
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (cursor != null) cursor.close();
        }

        return json;
    }

    public void insertSettingStruk(ContentValues values) {
        values.put("created_at", getCurrentDateTime());
        db.insert("tb_setting_struk", null, values);
    }

    public Cursor detailStruk(String idToko) {
        if (idToko == null || idToko.isEmpty()) {
            return null;
        }

        return db.rawQuery(
                "SELECT * FROM tb_setting_struk WHERE id_toko = ? AND deleted_at IS NULL",
                new String[]{idToko}
        );
    }

    public void updateStruk(ContentValues values, String id) {
        values.put("updated_at", getCurrentDateTime());
        db.update("tb_setting_struk", values, "id_setting_struk = ?", new String[]{id});
    }

    public Cursor listPenjualan() {
        Cursor cur = db.rawQuery("SELECT * FROM tb_penjualan", null);
        return cur;
    }

    public void insertDataPenjualan(String kode_rekapkas, String kode_pegawai, String invoice, String jenisBayar, String dibayar, String kembali, String idToko) {
        ContentValues values = new ContentValues();
        values.put("kode_rekapkas", kode_rekapkas);
        values.put("kode_pegawai", kode_pegawai);
        values.put("invoice", invoice);
        values.put("jenis_pembayaran", jenisBayar);
        values.put("id_toko", idToko);
        values.put("tanggal", ftanggal);
        values.put("waktu", fwaktu);
        values.put("bulan", fbulan);
        values.put("tahun", ftahun);

        if (DataPesanan.isDiskonEmpty()) {
            values.put("status_diskon", "0");
            values.put("tipe_diskon", "0");
            values.put("nominal_disc", "0");
        } else {
            values.put("status_diskon", "1");
            values.put("tipe_diskon", DataPesanan.getTipediskon());
            values.put("nominal_disc", DataPesanan.getDiskon());
        }

        if (DataPesanan.isPelangganEmpty()) {
            values.put("kode_pelanggan", "-");
            values.put("nama_pelanggan", "-");
        } else {
            values.put("kode_pelanggan", DataPesanan.getIdMember());
            values.put("nama_pelanggan", DataPesanan.getNamaPelanggan());
        }

        values.put("jenis_kedatangan", DataPesanan.getJenisKedatangan());
        values.put("total_jml_transaksi", DataPesanan.getTotalHargaPesanan());
        values.put("dibayar", dibayar);
        values.put("kembali", kembali);
        values.put("updated_at", getCurrentDateTime());

        Log.d("DB_INSERT_PENJUALAN", "insertDataPenjualan -> " + values.toString());

        db.insert("tb_penjualan", null, values);
    }

    public void insetDataDetailPenjualan(String invoice) {
        db.beginTransaction();
        try {
            for (ProdukPesananModel pesanan : DataPesanan.getArrayPesanan()) {
                Cursor cursorDetail = getDetailProduk(pesanan.getKd_produk());
                if (cursorDetail.moveToFirst()) {
                    String laba = cursorDetail.getString(10);
                    int totallaba = Integer.parseInt(laba) * Integer.parseInt(pesanan.getJml());

                    ContentValues values = new ContentValues();
                    values.put("invoice", invoice);
                    values.put("kode_produk", pesanan.getKd_produk());
                    values.put("nama_produk", pesanan.getNama_produk());
                    values.put("harga_produk", pesanan.getHarga());
                    values.put("qty", pesanan.getJml());
                    values.put("total_harga", pesanan.getTotal_harga());
                    values.put("laba", String.valueOf(totallaba));
                    values.put("updated_at", getCurrentDateTime());

                    Log.d("DB_INSERT_DETAIL", "Detail Pesanan -> " + values.toString());

                    db.insert("tb_penjualan_detail", null, values);

                    int stokIndex = cursorDetail.getColumnIndex("stok");
                    Integer stokLama = null;
                    if (!cursorDetail.isNull(stokIndex)) {
                        stokLama = cursorDetail.getInt(stokIndex);
                    }

                    if (stokLama != null) {
                        int stokBaru = stokLama - Integer.parseInt(pesanan.getJml());
                        if (stokBaru < 0) stokBaru = 0;

                        ContentValues updateStok = new ContentValues();
                        updateStok.put("stok", stokBaru);

                        db.update("tb_produk", updateStok, "id_produk = ?",
                                new String[]{cursorDetail.getString(0)});

                        Log.d("DB_UPDATE_STOK", "Update stok " + pesanan.getKd_produk()
                                + " dari " + stokLama + " -> " + stokBaru);
                    }
                }
                cursorDetail.close();
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    public Cursor listPesanan(String idToko) {
        if (idToko == null || idToko.isEmpty()) {
            return null;
        }

        return db.rawQuery(
                "SELECT * FROM tb_pesanan WHERE id_toko = ? AND deleted_at IS NULL",
                new String[]{idToko}
        );
    }

    public Cursor getPesananWhereInvoice(String invoice) {
        return db.rawQuery(
                "SELECT * FROM tb_pesanan WHERE invoice = ? AND deleted_at IS NULL",
                new String[]{invoice}
        );
    }

    public Cursor getPesananDetailWhereInvoice(String invoice) {
        return db.rawQuery(
                "SELECT * FROM tb_pesanan_detail WHERE invoice = ? AND deleted_at IS NULL",
                new String[]{invoice}
        );
    }

    public String getTotalPesananPerINV(String invoice) {
        Log.d("DB_DEBUG", "Memulai getTotalPesananPerINV untuk invoice: " + invoice);

        Cursor count = db.rawQuery(
                "SELECT * FROM tb_pesanan_detail WHERE invoice = ? AND deleted_at IS NULL",
                new String[]{invoice}
        );

        int total = count.getCount();
        Log.d("DB_DEBUG", "Jumlah data ditemukan untuk invoice " + invoice + ": " + total);

        count.close();
        Log.d("DB_DEBUG", "Cursor ditutup setelah query getTotalPesananPerINV.");

        return String.valueOf(total);
    }

    public String getTotalPesananAll() {
        Cursor count = db.rawQuery(
                "SELECT * FROM tb_pesanan WHERE deleted_at IS NULL",
                null
        );
        int total = count.getCount();
        count.close();
        return String.valueOf(total);
    }

    public void insertDataPesanan(String invoice, String kode_pegawai, String id_toko) {
        ContentValues values = new ContentValues();
        String now = getCurrentDateTime();

        values.put("kode_pegawai", kode_pegawai);
        values.put("id_toko", id_toko);
        values.put("invoice", invoice);
        values.put("jenis_pembayaran", "");
        values.put("tanggal", ftanggal);
        values.put("waktu", fwaktu);
        values.put("bulan", fbulan);
        values.put("tahun", ftahun);
        values.put("created_at", now);

        if (DataPesanan.isDiskonEmpty()) {
            values.put("status_diskon", "0");
            values.put("tipe_diskon", "0");
            values.put("nominal_disc", "0");
        } else {
            values.put("status_diskon", "1");
            values.put("tipe_diskon", DataPesanan.getTipediskon());
            values.put("nominal_disc", DataPesanan.getDiskon());
        }
        if (DataPesanan.isPelangganEmpty()) {
            values.put("kode_pelanggan", "-");
            values.put("nama_pelanggan", "-");
        } else {
            values.put("kode_pelanggan", DataPesanan.getIdMember());
            values.put("nama_pelanggan", DataPesanan.getNamaPelanggan());
        }
        values.put("jenis_kedatangan", DataPesanan.getJenisKedatangan());
        values.put("total_jml_transaksi", DataPesanan.getTotalHargaPesanan());
        values.put("dibayar", "");
        values.put("kembali", "");
        db.insert("tb_pesanan", null, values);
    }

    public void insetDataDetailPesanan(String invoice) {
        db.beginTransaction();
        String now = getCurrentDateTime();
        try {
            ContentValues values = new ContentValues();
            for (ProdukPesananModel pesanan : DataPesanan.getArrayPesanan()) {
                values.put("invoice", invoice);
                values.put("kode_produk", pesanan.getKd_produk());
                values.put("nama_produk", pesanan.getNama_produk());
                values.put("harga_produk", pesanan.getHarga());
                values.put("qty", pesanan.getJml());
                values.put("total_harga", pesanan.getTotal_harga());
                values.put("created_at", now);

                db.insert("tb_pesanan_detail", null, values);
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    public String[] getStringJenisKedatangan(ArrayList<JenisKedatanganModels> arrayList) {
        String[] str = new String[arrayList.size()];
        for (int j = 0; j < arrayList.size(); j++) {

            str[j] = "'" + arrayList.get(j).getJenis_kedatangan() + "'";
        }
        return str;
    }

    public Cursor listPesananWhereJenisKedatangan(ArrayList<JenisKedatanganModels> arrayList) {
        String[] values = getStringJenisKedatangan(arrayList);
        String clause = Arrays.toString(values).replace("[", "(").replace("]", ")");
        String query = "SELECT * FROM tb_pesanan WHERE deleted_at IS NULL AND jenis_kedatangan IN " + clause;
        return db.rawQuery(query, null);
    }

    public void deleteDataPenjualan(String invoice) {
        db.beginTransaction();
        try {
            String now = getCurrentDateTime();

            ContentValues values = new ContentValues();
            values.put("deleted_at", now);
            values.put("updated_at", now);

            db.update("tb_pesanan", values, "invoice = ?", new String[]{invoice});

            db.update("tb_pesanan_detail", values, "invoice = ?", new String[]{invoice});

            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    public void deleteDataDetailPenjualan(ArrayList<DaftarPesananModel> listDeletePesanan) {
        db.beginTransaction();
        try {
            String now = getCurrentDateTime();

            ContentValues values = new ContentValues();
            values.put("deleted_at", now);
            values.put("updated_at", now);

            for (DaftarPesananModel model : listDeletePesanan) {
                db.update("tb_pesanan", values, "invoice = ?", new String[]{model.getInvoice()});
                db.update("tb_pesanan_detail", values, "invoice = ?", new String[]{model.getInvoice()});
            }

            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    public Cursor listJenisKedatangan(String idToko) {
        return db.rawQuery(
                "SELECT * FROM tb_jenis_kedatangan WHERE id_toko = ? AND deleted_at IS NULL",
                new String[]{idToko}
        );
    }

    public void tambahJenisKedatangan(ContentValues values) {
        values.put("created_at", getCurrentDateTime());
        db.insert("tb_jenis_kedatangan", null, values);
    }

    public void deleteJenisKedatangan(String id) {
        ContentValues values = new ContentValues();
        values.put("deleted_at", getCurrentDateTime());
        db.update("tb_jenis_kedatangan", values, "id_jenis_kedatangan = ?", new String[]{id});
    }

    public void updateJenisKedatangan(ContentValues values, String id) {
        values.put("updated_at", getCurrentDateTime());
        db.update("tb_jenis_kedatangan", values, "id_jenis_kedatangan = ?", new String[]{id});
    }

    public ArrayList<String> spinJenisKedatangan(String idToko) {
        ArrayList<String> lisKategori = new ArrayList<>();
        String query = "SELECT * FROM tb_jenis_kedatangan WHERE id_toko = ? AND deleted_at IS NULL";
        Cursor cursor = db.rawQuery(query, new String[]{idToko});

        if (cursor.moveToFirst()) {
            do {
                lisKategori.add(cursor.getString(cursor.getColumnIndexOrThrow("jenis_kedatangan")));
            } while (cursor.moveToNext());
        }
        cursor.close();
        return lisKategori;
    }

    public void bukaKasir(ContentValues values) {
        values.put("updated_at", getCurrentDateTime());
        db.insert("tb_rekapkas", null, values);
    }

    public void insertModalAwal(ContentValues values) {
        values.put("updated_at", getCurrentDateTime());
        db.insert("tb_rekapkas_detail", null, values);
    }

    public Cursor dataRekapKas(String kode_rekapkas) {
        return db.rawQuery(
                "SELECT r.date_awal, d.total_modalawal " +
                        "FROM tb_rekapkas r " +
                        "INNER JOIN tb_rekapkas_detail d ON r.kode_rekapkas = d.kode_rekapkas " +
                        "WHERE r.kode_rekapkas = ? AND r.deleted_at IS NULL AND d.deleted_at IS NULL",
                new String[]{kode_rekapkas}
        );
    }

    public Cursor dataPenjualan(String kode_rekapkas) {
        return db.rawQuery(
                "SELECT jenis_pembayaran, SUM(total_jml_transaksi) AS total_jml_transaksi " +
                        "FROM tb_penjualan " +
                        "WHERE kode_rekapkas = ? AND deleted_at IS NULL " +
                        "GROUP BY jenis_pembayaran",
                new String[]{kode_rekapkas}
        );
    }

    public void updateRekapKasDetail(ContentValues values, String kode_rekapkas) {
        values.put("updated_at", getCurrentDateTime());
        db.update("tb_rekapkas_detail", values, "kode_rekapkas = ?", new String[]{kode_rekapkas});
    }

    public void insertKasKeluar(String kode_rekapkas, String nominal, String keperluan, String idToko, String kodePegawai) {
        ContentValues values = new ContentValues();
        values.put("kode_rekapkas", kode_rekapkas);
        values.put("kode_kaskeluar", generateKodeKasKeluar());
        values.put("nominal", nominal);
        values.put("keperluan", keperluan);
        values.put("id_toko", idToko);
        values.put("kode_pegawai", kodePegawai);
        values.put("tanggal", ftanggal);
        values.put("waktu", fwaktu);
        values.put("bulan", fbulan);
        values.put("tahun", ftahun);
        values.put("updated_at", getCurrentDateTime());
        db.insert("tb_kas_keluar", null, values);
    }

    public Cursor kasKeluar(String kode_rekapkas) {
        return db.rawQuery(
                "SELECT kode_rekapkas, SUM(nominal) AS nominal " +
                        "FROM tb_kas_keluar " +
                        "WHERE kode_rekapkas = ? AND deleted_at IS NULL",
                new String[]{kode_rekapkas}
        );
    }

    public void rekapKasTutupKasir(String kode_rekapkas) {
        ContentValues values = new ContentValues();
        values.put("status", "Tertutup");
        values.put("waktu_tutup", fwaktu);
        values.put("tgl_tutup", ftanggal);
        values.put("date_tutup", fwaktulengkap);
        values.put("updated_at", getCurrentDateTime());
        db.update("tb_rekapkas", values, "kode_rekapkas = ?", new String[]{kode_rekapkas});
    }

    public void deleteRekapKas(String kode_rekapkas) {
        ContentValues values = new ContentValues();
        values.put("deleted_at", getCurrentDateTime());
        db.update("tb_rekapkas", values, "kode_rekapkas = ?", new String[]{kode_rekapkas});
    }

    public Cursor listRiwayatPenjualanOwner() {
        return db.rawQuery(
                "SELECT tb_penjualan.invoice, tb_penjualan.jenis_kedatangan,\n" +
                        " tb_penjualan.kode_pegawai, tb_pegawai.nama_pegawai,\n" +
                        " tb_penjualan.tanggal, tb_penjualan.total_jml_transaksi,\n" +
                        " sum(tb_penjualan_detail.qty) AS total_qty\n" +
                        "FROM tb_penjualan\n" +
                        "JOIN tb_penjualan_detail ON tb_penjualan.invoice = tb_penjualan_detail.invoice\n" +
                        "JOIN tb_pegawai ON tb_penjualan.kode_pegawai = tb_pegawai.kode_pegawai\n" +
                        "GROUP BY tb_penjualan.invoice\n" +
                        "ORDER BY tb_penjualan.tanggal DESC, " +
                        "tb_penjualan.waktu DESC", null
        );
    }

    public Cursor listRiwayatPenjualanByTanggal(String id_toko, String dari, String hingga) {

        String query =
                "SELECT p.invoice, p.jenis_kedatangan, " +
                        "p.kode_pegawai, peg.nama_pegawai, " +
                        "p.nama_pelanggan, " +
                        "p.tanggal, p.total_jml_transaksi, " +
                        "SUM(d.qty) AS total_qty " +
                        "FROM tb_penjualan p " +
                        "JOIN tb_penjualan_detail d ON p.invoice = d.invoice " +
                        "JOIN tb_pegawai peg ON p.kode_pegawai = peg.kode_pegawai " +
                        "WHERE p.id_toko = ? " +
                        "AND date('20' || substr(p.tanggal,7,2) || '-' || substr(p.tanggal,4,2) || '-' || substr(p.tanggal,1,2)) " +
                        "BETWEEN date(?) AND date(?) " +
                        "AND p.deleted_at IS NULL AND d.deleted_at IS NULL " +
                        "GROUP BY p.invoice " +
                        "ORDER BY datetime(COALESCE(p.updated_at, p.tanggal || ' ' || IFNULL(p.waktu, '00:00:00'))) DESC";

        android.util.Log.d("DB_RIWAYAT", "DARI: " + dari + " | HINGGA: " + hingga);

        return db.rawQuery(query, new String[]{id_toko, dari, hingga});
    }

    public Cursor getDetailInvoice(String invoice) {
        return db.rawQuery(
                "SELECT p.invoice, p.jenis_kedatangan, " +
                        "p.kode_pegawai, peg.nama_pegawai, " +
                        "p.nama_pelanggan, p.kode_pelanggan, " +
                        "p.jenis_pembayaran, p.tanggal, " +
                        "p.total_jml_transaksi, p.dibayar, " +
                        "p.kembali, p.nominal_disc, p.id_toko, " +
                        "SUM(d.qty) AS total_qty " +
                        "FROM tb_penjualan p " +
                        "JOIN tb_penjualan_detail d ON p.invoice = d.invoice " +
                        "JOIN tb_pegawai peg ON p.kode_pegawai = peg.kode_pegawai " +
                        "WHERE p.invoice = ? " +
                        "AND p.deleted_at IS NULL AND d.deleted_at IS NULL",
                new String[]{invoice}
        );
    }

    public Cursor listPembelian(String invoice) {
        return db.rawQuery(
                "SELECT nama_produk, harga_produk, qty " +
                        "FROM tb_penjualan_detail " +
                        "WHERE invoice = ? AND deleted_at IS NULL",
                new String[]{invoice}
        );
    }

    public Cursor dataMetodePembayaranPerhari(String tanggal1, String tanggal2, ArrayList<Integer> tokoIds) {
        if (tokoIds == null || tokoIds.isEmpty()) return null;

        StringBuilder placeholders = new StringBuilder();
        for (int i = 0; i < tokoIds.size(); i++) {
            placeholders.append("?");
            if (i < tokoIds.size() - 1) placeholders.append(",");
        }

        String query =
                "SELECT jenis_pembayaran, SUM(total_jml_transaksi) AS total_jml_transaksi " +
                        "FROM tb_penjualan " +
                        "WHERE deleted_at IS NULL " +
                        "AND (substr(tanggal, 7, 2) || substr(tanggal, 4, 2) || substr(tanggal, 1, 2)) " +
                        "BETWEEN (substr(?, 7, 2) || substr(?, 4, 2) || substr(?, 1, 2)) " +
                        "AND (substr(?, 7, 2) || substr(?, 4, 2) || substr(?, 1, 2)) " +
                        "AND id_toko IN (" + placeholders + ") " +
                        "GROUP BY jenis_pembayaran";

        String[] args = new String[6 + tokoIds.size()];
        args[0] = tanggal1; args[1] = tanggal1; args[2] = tanggal1;
        args[3] = tanggal2; args[4] = tanggal2; args[5] = tanggal2;
        for (int i = 0; i < tokoIds.size(); i++) args[i + 6] = String.valueOf(tokoIds.get(i));

        return db.rawQuery(query, args);
    }

    public void deletePenjualan(String invoice) {
        ContentValues values = new ContentValues();
        values.put("deleted_at", getCurrentDateTime());
        db.update("tb_penjualan", values, "invoice = ?", new String[]{invoice});

        ContentValues valuesDetail = new ContentValues();
        valuesDetail.put("deleted_at", getCurrentDateTime());
        db.update("tb_penjualan_detail", valuesDetail, "invoice = ?", new String[]{invoice});
    }

    public Cursor dataProdukTerlarisPerhari(String tanggal1, String tanggal2, ArrayList<Integer> tokoIds) {
        if (tokoIds == null || tokoIds.isEmpty()) return null;

        StringBuilder placeholders = new StringBuilder();
        for (int i = 0; i < tokoIds.size(); i++) {
            placeholders.append("?");
            if (i < tokoIds.size() - 1) placeholders.append(",");
        }

        String query = "SELECT d.nama_produk, " +
                "SUM(d.qty) AS qty, " +
                "SUM(d.total_harga) AS total_harga " +
                "FROM tb_penjualan_detail d " +
                "JOIN tb_penjualan p ON d.invoice = p.invoice " +
                "WHERE p.deleted_at IS NULL AND d.deleted_at IS NULL " +
                "AND (substr(p.tanggal, 7, 2) || substr(p.tanggal, 4, 2) || substr(p.tanggal, 1, 2)) " +
                "BETWEEN (substr(?, 7, 2) || substr(?, 4, 2) || substr(?, 1, 2)) " +
                "AND (substr(?, 7, 2) || substr(?, 4, 2) || substr(?, 1, 2)) " +
                "AND p.id_toko IN (" + placeholders + ") " +
                "GROUP BY d.nama_produk " +
                "ORDER BY SUM(d.qty) DESC " +
                "LIMIT 5";

        String[] args = new String[6 + tokoIds.size()];
        args[0] = tanggal1; args[1] = tanggal1; args[2] = tanggal1;
        args[3] = tanggal2; args[4] = tanggal2; args[5] = tanggal2;
        for (int i = 0; i < tokoIds.size(); i++) args[i + 6] = String.valueOf(tokoIds.get(i));

        return db.rawQuery(query, args);
    }

    public Cursor laporanDataPenjualan(String tanggal1, String tanggal2, ArrayList<Integer> tokoIds) {

        if (tokoIds == null || tokoIds.isEmpty()) {
            Log.d("LAPORAN_DEBUG", "tokoIds kosong!");
            return null;
        }

        StringBuilder placeholders = new StringBuilder();
        for (int i = 0; i < tokoIds.size(); i++) {
            placeholders.append("?");
            if (i < tokoIds.size() - 1) placeholders.append(",");
        }

        String query =
                "SELECT " +
                        "COALESCE(SUM(p.total_jml_transaksi),0) AS total_jml_transaksi, " +
                        "COUNT(p.invoice) AS transaksi, " +
                        "COALESCE((SELECT SUM(d.laba) " +
                        "          FROM tb_penjualan_detail d " +
                        "          JOIN tb_penjualan pj ON pj.invoice = d.invoice " +
                        "          WHERE d.deleted_at IS NULL " +
                        "          AND pj.deleted_at IS NULL " +
                        "          AND (substr(pj.tanggal,7,2) || substr(pj.tanggal,4,2) || substr(pj.tanggal,1,2)) " +
                        "          BETWEEN (substr(?,7,2) || substr(?,4,2) || substr(?,1,2)) " +
                        "          AND (substr(?,7,2) || substr(?,4,2) || substr(?,1,2)) " +
                        "          AND pj.id_toko IN (" + placeholders + ")" +
                        "),0) AS total_laba " +
                        "FROM tb_penjualan p " +
                        "WHERE p.deleted_at IS NULL " +
                        "AND (substr(p.tanggal,7,2) || substr(p.tanggal,4,2) || substr(p.tanggal,1,2)) " +
                        "BETWEEN (substr(?,7,2) || substr(?,4,2) || substr(?,1,2)) " +
                        "AND (substr(?,7,2) || substr(?,4,2) || substr(?,1,2)) " +
                        "AND p.id_toko IN (" + placeholders + ")";

        String[] args = new String[12 + (tokoIds.size() * 2)];

        int index = 0;

        args[index++] = tanggal1; args[index++] = tanggal1; args[index++] = tanggal1;
        args[index++] = tanggal2; args[index++] = tanggal2; args[index++] = tanggal2;

        for (Integer id : tokoIds) {
            args[index++] = String.valueOf(id);
        }

        args[index++] = tanggal1; args[index++] = tanggal1; args[index++] = tanggal1;
        args[index++] = tanggal2; args[index++] = tanggal2; args[index++] = tanggal2;

        for (Integer id : tokoIds) {
            args[index++] = String.valueOf(id);
        }

        Cursor cursor = db.rawQuery(query, args);

        return cursor;
    }

    public Cursor listLaporanKas(String tanggal1, String tanggal2, ArrayList<Integer> tokoIds) {
        if (tokoIds == null || tokoIds.isEmpty()) return null;

        StringBuilder placeholders = new StringBuilder();
        for (int i = 0; i < tokoIds.size(); i++) {
            placeholders.append("?");
            if (i < tokoIds.size() - 1) placeholders.append(",");
        }

        String query = "SELECT r.kode_rekapkas, r.date_awal, r.date_tutup, " +
                "d.total_penjualan, d.total_modalawal, d.total_kaskeluar, " +
                "d.penerimaan_sistem, d.penerimaan_tunai, d.selisih " +
                "FROM tb_rekapkas r " +
                "LEFT JOIN tb_rekapkas_detail d ON r.kode_rekapkas = d.kode_rekapkas " +
                "WHERE r.deleted_at IS NULL AND d.deleted_at IS NULL " +
                "AND (substr(r.tgl_tutup, 7, 2) || substr(r.tgl_tutup, 4, 2) || substr(r.tgl_tutup, 1, 2)) " +
                "BETWEEN (substr(?, 7, 2) || substr(?, 4, 2) || substr(?, 1, 2)) " +
                "AND (substr(?, 7, 2) || substr(?, 4, 2) || substr(?, 1, 2)) " +
                "AND r.id_toko IN (" + placeholders + ") " +
                "ORDER BY (substr(r.tgl_buka, 7, 2) || substr(r.tgl_buka, 4, 2) || substr(r.tgl_buka, 1, 2)) DESC, " +
                "r.waktu_buka DESC";

        String[] args = new String[6 + tokoIds.size()];
        args[0] = tanggal1; args[1] = tanggal1; args[2] = tanggal1;
        args[3] = tanggal2; args[4] = tanggal2; args[5] = tanggal2;
        for (int i = 0; i < tokoIds.size(); i++) args[i + 6] = String.valueOf(tokoIds.get(i));

        return db.rawQuery(query, args);
    }

    public Cursor detailLaporanKas(String kode_rekapkas) {
        return db.rawQuery(
                "SELECT r.kode_rekapkas, r.date_awal, r.date_tutup, " +
                        "d.total_penjualan, d.total_modalawal, d.total_kaskeluar, " +
                        "d.penerimaan_sistem, d.penerimaan_tunai, d.penerimaan_non_tunai, d.selisih " +
                        "FROM tb_rekapkas r " +
                        "LEFT JOIN tb_rekapkas_detail d ON r.kode_rekapkas = d.kode_rekapkas " +
                        "WHERE r.kode_rekapkas = ? " +
                        "AND r.deleted_at IS NULL AND d.deleted_at IS NULL " +
                        "GROUP BY r.kode_rekapkas",
                new String[]{kode_rekapkas}
        );
    }

    public Cursor listKasKeluar(String kode_rekapkas) {
        return db.rawQuery(
                "SELECT nominal, keperluan FROM tb_kas_keluar " +
                        "WHERE kode_rekapkas = ? AND deleted_at IS NULL " +
                        "ORDER BY updated_at DESC",
                new String[]{kode_rekapkas}
        );
    }

    public Cursor listStokProduk(ArrayList<Integer> tokoIds) {
        if (tokoIds == null || tokoIds.isEmpty()) {
            return db.rawQuery(
                    "SELECT kode_produk, nama_produk, stok, harga_beli " +
                            "FROM tb_produk WHERE deleted_at IS NULL " +
                            "ORDER BY nama_produk ASC",
                    null
            );
        }

        StringBuilder placeholders = new StringBuilder();
        for (int i = 0; i < tokoIds.size(); i++) {
            placeholders.append("?");
            if (i < tokoIds.size() - 1) placeholders.append(",");
        }

        String query = "SELECT kode_produk, nama_produk, stok, harga_beli " +
                "FROM tb_produk " +
                "WHERE id_toko IN (" + placeholders + ") " +
                "AND deleted_at IS NULL " +
                "ORDER BY nama_produk ASC";

        String[] args = new String[tokoIds.size()];
        for (int i = 0; i < tokoIds.size(); i++) {
            args[i] = String.valueOf(tokoIds.get(i));
        }

        return db.rawQuery(query, args);
    }

    public Cursor getRowsPegawai() {
        return db.rawQuery(
                "SELECT COUNT(*) AS jml_pegawai " +
                        "FROM tb_pegawai " +
                        "WHERE posisi = 'Pegawai' AND deleted_at IS NULL",
                null
        );
    }

    public void insertPrinter(ContentValues values) {
        values.put("created_at", String.valueOf(System.currentTimeMillis()));
        db.insert("tb_printer", null, values);
    }

    public Cursor checkPrinter(String alamat_printer, String idToko) {
        return db.rawQuery(
                "SELECT * FROM tb_printer " +
                        "WHERE alamat_printer = ? " +
                        "AND id_toko = ? " +
                        "AND deleted_at IS NULL",
                new String[]{alamat_printer, idToko}
        );
    }

    public Cursor listPrinter(String idToko) {
        return db.rawQuery(
                "SELECT * FROM tb_printer WHERE id_toko = ? AND deleted_at IS NULL ORDER BY updated_at DESC",
                new String[]{idToko}
        );
    }

    public Cursor getDetailPrinter(String id) {
        return db.rawQuery(
                "SELECT * FROM tb_printer WHERE id_printer = ? AND deleted_at IS NULL",
                new String[]{id}
        );
    }

    public void updatePrinter(ContentValues values, String id) {
        values.put("updated_at", String.valueOf(System.currentTimeMillis()));
        db.update("tb_printer", values, "id_printer = ?", new String[]{id});
    }

    public void deletePrinter(String id) {
        ContentValues values = new ContentValues();
        values.put("deleted_at", String.valueOf(System.currentTimeMillis()));
        db.update("tb_printer", values, "id_printer = ?", new String[]{id});
    }

    public void updateUkuranPrinter(String id, String ukuran) {

        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(ukuran_struk, ukuran);

        db.update(
                "tb_printer",
                values,
                "id=?",
                new String[]{id}
        );
    }

    private String getHakAksesWhereClause(String hakAkses) {
        if ("KASIR".equalsIgnoreCase(hakAkses)) {
            return " AND p.kode_pegawai = ? ";
        } else {
            return " AND p.id_toko = ? ";
        }
    }

    public Cursor getListToko() {
        String sql = "SELECT id_toko, nama_toko " +
                "FROM tb_toko " +
                "WHERE deleted_at IS NULL " +
                "ORDER BY nama_toko ASC";

        return db.rawQuery(sql, null);
    }

    public Cursor listPesananAllToko() {
        return db.rawQuery(
                "SELECT * FROM tb_penjualan WHERE deleted_at IS NULL ORDER BY waktu DESC",
                null
        );
    }

    public Cursor getLaporanPenjualanTahunan(String tahun, String hakAkses, String kodePegawai, String idToko) {
        String akses = getHakAksesWhereClause(hakAkses);
        String labaFilter = "KASIR".equalsIgnoreCase(hakAkses)
                ? " AND px.kode_pegawai = p.kode_pegawai "
                : " AND px.id_toko = p.id_toko ";

        String query =
                "SELECT p.tanggal, COUNT(p.id_penjualan) AS jumlah_transaksi, " +
                        "SUM(p.total_jml_transaksi) AS penjualan_kotor, " +
                        "(SELECT SUM(d.laba) FROM tb_penjualan_detail d " +
                        " JOIN tb_penjualan px ON px.invoice = d.invoice " +
                        " WHERE px.tanggal = p.tanggal " + labaFilter + ") AS laba_kotor, " +
                        "(SELECT SUM(px.total_jml_transaksi) FROM tb_penjualan px " +
                        " WHERE px.tahun = ? " + akses.replace("p.", "px.") + ") AS total_penjualan, " +
                        "(SELECT SUM(d2.laba) FROM tb_penjualan_detail d2 " +
                        " JOIN tb_penjualan py ON py.invoice = d2.invoice " +
                        " WHERE py.tahun = ? " + akses.replace("p.", "py.") + ") AS total_laba " +
                        "FROM tb_penjualan p WHERE p.tahun = ? " + akses +
                        "GROUP BY p.tanggal " +
                        "ORDER BY ('20' || substr(p.tanggal,7,2) || '-' || substr(p.tanggal,4,2) || '-' || substr(p.tanggal,1,2)) DESC\n";

        String param = "KASIR".equalsIgnoreCase(hakAkses) ? kodePegawai : idToko;
        return db.rawQuery(query, new String[]{tahun, param, tahun, param, tahun, param});
    }

    public Cursor getLaporanPenjualanBulanan(String tahun, String bulan, String hakAkses, String kodePegawai, String idToko) {
        String akses = getHakAksesWhereClause(hakAkses);
        String labaFilter = "KASIR".equalsIgnoreCase(hakAkses)
                ? " AND px.kode_pegawai = p.kode_pegawai "
                : " AND px.id_toko = p.id_toko ";

        String query =
                "SELECT p.tanggal, COUNT(p.id_penjualan) AS jumlah_transaksi, " +
                        "SUM(p.total_jml_transaksi) AS penjualan_kotor, " +
                        "(SELECT SUM(d.laba) FROM tb_penjualan_detail d " +
                        " JOIN tb_penjualan px ON px.invoice = d.invoice " +
                        " WHERE px.tanggal = p.tanggal" + labaFilter + ") AS laba_kotor, " +
                        "(SELECT SUM(px.total_jml_transaksi) FROM tb_penjualan px " +
                        " WHERE px.tahun = ? AND px.bulan = ? " + akses.replace("p.", "px.") + ") AS total_penjualan, " +
                        "(SELECT SUM(d2.laba) FROM tb_penjualan_detail d2 " +
                        " JOIN tb_penjualan py ON py.invoice = d2.invoice " +
                        " WHERE py.tahun = ? AND py.bulan = ? " + akses.replace("p.", "py.") + ") AS total_laba " +
                        "FROM tb_penjualan p WHERE p.tahun = ? AND p.bulan = ? " + akses +
                        "GROUP BY p.tanggal " +
                        "ORDER BY ('20' || substr(p.tanggal,7,2) || '-' || substr(p.tanggal,4,2) || '-' || substr(p.tanggal,1,2)) DESC\n";

        String param = "KASIR".equalsIgnoreCase(hakAkses) ? kodePegawai : idToko;
        return db.rawQuery(query, new String[]{tahun, bulan, param, tahun, bulan, param, tahun, bulan, param});
    }

    public Cursor getLaporanPenjualanHarian(String startDate, String endDate, String hakAkses, String kodePegawai, String idToko) {
        String akses = getHakAksesWhereClause(hakAkses);
        String dateExpr = "('20' || substr(%s,7,2) || '-' || substr(%s,4,2) || '-' || substr(%s,1,2))";
        String labaFilter = "KASIR".equalsIgnoreCase(hakAkses)
                ? " AND px.kode_pegawai = p.kode_pegawai "
                : " AND px.id_toko = p.id_toko ";

        String query =
                "SELECT p.tanggal, COUNT(p.id_penjualan) AS jumlah_transaksi, " +
                        "SUM(p.total_jml_transaksi) AS penjualan_kotor, " +
                        "(SELECT SUM(d.laba) FROM tb_penjualan_detail d " +
                        " JOIN tb_penjualan px ON px.invoice = d.invoice " +
                        " WHERE px.tanggal = p.tanggal" + labaFilter + ") AS laba_kotor, " +
                        "(SELECT SUM(px.total_jml_transaksi) FROM tb_penjualan px " +
                        " WHERE " + String.format(dateExpr, "px.tanggal", "px.tanggal", "px.tanggal") +
                        " BETWEEN " + String.format(dateExpr, "?", "?", "?") +
                        " AND " + String.format(dateExpr, "?", "?", "?") +
                        akses.replace("p.", "px.") + ") AS total_penjualan, " +
                        "(SELECT SUM(d2.laba) FROM tb_penjualan_detail d2 " +
                        " JOIN tb_penjualan py ON py.invoice = d2.invoice " +
                        " WHERE " + String.format(dateExpr, "py.tanggal", "py.tanggal", "py.tanggal") +
                        " BETWEEN " + String.format(dateExpr, "?", "?", "?") +
                        " AND " + String.format(dateExpr, "?", "?", "?") +
                        akses.replace("p.", "py.") + ") AS total_laba " +
                        "FROM tb_penjualan p " +
                        "WHERE " + String.format(dateExpr, "p.tanggal", "p.tanggal", "p.tanggal") +
                        " BETWEEN " + String.format(dateExpr, "?", "?", "?") +
                        " AND " + String.format(dateExpr, "?", "?", "?") +
                        akses +
                        "GROUP BY p.tanggal " +
                        "ORDER BY ('20' || substr(p.tanggal,7,2) || '-' || substr(p.tanggal,4,2) || '-' || substr(p.tanggal,1,2)) DESC\n";

        String param = "KASIR".equalsIgnoreCase(hakAkses) ? kodePegawai : idToko;
        return db.rawQuery(query, new String[]{
                startDate, startDate, startDate, endDate, endDate, endDate, param,
                startDate, startDate, startDate, endDate, endDate, endDate, param,
                startDate, startDate, startDate, endDate, endDate, endDate, param
        });
    }

    public Cursor getLaporanPenjualanDetailTahunan(String tahun, String hakAkses, String kodePegawai, String idToko) {
        String akses = getHakAksesWhereClause(hakAkses);

        String query =
                "SELECT p.tanggal, d.nama_produk, SUM(CAST(d.qty AS INTEGER)) AS qty, " +
                        "SUM(d.total_harga) AS penjualan_kotor, SUM(d.laba) AS laba_kotor, " +
                        "(SELECT SUM(px.total_jml_transaksi) FROM tb_penjualan px " +
                        " WHERE px.tahun = ? " + akses.replace("p.", "px.") + ") AS total_penjualan, " +
                        "(SELECT SUM(d2.laba) FROM tb_penjualan_detail d2 " +
                        " JOIN tb_penjualan py ON py.invoice = d2.invoice " +
                        " WHERE py.tahun = ? " + akses.replace("p.", "py.") + ") AS total_laba " +
                        "FROM tb_penjualan_detail d JOIN tb_penjualan p ON p.invoice = d.invoice " +
                        "WHERE p.tahun = ? " + akses +
                        "GROUP BY p.tanggal, d.kode_produk " +
                        "ORDER BY ('20' || substr(p.tanggal,7,2) || '-' || substr(p.tanggal,4,2) || '-' || substr(p.tanggal,1,2)) DESC\n";

        String param = "KASIR".equalsIgnoreCase(hakAkses) ? kodePegawai : idToko;
        return db.rawQuery(query, new String[]{tahun, param, tahun, param, tahun, param});
    }

    public Cursor getLaporanPenjualanDetailBulanan(String tahun, String bulan, String hakAkses, String kodePegawai, String idToko) {
        String akses = getHakAksesWhereClause(hakAkses);

        String query =
                "SELECT p.tanggal, d.nama_produk, SUM(CAST(d.qty AS INTEGER)) AS qty, " +
                        "SUM(d.total_harga) AS penjualan_kotor, SUM(d.laba) AS laba_kotor, " +
                        "(SELECT SUM(px.total_jml_transaksi) FROM tb_penjualan px " +
                        " WHERE px.tahun = ? AND px.bulan = ? " + akses.replace("p.", "px.") + ") AS total_penjualan, " +
                        "(SELECT SUM(d2.laba) FROM tb_penjualan_detail d2 " +
                        " JOIN tb_penjualan py ON py.invoice = d2.invoice " +
                        " WHERE py.tahun = ? AND py.bulan = ? " + akses.replace("p.", "py.") + ") AS total_laba " +
                        "FROM tb_penjualan_detail d JOIN tb_penjualan p ON p.invoice = d.invoice " +
                        "WHERE p.tahun = ? AND p.bulan = ? " + akses +
                        "GROUP BY p.tanggal, d.kode_produk " +
                        "ORDER BY ('20' || substr(p.tanggal,7,2) || '-' || substr(p.tanggal,4,2) || '-' || substr(p.tanggal,1,2)) DESC\n";

        String param = "KASIR".equalsIgnoreCase(hakAkses) ? kodePegawai : idToko;
        return db.rawQuery(query, new String[]{tahun, bulan, param, tahun, bulan, param, tahun, bulan, param});
    }

    public Cursor getLaporanPenjualanDetailHarian(String startDate, String endDate, String hakAkses, String kodePegawai, String idToko) {
        String akses = getHakAksesWhereClause(hakAkses);
        String dateExpr = "('20' || substr(%s,7,2) || '-' || substr(%s,4,2) || '-' || substr(%s,1,2))";

        String query =
                "SELECT p.tanggal, d.nama_produk, SUM(CAST(d.qty AS INTEGER)) AS qty, " +
                        "SUM(d.total_harga) AS penjualan_kotor, SUM(d.laba) AS laba_kotor, " +
                        "(SELECT SUM(px.total_jml_transaksi) FROM tb_penjualan px " +
                        " WHERE " + String.format(dateExpr, "px.tanggal", "px.tanggal", "px.tanggal") +
                        " BETWEEN " + String.format(dateExpr, "?", "?", "?") +
                        " AND " + String.format(dateExpr, "?", "?", "?") +
                        akses.replace("p.", "px.") + ") AS total_penjualan, " +
                        "(SELECT SUM(d2.laba) FROM tb_penjualan_detail d2 " +
                        " JOIN tb_penjualan py ON py.invoice = d2.invoice " +
                        " WHERE " + String.format(dateExpr, "py.tanggal", "py.tanggal", "py.tanggal") +
                        " BETWEEN " + String.format(dateExpr, "?", "?", "?") +
                        " AND " + String.format(dateExpr, "?", "?", "?") +
                        akses.replace("p.", "py.") + ") AS total_laba " +
                        "FROM tb_penjualan_detail d JOIN tb_penjualan p ON p.invoice = d.invoice " +
                        "WHERE " + String.format(dateExpr, "p.tanggal", "p.tanggal", "p.tanggal") +
                        " BETWEEN " + String.format(dateExpr, "?", "?", "?") +
                        " AND " + String.format(dateExpr, "?", "?", "?") +
                        akses +
                        "GROUP BY p.tanggal, d.kode_produk " +
                        "ORDER BY ('20' || substr(p.tanggal,7,2) || '-' || substr(p.tanggal,4,2) || '-' || substr(p.tanggal,1,2)) DESC\n";

        String param = "KASIR".equalsIgnoreCase(hakAkses) ? kodePegawai : idToko;
        return db.rawQuery(query, new String[]{
                startDate, startDate, startDate, endDate, endDate, endDate, param,
                startDate, startDate, startDate, endDate, endDate, endDate, param,
                startDate, startDate, startDate, endDate, endDate, endDate, param
        });
    }

    public Map<String, Object> getSyncData(String kodeOwner) {

        Map<String, Object> result = new HashMap<>();
        List<Map<String, Object>> dataList = new ArrayList<>();

        SQLiteDatabase db = this.getReadableDatabase();

        Cursor tokoCursor = db.rawQuery("SELECT * FROM tb_toko", null);

        if (tokoCursor.moveToFirst()) {
            do {
                Map<String, Object> tokoMap = new HashMap<>();

                int idToko = tokoCursor.getInt(tokoCursor.getColumnIndexOrThrow("id_toko"));

                tokoMap.put("id_toko", idToko);
                tokoMap.put("nama_toko", tokoCursor.getString(tokoCursor.getColumnIndexOrThrow("nama_toko")));
                tokoMap.put("kota", tokoCursor.getString(tokoCursor.getColumnIndexOrThrow("kota")));
                tokoMap.put("provinsi", tokoCursor.getString(tokoCursor.getColumnIndexOrThrow("provinsi")));
                tokoMap.put("email", tokoCursor.getString(tokoCursor.getColumnIndexOrThrow("email")));
                tokoMap.put("no_telepon", tokoCursor.getString(tokoCursor.getColumnIndexOrThrow("no_telepon")));
                tokoMap.put("id_jenis_usaha", tokoCursor.getString(tokoCursor.getColumnIndexOrThrow("id_jenis_usaha")));
                tokoMap.put("nama_jenis_usaha", tokoCursor.getString(tokoCursor.getColumnIndexOrThrow("nama_jenis_usaha")));
                tokoMap.put("created_at", tokoCursor.getString(tokoCursor.getColumnIndexOrThrow("created_at")));
                tokoMap.put("exp_date", tokoCursor.getString(tokoCursor.getColumnIndexOrThrow("exp_date")));
                tokoMap.put("updated_at", tokoCursor.getString(tokoCursor.getColumnIndexOrThrow("updated_at")));
                tokoMap.put("deleted_at", tokoCursor.getString(tokoCursor.getColumnIndexOrThrow("deleted_at")));

                List<Map<String, Object>> pegawaiList = new ArrayList<>();

                Cursor pegawaiCursor = db.rawQuery(
                        "SELECT * FROM tb_pegawai WHERE id_toko = ?",
                        new String[]{String.valueOf(idToko)}
                );

                if (pegawaiCursor.moveToFirst()) {
                    do {
                        Map<String, Object> pegawaiMap = new HashMap<>();

                        String kodePegawai = pegawaiCursor.getString(
                                pegawaiCursor.getColumnIndexOrThrow("kode_pegawai")
                        );

                        pegawaiMap.put("kode_pegawai", kodePegawai);
                        pegawaiMap.put("kode_franchise", kodeOwner + "-" + kodePegawai);
                        pegawaiMap.put("nama_pegawai", pegawaiCursor.getString(pegawaiCursor.getColumnIndexOrThrow("nama_pegawai")));
                        pegawaiMap.put("no_hp", pegawaiCursor.getString(pegawaiCursor.getColumnIndexOrThrow("no_hp")));
                        pegawaiMap.put("hak_akses", pegawaiCursor.getString(pegawaiCursor.getColumnIndexOrThrow("hak_akses")));
                        pegawaiMap.put("posisi", pegawaiCursor.getString(pegawaiCursor.getColumnIndexOrThrow("posisi")));
                        pegawaiMap.put("status", pegawaiCursor.getString(pegawaiCursor.getColumnIndexOrThrow("status")));
                        pegawaiMap.put("username", pegawaiCursor.getString(pegawaiCursor.getColumnIndexOrThrow("username")));
                        pegawaiMap.put("password", pegawaiCursor.getString(pegawaiCursor.getColumnIndexOrThrow("password")));
                        pegawaiMap.put("pin", pegawaiCursor.getString(pegawaiCursor.getColumnIndexOrThrow("pin")));
                        pegawaiMap.put("created_at", pegawaiCursor.getString(pegawaiCursor.getColumnIndexOrThrow("created_at")));
                        pegawaiMap.put("updated_at", pegawaiCursor.getString(pegawaiCursor.getColumnIndexOrThrow("updated_at")));
                        pegawaiMap.put("deleted_at", pegawaiCursor.getString(pegawaiCursor.getColumnIndexOrThrow("deleted_at")));

                        pegawaiList.add(pegawaiMap);

                    } while (pegawaiCursor.moveToNext());
                }

                pegawaiCursor.close();

                Map<String, Object> item = new HashMap<>();
                item.put("toko", tokoMap);
                item.put("pegawai", pegawaiList);

                dataList.add(item);

            } while (tokoCursor.moveToNext());
        }

        tokoCursor.close();

        result.put("kode_owner", kodeOwner);
        result.put("data", dataList);

        return result;
    }

    public String getExpDateToko() {
        SQLiteDatabase db = this.getReadableDatabase();
        String expDate = null;

        Cursor cursor = db.rawQuery("SELECT exp_date FROM tb_toko LIMIT 1", null);

        if (cursor != null) {
            if (cursor.moveToFirst()) {
                expDate = cursor.getString(0);
            }
            cursor.close();
        }

        return expDate;
    }
}