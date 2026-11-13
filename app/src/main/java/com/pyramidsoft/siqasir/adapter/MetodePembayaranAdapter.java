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
import com.pyramidsoft.siqasir.model.MetodePembayaranModels;
import com.pyramidsoft.siqasir.utils.DecimalsFormat;

import java.util.ArrayList;

public class MetodePembayaranAdapter extends RecyclerView.Adapter<MetodePembayaranAdapter.MetodeHolder> {

    private ArrayList<MetodePembayaranModels> arrayList;
    private Context context;
    SQLiteDatabase mDatabase;
    DataBaseHelper dbHelper;

    public MetodePembayaranAdapter (ArrayList<MetodePembayaranModels> arrayList, Context context, SQLiteDatabase mDatabase) {
        this.arrayList = arrayList;
        this.context = context;
        this.mDatabase = mDatabase;
    }

    @NonNull
    @Override
    public MetodeHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(context);
        View view = inflater.inflate(R.layout.item_list_metode_pembayaran, null);

        return new MetodeHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MetodeHolder holder, int position) {
        holder.tvMetodePembayaran.setText(arrayList.get(position).getMetode());
        holder.tvTotal.setText("Rp"+ DecimalsFormat.priceWithoutDecimal(arrayList.get(position).getNominal()));
    }

    @Override
    public int getItemCount() {
        return arrayList.size();
    }

    public class MetodeHolder extends RecyclerView.ViewHolder {

        TextView tvMetodePembayaran, tvTotal;

        public MetodeHolder(@NonNull View itemView) {
            super(itemView);
            dbHelper = new DataBaseHelper(context);

            tvMetodePembayaran = itemView.findViewById(R.id.tvMetodePembayaran);
            tvTotal = itemView.findViewById(R.id.tvTotal);
        }
    }
}
