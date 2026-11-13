package com.pyramidsoft.siqasir.ui.ownerpayment;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.pyramidsoft.siqasir.R;

import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

/**
 * RiwayatPembayaranAdapter
 *
 * Adapter RecyclerView untuk menampilkan daftar riwayat pembayaran owner
 * di OwnerPaymentHistoryFragment.
 *
 * Setiap item menampilkan:
 *   - Tanggal transaksi (format: dd MMM yyyy HH:mm)
 *   - Nominal (format: Rp XX.XXX)
 *   - Durasi berlangganan
 *   - Badge status berwarna (PAID / PENDING / CANCELLED / EXPIRED)
 *   - Kode transaksi (external_id)
 *   - Tanggal aktif baru (new_exp_date) jika status PAID
 */
public class RiwayatPembayaranAdapter
        extends RecyclerView.Adapter<RiwayatPembayaranAdapter.ViewHolder> {

    private final ArrayList<RiwayatPembayaranModel> dataList;
    private final Context context;

    // Format tanggal dari server: "2026-05-01 10:23:00"
    private static final SimpleDateFormat SDF_SERVER =
            new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());

    // Format tampil ke user
    private static final SimpleDateFormat SDF_TAMPIL =
            new SimpleDateFormat("dd MMM yyyy, HH:mm", new Locale("id", "ID"));

    private static final SimpleDateFormat SDF_TANGGAL =
            new SimpleDateFormat("dd MMM yyyy", new Locale("id", "ID"));

    public RiwayatPembayaranAdapter(ArrayList<RiwayatPembayaranModel> dataList, Context context) {
        this.dataList = dataList;
        this.context  = context;
    }

    // ─── ViewHolder ───────────────────────────────────────────────────────────

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTanggal;
        TextView tvNominal;
        TextView tvDurasi;
        TextView tvStatus;
        TextView tvKodeTransaksi;
        TextView tvExpBaru;
        View     dividerExpBaru;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTanggal        = itemView.findViewById(R.id.tvTanggalBayar);
            tvNominal        = itemView.findViewById(R.id.tvNominalBayar);
            tvDurasi         = itemView.findViewById(R.id.tvDurasiBayar);
            tvStatus         = itemView.findViewById(R.id.tvStatusBayar);
            tvKodeTransaksi  = itemView.findViewById(R.id.tvKodeTransaksi);
            tvExpBaru        = itemView.findViewById(R.id.tvExpBaruBayar);
            dividerExpBaru   = itemView.findViewById(R.id.dividerExpBaru);
        }
    }

    // ─── Lifecycle ────────────────────────────────────────────────────────────

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context)
                .inflate(R.layout.item_riwayat_pembayaran_owner, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        RiwayatPembayaranModel item = dataList.get(position);

        // ── Tanggal transaksi ──────────────────────────────────────────────
        holder.tvTanggal.setText(formatTanggal(item.getCreatedAt(), SDF_TAMPIL));

        // ── Nominal ────────────────────────────────────────────────────────
        holder.tvNominal.setText("Rp " + formatRupiah(item.getAmount()));

        // ── Durasi ─────────────────────────────────────────────────────────
        int durasi = item.getDurasiBulan();
        holder.tvDurasi.setText(durasi + " bulan");

        // ── Status badge ───────────────────────────────────────────────────
        String status = item.getStatus() != null ? item.getStatus() : "-";
        holder.tvStatus.setText(status);
        applyStatusStyle(holder.tvStatus, status);

        // ── Kode transaksi ─────────────────────────────────────────────────
        String extId = item.getExternalId();
        if (extId != null && !extId.isEmpty()) {
            holder.tvKodeTransaksi.setText(extId);
            holder.tvKodeTransaksi.setVisibility(View.VISIBLE);
        } else {
            holder.tvKodeTransaksi.setVisibility(View.GONE);
        }

        // ── Aktif sampai (new_exp_date) — hanya tampil jika PAID ──────────
        String newExp = item.getNewExpDate();
        if ("PAID".equalsIgnoreCase(status) && newExp != null && !newExp.isEmpty()) {
            holder.tvExpBaru.setText("Aktif s/d " + formatTanggal(newExp, SDF_TANGGAL));
            holder.tvExpBaru.setVisibility(View.VISIBLE);
            holder.dividerExpBaru.setVisibility(View.VISIBLE);
        } else {
            holder.tvExpBaru.setVisibility(View.GONE);
            holder.dividerExpBaru.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return dataList != null ? dataList.size() : 0;
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    /**
     * Terapkan warna dan background badge sesuai status pembayaran.
     */
    private void applyStatusStyle(TextView tv, String status) {
        switch (status.toUpperCase()) {
            case "PAID":
                tv.setTextColor(Color.parseColor("#166534"));
                tv.setBackgroundColor(Color.parseColor("#DCFCE7"));
                break;
            case "PENDING":
                tv.setTextColor(Color.parseColor("#854D0E"));
                tv.setBackgroundColor(Color.parseColor("#FEF9C3"));
                break;
            case "CANCELLED":
                tv.setTextColor(Color.parseColor("#991B1B"));
                tv.setBackgroundColor(Color.parseColor("#FEE2E2"));
                break;
            case "EXPIRED":
                tv.setTextColor(Color.parseColor("#6B7280"));
                tv.setBackgroundColor(Color.parseColor("#F3F4F6"));
                break;
            default:
                tv.setTextColor(Color.parseColor("#374151"));
                tv.setBackgroundColor(Color.parseColor("#E5E7EB"));
                break;
        }
    }

    /**
     * Format tanggal dari string server ke format tampil yang diinginkan.
     * Jika parsing gagal, kembalikan string asli.
     */
    private String formatTanggal(String raw, SimpleDateFormat outputFmt) {
        if (raw == null || raw.isEmpty()) return "-";
        try {
            Date d = SDF_SERVER.parse(raw);
            return outputFmt.format(d);
        } catch (ParseException e) {
            return raw;
        }
    }

    /**
     * Format angka ke format rupiah tanpa desimal: 49000 → "49.000"
     */
    private String formatRupiah(long amount) {
        NumberFormat nf = NumberFormat.getInstance(new Locale("id", "ID"));
        return nf.format(amount);
    }

    /**
     * Update data list dan refresh RecyclerView.
     * Dipanggil dari Fragment setelah data baru diterima dari server.
     */
    public void updateData(ArrayList<RiwayatPembayaranModel> newData) {
        dataList.clear();
        dataList.addAll(newData);
        notifyDataSetChanged();
    }
}