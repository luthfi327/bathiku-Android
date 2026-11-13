package com.pyramidsoft.siqasir.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.pyramidsoft.siqasir.R;
import com.pyramidsoft.siqasir.model.ProdukPesananModel;
import com.pyramidsoft.siqasir.utils.DecimalsFormat;

import java.util.ArrayList;
import java.util.List;

public class ListDiskonProdukAtPembayaran extends RecyclerView.Adapter<ListDiskonProdukAtPembayaran.holder> {

    Context context;
    public static List<ProdukPesananModel> list = new ArrayList<>();

    public ListDiskonProdukAtPembayaran(Context context, List<ProdukPesananModel> list) {
        this.context = context;
        this.list = list;
    }

    @NonNull
    @Override
    public ListDiskonProdukAtPembayaran.holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(context);
        View view = inflater.inflate(R.layout.item_list_pesanan_pembayaran, null);
        return new holder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ListDiskonProdukAtPembayaran.holder holder, int i) {
        holder.tvheader_title.setTextSize(12);
        holder.tvHargaItem.setTextSize(12);
        holder.tvheader_title.setText("Diskon " +  list.get(i).getNama_produk() + " X " + list.get(i).getJml() );
        int jml = Integer.parseInt(list.get(i).getJml());
        int hrgDiskon = Integer.parseInt(list.get(i).getHarga_sale()) - Integer.parseInt(list.get(i).getHarga_diskon());
        int totalDiskon = hrgDiskon * jml;
        holder.tvHargaItem.setText("- Rp. " + DecimalsFormat.priceWithoutDecimal(String.valueOf(totalDiskon)));
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    public class holder extends RecyclerView.ViewHolder {

        TextView tvheader_title, tvHargaItem;
        RelativeLayout relItem;

        public holder(@NonNull View itemView) {
            super(itemView);
            tvheader_title = itemView.findViewById(R.id.tvheader_title);
            tvHargaItem = itemView.findViewById(R.id.tvHargaItem);
            relItem = itemView.findViewById(R.id.relItem);
        }
    }
}
