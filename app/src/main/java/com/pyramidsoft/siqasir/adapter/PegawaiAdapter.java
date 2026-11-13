package com.pyramidsoft.siqasir.adapter;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.RecyclerView;

import com.pyramidsoft.siqasir.DetailPegawaiActivity;
import com.pyramidsoft.siqasir.R;
import com.pyramidsoft.siqasir.UpdateDataPegawaiActivity;
import com.pyramidsoft.siqasir.db.DataBaseHelper;
import com.pyramidsoft.siqasir.model.PegawaiModels;
import com.pyramidsoft.siqasir.utils.GlobalToast;

import java.util.ArrayList;

public class PegawaiAdapter extends RecyclerView.Adapter<PegawaiAdapter.PegawaiHolder> {

    private ArrayList<PegawaiModels> arrayList;
    private final Context context;
    private final DataBaseHelper dbHelper;

    public PegawaiAdapter(ArrayList<PegawaiModels> arrayList, Context context) {
        this.arrayList = arrayList;
        this.context = context;
        this.dbHelper = new DataBaseHelper(context);
    }

    @NonNull
    @Override
    public PegawaiHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_list_pegawai, parent, false);
        return new PegawaiHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PegawaiHolder holder, int position) {
        PegawaiModels pegawai = arrayList.get(position);
        holder.tvPegawai.setText(pegawai.getPosisi() + " - " + pegawai.getNamaPegawai());

        holder.relDetailPegawai.setOnClickListener(v -> {
            Intent intent = new Intent(context, DetailPegawaiActivity.class);
            intent.putExtra("kode_pegawai", pegawai.getKodePegawai());
            context.startActivity(intent);
        });

        holder.btvHapus.setOnClickListener(v -> {
            final Dialog dialog = new Dialog(context);
            dialog.setContentView(R.layout.dialog_hapus_data);

            TextView tvBatal = dialog.findViewById(R.id.tvBatal);
            Button btIya = dialog.findViewById(R.id.btIya);
            TextView tvObject = dialog.findViewById(R.id.tvObject);

            tvObject.setText(pegawai.getPosisi() + " - " + pegawai.getNamaPegawai());

            btIya.setOnClickListener(view -> {
                int deleted = dbHelper.deletePegawai(pegawai.getKodePegawai());
                Log.d("DEBUG_DELETE", "deletePegawai returned: " + deleted);

                GlobalToast.ShowToast(context, "Hapus : " + pegawai.getPosisi() + "\n" + pegawai.getNamaPegawai());

                Intent intent = new Intent("listPegawai-state");
                intent.putExtra("refresh", "list");
                LocalBroadcastManager.getInstance(context).sendBroadcast(intent);

                dialog.dismiss();
            });


            tvBatal.setOnClickListener(view -> dialog.dismiss());
            dialog.show();
        });

        holder.btvEdit.setOnClickListener(v -> {
            Intent intent = new Intent(context, UpdateDataPegawaiActivity.class);
            intent.putExtra("kode_pegawai", pegawai.getKodePegawai());
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return arrayList.size();
    }

    public void updateData(ArrayList<PegawaiModels> newData) {
        arrayList.clear();
        arrayList.addAll(newData);
        notifyDataSetChanged();
    }

    static class PegawaiHolder extends RecyclerView.ViewHolder {
        TextView tvPegawai;
        ImageView btvHapus, btvEdit;
        RelativeLayout relDetailPegawai;

        public PegawaiHolder(@NonNull View itemView) {
            super(itemView);
            tvPegawai = itemView.findViewById(R.id.tvPegawai);
            btvHapus = itemView.findViewById(R.id.btv_hapus);
            btvEdit = itemView.findViewById(R.id.btv_edit);
            relDetailPegawai = itemView.findViewById(R.id.rel_detail_pegawai);
        }
    }
}
