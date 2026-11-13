package com.pyramidsoft.siqasir.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.pyramidsoft.siqasir.R;
import com.pyramidsoft.siqasir.model.ProdukPesananModel;
import com.pyramidsoft.siqasir.utils.DecimalsFormat;

import java.util.ArrayList;
import java.util.List;

public class ListProdukAtPembayaranAdapter extends RecyclerView.Adapter<ListProdukAtPembayaranAdapter.holder> {

    Context context;
    public static List<ProdukPesananModel> list = new ArrayList<>();

    public ListProdukAtPembayaranAdapter(Context context, List<ProdukPesananModel> list) {
        this.context = context;
        this.list = list;
    }

    @NonNull
    @Override
    public holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(context);
        View view = inflater.inflate(R.layout.item_list_pesanan_pembayaran, null);
        return new holder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull holder holder, int i) {
        holder.tvheader_title.setText(list.get(i).getJml() + " X " + list.get(i).getNama_produk());
        int jml = Integer.parseInt(list.get(i).getJml());
        int hrgSale = Integer.parseInt(list.get(i).getHarga_sale());
        int totalHargaSale = hrgSale * jml;
        holder.tvHargaItem.setText("Rp. " + DecimalsFormat.priceWithoutDecimal(String.valueOf(totalHargaSale)));
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    public class holder extends RecyclerView.ViewHolder {

        TextView tvheader_title, tvHargaItem;

        public holder(@NonNull View itemView) {
            super(itemView);
            tvheader_title = itemView.findViewById(R.id.tvheader_title);
            tvHargaItem = itemView.findViewById(R.id.tvHargaItem);
        }
    }
}
