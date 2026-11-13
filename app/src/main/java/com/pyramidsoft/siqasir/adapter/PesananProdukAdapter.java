package com.pyramidsoft.siqasir.adapter;

import android.content.Context;
import android.content.Intent;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.RecyclerView;

import com.pyramidsoft.siqasir.R;
import com.pyramidsoft.siqasir.model.ProdukPesananModel;
import com.pyramidsoft.siqasir.utils.DataPesanan;
import com.pyramidsoft.siqasir.utils.DecimalsFormat;

import java.util.List;

public class PesananProdukAdapter extends RecyclerView.Adapter<PesananProdukAdapter.holder> {

    List<ProdukPesananModel> arrayPesanan;
    private final Context context;

    public PesananProdukAdapter(List<ProdukPesananModel> arrayList, Context context) {
        this.arrayPesanan = arrayList;
        this.context = context;
    }

    @NonNull
    @Override
    public holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(context);
        View view = inflater.inflate(R.layout.item_list_buatpesanan, null);
        return new holder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull holder holder, int i) {

        ProdukPesananModel model = arrayPesanan.get(i);

        holder.tvNamaProduk.setText(model.getNama_produk());
        holder.tvTotalHarga.setText(
                "Rp. " + DecimalsFormat.priceWithoutDecimal(model.getTotal_harga())
        );

        holder.etQty.setText(model.getJml());

        // HAPUS watcher lama
        if (holder.etQty.getTag() instanceof TextWatcher) {
            holder.etQty.removeTextChangedListener((TextWatcher) holder.etQty.getTag());
        }

        TextWatcher watcher = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                int position = holder.getAdapterPosition();
                if (position == RecyclerView.NO_POSITION) return;
                if (s == null || s.toString().isEmpty()) return;

                int qty;
                try {
                    qty = Integer.parseInt(s.toString());
                } catch (NumberFormatException e) {
                    qty = 1;
                }

                if (qty < 1) qty = 1;

                DataPesanan.updateQty(position, qty);

                holder.tvTotalHarga.setText(
                        "Rp. " + DecimalsFormat.priceWithoutDecimal(
                                arrayPesanan.get(position).getTotal_harga()
                        )
                );

                sendRefresh();
            }
        };

        holder.etQty.addTextChangedListener(watcher);
        holder.etQty.setTag(watcher);

        holder.plusIcon.setOnClickListener(v -> {
            int position = holder.getAdapterPosition();
            if (position == RecyclerView.NO_POSITION) return;

            DataPesanan.addPesanan(position);
            notifyItemChanged(position);
            sendRefresh();
        });

        holder.minIcon.setOnClickListener(v -> {
            int position = holder.getAdapterPosition();
            if (position == RecyclerView.NO_POSITION) return;

            DataPesanan.kurangiPesanan(position);
            notifyItemChanged(position);
            sendRefresh();
        });

        holder.deleteIcon.setOnClickListener(v -> {
            int position = holder.getAdapterPosition();
            if (position == RecyclerView.NO_POSITION) return;

            DataPesanan.removePesanan(position);
            notifyDataSetChanged();
            sendRefresh();
        });
    }

    private void sendRefresh() {
        Intent intent = new Intent("dataPesanan-state");
        intent.putExtra("refresh", "pesanan");
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
    }

    @Override
    public int getItemCount() {
        return arrayPesanan.size();
    }

    public class holder extends RecyclerView.ViewHolder {

        ImageView deleteIcon, minIcon, plusIcon;
        TextView tvNamaProduk, tvTotalHarga;
        EditText etQty;
        LinearLayout linear;

        public holder(@NonNull View itemView) {
            super(itemView);
            deleteIcon = itemView.findViewById(R.id.deleteIcon);
            minIcon = itemView.findViewById(R.id.minIcon);
            plusIcon = itemView.findViewById(R.id.plusIcon);
            tvNamaProduk = itemView.findViewById(R.id.tvNamaProduk);
            tvTotalHarga = itemView.findViewById(R.id.tvTotalHarga);
            etQty = itemView.findViewById(R.id.etQty);
            linear = itemView.findViewById(R.id.linear);
        }
    }
}
