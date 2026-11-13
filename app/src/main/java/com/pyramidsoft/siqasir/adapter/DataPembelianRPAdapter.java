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
import com.pyramidsoft.siqasir.model.ListPembelianModels;
import com.pyramidsoft.siqasir.utils.DecimalsFormat;

import java.util.ArrayList;

public class DataPembelianRPAdapter extends RecyclerView.Adapter<DataPembelianRPAdapter.ListHolder> {


    public static ArrayList<ListPembelianModels> arrayList = new ArrayList<>();
    private Context context;
    SQLiteDatabase mDatabase;
    DataBaseHelper dbHelper;

    public DataPembelianRPAdapter(ArrayList<ListPembelianModels> arrayList, Context context, SQLiteDatabase mDatabase) {
        this.arrayList = arrayList;
        this.context = context;
        this.mDatabase = mDatabase;
    }

    @NonNull
    @Override
    public ListHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(context);
        View view = inflater.inflate(R.layout.item_list_pesanan_pembayaran, null);
        return new ListHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ListHolder holder, int position) {
        int hrg = Integer.parseInt(arrayList.get(position).getHargaproduk());
        int qty = Integer.parseInt(arrayList.get(position).getQty());
        int total_harga = hrg * qty;

        holder.tvHeaderTitle.setText(arrayList.get(position).getQty() + " X " + arrayList.get(position).getNamaproduk());
        holder.tvHargaItem.setText("Rp. " + DecimalsFormat.priceWithoutDecimal(String.valueOf(total_harga)));
    }

    @Override
    public int getItemCount() {
        return arrayList.size();
    }

    public class ListHolder extends RecyclerView.ViewHolder {
        TextView tvHeaderTitle, tvHargaItem;
        public ListHolder(@NonNull View itemView) {
            super(itemView);
            dbHelper = new DataBaseHelper(context);

            tvHeaderTitle = itemView.findViewById(R.id.tvheader_title);
            tvHargaItem = itemView.findViewById(R.id.tvHargaItem);
        }
    }
}
