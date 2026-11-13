package com.pyramidsoft.siqasir.adapter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.RecyclerView;

import com.pyramidsoft.siqasir.R;
import com.pyramidsoft.siqasir.model.ListProdukAtHomeModel;
import com.pyramidsoft.siqasir.model.ProdukPesananModel;
import com.pyramidsoft.siqasir.utils.DataPesanan;
import com.pyramidsoft.siqasir.utils.DecimalsFormat;

import java.util.ArrayList;

public class ListProdukAtHomeAdapter extends RecyclerView.Adapter<ListProdukAtHomeAdapter.ListSemuaHolder> {

    private final ArrayList<ListProdukAtHomeModel> arrayList;
    public static ArrayList<ProdukPesananModel> pesanan;
    private final Context context;

    public ListProdukAtHomeAdapter(ArrayList<ListProdukAtHomeModel> list, Context context) {
        this.arrayList = list;
        this.context = context;
    }

    public void filter(String text, ArrayList<ListProdukAtHomeModel> arrayListCopy) {
        arrayList.clear();
        if (text.isEmpty()) {
            arrayList.addAll(arrayListCopy);
        } else {
            text = text.toLowerCase();
            for (ListProdukAtHomeModel item : arrayListCopy) {
                if (item.getNama_produk().toLowerCase().contains(text)) {
                    arrayList.add(item);
                }
            }
        }
    }

    @NonNull
    @Override
    public ListSemuaHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(context);
        View view = inflater.inflate(R.layout.item_list_semua_produk, null);
        return new ListSemuaHolder(view);
    }

    @SuppressLint("RecyclerView")
    @Override
    public void onBindViewHolder(@NonNull ListSemuaHolder holder, int position) {
        byte[] gbrProduk = arrayList.get(position).getIvProduk();
        Bitmap bitmap = BitmapFactory.decodeByteArray(gbrProduk, 0, gbrProduk.length);
        holder.ivProduk.setImageBitmap(bitmap);
        holder.tvNamaProduk.setText(arrayList.get(position).getNama_produk());
        if (arrayList.get(position).getHrg_potongan().equals("0")) {
            holder.tvHargaPotongan.setVisibility(View.VISIBLE);
            holder.tvHargaSale.setText("Rp. " + DecimalsFormat.priceWithoutDecimal(arrayList.get(position).getHrg_sale()));
        } else {
            holder.tvHargaPotongan.setVisibility(View.VISIBLE);
            holder.tvHargaPotongan.setText("Rp. " + DecimalsFormat.priceWithoutDecimal(arrayList.get(position).getHrg_sale()));
            holder.tvHargaPotongan.setPaintFlags(holder.tvHargaPotongan.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            holder.tvHargaSale.setText("Rp. " + DecimalsFormat.priceWithoutDecimal(arrayList.get(position).getHrg_potongan()));
        }
        holder.kategori.setText(arrayList.get(position).getKategori());

        Integer stok = arrayList.get(position).getStok();
        if (stok == null) {
            holder.tvStok.setVisibility(View.VISIBLE);
            holder.tvStok.setText(" ");
        } else {
            holder.tvStok.setVisibility(View.VISIBLE);
            holder.tvStok.setText("Stok: " + stok);
        }

        holder.btGet.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String harga;
                if (arrayList.get(position).getHrg_potongan().equals("0")) {
                    harga = arrayList.get(position).getHrg_sale();
                } else {
                    harga = arrayList.get(position).getHrg_potongan();
                }
                ProdukPesananModel produkPesananModel = new ProdukPesananModel(
                        arrayList.get(position).getKd_produk(),
                        arrayList.get(position).getNama_produk(),
                        harga,
                        harga,
                        arrayList.get(position).getHrg_potongan(),
                        arrayList.get(position).getHrg_sale(),
                        "1"
                );
                DataPesanan.addNewPesanan(produkPesananModel);
                Intent intent = new Intent("dataPesanan-state");
                intent.putExtra("refresh", "pesanan");

                LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
            }
        });

        holder.cardview.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String harga;
                if (arrayList.get(position).getHrg_potongan().equals("0")) {
                    harga = arrayList.get(position).getHrg_sale();
                } else {
                    harga = arrayList.get(position).getHrg_potongan();
                }
                ProdukPesananModel produkPesananModel = new ProdukPesananModel(
                        arrayList.get(position).getKd_produk(),
                        arrayList.get(position).getNama_produk(),
                        harga,
                        harga,
                        arrayList.get(position).getHrg_potongan(),
                        arrayList.get(position).getHrg_sale(),
                        "1"
                );
                DataPesanan.addNewPesanan(produkPesananModel);
                Intent intent = new Intent("dataPesanan-state");
                intent.putExtra("refresh", "pesanan");

                LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
            }
        });
    }

    @Override
    public int getItemCount() {
        return arrayList.size();
    }

    public class ListSemuaHolder extends RecyclerView.ViewHolder {

        ImageView ivProduk;
        TextView tvNamaProduk, kategori, tvHargaPotongan, tvHargaSale, tvStok;
        CardView cardview;
        Button btGet;

        public ListSemuaHolder(@NonNull View itemView) {
            super(itemView);
            ivProduk = itemView.findViewById(R.id.ivProduk);
            tvNamaProduk = itemView.findViewById(R.id.tvNamaProduk);
            kategori = itemView.findViewById(R.id.kategori);
            tvHargaPotongan = itemView.findViewById(R.id.tvHargaPotongan);
            tvHargaSale = itemView.findViewById(R.id.tvHargaSale);
            cardview = itemView.findViewById(R.id.cardview);
            btGet = itemView.findViewById(R.id.btGet);
            tvStok = itemView.findViewById(R.id.tvStok);
        }
    }
}
