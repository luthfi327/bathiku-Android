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
import com.pyramidsoft.siqasir.model.KasKeluarModel;

import java.util.ArrayList;

public class ListKasKeluarAdapter extends RecyclerView.Adapter<ListKasKeluarAdapter.ListHolder> {

    private ArrayList<KasKeluarModel> arrayList;
    private Context context;
    SQLiteDatabase mDatabase;
    DataBaseHelper dbHelper;

    public ListKasKeluarAdapter(ArrayList<KasKeluarModel> arrayList, Context context, SQLiteDatabase mDatabase) {
        this.arrayList = arrayList;
        this.context = context;
        this.mDatabase = mDatabase;
    }

    public ArrayList<KasKeluarModel> getData() {
        return arrayList;
    }

    @NonNull
    @Override
    public ListHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(context);
        View view = inflater.inflate(R.layout.item_kas_keluar, null);

        return new ListHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ListHolder holder, int position) {
        holder.tvKeperluan.setText(arrayList.get(position).getKeperluan());
        holder.tvNominal.setText(arrayList.get(position).getNominal());
    }

    @Override
    public int getItemCount() {
        return arrayList.size();
    }

    public class ListHolder extends RecyclerView.ViewHolder {

        TextView tvKeperluan;
        TextView tvNominal;

        public ListHolder(@NonNull View itemView) {
            super(itemView);
            dbHelper = new DataBaseHelper(context);
            tvKeperluan = itemView.findViewById(R.id.tvKeperluan);
            tvNominal = itemView.findViewById(R.id.tvNominal);
        }
    }
}
