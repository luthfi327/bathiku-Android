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
import com.pyramidsoft.siqasir.model.JenisPembayaranModels;
import com.pyramidsoft.siqasir.utils.DecimalsFormat;

import java.util.ArrayList;

public class JenisPembayaranAdapter extends RecyclerView.Adapter<JenisPembayaranAdapter.JenisPembayaranHolder> {

    private ArrayList<JenisPembayaranModels> arrayList;
    private Context context;
    SQLiteDatabase mDatabase;
    DataBaseHelper dbHelper;

    public JenisPembayaranAdapter(ArrayList<JenisPembayaranModels> arrayList, Context context, SQLiteDatabase mDatabase) {
        this.arrayList = arrayList;
        this.context = context;
        this.mDatabase = mDatabase;
    }

    @NonNull
    @Override
    public JenisPembayaranHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(context);
        View view = inflater.inflate(R.layout.item_jenis_pembayaran, null);

        return new JenisPembayaranHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull JenisPembayaranHolder holder, int position) {
        holder.tvJenisPembayaran.setText(arrayList.get(position).getJenis_pembayaran());
        holder.tvNominal.setText("Rp"+ DecimalsFormat.priceWithoutDecimal(String.valueOf(arrayList.get(position).getTotal())));
    }

    @Override
    public int getItemCount() {
        return arrayList.size();
    }

    public class JenisPembayaranHolder extends RecyclerView.ViewHolder {
        TextView tvJenisPembayaran, tvNominal;
        public JenisPembayaranHolder(@NonNull View itemView) {
            super(itemView);
            tvJenisPembayaran = itemView.findViewById(R.id.tvJenis_pembayaran);
            tvNominal = itemView.findViewById(R.id.tvNominal);
            dbHelper = new DataBaseHelper(context);
        }
    }
}
