package com.pyramidsoft.siqasir.ui.laporanpenjualan;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.pyramidsoft.siqasir.R;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

public class LaporanPenjualanAdapter
        extends RecyclerView.Adapter<LaporanPenjualanAdapter.ViewHolder> {

    private final List<LaporanPenjualanItem> data;
    private final NumberFormat rupiah = NumberFormat.getCurrencyInstance(new Locale("id", "ID"));

    public LaporanPenjualanAdapter(List<LaporanPenjualanItem> data) {
        this.data = data;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType
    ) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_laporan_penjualan, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(
            @NonNull ViewHolder holder,
            int position
    ) {
        LaporanPenjualanItem item = data.get(position);

        holder.tvTanggal.setText(item.tanggal);
        holder.tvPenjualan.setText(rupiah.format(item.totalPenjualan));
        holder.tvLaba.setText(rupiah.format(item.totalLaba));
        holder.tvTrx.setText(String.valueOf(item.totalTransaksi));
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        TextView tvTanggal, tvPenjualan, tvLaba, tvTrx;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTanggal = itemView.findViewById(R.id.tvTanggal);
            tvPenjualan = itemView.findViewById(R.id.tvPenjualan);
            tvLaba = itemView.findViewById(R.id.tvLaba);
            tvTrx = itemView.findViewById(R.id.tvTrx);
        }
    }
}
