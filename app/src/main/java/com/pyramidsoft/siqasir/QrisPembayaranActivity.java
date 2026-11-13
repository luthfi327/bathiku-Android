package com.pyramidsoft.siqasir;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Toast;
import com.pyramidsoft.siqasir.utils.ApiKeyHttpClient;
import androidx.appcompat.app.AppCompatActivity;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.pyramidsoft.siqasir.databinding.ActivityQrisPembayaranBinding;
import com.pyramidsoft.siqasir.utils.SessionManager;

import org.json.JSONObject;

import java.io.IOException;
import java.text.NumberFormat;
import java.util.Locale;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * QrisPembayaranActivity
 *
 * Menampilkan QR Code pembayaran QRIS dan menunggu konfirmasi dari server.
 *
 * PERUBAHAN FASE 5:
 * - Field `harga` (amount yang dikirim ke POST /api/qris/create) sekarang
 *   WAJIB diterima dari Intent extra "harga" yang sudah dihitung di
 *   PaketLanggananActivity — tidak dihitung ulang di sini.
 * - Menangani HTTP 422 dari server (harga tidak valid): tampilkan dialog
 *   error dan kembali ke PaketLanggananActivity untuk refresh harga.
 * - Menangani HTTP 403 dari server (masih trial): tampilkan dialog dan finish.
 * - Intent extra "harga_bulan1_server" dan "kategori_harga" diterima untuk
 *   referensi logging dan display, bukan untuk menghitung ulang harga.
 */
public class QrisPembayaranActivity extends AppCompatActivity {

    private static final String TAG      = "QrisPembayaran";
    private static final String BASE_URL = "https://siqasir.awandigital.id/api";

    private ActivityQrisPembayaranBinding binding;

    // FASE 5: harga sepenuhnya dari Intent — tidak ada fallback kalkulasi ulang
    private int harga;           // total yang akan dibayar (sudah termasuk fee QRIS)
    private int durasi;
    private int maxToko;
    private int hargaBulan1Server; // untuk referensi/logging
    private String kategoriHarga;  // untuk referensi/logging

    private String externalId;
    private String kodeOwner;
    private String qrStringFromIntent;
    private long sisaWaktu = 0;
    private int amountFromApi = 0;

    private CountDownTimer timer;
    private Handler handler = new Handler();
    private Runnable statusRunnable;

    private OkHttpClient client = ApiKeyHttpClient.get();
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityQrisPembayaranBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        sessionManager = new SessionManager(this);

        // ---------------------------------------------------------------
        // FASE 5: Terima semua nilai dari Intent — tidak ada kalkulasi ulang
        // ---------------------------------------------------------------
        externalId         = getIntent().getStringExtra("external_id");
        harga              = getIntent().getIntExtra("harga", 0);
        durasi             = getIntent().getIntExtra("durasi", 0);
        maxToko            = getIntent().getIntExtra("max_toko", 1);
        hargaBulan1Server  = getIntent().getIntExtra("harga_bulan1_server", 0);
        kategoriHarga      = getIntent().getStringExtra("kategori_harga");
        int amountIntent   = getIntent().getIntExtra("amount", 0);
        kodeOwner          = sessionManager.getKodeOwnerutama();
        qrStringFromIntent = getIntent().getStringExtra("qr_string");
        sisaWaktu          = getIntent().getLongExtra("sisa_waktu", 0);

        // Tampilkan nominal di layar
        if (amountIntent > 0) {
            binding.txtTotal.setText(formatRupiah(amountIntent));
        } else {
            binding.txtTotal.setText(formatRupiah(harga));
        }

        Log.d(TAG, "onCreate — harga=" + harga
                + " durasi=" + durasi
                + " maxToko=" + maxToko
                + " hargaBulan1Server=" + hargaBulan1Server
                + " kategoriHarga=" + kategoriHarga);

        binding.btnBatal.setOnClickListener(v -> showCancelConfirmation());

        if (qrStringFromIntent != null && !qrStringFromIntent.isEmpty()) {
            // Sudah ada QR dari intent (resume dari PENDING) — langsung tampilkan
            generateQr(qrStringFromIntent);
            startCheckStatusByOwner();
            startTimer(sisaWaktu);
        } else {
            // Buat QR baru ke server
            createQris();
        }
    }

    // =========================================================================
    // Buat QR ke server
    // =========================================================================

    /**
     * POST /api/qris/create
     *
     * FASE 5: amount yang dikirim adalah nilai `harga` dari Intent
     * (sudah dihitung di PaketLanggananActivity dari harga server).
     *
     * Menangani respons:
     *   - 200 OK      → tampilkan QR, mulai timer & polling
     *   - 422         → harga tidak valid (server tolak) → dialog + kembali ke PaketLangganan
     *   - 403         → masih trial → dialog + finish
     *   - lainnya     → dialog error umum
     */
    private void createQris() {
        if (harga <= 0) {
            Toast.makeText(this, "Harga tidak valid. Kembali dan coba lagi.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        // FASE 5: Kirim `harga` (dari Intent, berasal dari server Fase 1)
        //         bukan harga hardcode atau hasil kalkulasi ulang Android.
        RequestBody body = new FormBody.Builder()
                .add("amount",       String.valueOf(harga))
                .add("durasi_bulan", String.valueOf(durasi))
                .add("kode_owner",   kodeOwner)
                .add("max_toko",     String.valueOf(maxToko))
                .build();

        Request request = new Request.Builder()
                .url(BASE_URL + "/qris/create")
                .post(body)
                .build();

        Log.d(TAG, "createQris — POST /qris/create, amount=" + harga
                + " durasi=" + durasi + " kodeOwner=" + kodeOwner);

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "createQris onFailure: " + e.getMessage());
                runOnUiThread(() ->
                        Toast.makeText(QrisPembayaranActivity.this,
                                "Koneksi gagal. Periksa jaringan Anda.",
                                Toast.LENGTH_SHORT).show()
                );
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                final int httpCode = response.code();
                final String res   = response.body().string();
                Log.d(TAG, "createQris response HTTP " + httpCode + ": " + res);

                runOnUiThread(() -> {
                    // -------------------------------------------------------
                    // FASE 5: Tangani HTTP 422 — harga ditolak server
                    // -------------------------------------------------------
                    if (httpCode == 422) {
                        handleHargaTidakValid(res);
                        return;
                    }

                    // FASE 5: Tangani HTTP 403 — toko masih masa trial
                    if (httpCode == 403) {
                        try {
                            JSONObject obj   = new JSONObject(res);
                            String message   = obj.optString("message", "Masa trial belum selesai.");
                            new AlertDialog.Builder(QrisPembayaranActivity.this)
                                    .setTitle("Tidak Bisa Bayar")
                                    .setMessage(message)
                                    .setPositiveButton("OK", (d, w) -> finish())
                                    .setCancelable(false)
                                    .show();
                        } catch (Exception e) {
                            finish();
                        }
                        return;
                    }

                    // Respons error lain
                    if (!response.isSuccessful()) {
                        Toast.makeText(QrisPembayaranActivity.this,
                                "Gagal membuat pembayaran (HTTP " + httpCode + ")",
                                Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // Sukses: parse QR string
                    try {
                        JSONObject obj    = new JSONObject(res);
                        externalId        = obj.getString("external_id");
                        String qrString   = obj.getString("qr_string");
                        String expiredAt  = obj.getString("expired_at");
                        long sisaMillis   = getRemainingTime(expiredAt);

                        generateQr(qrString);
                        startTimer(sisaMillis);
                        startCheckStatus();

                    } catch (Exception e) {
                        e.printStackTrace();
                        Toast.makeText(QrisPembayaranActivity.this,
                                "Gagal memproses QR. Coba lagi.",
                                Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }

    /**
     * FASE 5: Menangani respons HTTP 422 dari server.
     *
     * Terjadi ketika `amount` yang dikirim Android tidak cocok dengan
     * harga yang dihitung server (selisih > Rp 1.000).
     *
     * Dalam kondisi normal Fase 5, ini TIDAK SEHARUSNYA terjadi karena
     * harga sudah diambil dari server di PaketLanggananActivity.
     * Bisa terjadi jika: harga berubah antara waktu fetch dan waktu bayar
     * (misal admin mengubah promo), atau ada latency jaringan ekstrem.
     *
     * Solusi: tampilkan dialog informatif dan kembali ke PaketLanggananActivity
     * agar user bisa refresh harga terbaru.
     */
    private void handleHargaTidakValid(String responseBody) {
        try {
            JSONObject obj         = new JSONObject(responseBody);
            int hargaServer        = obj.optInt("harga_server", 0);
            int hargaDiterima      = obj.optInt("harga_diterima", 0);

            Log.w(TAG, "handleHargaTidakValid — harga_server=" + hargaServer
                    + " harga_diterima=" + hargaDiterima);

            String pesan = "Harga yang tersimpan sudah tidak berlaku.\n\n" +
                    "Harga saat ini: Rp " + formatRupiahPlain(hargaServer) + "\n" +
                    "Harga tersimpan: Rp " + formatRupiahPlain(hargaDiterima) + "\n\n" +
                    "Halaman akan direfresh untuk mengambil harga terbaru.";

            new AlertDialog.Builder(this)
                    .setTitle("Harga Berubah")
                    .setMessage(pesan)
                    .setPositiveButton("Refresh Harga", (dialog, which) -> {
                        // Kembali ke PaketLanggananActivity dan refresh
                        Intent intent = new Intent(this, PaketLanggananActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                        startActivity(intent);
                        finish();
                    })
                    .setCancelable(false)
                    .show();

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this,
                    "Harga tidak valid. Kembali dan coba lagi.",
                    Toast.LENGTH_LONG).show();
            finish();
        }
    }

    // =========================================================================
    // Generate QR
    // =========================================================================

    private void generateQr(String qrString) {
        try {
            QRCodeWriter writer = new QRCodeWriter();
            int size = 600;
            BitMatrix bitMatrix = writer.encode(qrString, BarcodeFormat.QR_CODE, size, size);
            Bitmap bmp = Bitmap.createBitmap(size, size, Bitmap.Config.RGB_565);
            for (int x = 0; x < size; x++) {
                for (int y = 0; y < size; y++) {
                    bmp.setPixel(x, y, bitMatrix.get(x, y) ? 0xFF000000 : 0xFFFFFFFF);
                }
            }
            binding.imgQr.setImageBitmap(bmp);
        } catch (Exception e) {
            Toast.makeText(this, "QR gagal dibuat", Toast.LENGTH_SHORT).show();
        }
    }

    // =========================================================================
    // Polling status pembayaran
    // =========================================================================

    private void startCheckStatus() {
        statusRunnable = new Runnable() {
            @Override
            public void run() {
                checkStatus();
                handler.postDelayed(this, 5000);
            }
        };
        handler.post(statusRunnable);
    }

    private void startCheckStatusByOwner() {
        statusRunnable = new Runnable() {
            @Override
            public void run() {
                checkStatusByOwner();
                handler.postDelayed(this, 5000);
            }
        };
        handler.post(statusRunnable);
    }

    private void checkStatus() {
        Request request = new Request.Builder()
                .url(BASE_URL + "/qris/status?external_id=" + externalId)
                .get()
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {}

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String res = response.body().string();
                try {
                    JSONObject obj = new JSONObject(res);
                    // Handle struktur response: { "status": true, "data": { ... } }
                    JSONObject data = obj.has("data") && !obj.isNull("data")
                            ? obj.getJSONObject("data") : obj;
                    String status = data.optString("status", "");

                    if ("PAID".equals(status)) {
                        runOnUiThread(() -> {
                            Toast.makeText(QrisPembayaranActivity.this,
                                    "Pembayaran berhasil! 🎉", Toast.LENGTH_LONG).show();
                            finish();
                        });
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void checkStatusByOwner() {
        Request request = new Request.Builder()
                .url(BASE_URL + "/qris/status?kode_owner=" + kodeOwner)
                .get()
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {}

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String res = response.body().string();
                try {
                    JSONObject obj = new JSONObject(res);
                    JSONObject data = obj.has("data") && !obj.isNull("data")
                            ? obj.getJSONObject("data") : obj;
                    String status = data.optString("status", "");
                    amountFromApi = data.optInt("amount", 0);

                    runOnUiThread(() -> {
                        if (amountFromApi > 0) {
                            binding.txtTotal.setText(formatRupiah(amountFromApi));
                        }
                        if ("PAID".equals(status)) {
                            Toast.makeText(QrisPembayaranActivity.this,
                                    "Pembayaran berhasil! 🎉", Toast.LENGTH_LONG).show();
                            finish();
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    // =========================================================================
    // Batal pembayaran
    // =========================================================================

    private void showCancelConfirmation() {
        new AlertDialog.Builder(this)
                .setTitle("Batalkan Pembayaran")
                .setMessage("Yakin ingin membatalkan pembayaran ini?")
                .setPositiveButton("Ya, Batalkan", (dialog, which) -> cancelPayment())
                .setNegativeButton("Tidak", null)
                .show();
    }

    private void cancelPayment() {
        Log.d(TAG, "cancelPayment — externalId=" + externalId);

        if (externalId == null || externalId.isEmpty()) {
            Toast.makeText(this, "ID pembayaran tidak ditemukan", Toast.LENGTH_SHORT).show();
            return;
        }

        Request request = new Request.Builder()
                .url(BASE_URL + "/qris/cancel/" + externalId)
                .post(RequestBody.create(null, new byte[0]))
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "cancelPayment onFailure: " + e.getMessage());
                runOnUiThread(() ->
                        Toast.makeText(QrisPembayaranActivity.this,
                                "Gagal membatalkan. Coba lagi.",
                                Toast.LENGTH_SHORT).show()
                );
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String res = response.body().string();
                Log.d(TAG, "cancelPayment response: " + res);
                runOnUiThread(() -> {
                    if (response.isSuccessful()) {
                        Toast.makeText(QrisPembayaranActivity.this,
                                "Pembayaran dibatalkan", Toast.LENGTH_SHORT).show();
                        afterCancelFlow();
                    } else {
                        Toast.makeText(QrisPembayaranActivity.this,
                                "Gagal batal (" + response.code() + ")",
                                Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }

    private void afterCancelFlow() {
        if (timer != null) timer.cancel();
        if (handler != null && statusRunnable != null) {
            handler.removeCallbacks(statusRunnable);
        }
        blurQr();

        new CountDownTimer(5000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                binding.txtTimer.setText("Kembali dalam " + (millisUntilFinished / 1000) + " detik");
            }
            @Override
            public void onFinish() { finish(); }
        }.start();
    }

    // =========================================================================
    // Timer countdown
    // =========================================================================

    private void startTimer(long duration) {
        if (duration <= 0) {
            binding.txtTimer.setText("QR expired");
            blurQr();
            return;
        }

        timer = new CountDownTimer(duration, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                long menit = millisUntilFinished / 60000;
                long detik = (millisUntilFinished % 60000) / 1000;
                binding.txtTimer.setText(String.format("Berlaku %02d:%02d", menit, detik));
            }
            @Override
            public void onFinish() {
                binding.txtTimer.setText("QR expired");
                blurQr();
            }
        };
        timer.start();
    }

    private void blurQr() {
        binding.imgQr.setAlpha(0.3f);
        binding.layoutExpired.setVisibility(View.VISIBLE);
    }

    // =========================================================================
    // Helpers format & waktu
    // =========================================================================

    private String formatRupiah(int amount) {
        NumberFormat formatter = NumberFormat.getInstance(new Locale("id", "ID"));
        return "Rp " + formatter.format(amount);
    }

    private String formatRupiahPlain(int amount) {
        NumberFormat formatter = NumberFormat.getInstance(new Locale("id", "ID"));
        return formatter.format(amount);
    }

    private long getRemainingTime(String expiredAt) {
        try {
            java.text.SimpleDateFormat sdf =
                    new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSX", Locale.getDefault());
            sdf.setLenient(false);
            java.util.Date expDate = sdf.parse(expiredAt);
            java.util.Date now     = new java.util.Date();
            long diff = expDate.getTime() - now.getTime();
            Log.d(TAG, "getRemainingTime expired_at=" + expiredAt + " diff=" + diff + "ms");
            return Math.max(diff, 0);
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

    // =========================================================================
    // Lifecycle cleanup
    // =========================================================================

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (timer != null) timer.cancel();
        if (handler != null && statusRunnable != null) {
            handler.removeCallbacks(statusRunnable);
        }
    }
}
