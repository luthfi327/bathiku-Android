package com.pyramidsoft.siqasir;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.pyramidsoft.siqasir.databinding.ActivityPaketLanggananBinding;
import com.pyramidsoft.siqasir.db.DataBaseHelper;
import com.pyramidsoft.siqasir.utils.ApiKeyHttpClient;
import com.pyramidsoft.siqasir.utils.SessionManager;

import org.json.JSONObject;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * PaketLanggananActivity
 *
 * Menampilkan pilihan paket langganan premium dan menghitung total pembayaran.
 *
 * PERUBAHAN FASE 5:
 * - Harga per bulan pertama (bulan ke-4 atau promo khusus) tidak lagi hardcode.
 *   Diambil dari endpoint GET /api/payment/get-pricing/{kode_owner} saat onCreate.
 * - Selama fetch harga, UI diblokir dengan loading overlay.
 * - Jika server menolak (HTTP 403 = masih trial), tampil dialog info lalu Activity ditutup.
 * - Jika jaringan gagal, tampil dialog error + tombol "Coba Lagi".
 * - Badge "Harga Spesial!" muncul jika server melaporkan ada_promo_khusus = true.
 * - Nilai harga_per_bulan_pertama dan kategori_harga disimpan sebagai field instance
 *   dan diteruskan ke QrisPembayaranActivity via Intent extra.
 */
public class PaketLanggananActivity extends AppCompatActivity {

    // =========================================================================
    // URL Server
    // =========================================================================
    private static final String BASE_URL       = "https://siqasir.awandigital.id/api";
    private static final String URL_PRICING    = BASE_URL + "/payment/get-pricing/";
    private static final String URL_FIRST_TIME = BASE_URL + "/payment/first-time/";
    private static final String URL_QRIS_STATUS= BASE_URL + "/qris/status";

    // =========================================================================
    // Konstanta harga fallback (dipakai HANYA jika server tidak bisa dihubungi
    // dan user memilih melanjutkan; dalam kondisi normal tidak digunakan)
    // =========================================================================
    private static final int HARGA_FALLBACK_BULAN4  = 19000;
    private static final int HARGA_FALLBACK_NORMAL  = 49000;

    // =========================================================================
    // Binding & Helpers
    // =========================================================================
    private ActivityPaketLanggananBinding binding;
    private DataBaseHelper dbHelper;
    private SessionManager sessionManager;
    private OkHttpClient client = ApiKeyHttpClient.get();
    private String kodeOwner = "";

    // =========================================================================
    // State Harga dari Server (Fase 5 - pengganti hardcode)
    // =========================================================================
    /** Harga bulan pertama yang berlaku, diterima dari server */
    private int hargaBulan1DariServer = 0;
    /** Kategori harga: "trial_promo" | "promo_bulan4" | "normal" */
    private String kategoriHarga = "";
    /** True jika server melaporkan ada promo khusus untuk toko ini */
    private boolean adaPromoKhusus = false;
    /** True setelah harga berhasil diambil dari server */
    private boolean isPricingLoaded = false;

    // =========================================================================
    // State UI & Kalkulasi
    // =========================================================================
    private int selectedHarga    = 0;   // total akhir (termasuk fee QRIS)
    private int selectedDurasi   = 0;   // durasi paket yang dipilih (bulan)
    private int hargaPerToko     = 0;   // harga per bulan untuk paket multi-bulan
    private int jumlahToko       = 1;
    private boolean isFirstTime  = false;
    private boolean isLoadedFirstTime = false;
    private int subtotalFinal    = 0;

    private static final double QRIS_FEE = 0.0063;

    // =========================================================================
    // Lifecycle
    // =========================================================================

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityPaketLanggananBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(false);
            getSupportActionBar().setTitle("");
        }

        binding.btnBack.setOnClickListener(v -> onBackPressed());

        dbHelper       = new DataBaseHelper(this);
        sessionManager = new SessionManager(this);
        kodeOwner      = sessionManager.getKodeOwnerutama();

        // Tampilkan tanggal masa aktif dari SQLite lokal
        String expDateDb = dbHelper.getExpiredDate();
        binding.txtHargaAsli3.setPaintFlags(
                binding.txtHargaAsli3.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
        binding.txtHargaAsli6.setPaintFlags(
                binding.txtHargaAsli6.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
        binding.txtHargaAsli12.setPaintFlags(
                binding.txtHargaAsli12.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);

        if (expDateDb != null) {
            binding.txtExpiredDate.setText(formatExpired(expDateDb));
            if (isExpired(expDateDb)) {
                binding.txtExpiredDate.setText("Expired");
                binding.txtExpiredDate.setTextColor(Color.RED);
            }
        } else {
            binding.txtExpiredDate.setText("-");
        }

        setupClick();

        // FASE 5: Fetch harga dari server sebagai langkah pertama.
        // checkFirstTimePayment() dipanggil DI DALAM callback fetchPricingFromServer()
        // setelah harga berhasil diperoleh — menjamin urutan yang benar.
        fetchPricingFromServer();
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkStatusByKodeOwner();
    }

    @Override
    public boolean onOptionsItemSelected(android.view.MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // =========================================================================
    // FASE 5: Fetch harga dari server
    // =========================================================================

    /**
     * Mengambil harga yang berlaku dari server.
     * Dipanggil saat onCreate() — memblokir UI dengan loading indicator.
     *
     * Alur:
     *   1. Tampilkan loading overlay, sembunyikan konten paket
     *   2. GET /api/payment/get-pricing/{kode_owner}
     *   3a. Sukses (200) → simpan harga ke field, sembunyikan loading, lanjut ke checkFirstTimePayment()
     *   3b. HTTP 403 (masih trial) → tampilkan dialog info, finish()
     *   3c. Gagal jaringan → tampilkan dialog error + tombol Coba Lagi
     */
    private void fetchPricingFromServer() {
        android.util.Log.d("PRICING_DEBUG", "kodeOwner=" + kodeOwner);
        if (kodeOwner == null || kodeOwner.isEmpty()) {
            showErrorDialog("Sesi tidak valid. Silakan login ulang.", true);
            return;
        }

        showLoadingState(true);

        Request request = new Request.Builder()
                .url(URL_PRICING + kodeOwner)
                .get()
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, java.io.IOException e) {
                runOnUiThread(() -> {
                    showLoadingState(false);
                    showNetworkErrorDialog();
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws java.io.IOException {
                final int httpCode = response.code();
                final String resBody = response.body().string();

                runOnUiThread(() -> {
                    showLoadingState(false);

                    try {
                        android.util.Log.d("PRICING_DEBUG", "fetchPricing sukses: httpCode=" + httpCode + " body=" + resBody);
                        JSONObject json = new JSONObject(resBody);

                        // HTTP 403 = toko masih dalam masa trial
                        if (httpCode == 403) {
                            String pesan = json.optString("message",
                                    "Masa trial Anda belum selesai. Fitur perpanjang akan tersedia setelah 4 bulan pertama.");
                            showTrialInfoDialog(pesan);
                            return;
                        }

                        // HTTP lain selain 200 = error tak terduga
                        if (httpCode != 200) {
                            showNetworkErrorDialog();
                            return;
                        }

                        // Sukses: parsing data harga
                        JSONObject data = json.getJSONObject("data");
                        hargaBulan1DariServer = data.getInt("harga");
                        kategoriHarga         = data.optString("kategori", "normal");
                        adaPromoKhusus        = data.optBoolean("ada_promo_khusus", false);
                        isPricingLoaded       = true;

                        // Tampilkan badge promo khusus jika ada
                        applyPromoKhususBadge();

                        // Lanjutkan ke cek first-time payment
                        checkFirstTimePayment();

                    } catch (Exception e) {
                        e.printStackTrace();
                        showNetworkErrorDialog();
                    }
                });
            }
        });
    }

    /**
     * Menampilkan atau menyembunyikan loading overlay.
     * Saat loading: sembunyikan seluruh konten paket, tampilkan progress + teks.
     */
    private void showLoadingState(boolean isLoading) {
        if (isLoading) {
            binding.layoutLoadingPricing.setVisibility(View.VISIBLE);
            binding.layoutKontenPaket.setVisibility(View.GONE);
        } else {
            binding.layoutLoadingPricing.setVisibility(View.GONE);
            binding.layoutKontenPaket.setVisibility(View.VISIBLE);
        }
    }

    /**
     * Dialog jika server menolak karena toko masih masa trial.
     * Menutup Activity setelah user mengklik OK.
     */
    private void showTrialInfoDialog(String pesan) {
        new AlertDialog.Builder(this)
                .setTitle("Masa Trial Aktif")
                .setMessage(pesan)
                .setPositiveButton("Mengerti", (dialog, which) -> finish())
                .setCancelable(false)
                .show();
    }

    /**
     * Dialog error jaringan dengan tombol "Coba Lagi" dan "Kembali".
     */
    private void showNetworkErrorDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Gagal Mengambil Harga")
                .setMessage("Tidak dapat terhubung ke server. Periksa koneksi internet Anda dan coba lagi.")
                .setPositiveButton("Coba Lagi", (dialog, which) -> fetchPricingFromServer())
                .setNegativeButton("Kembali", (dialog, which) -> finish())
                .setCancelable(false)
                .show();
    }

    /**
     * Dialog error umum, opsional langsung finish.
     */
    private void showErrorDialog(String pesan, boolean finishAfter) {
        new AlertDialog.Builder(this)
                .setTitle("Error")
                .setMessage(pesan)
                .setPositiveButton("OK", (dialog, which) -> {
                    if (finishAfter) finish();
                })
                .show();
    }

    /**
     * Menampilkan atau menyembunyikan badge "Harga Spesial!" berdasarkan
     * nilai adaPromoKhusus yang diterima dari server.
     */
    private void applyPromoKhususBadge() {
        if (adaPromoKhusus) {
            binding.cardPromoKhusus.setVisibility(View.VISIBLE);
            binding.txtPromoKhususLabel.setText(
                    "✨ Harga Spesial! Anda mendapatkan harga khusus Rp "
                            + formatRupiah(hargaBulan1DariServer) + " untuk bulan pertama");
        } else {
            binding.cardPromoKhusus.setVisibility(View.GONE);
        }
    }

    // =========================================================================
    // Cek first-time payment (logika lama, dipanggil SETELAH pricing loaded)
    // =========================================================================

    private void checkFirstTimePayment() {
        if (kodeOwner == null || kodeOwner.isEmpty()) return;

        Request request = new Request.Builder()
                .url(URL_FIRST_TIME + kodeOwner)
                .get()
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, java.io.IOException e) {
                e.printStackTrace();
                // Gagal cek first-time: tetap lanjutkan dengan isFirstTime = false
                runOnUiThread(() -> {
                    isLoadedFirstTime = true;
                    applyFirstTimeUI();
                    pilihPaket(hargaPaket3Bulan(), 3);
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws java.io.IOException {
                String res = response.body().string();
                try {
                    JSONObject obj   = new JSONObject(res);
                    // Sesuaikan parsing dengan struktur response aktual server
                    // Server mengembalikan: { "status": true, "data": { "is_first_time": true } }
                    boolean firstTime;
                    if (obj.has("data")) {
                        firstTime = obj.getJSONObject("data").optBoolean("is_first_time", false);
                    } else {
                        firstTime = obj.optBoolean("is_first_time", false);
                    }
                    isFirstTime       = firstTime;
                    isLoadedFirstTime = true;

                    runOnUiThread(() -> {
                        applyFirstTimeUI();
                        // Pilih paket 3 bulan sebagai default
                        pilihPaket(hargaPaket3Bulan(), 3);
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                    runOnUiThread(() -> {
                        isLoadedFirstTime = true;
                        applyFirstTimeUI();
                        pilihPaket(hargaPaket3Bulan(), 3);
                    });
                }
            }
        });
    }

    // =========================================================================
    // Helper kalkulasi harga paket (menggunakan harga dari server)
    // =========================================================================

    /**
     * Mengembalikan harga untuk paket 1 bulan.
     * Ini adalah harga_bulan1_dari_server (bisa promo / bulan4 / normal).
     */
    private int hargaPaket1Bulan() {
        return isPricingLoaded ? hargaBulan1DariServer : HARGA_FALLBACK_NORMAL;
    }

    /**
     * Harga per bulan untuk paket 3 bulan.
     * Diskon tetap menggunakan rasio asli: 43000/49000 ≈ 87.75%
     * Dihitung proporsional terhadap harga normal server.
     */
    private int hargaPaket3Bulan() {
        // Harga per bulan ke-2 dst untuk paket 3, 6, 12 masih menggunakan
        // harga referensi bulanan yang diturunkan dari harga normal.
        // Logika: harga multi-bulan adalah diskon dari harga satuan NORMAL (49.000),
        // bukan dari harga bulan pertama (yang bisa bervariasi karena promo).
        return 43000;
    }

    private int hargaPaket6Bulan()  { return 36500; }
    private int hargaPaket12Bulan() { return 29000; }

    // =========================================================================
    // Cek status QRIS yang masih pending saat resume
    // =========================================================================

    private void checkStatusByKodeOwner() {
        if (kodeOwner == null || kodeOwner.isEmpty()) return;

        Request request = new Request.Builder()
                .url(URL_QRIS_STATUS + "?kode_owner=" + kodeOwner)
                .get()
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, java.io.IOException e) {
                e.printStackTrace();
            }

            @Override
            public void onResponse(Call call, Response response) throws java.io.IOException {
                String res = response.body().string();
                try {
                    JSONObject obj = new JSONObject(res);

                    // Handle struktur response baru { "status": true, "data": { ... } }
                    JSONObject data;
                    if (obj.has("data") && !obj.isNull("data")) {
                        data = obj.getJSONObject("data");
                    } else {
                        data = obj;
                    }

                    String status     = data.optString("status", "");
                    String externalId = data.optString("external_id", "");
                    String qrString   = data.optString("qr_string", "");
                    String expiredAt  = data.optString("expired_at", "");
                    int amount        = data.optInt("amount", 0);

                    runOnUiThread(() -> {
                        if ("PENDING".equals(status)) {
                            long sisaMillis = getRemainingTime(expiredAt);
                            if (sisaMillis > 0 && qrString != null && !qrString.isEmpty()) {
                                Intent intent = new Intent(PaketLanggananActivity.this,
                                        QrisPembayaranActivity.class);
                                intent.putExtra("qr_string",   qrString);
                                intent.putExtra("sisa_waktu",  sisaMillis);
                                intent.putExtra("amount",      amount);
                                intent.putExtra("external_id", externalId);
                                startActivity(intent);
                            }
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    // =========================================================================
    // UI: Terapkan tampilan first-time / non-first-time
    // =========================================================================

    private void applyFirstTimeUI() {
        android.util.Log.d("PRICING_DEBUG", "isFirstTime=" + isFirstTime +
                " hargaBulan1=" + hargaBulan1DariServer +
                " adaPromo=" + adaPromoKhusus);

        if (isFirstTime) {
            // Toko baru pertama kali bayar: tampilkan card promo lama (kuning)
            binding.cardPromo.setVisibility(View.VISIBLE);

            binding.txtHargaAsli1.setPaintFlags(
                    binding.txtHargaAsli1.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            binding.txtHargaPromo1.setVisibility(View.VISIBLE);
            binding.txtHargaNormal1.setVisibility(View.GONE);

            // FASE 5: Tampilkan harga dari server, bukan hardcode 19.000
            binding.txtHargaPromo1.setText("Rp " + formatRupiah(hargaPaket1Bulan()));

            // Label harga multi-bulan: bulan pertama pakai harga server, sisanya harga lama
            binding.txtHargaBulan3.setText(
                    "Rp " + formatRupiah(hargaPaket1Bulan()) + " + " + formatRupiah(hargaPaket3Bulan()) + " × 2");
            binding.txtHargaTotal3.setText(
                    "Rp " + formatRupiah(hitungTotalPaket(3)));

            binding.txtHargaBulan6.setText(
                    "Rp " + formatRupiah(hargaPaket1Bulan()) + " + " + formatRupiah(hargaPaket6Bulan()) + " × 5");
            binding.txtHargaTotal6.setText(
                    "Rp " + formatRupiah(hitungTotalPaket(6)));

            binding.txtHargaBulan12.setText(
                    "Rp " + formatRupiah(hargaPaket1Bulan()) + " + " + formatRupiah(hargaPaket12Bulan()) + " × 11");
            binding.txtHargaTotal12.setText(
                    "Rp " + formatRupiah(hitungTotalPaket(12)));

        } else {
            // Sudah pernah bayar sebelumnya
            binding.cardPromo.setVisibility(View.GONE);
            binding.txtHargaAsli1.setPaintFlags(0);
            binding.txtHargaAsli1.setVisibility(View.GONE);
            binding.txtHargaPromo1.setVisibility(View.GONE);
            binding.txtHargaNormal1.setVisibility(View.VISIBLE);

            // FASE 5: Tampilkan harga bulan 1 dari server
            binding.txtHargaNormal1.setText("Rp " + formatRupiah(hargaPaket1Bulan()));

            binding.txtHargaBulan3.setText("Rp " + formatRupiah(hargaPaket3Bulan()) + " × 3");
            binding.txtHargaTotal3.setText("Rp " + formatRupiah(hitungTotalPaket(3)));

            binding.txtHargaBulan6.setText("Rp " + formatRupiah(hargaPaket6Bulan()) + " × 6");
            binding.txtHargaTotal6.setText("Rp " + formatRupiah(hitungTotalPaket(6)));

            binding.txtHargaBulan12.setText("Rp " + formatRupiah(hargaPaket12Bulan()) + " × 12");
            binding.txtHargaTotal12.setText("Rp " + formatRupiah(hitungTotalPaket(12)));
        }
    }

    /**
     * Menghitung total harga paket sebelum fee QRIS.
     * Logika tetap sama dengan sebelumnya, tapi harga bulan pertama
     * sekarang diambil dari hargaPaket1Bulan() (server) bukan hardcode.
     */
    private int hitungTotalPaket(int durasi) {
        if (isFirstTime) {
            if (durasi == 1) return hargaPaket1Bulan();
            int hargaBulanLanjut;
            if (durasi == 3)  hargaBulanLanjut = hargaPaket3Bulan();
            else if (durasi == 6)  hargaBulanLanjut = hargaPaket6Bulan();
            else hargaBulanLanjut = hargaPaket12Bulan();
            return hargaPaket1Bulan() + (hargaBulanLanjut * (durasi - 1));
        } else {
            int hargaPerBulan;
            if (durasi == 1)       hargaPerBulan = hargaPaket1Bulan();
            else if (durasi == 3)  hargaPerBulan = hargaPaket3Bulan();
            else if (durasi == 6)  hargaPerBulan = hargaPaket6Bulan();
            else                   hargaPerBulan = hargaPaket12Bulan();
            return hargaPerBulan * durasi;
        }
    }

    // =========================================================================
    // Click handlers & pilih paket
    // =========================================================================

    private void setupClick() {
        // FASE 5: Harga di setupClick() sekarang menggunakan method helper
        // yang mengembalikan nilai dari server.
        // Nilai literal (43000, 36500, 29000) adalah harga multi-bulan yang
        // memang tidak berubah — hanya harga bulan ke-1 yang dinamis dari server.
        binding.card1Bulan.setOnClickListener(v -> pilihPaket(hargaPaket1Bulan(), 1));
        binding.card3Bulan.setOnClickListener(v -> pilihPaket(hargaPaket3Bulan(), 3));
        binding.card6Bulan.setOnClickListener(v -> pilihPaket(hargaPaket6Bulan(), 6));
        binding.card12Bulan.setOnClickListener(v -> pilihPaket(hargaPaket12Bulan(), 12));

        binding.btnPlus.setOnClickListener(v -> {
            jumlahToko++;
            binding.edtJumlahToko.setText(String.valueOf(jumlahToko));
            updateTotal();
        });

        binding.btnMinus.setOnClickListener(v -> {
            if (jumlahToko > 1) {
                jumlahToko--;
                binding.edtJumlahToko.setText(String.valueOf(jumlahToko));
                updateTotal();
            }
        });

        binding.edtJumlahToko.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            @Override public void onTextChanged(CharSequence s, int st, int b, int c) {}

            @Override
            public void afterTextChanged(android.text.Editable s) {
                if (s.toString().isEmpty()) {
                    jumlahToko = 1;
                    updateTotal();
                    return;
                }
                try {
                    int input = Integer.parseInt(s.toString());
                    if (input < 1) input = 1;
                    if (!s.toString().equals(String.valueOf(input))) {
                        binding.edtJumlahToko.setText(String.valueOf(input));
                        binding.edtJumlahToko.setSelection(
                                binding.edtJumlahToko.getText().length());
                        return;
                    }
                    jumlahToko = input;
                    updateTotal();
                } catch (Exception e) {
                    jumlahToko = 1;
                    updateTotal();
                }
            }
        });

        binding.btnBayar.setOnClickListener(v -> {
            if (selectedHarga == 0) {
                Toast.makeText(this, "Silakan pilih paket dulu", Toast.LENGTH_SHORT).show();
                return;
            }
            if (!isPricingLoaded) {
                Toast.makeText(this, "Menunggu harga dari server...", Toast.LENGTH_SHORT).show();
                return;
            }

            new AlertDialog.Builder(this)
                    .setTitle("Konfirmasi Pembayaran")
                    .setMessage(
                            "Langganan " + selectedDurasi + " bulan\n" +
                                    "Jumlah toko: " + jumlahToko + "\n\n" +
                                    "Subtotal: Rp " + formatRupiah(subtotalFinal) + "\n" +
                                    "Fee QRIS (0.63%)\n" +
                                    "Total Bayar: Rp " + formatRupiah(selectedHarga)
                    )
                    .setPositiveButton("Bayar", (dialog, which) -> {
                        Intent intent = new Intent(this, QrisPembayaranActivity.class);
                        // FASE 5: Kirim harga dari server, bukan hardcode
                        intent.putExtra("harga",           selectedHarga);
                        intent.putExtra("durasi",          selectedDurasi);
                        intent.putExtra("max_toko",        jumlahToko);
                        // Extra baru: kirim harga bulan 1 server agar QrisPembayaranActivity
                        // tidak perlu menghitung ulang
                        intent.putExtra("harga_bulan1_server", hargaBulan1DariServer);
                        intent.putExtra("kategori_harga",      kategoriHarga);
                        startActivity(intent);
                    })
                    .setNegativeButton("Batal", null)
                    .show();
        });
    }

    private void pilihPaket(int harga, int durasi) {
        hargaPerToko   = harga;
        selectedDurasi = durasi;
        jumlahToko     = 1;
        binding.edtJumlahToko.setText("1");
        binding.cardJumlahToko.setVisibility(View.VISIBLE);

        updateTotal();

        binding.card1Bulan.setStrokeColor(Color.parseColor("#E0E0E0"));
        binding.card3Bulan.setStrokeColor(Color.parseColor("#E0E0E0"));
        binding.card6Bulan.setStrokeColor(Color.parseColor("#E0E0E0"));
        binding.card12Bulan.setStrokeColor(Color.parseColor("#E0E0E0"));

        if (durasi == 1)  binding.card1Bulan.setStrokeColor(Color.parseColor("#6F42C1"));
        if (durasi == 3)  binding.card3Bulan.setStrokeColor(Color.parseColor("#6F42C1"));
        if (durasi == 6)  binding.card6Bulan.setStrokeColor(Color.parseColor("#6F42C1"));
        if (durasi == 12) binding.card12Bulan.setStrokeColor(Color.parseColor("#6F42C1"));
    }

    private void updateTotal() {
        if (!isLoadedFirstTime) return;

        int subtotal;
        if (isFirstTime) {
            if (selectedDurasi == 1) {
                subtotal = hargaPaket1Bulan() * jumlahToko;
            } else {
                subtotal = (hargaPaket1Bulan() + ((selectedDurasi - 1) * hargaPerToko)) * jumlahToko;
            }
        } else {
            subtotal = hargaPerToko * selectedDurasi * jumlahToko;
        }

        subtotalFinal = subtotal;
        int fee       = (int) Math.ceil(subtotal * QRIS_FEE);
        selectedHarga = subtotal + fee;

        binding.txtSubtotal.setText("Subtotal: Rp " + formatRupiah(subtotal));
        binding.txtFee.setText("Biaya QRIS (0.63%): Rp " + formatRupiah(fee));
        binding.txtTotalHarga.setText("Total: Rp " + formatRupiah(selectedHarga));
    }

    // =========================================================================
    // Helpers format & waktu
    // =========================================================================

    private String formatRupiah(int amount) {
        NumberFormat formatter = NumberFormat.getInstance(new Locale("id", "ID"));
        return formatter.format(amount);
    }

    private String formatExpired(String dbDate) {
        try {
            SimpleDateFormat input  = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            SimpleDateFormat output = new SimpleDateFormat("dd MMMM yyyy", new Locale("id", "ID"));
            Date date = input.parse(dbDate);
            return output.format(date);
        } catch (Exception e) {
            e.printStackTrace();
            return "-";
        }
    }

    private boolean isExpired(String dbDate) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            Date expDate = sdf.parse(dbDate);
            return expDate.before(new Date());
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private long getRemainingTime(String expiredAt) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            Date expDate = sdf.parse(expiredAt);
            long diff = expDate.getTime() - new Date().getTime();
            return Math.max(diff, 0);
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }
}
