package com.pyramidsoft.siqasir.ui.laporanpenjualandetail;

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

public class LaporanPenjualanDetailAdapter
        extends RecyclerView.Adapter<LaporanPenjualanDetailAdapter.ViewHolder> {

    private final List<LaporanPenjualanDetailItem> data;
    private final NumberFormat rupiah =
            NumberFormat.getCurrencyInstance(new Locale("id", "ID"));

    public LaporanPenjualanDetailAdapter(List<LaporanPenjualanDetailItem> data) {
        this.data = data;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType
    ) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_laporan_penjualan_detail, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(
            @NonNull ViewHolder holder,
            int position
    ) {
        LaporanPenjualanDetailItem item = data.get(position);

        // ===== DATA ROW =====
        holder.tvProduk.setText(item.produk);
        holder.tvQty.setText(String.valueOf(item.qty));
        holder.tvPenjualan.setText(rupiah.format(item.totalPenjualan));
        holder.tvLaba.setText(rupiah.format(item.totalLaba));

        // ===== PEMBATAS TANGGAL =====
        if (position == 0 ||
                !data.get(position - 1).tanggal.equals(item.tanggal)) {

            holder.layoutTanggal.setVisibility(View.VISIBLE);
            holder.tvHeaderTanggal.setText(item.tanggal);

        } else {
            holder.layoutTanggal.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        View layoutTanggal;
        TextView tvHeaderTanggal;
        TextView tvProduk, tvQty, tvPenjualan, tvLaba;

        ViewHolder(@NonNull View itemView) {
            super(itemView);

            layoutTanggal = itemView.findViewById(R.id.layoutTanggal);
            tvHeaderTanggal = itemView.findViewById(R.id.tvHeaderTanggal);

            tvProduk = itemView.findViewById(R.id.tvProduk);
            tvQty = itemView.findViewById(R.id.tvQty);
            tvPenjualan = itemView.findViewById(R.id.tvPenjualan);
            tvLaba = itemView.findViewById(R.id.tvLaba);
        }
    }
}
