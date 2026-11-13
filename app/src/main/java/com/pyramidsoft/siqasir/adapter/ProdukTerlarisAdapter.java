package com.pyramidsoft.siqasir.adapter;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.pyramidsoft.siqasir.R;
import com.pyramidsoft.siqasir.db.DataBaseHelper;
import com.pyramidsoft.siqasir.model.ProdukTerlarisModels;
import com.pyramidsoft.siqasir.utils.DecimalsFormat;

import java.util.ArrayList;

public class ProdukTerlarisAdapter extends RecyclerView.Adapter<ProdukTerlarisAdapter.ProdukTerlaris> {

    private ArrayList<ProdukTerlarisModels> arrayList;
    private Context context;
    SQLiteDatabase mDatabase;
    DataBaseHelper dbHelper;

    public ProdukTerlarisAdapter(ArrayList<ProdukTerlarisModels> arrayList, Context context, SQLiteDatabase mDatabase) {
        this.arrayList = arrayList;
        this.context = context;
        this.mDatabase = mDatabase;
    }

    @NonNull
    @Override
    public ProdukTerlaris onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(context);
        View view = inflater.inflate(R.layout.item_list_produk_terlaris, parent, false);
        return new ProdukTerlaris(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ProdukTerlaris holder, int position) {
        holder.tvNamaProduk.setText(arrayList.get(position).getProduk());
        holder.tvQty.setText(arrayList.get(position).getQty());
        if (arrayList.get(position).getNominal() == null){
            holder.tvTotal.setText("Rp.0");
        }else {
            holder.tvTotal.setText("Rp" + DecimalsFormat.priceWithoutDecimal(arrayList.get(position).getNominal()));
        }
    }

    @Override
    public int getItemCount() {
        return arrayList.size();
    }

    public class ProdukTerlaris extends RecyclerView.ViewHolder {

        TextView tvNamaProduk;
        TextView tvQty;
        TextView tvTotal;

        public ProdukTerlaris(@NonNull View itemView) {
            super(itemView);
            dbHelper = new DataBaseHelper(context);

            tvNamaProduk = itemView.findViewById(R.id.tvNamaProduk);
            tvQty = itemView.findViewById(R.id.tvQty);
            tvTotal = itemView.findViewById(R.id.tvTotal);
        }
    }
}
