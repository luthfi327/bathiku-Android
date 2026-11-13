package com.pyramidsoft.siqasir.adapter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.pyramidsoft.siqasir.LaporanKasDetailActivity;
import com.pyramidsoft.siqasir.R;
import com.pyramidsoft.siqasir.db.DataBaseHelper;
import com.pyramidsoft.siqasir.model.KasModel;

import java.util.ArrayList;

public class KasAdapter extends RecyclerView.Adapter<KasAdapter.KasHolder> {

    private ArrayList<KasModel> arrayList;
    private Context context;
    SQLiteDatabase mDatabase;
    DataBaseHelper dbHelper;

    public KasAdapter(ArrayList<KasModel> arrayList, Context context, SQLiteDatabase mDatabase) {
        this.arrayList = arrayList;
        this.context = context;
        this.mDatabase = mDatabase;
    }

    @NonNull
    @Override
    public KasHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(context);
        View view = inflater.inflate(R.layout.item_list_kas, null);

        return new KasHolder(view);
    }

    @SuppressLint("RecyclerView")
    @Override
    public void onBindViewHolder(@NonNull KasHolder holder, int position) {
        holder.tvTanggal.setText(arrayList.get(position).getTgl_buka() + " - " + arrayList.get(position).getTgl_tutup());
        holder.tvTanggal.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(context, LaporanKasDetailActivity.class);
                intent.putExtra("kode_rekapkas", arrayList.get(position).getKode_rekapkas());
                context.startActivity(intent);
            }
        });
    }

    @Override
    public int getItemCount() {
        return arrayList.size();
    }

    public class KasHolder extends RecyclerView.ViewHolder {
        TextView tvTanggal;

        public KasHolder (@NonNull View itemView) {
            super(itemView);
            dbHelper = new DataBaseHelper(context);
            tvTanggal = itemView.findViewById(R.id.tvTanggal);
        }
    }

}
