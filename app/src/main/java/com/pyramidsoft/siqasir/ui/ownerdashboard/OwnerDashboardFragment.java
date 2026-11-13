package com.pyramidsoft.siqasir.ui.ownerdashboard;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.pyramidsoft.siqasir.PaketLanggananActivity;
import com.pyramidsoft.siqasir.PilihKaryawan;
import com.pyramidsoft.siqasir.databinding.FragmentOwnerDashboardBinding;
import com.pyramidsoft.siqasir.db.DataBaseHelper;
import com.pyramidsoft.siqasir.utils.ApiKeyHttpClient;
import com.pyramidsoft.siqasir.utils.SessionManager;

import org.json.JSONObject;

import java.io.IOException;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * OwnerDashboardFragment
 *
 * Fragment dashboard khusus untuk user dengan hak_akses Owner atau Admin.
 * Menampilkan ringkasan operasional toko dari data SQLite lokal
 * dan status langganan premium dari server.
 *
 * PERUBAHAN GAP 5 — Verifikasi Role dari Server (Anti-Bypass):
 *
 * Masalah sebelumnya:
 *   Cek hak_akses hanya dilakukan terhadap SharedPreferences lokal.
 *   SharedPreferences dapat dimanipulasi secara manual (root, editor aplikasi pihak ketiga),
 *   sehingga kasir biasa bisa mengganti hak_akses menjadi "Owner" dan mengakses
 *   OwnerDashboardFragment tanpa sepengetahuan owner.
 *
 * Solusi GAP 5:
 *   Saat onViewCreated() dipanggil pertama kali, fragment memanggil endpoint:
 *     GET /api/toko/status?kode_owner={kode}&kode_users={kode_pegawai}
 *   Server mengembalikan hak_akses yang sebenarnya dari tabel siqasir.users
 *   (filter: status = 'Aktif').
 *   Fragment membandingkan nilai lokal vs server:
 *     - Cocok  → lanjut tampilkan dashboard normal
 *     - Tidak cocok → paksa logout ke PilihKaryawan + tampilkan pesan error
 *     - Server error / timeout → tampilkan error, blokir akses dashboard
 *
 * Alur verifikasi:
 *   1. Tampilkan loading overlay di atas semua konten
 *   2. Fetch ke server (timeout 10 detik via ApiKeyHttpClient)
 *   3. a) Sukses + cocok  → sembunyikan overlay, tampilkan dashboard
 *      b) Sukses + mismatch → paksa logout ke PilihKaryawan
 *      c) HTTP error      → tampilkan error state, sembunyikan dashboard
 *      d) IOException     → tampilkan error koneksi, sembunyikan dashboard
 *
 * Catatan: Tidak ada bypass — jika verifikasi server belum selesai atau gagal,
 * konten dashboard tidak akan pernah ditampilkan.
 */
public class OwnerDashboardFragment extends Fragment {

    private static final String TAG      = "OwnerDashboard";
    private static final String BASE_URL = "https://siqasir.awandigital.id/api";

    private FragmentOwnerDashboardBinding binding;
    private SessionManager sessionManager;
    private DataBaseHelper dbHelper;
    private OkHttpClient   client = ApiKeyHttpClient.get();

    /** Flag: true jika verifikasi server sudah selesai dan lolos */
    private boolean verifikasiSelesai = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentOwnerDashboardBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        sessionManager = new SessionManager(requireActivity());
        dbHelper       = new DataBaseHelper(requireActivity());

        // ── GAP 5: Blokir semua konten sampai verifikasi server selesai ──────
        // Tampilkan loading overlay, sembunyikan semua kartu dashboard.
        tampilkanLoadingVerifikasi();

        // ── GAP 5: Jalankan verifikasi role dari server ───────────────────────
        // Semua inisialisasi konten (loadRingkasan, loadChart, dll.)
        // HANYA dijalankan di dalam verifikasiRoleDariServer() setelah lolos.
        verifikasiRoleDariServer();
    }

    // =========================================================================
    // GAP 5 — VERIFIKASI ROLE DARI SERVER
    // =========================================================================

    /**
     * Tampilkan loading state — sembunyikan semua konten dashboard.
     * Dipanggil sebelum request ke server dimulai.
     */
    private void tampilkanLoadingVerifikasi() {
        // Sembunyikan seluruh konten dashboard
        binding.scrollViewDashboard.setVisibility(View.INVISIBLE);
        // Tampilkan overlay loading verifikasi
        binding.layoutVerifikasiLoading.setVisibility(View.VISIBLE);
        binding.layoutVerifikasiError.setVisibility(View.GONE);
    }

    /**
     * Sembunyikan loading state, tampilkan konten dashboard.
     * Dipanggil setelah verifikasi berhasil.
     */
    private void tampilkanKontenDashboard() {
        binding.layoutVerifikasiLoading.setVisibility(View.GONE);
        binding.layoutVerifikasiError.setVisibility(View.GONE);
        binding.scrollViewDashboard.setVisibility(View.VISIBLE);
    }

    /**
     * Tampilkan error state — konten dashboard tetap tersembunyi.
     * Dipanggil jika server error atau tidak ada koneksi.
     *
     * @param pesan Pesan error yang ditampilkan ke user
     */
    private void tampilkanErrorVerifikasi(String pesan) {
        binding.layoutVerifikasiLoading.setVisibility(View.GONE);
        binding.layoutVerifikasiError.setVisibility(View.VISIBLE);
        binding.scrollViewDashboard.setVisibility(View.INVISIBLE);
        binding.tvVerifikasiErrorMsg.setText(pesan);

        // Tombol "Coba Lagi" untuk retry verifikasi
        binding.btnVerifikasiRetry.setOnClickListener(v -> {
            tampilkanLoadingVerifikasi();
            verifikasiRoleDariServer();
        });
    }

    /**
     * Paksa logout dan kembali ke PilihKaryawan.
     * Dipanggil saat hak_akses lokal tidak cocok dengan server (bypass terdeteksi).
     *
     * @param alasan Alasan logout (untuk log)
     */
    private void paksaLogout(String alasan) {
        Log.w(TAG, "PAKSA LOGOUT: " + alasan);

        // Clear session lokal
        sessionManager.logOuts();

        // Kembali ke PilihKaryawan, clear back stack
        Intent intent = new Intent(requireActivity(), PilihKaryawan.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }

    /**
     * Inti verifikasi GAP 5.
     *
     * Memanggil GET /api/toko/status?kode_owner=X&kode_users=Y
     * Server (setelah dimodifikasi GAP 5) mengembalikan hak_akses dari DB.
     * Fragment membandingkan dengan nilai lokal dari SharedPreferences.
     *
     * Tiga kemungkinan hasil:
     *   A) Cocok → inisialisasi dashboard normal
     *   B) Tidak cocok → paksa logout ke PilihKaryawan
     *   C) Error → blokir akses, tampilkan pesan error
     */
    private void verifikasiRoleDariServer() {
        String kodeOwner  = sessionManager.getKodeOwnerutama();
        String kodeUsers  = sessionManager.getKodeUsers();
        String hakAksesLokal = sessionManager.getHakAkses();

        // Guard: pastikan session masih valid
        if (kodeOwner == null || kodeOwner.isEmpty()) {
            paksaLogout("kode_owner kosong di session");
            return;
        }
        if (kodeUsers == null || kodeUsers.isEmpty()) {
            paksaLogout("kode_users kosong di session");
            return;
        }
        if (hakAksesLokal == null || hakAksesLokal.isEmpty()) {
            paksaLogout("hak_akses lokal kosong di session");
            return;
        }

        // Build URL dengan dua parameter
        String url = BASE_URL
                + "/toko/status?kode_owner=" + kodeOwner
                + "&kode_users=" + kodeUsers;

        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();

        Log.d(TAG, "Verifikasi role: kode_owner=" + kodeOwner
                + " kode_users=" + kodeUsers
                + " hak_akses_lokal=" + hakAksesLokal);

        client.newCall(request).enqueue(new Callback() {

            // ── Kasus C: Tidak ada koneksi / timeout ─────────────────────────
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e(TAG, "verifikasiRoleDariServer onFailure: " + e.getMessage());

                requireActivity().runOnUiThread(() ->
                        tampilkanErrorVerifikasi(
                                "Tidak dapat terhubung ke server.\n" +
                                        "Periksa koneksi internet Anda\ndan coba lagi."
                        )
                );
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response)
                    throws IOException {

                String body     = response.body() != null ? response.body().string() : "";
                int    httpCode = response.code();

                requireActivity().runOnUiThread(() ->
                        prosesResponVerifikasi(httpCode, body, hakAksesLokal)
                );
            }
        });
    }

    /**
     * Proses response dari server verifikasi.
     * Dipanggil di UI thread.
     *
     * @param httpCode       HTTP status code
     * @param body           Response body JSON
     * @param hakAksesLokal  Nilai hak_akses dari SharedPreferences lokal
     */
    private void prosesResponVerifikasi(int httpCode, String body, String hakAksesLokal) {
        try {
            // ── HTTP error ───────────────────────────────────────────────────
            if (httpCode == 404) {
                tampilkanErrorVerifikasi(
                        "Akun Anda tidak ditemukan di server.\n" +
                                "Hubungi admin Pyramidsoft."
                );
                return;
            }
            if (httpCode != 200) {
                tampilkanErrorVerifikasi(
                        "Server mengembalikan error (" + httpCode + ").\n" +
                                "Coba lagi dalam beberapa saat."
                );
                return;
            }

            // ── Parse JSON ───────────────────────────────────────────────────
            JSONObject json = new JSONObject(body);

            if (!json.optBoolean("status", false)) {
                String msg = json.optString("message", "Verifikasi gagal.");
                tampilkanErrorVerifikasi(msg);
                return;
            }

            JSONObject data          = json.getJSONObject("data");
            String hakAksesServer    = data.optString("hak_akses", null);
            String expDate           = data.optString("exp_date", null);
            int    maxToko           = data.optInt("max_toko", 1);

            Log.d(TAG, "Verifikasi server: hak_akses_server=" + hakAksesServer
                    + " hak_akses_lokal=" + hakAksesLokal);

            // ── Kasus B: Mismatch — hak_akses tidak cocok ────────────────────
            // Server mengembalikan nilai null jika kode_users tidak ditemukan
            // atau user tidak aktif. Keduanya dianggap mismatch.
            if (hakAksesServer == null || hakAksesServer.isEmpty()) {
                // User tidak ditemukan di server dengan status Aktif
                // → kemungkinan sudah dinonaktifkan oleh owner
                paksaLogout("user tidak ditemukan di server (status tidak Aktif)");
                return;
            }

            if (!hakAksesServer.equalsIgnoreCase(hakAksesLokal)) {
                // hak_akses lokal berbeda dari server → bypass terdeteksi
                Log.w(TAG, "BYPASS TERDETEKSI: lokal=" + hakAksesLokal
                        + " server=" + hakAksesServer);
                paksaLogout("hak_akses mismatch: lokal=" + hakAksesLokal
                        + " server=" + hakAksesServer);
                return;
            }

            // ── Kasus A: Lolos verifikasi ─────────────────────────────────────
            // Sync exp_date dari server ke SQLite lokal jika perlu
            if (expDate != null && !expDate.isEmpty()) {
                dbHelper.syncAndCheckExpDate(expDate, maxToko);
            }

            verifikasiSelesai = true;

            // Inisialisasi konten dashboard — HANYA dijalankan setelah lolos
            inisialisasiDashboard();

        } catch (Exception e) {
            Log.e(TAG, "prosesResponVerifikasi exception: " + e.getMessage());
            tampilkanErrorVerifikasi(
                    "Gagal memproses respons server.\nCoba lagi dalam beberapa saat."
            );
        }
    }

    // =========================================================================
    // INISIALISASI DASHBOARD — dipanggil HANYA setelah verifikasi lolos
    // =========================================================================

    /**
     * Inisialisasi semua konten dashboard.
     * Sebelumnya dipanggil langsung dari onViewCreated().
     * GAP 5: hanya dipanggil dari prosesResponVerifikasi() setelah role terverifikasi.
     */
    private void inisialisasiDashboard() {
        // Tampilkan konten dashboard
        tampilkanKontenDashboard();

        String hakAkses = sessionManager.getHakAkses();
        boolean isOwnerOrAdmin = "Owner".equalsIgnoreCase(hakAkses)
                || "Admin".equalsIgnoreCase(hakAkses);

        if (!isOwnerOrAdmin) {
            binding.cardStatusAkun.setVisibility(View.GONE);
            binding.btnPerpanjang.setVisibility(View.GONE);
        }

        // Muat semua data
        loadRingkasanHariIni();
        loadRingkasanBulanIni();
        loadChartTujuhHari();

        if (isOwnerOrAdmin) {
            loadStatusAkunDariLokal();
            fetchStatusAkunDariServer();
        }

        // Tombol perpanjang → navigasi ke PaketLanggananActivity
        binding.btnPerpanjang.setOnClickListener(v -> {
            Intent intent = new Intent(requireActivity(), PaketLanggananActivity.class);
            startActivity(intent);
        });

        // Pull-to-refresh — verifikasi ulang sebelum refresh konten
        binding.swipeRefresh.setOnRefreshListener(() -> {
            binding.swipeRefresh.setRefreshing(false);
            // Swipe refresh hanya refresh data, tidak verifikasi ulang role
            // (verifikasi sudah dilakukan saat fragment dibuka)
            if (verifikasiSelesai) {
                loadRingkasanHariIni();
                loadRingkasanBulanIni();
                loadChartTujuhHari();
                if (isOwnerOrAdmin) {
                    loadStatusAkunDariLokal();
                    fetchStatusAkunDariServer();
                }
            }
        });
    }

    // =========================================================================
    // KARTU STATUS AKUN — data dari SQLite lokal terlebih dahulu
    // =========================================================================

    private void loadStatusAkunDariLokal() {
        try {
            String namaToko = sessionManager.getNamatoko();
            if (namaToko == null) namaToko = "-";

            String expDateStr = dbHelper.getExpiredDate();
            binding.txtNamaToko.setText(namaToko);

            if (expDateStr == null || expDateStr.isEmpty()) {
                binding.txtExpDate.setText("Trial (belum diketahui)");
                binding.txtSisaHari.setText("");
                binding.txtStatusAkun.setText("Trial");
                binding.txtStatusAkun.setTextColor(Color.parseColor("#F59E0B"));
                binding.btnPerpanjang.setVisibility(View.GONE);
                return;
            }

            SimpleDateFormat sdf       = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            SimpleDateFormat sdfTampil = new SimpleDateFormat("dd MMM yyyy", new Locale("id", "ID"));
            Date expDate = sdf.parse(expDateStr);
            Date now     = new Date();

            long diffMs   = expDate.getTime() - now.getTime();
            long sisaHari = diffMs / (1000 * 60 * 60 * 24);

            binding.txtExpDate.setText(sdfTampil.format(expDate));

            if (expDate.before(now)) {
                binding.txtStatusAkun.setText("Expired");
                binding.txtStatusAkun.setTextColor(Color.parseColor("#EF4444"));
                binding.txtSisaHari.setText("Akun sudah tidak aktif");
                binding.txtSisaHari.setTextColor(Color.parseColor("#EF4444"));
                binding.btnPerpanjang.setVisibility(View.VISIBLE);
            } else if (sisaHari <= 30) {
                binding.txtStatusAkun.setText("Premium");
                binding.txtStatusAkun.setTextColor(Color.parseColor("#10B981"));
                binding.txtSisaHari.setText("Sisa " + sisaHari + " hari");
                binding.txtSisaHari.setTextColor(Color.parseColor("#F59E0B"));
                binding.btnPerpanjang.setVisibility(View.VISIBLE);
            } else {
                binding.txtStatusAkun.setText("Premium");
                binding.txtStatusAkun.setTextColor(Color.parseColor("#10B981"));
                binding.txtSisaHari.setText("Sisa " + sisaHari + " hari");
                binding.txtSisaHari.setTextColor(Color.parseColor("#6B7280"));
                binding.btnPerpanjang.setVisibility(View.GONE);
            }

        } catch (Exception e) {
            e.printStackTrace();
            binding.txtExpDate.setText("-");
            binding.txtStatusAkun.setText("-");
        }
    }

    /**
     * Fetch status toko terkini dari server (sync exp_date).
     * Catatan: ini BERBEDA dari verifikasiRoleDariServer().
     * Method ini hanya untuk sinkronisasi exp_date setelah pembayaran berhasil.
     * Tidak melakukan verifikasi role.
     */
    private void fetchStatusAkunDariServer() {
        String kodeOwner = sessionManager.getKodeOwnerutama();
        if (kodeOwner == null || kodeOwner.isEmpty()) return;

        Request request = new Request.Builder()
                .url(BASE_URL + "/toko/status?kode_owner=" + kodeOwner)
                .get()
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.w(TAG, "fetchStatusAkunDariServer gagal: " + e.getMessage());
                // Gagal fetch — data lokal sudah tampil, tidak tampilkan error
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response)
                    throws IOException {
                if (!response.isSuccessful()) return;
                String body = response.body().string();

                try {
                    JSONObject json = new JSONObject(body);
                    JSONObject data = json.has("data") && !json.isNull("data")
                            ? json.getJSONObject("data") : json;

                    String expServer  = data.optString("exp_date", null);
                    int maxTokoServer = data.optInt("max_toko", 1);

                    if (expServer != null && !expServer.isEmpty()) {
                        dbHelper.syncAndCheckExpDate(expServer, maxTokoServer);
                        requireActivity().runOnUiThread(() -> loadStatusAkunDariLokal());
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    // =========================================================================
    // KARTU RINGKASAN HARI INI
    // =========================================================================

    private void loadRingkasanHariIni() {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yy", Locale.getDefault());
            String hariIni = sdf.format(new Date());

            SQLiteDatabase db = dbHelper.getReadableDatabase();

            Cursor curTrx = db.rawQuery(
                    "SELECT COUNT(*) FROM tb_penjualan " +
                            "WHERE tanggal = ? AND deleted_at IS NULL",
                    new String[]{hariIni}
            );
            int totalTrx = 0;
            if (curTrx.moveToFirst()) totalTrx = curTrx.getInt(0);
            curTrx.close();

            Cursor curOmzet = db.rawQuery(
                    "SELECT COALESCE(SUM(CAST(total_jml_transaksi AS INTEGER)), 0) " +
                            "FROM tb_penjualan " +
                            "WHERE tanggal = ? AND deleted_at IS NULL",
                    new String[]{hariIni}
            );
            long totalOmzet = 0;
            if (curOmzet.moveToFirst()) totalOmzet = curOmzet.getLong(0);
            curOmzet.close();

            final int  finalTrx   = totalTrx;
            final long finalOmzet = totalOmzet;

            requireActivity().runOnUiThread(() -> {
                binding.txtTrxHariIni.setText(finalTrx + " transaksi");
                binding.txtOmzetHariIni.setText("Rp " + formatRupiah(finalOmzet));
            });

        } catch (Exception e) {
            e.printStackTrace();
            requireActivity().runOnUiThread(() -> {
                binding.txtTrxHariIni.setText("0 transaksi");
                binding.txtOmzetHariIni.setText("Rp 0");
            });
        }
    }

    // =========================================================================
    // KARTU RINGKASAN BULAN INI
    // =========================================================================

    private void loadRingkasanBulanIni() {
        try {
            Calendar cal    = Calendar.getInstance();
            String bulanIni = String.format(Locale.getDefault(), "%02d", cal.get(Calendar.MONTH) + 1);
            String tahunIni = String.valueOf(cal.get(Calendar.YEAR));

            SQLiteDatabase db = dbHelper.getReadableDatabase();

            Cursor curTrx = db.rawQuery(
                    "SELECT COUNT(*) FROM tb_penjualan " +
                            "WHERE bulan = ? AND tahun = ? AND deleted_at IS NULL",
                    new String[]{bulanIni, tahunIni}
            );
            int totalTrx = 0;
            if (curTrx.moveToFirst()) totalTrx = curTrx.getInt(0);
            curTrx.close();

            Cursor curOmzet = db.rawQuery(
                    "SELECT COALESCE(SUM(CAST(total_jml_transaksi AS INTEGER)), 0) " +
                            "FROM tb_penjualan " +
                            "WHERE bulan = ? AND tahun = ? AND deleted_at IS NULL",
                    new String[]{bulanIni, tahunIni}
            );
            long totalOmzet = 0;
            if (curOmzet.moveToFirst()) totalOmzet = curOmzet.getLong(0);
            curOmzet.close();

            final int  finalTrx   = totalTrx;
            final long finalOmzet = totalOmzet;

            requireActivity().runOnUiThread(() -> {
                binding.txtTrxBulanIni.setText(finalTrx + " transaksi");
                binding.txtOmzetBulanIni.setText("Rp " + formatRupiah(finalOmzet));
            });

        } catch (Exception e) {
            e.printStackTrace();
            requireActivity().runOnUiThread(() -> {
                binding.txtTrxBulanIni.setText("0 transaksi");
                binding.txtOmzetBulanIni.setText("Rp 0");
            });
        }
    }

    // =========================================================================
    // BAR CHART PENJUALAN 7 HARI TERAKHIR
    // =========================================================================

    private void loadChartTujuhHari() {
        try {
            SQLiteDatabase   db       = dbHelper.getReadableDatabase();
            SimpleDateFormat sdf      = new SimpleDateFormat("dd-MM-yy", Locale.getDefault());
            SimpleDateFormat labelFmt = new SimpleDateFormat("dd/MM", Locale.getDefault());

            List<Float>  nilaiList = new ArrayList<>();
            List<String> labelList = new ArrayList<>();

            Calendar cal = Calendar.getInstance();

            for (int i = 6; i >= 0; i--) {
                Calendar hari = (Calendar) cal.clone();
                hari.add(Calendar.DAY_OF_YEAR, -i);
                String tgl = sdf.format(hari.getTime());

                Cursor cur = db.rawQuery(
                        "SELECT COALESCE(SUM(CAST(total_jml_transaksi AS INTEGER)), 0) " +
                                "FROM tb_penjualan " +
                                "WHERE tanggal = ? AND deleted_at IS NULL",
                        new String[]{tgl}
                );
                long omzet = 0;
                if (cur.moveToFirst()) omzet = cur.getLong(0);
                cur.close();

                nilaiList.add((float) omzet);
                labelList.add(labelFmt.format(hari.getTime()));
            }

            final List<Float>  finalNilai = nilaiList;
            final List<String> finalLabel = labelList;

            requireActivity().runOnUiThread(() ->
                    binding.barChartPenjualan.setData(finalNilai, finalLabel)
            );

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // =========================================================================
    // Helper
    // =========================================================================

    private String formatRupiah(long amount) {
        NumberFormat nf = NumberFormat.getInstance(new Locale("id", "ID"));
        return nf.format(amount);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Cancel pending network calls agar tidak crash saat fragment destroyed
        client.dispatcher().cancelAll();
        binding = null;
    }
}