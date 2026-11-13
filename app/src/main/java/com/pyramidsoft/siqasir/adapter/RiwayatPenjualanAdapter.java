package com.pyramidsoft.siqasir.adapter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.pyramidsoft.siqasir.CetakUlangRiwayatPenjualan;
import com.pyramidsoft.siqasir.R;
import com.pyramidsoft.siqasir.db.DataBaseHelper;
import com.pyramidsoft.siqasir.model.RiwayatPenjualanModels;
import com.pyramidsoft.siqasir.utils.DecimalsFormat;

import java.util.ArrayList;

public class RiwayatPenjualanAdapter extends RecyclerView.Adapter<RiwayatPenjualanAdapter.RiwayatHolder> {

    private ArrayList<RiwayatPenjualanModels> arrayList;
    private Context context;
    SQLiteDatabase mDatabase;
    DataBaseHelper dbHelper;

    public RiwayatPenjualanAdapter(ArrayList<RiwayatPenjualanModels> arrayList, Context context, SQLiteDatabase mDatabase) {
        this.arrayList = arrayList;
        this.context = context;
        this.mDatabase = mDatabase;
    }

    public void filter(String text, ArrayList<RiwayatPenjualanModels> arrayListCopy) {
        arrayList.clear();
        if (text.isEmpty()) {
            arrayList.addAll(arrayListCopy);
        } else {
            text = text.toLowerCase();
            for (RiwayatPenjualanModels item : arrayListCopy) {
                if (item.getNamaCust().toLowerCase().contains(text)) {
                    arrayList.add(item);
                } else if (item.getInvoice().toLowerCase().contains(text)) {
                    arrayList.add(item);
                }
            }
        }
    }

    @NonNull
    @Override
    public RiwayatHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(context);
        View view = inflater.inflate(R.layout.item_list_riwayat_penjualan, null);
        return new RiwayatHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RiwayatHolder holder, @SuppressLint("RecyclerView") int position) {

        holder.tvLayanan.setText(arrayList.get(position).getJenisKedatangan());
        holder.tvNamaCust.setText(arrayList.get(position).getNamaCust());
        holder.tvInvoice.setText(arrayList.get(position).getInvoice());
        holder.tvQty.setText("Jumlah : " + arrayList.get(position).getQty());
        holder.tvTotalTransaksi.setText("Rp" + DecimalsFormat.priceWithoutDecimal(arrayList.get(position).getTotalTransaksi()));
        holder.llRiwayatPenjualan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(context, CetakUlangRiwayatPenjualan.class);
                intent.putExtra("invoice", arrayList.get(position).getInvoice());
                context.startActivity(intent);
            }
        });
    }

    @Override
    public int getItemCount() {
        return arrayList.size();
    }

    public class RiwayatHolder extends RecyclerView.ViewHolder {
        TextView tvLayanan, tvInvoice, tvNamaCust, tvQty, tvTotalTransaksi;
        LinearLayout llRiwayatPenjualan;

        public RiwayatHolder(@NonNull View itemView) {
            super(itemView);
            dbHelper = new DataBaseHelper(context);

            tvLayanan = itemView.findViewById(R.id.tvLayanan);
            tvInvoice = itemView.findViewById(R.id.tvInvoice);
            tvNamaCust = itemView.findViewById(R.id.tvNamaCust);
            tvQty = itemView.findViewById(R.id.tvQty);
            tvTotalTransaksi = itemView.findViewById(R.id.tvTotalTransaksi);
            llRiwayatPenjualan = itemView.findViewById(R.id.ll_riwayat_penjualan);
        }
    }
}
