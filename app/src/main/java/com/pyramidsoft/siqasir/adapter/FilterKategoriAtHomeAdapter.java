package com.pyramidsoft.siqasir.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.pyramidsoft.siqasir.R;
import com.pyramidsoft.siqasir.ui.home.HomeMainFragment;
import com.pyramidsoft.siqasir.model.KategoriModels;

import java.util.ArrayList;

public class FilterKategoriAtHomeAdapter extends RecyclerView.Adapter<FilterKategoriAtHomeAdapter.FilterHolder> {

    private ArrayList<KategoriModels> arrayList;
    private Context context;

    public FilterKategoriAtHomeAdapter(ArrayList<KategoriModels> arrayList, Context context) {
        this.arrayList = arrayList;
        this.context = context;
    }

    @NonNull
    @Override
    public FilterHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(context);
        View view = inflater.inflate(R.layout.item_filter_kategori, null);
        return new FilterHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FilterHolder holder, int position) {
        if (HomeMainFragment.valuesWhere.contains(arrayList.get(position))) {
            holder.cbKategori.setChecked(true);
        }

        holder.tvKategori.setText(arrayList.get(position).getKategori());
        holder.cbKategori.setOnCheckedChangeListener((compoundButton, b) -> {
            if (b) {
                HomeMainFragment.addWhereValue(arrayList.get(position));
            } else {
                HomeMainFragment.remoceWhereValue(arrayList.get(position));
            }
        });
    }

    @Override
    public int getItemCount() {
        return arrayList.size();
    }


    public class FilterHolder extends RecyclerView.ViewHolder {
        TextView tvKategori;
        CheckBox cbKategori;

        public FilterHolder(View itemView) {
            super(itemView);
            tvKategori = itemView.findViewById(R.id.tvKategori);
            cbKategori = itemView.findViewById(R.id.cbKategori);
        }
    }

}