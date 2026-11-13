package com.pyramidsoft.siqasir.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.pyramidsoft.siqasir.R;
import com.pyramidsoft.siqasir.model.StokModel;

import java.util.ArrayList;

public class StokAdapter extends RecyclerView.Adapter<StokAdapter.StokHolder> {

    private final ArrayList<StokModel> list;

    public StokAdapter(ArrayList<StokModel> list) {
        this.list = list;
    }

    @NonNull
    @Override
    public StokHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_stok_laporan, parent, false);
        return new StokHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull StokHolder holder, int position) {
        StokModel m = list.get(position);

        holder.tvNama.setText(m.getNama());
        holder.tvKode.setText("Kode: " + m.getKode());
        holder.tvQty.setText(m.getQty());
        holder.tvHpp.setText(m.getHpp());

        if ("--".equals(m.getQty()) || "0".equals(m.getQty())) {
            holder.tvQty.setAlpha(0.6f);
        } else {
            holder.tvQty.setAlpha(1f);
        }
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    static class StokHolder extends RecyclerView.ViewHolder {

        TextView tvNama, tvKode, tvQty, tvHpp;

        public StokHolder(@NonNull View itemView) {
            super(itemView);
            tvNama = itemView.findViewById(R.id.tvNama);
            tvKode = itemView.findViewById(R.id.tvKode);
            tvQty  = itemView.findViewById(R.id.tvQty);
            tvHpp  = itemView.findViewById(R.id.tvHpp);
        }
    }
}
