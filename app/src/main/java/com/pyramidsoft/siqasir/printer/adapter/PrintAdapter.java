package com.pyramidsoft.siqasir.printer.adapter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.pyramidsoft.siqasir.R;
import com.pyramidsoft.siqasir.db.DataBaseHelper;
import com.pyramidsoft.siqasir.printer.PengaturanDetailOutputPrinter;
import com.pyramidsoft.siqasir.printer.model.PrintModel;

import java.util.ArrayList;

public class PrintAdapter extends RecyclerView.Adapter<PrintAdapter.holder> {

    Context context;
    ArrayList<PrintModel> arrayList = new ArrayList<>();
    PrintAdapterCallback mAdapterCallback;
    SQLiteDatabase mDatabase;
    DataBaseHelper dbHelper;

    public PrintAdapter(Context context, ArrayList<PrintModel> arrayList, PrintAdapterCallback mAdapterCallback, SQLiteDatabase mDatabase) {
        this.context = context;
        this.arrayList = arrayList;
        this.mAdapterCallback = mAdapterCallback;
        this.mDatabase = mDatabase;
    }

    @NonNull
    @Override
    public holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View view = inflater.inflate(R.layout.item_list_printer, parent, false);
        return new holder(view);
    }

    @SuppressLint("RecyclerView")
    @Override
    public void onBindViewHolder(@NonNull holder holder, int position) {
        holder.namaprinter.setText(arrayList.get(position).getNama_printer());
        holder.alamatprinter.setText(arrayList.get(position).getAlamat_printer());
        holder.action_settings_detail.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(context, PengaturanDetailOutputPrinter.class);
                intent.putExtra("id", arrayList.get(position).getId());
                context.startActivity(intent);
            }
        });
    }

    @Override
    public int getItemCount() {
        return arrayList.size();
    }

    public class holder extends RecyclerView.ViewHolder {

        ImageView action_settings_detail;
        TextView namaprinter, alamatprinter;
        CardView cardv;
        Button testprint;

        public holder(@NonNull View itemView) {
            super(itemView);
            dbHelper = new DataBaseHelper(context);
            action_settings_detail = itemView.findViewById(R.id.action_settings_detail);
            namaprinter = itemView.findViewById(R.id.namaprinter);
            alamatprinter = itemView.findViewById(R.id.alamatprinter);
            cardv = itemView.findViewById(R.id.cardv);
            testprint = itemView.findViewById(R.id.btTestPrint);

            testprint.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    mAdapterCallback.onButtontesAdapterClicked(getAdapterPosition());
                }
            });

            cardv.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    mAdapterCallback.Buttonsettingimg(getAdapterPosition());
                }
            });
        }
    }

    public interface PrintAdapterCallback {
        void onButtontesAdapterClicked(int position);
        void Buttonsettingimg(int pos);
    }

}
