package com.pyramidsoft.siqasir.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.pyramidsoft.siqasir.R;
import com.pyramidsoft.siqasir.model.DataWilayahModels;

import java.util.ArrayList;
import java.util.List;

public class AutoCompleteTextViewAdapter extends ArrayAdapter<DataWilayahModels> {
    private ArrayList<DataWilayahModels> wilayahListFull;

    public AutoCompleteTextViewAdapter(@NonNull Context context, @NonNull List<DataWilayahModels> dataList) {
        super(context, 0, dataList);
        wilayahListFull = new ArrayList<>(dataList);
    }

    @NonNull
    @Override
    public Filter getFilter() {
        return wilayahFilter;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.auto_complete_textview, parent, false);
        }
        TextView wil = convertView.findViewById(R.id.tvWilayah);
        DataWilayahModels wilayahModels = getItem(position);
        if (wilayahModels != null) {
            wil.setText(wilayahModels.getProvinsi());
        }
        return convertView;
    }

    private Filter wilayahFilter = new Filter() {
        @Override
        protected FilterResults performFiltering(CharSequence constraint) {
            FilterResults results = new FilterResults();
            List<DataWilayahModels> suggestion = new ArrayList<>();

            if (constraint == null || constraint.length() == 0) {
                suggestion.addAll(wilayahListFull);
            } else {
                String filterPatern = constraint.toString().toLowerCase().trim();

                for (DataWilayahModels item : wilayahListFull) {
                    if (item.getProvinsi().toLowerCase().contains(filterPatern))
                        suggestion.add(item);
                }
            }

            results.values = suggestion;
            results.count = suggestion.size();
            return results;
        }

        @Override
        protected void publishResults(CharSequence constraint, FilterResults results) {
            clear();
            addAll((List) results.values);
            notifyDataSetChanged();
        }

        @Override
        public CharSequence convertResultToString(Object resultValue) {
            return ((DataWilayahModels) resultValue).getProvinsi();
        }
    };

}
