package com.pyramidsoft.siqasir.ui.ownerpayment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.card.MaterialCardView;
import com.pyramidsoft.siqasir.R;
import com.pyramidsoft.siqasir.utils.ApiKeyHttpClient;
import com.pyramidsoft.siqasir.utils.SessionManager;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Locale;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * OwnerPaymentHistoryFragment
 *
 * Menampilkan SELURUH riwayat pembayaran premium milik owner yang sedang login.
 *
 * Endpoint yang digunakan (GAP 3):
 *   GET /api/payment/history/{kode_owner}
 *   → dilindungi middleware android.key (header X-Android-Key otomatis via ApiKeyHttpClient)
 *   → mengembalikan array semua pembayaran, urut created_at DESC
 *
 * Sebelumnya (lama): menggunakan GET /api/qris/status?kode_owner=...
 * yang hanya mengembalikan 1 record terbaru saja.
 *
 * Alur:
 *   onViewCreated → ambil kode_owner dari SessionManager
 *              → tampilkan skeleton/loading state
 *              → fetch ke server
 *              → parse JSON → isi RecyclerView
 *              → hitung total PAID → tampilkan di summary card
 *
 * Error handling:
 *   - kode_owner kosong  → tampilkan error state "Sesi tidak valid"
 *   - HTTP 404           → tampilkan empty state "Belum ada riwayat"
 *   - HTTP error lain    → tampilkan error state dengan kode HTTP
 *   - IOException        → tampilkan error state "Tidak ada koneksi"
 */
public class OwnerPaymentHistoryFragment extends Fragment {

    private static final String TAG      = "OwnerPayHist";
    private static final String BASE_URL = "https://siqasir.awandigital.id/api";

    // ─── Views ──────────────────────────────────────────────────────────────
    private SwipeRefreshLayout swipeRefresh;
    private RecyclerView       recyclerView;
    private LinearLayout       layoutLoading;
    private LinearLayout       layoutError;
    private LinearLayout       layoutEmpty;
    private MaterialCardView   cardSummary;  // FIX: was LinearLayout, must match XML (MaterialCardView)
    private TextView           tvErrorMsg;
    private TextView           tvTotalPaid;
    private TextView           tvJumlahTransaksi;

    // ─── Data & Utils ────────────────────────────────────────────────────────
    private RiwayatPembayaranAdapter adapter;
    private ArrayList<RiwayatPembayaranModel> dataList = new ArrayList<>();
    private SessionManager sessionManager;
    private OkHttpClient   client = ApiKeyHttpClient.get();

    // ─── Lifecycle ────────────────────────────────────────────────────────────

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_owner_payment_history, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        sessionManager = new SessionManager(requireActivity());

        // Bind views
        swipeRefresh       = view.findViewById(R.id.swipeRefreshPayHist);
        recyclerView       = view.findViewById(R.id.rvRiwayatPembayaran);
        layoutLoading      = view.findViewById(R.id.layoutLoadingPayHist);
        layoutError        = view.findViewById(R.id.layoutErrorPayHist);
        layoutEmpty        = view.findViewById(R.id.layoutEmptyPayHist);
        cardSummary        = view.findViewById(R.id.cardSummaryPayHist);
        tvErrorMsg         = view.findViewById(R.id.tvErrorMsgPayHist);
        tvTotalPaid        = view.findViewById(R.id.tvTotalPaidHist);
        tvJumlahTransaksi  = view.findViewById(R.id.tvJumlahTransaksiHist);

        // Setup RecyclerView
        adapter      = new RiwayatPembayaranAdapter(dataList, requireContext());
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(adapter);

        // Sembunyikan card summary sampai data masuk
        cardSummary.setVisibility(View.GONE);

        // Pull-to-refresh
        swipeRefresh.setColorSchemeColors(
                getResources().getColor(R.color.colorPrimary),
                getResources().getColor(R.color.secondary_purple)
        );
        swipeRefresh.setOnRefreshListener(this::fetchRiwayat);

        // Load pertama kali
        fetchRiwayat();
    }

    // ─── Fetch Data dari Server ───────────────────────────────────────────────

    /**
     * Mengambil semua riwayat pembayaran dari endpoint baru GAP 3:
     *   GET /api/payment/history/{kode_owner}
     */
    private void fetchRiwayat() {
        String kodeOwner = sessionManager.getKodeOwnerutama();

        if (kodeOwner == null || kodeOwner.isEmpty()) {
            showErrorState("Sesi tidak valid. Silakan logout dan login ulang.");
            swipeRefresh.setRefreshing(false);
            return;
        }

        showLoadingState();

        // Endpoint baru GAP 3 — ARRAY semua pembayaran
        String url = BASE_URL + "/payment/history/" + kodeOwner;

        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                android.util.Log.e(TAG, "Fetch gagal: " + e.getMessage());
                requireActivity().runOnUiThread(() -> {
                    swipeRefresh.setRefreshing(false);
                    showErrorState("Tidak dapat terhubung ke server.\nPeriksa koneksi internet Anda.");
                });
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response)
                    throws IOException {

                String body = response.body() != null ? response.body().string() : "";
                int httpCode = response.code();

                requireActivity().runOnUiThread(() -> {
                    swipeRefresh.setRefreshing(false);
                    handleResponse(httpCode, body);
                });
            }
        });
    }

    /**
     * Parse response dari server dan update UI.
     * Dipanggil di UI thread.
     */
    private void handleResponse(int httpCode, String body) {
        try {
            JSONObject json = new JSONObject(body);

            // HTTP 404 → kode_owner tidak ditemukan
            if (httpCode == 404) {
                showEmptyState();
                return;
            }

            // HTTP lain selain 200 → error
            if (httpCode != 200) {
                String msg = json.optString("message", "Terjadi kesalahan (HTTP " + httpCode + ")");
                showErrorState(msg);
                return;
            }

            // Parse array data
            JSONArray dataArr = json.optJSONArray("data");
            if (dataArr == null || dataArr.length() == 0) {
                showEmptyState();
                return;
            }

            ArrayList<RiwayatPembayaranModel> hasil = new ArrayList<>();
            long totalNominalPaid = 0;
            int  jumlahPaid       = 0;

            for (int i = 0; i < dataArr.length(); i++) {
                JSONObject item = dataArr.getJSONObject(i);

                long   id            = item.optLong("id", 0);
                String externalId    = item.optString("external_id", "");
                long   amount        = item.optLong("amount", 0);
                int    durasiBulan   = item.optInt("durasi_bulan", 1);
                String status        = item.optString("status", "");
                String hargaKategori = item.optString("harga_kategori", "");
                String paidAt        = item.optString("paid_at", "");
                String createdAt     = item.optString("created_at", "");
                String expiredAt     = item.optString("expired_at", "");
                String oldExpDate    = item.optString("old_exp_date", "");
                String newExpDate    = item.optString("new_exp_date", "");

                hasil.add(new RiwayatPembayaranModel(
                        id, externalId, amount, durasiBulan, status,
                        hargaKategori, paidAt, createdAt, expiredAt,
                        oldExpDate, newExpDate
                ));

                // Akumulasi total PAID untuk summary card
                if ("PAID".equalsIgnoreCase(status)) {
                    totalNominalPaid += amount;
                    jumlahPaid++;
                }
            }

            // Update adapter
            adapter.updateData(hasil);

            // Tampilkan summary card
            showSummaryCard(totalNominalPaid, jumlahPaid, dataArr.length());

            // Tampilkan state data
            showDataState();

        } catch (Exception e) {
            android.util.Log.e(TAG, "Parse error: " + e.getMessage());
            showErrorState("Gagal memuat data. Silakan coba lagi.");
        }
    }

    // ─── State Management ─────────────────────────────────────────────────────

    private void showLoadingState() {
        layoutLoading.setVisibility(View.VISIBLE);
        layoutError.setVisibility(View.GONE);
        layoutEmpty.setVisibility(View.GONE);
        recyclerView.setVisibility(View.GONE);
        cardSummary.setVisibility(View.GONE);
    }

    private void showDataState() {
        layoutLoading.setVisibility(View.GONE);
        layoutError.setVisibility(View.GONE);
        layoutEmpty.setVisibility(View.GONE);
        recyclerView.setVisibility(View.VISIBLE);
    }

    private void showEmptyState() {
        layoutLoading.setVisibility(View.GONE);
        layoutError.setVisibility(View.GONE);
        layoutEmpty.setVisibility(View.VISIBLE);
        recyclerView.setVisibility(View.GONE);
        cardSummary.setVisibility(View.GONE);
    }

    private void showErrorState(String message) {
        layoutLoading.setVisibility(View.GONE);
        layoutError.setVisibility(View.VISIBLE);
        layoutEmpty.setVisibility(View.GONE);
        recyclerView.setVisibility(View.GONE);
        cardSummary.setVisibility(View.GONE);
        tvErrorMsg.setText(message);
    }

    /**
     * Update summary card di atas RecyclerView dengan rekap total pembayaran.
     */
    private void showSummaryCard(long totalPaid, int jumlahPaid, int totalSemua) {
        String rupiah = "Rp " + formatRupiah(totalPaid);
        tvTotalPaid.setText(rupiah);
        tvJumlahTransaksi.setText(jumlahPaid + " dari " + totalSemua + " transaksi berhasil");
        cardSummary.setVisibility(View.VISIBLE);
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private String formatRupiah(long amount) {
        NumberFormat nf = NumberFormat.getInstance(new Locale("id", "ID"));
        return nf.format(amount);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Cancel pending requests agar tidak crash saat fragment destroyed
        client.dispatcher().cancelAll();
    }
}