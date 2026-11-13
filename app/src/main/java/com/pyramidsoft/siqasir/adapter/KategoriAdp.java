package com.pyramidsoft.siqasir.adapter;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.RecyclerView;

import com.pyramidsoft.siqasir.R;
import com.pyramidsoft.siqasir.db.DataBaseHelper;
import com.pyramidsoft.siqasir.model.KategoriModels;
import com.pyramidsoft.siqasir.utils.AndLog;
import com.pyramidsoft.siqasir.utils.GlobalToast;
import com.pyramidsoft.siqasir.utils.SessionManager;

import java.util.ArrayList;

public class KategoriAdp extends RecyclerView.Adapter<KategoriAdp.KatHolder> {

    private ArrayList<KategoriModels> arrayList;
    private Context context;
    SQLiteDatabase mDatabase;
    DataBaseHelper dbHelper;
    SessionManager sessionManager;

    public KategoriAdp(ArrayList<KategoriModels> arrayList, Context context, SQLiteDatabase mDatabase) {
        this.arrayList = arrayList;
        this.context = context;
        this.mDatabase = mDatabase;

    }

    @NonNull
    @Override
    public KatHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(context);
        View view = inflater.inflate(R.layout.item_list_kategori, null);
        return new KatHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull KatHolder holder, @SuppressLint("RecyclerView") int position) {
        holder.tvKategori.setText(arrayList.get(position).getKategori());
        holder.btvHapus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DialogHapusConfirm(arrayList.get(position).getIdKategori(), arrayList.get(position).getKategori());
            }
        });

        holder.btvEdit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DialogForm(arrayList.get(position).getIdKategori(), arrayList.get(position).getKategori());
            }
        });

    }

    private void DialogHapusConfirm(String id_kategori, String kategori) {
        final Dialog dialog = new Dialog(context);
        dialog.setContentView(R.layout.dialog_delete_confirm);
        TextView tvMessage = (TextView) dialog.findViewById(R.id.tvMessages);
        tvMessage.setText("Anda Yakin Menghapus Kategori " + "(" + kategori + ") ?");
        TextView tvBatal = (TextView) dialog.findViewById(R.id.tvBatal);
        Button btHapus = (Button) dialog.findViewById(R.id.btHapus);

        btHapus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                dbHelper.deleteData(id_kategori);

                GlobalToast.ShowToast(context, "Kategori " + kategori + " Terhapus!");
                Intent intent = new Intent("listMasterKategori-state");
                intent.putExtra("refresh", "list");

                LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
                dialog.dismiss();
            }
        });
        tvBatal.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
                GlobalToast.ShowToast(context, "Batal");
            }
        });

        dialog.show();
    }

    private void DialogForm(String idKategori, String Kategori) {
        final Dialog dialog = new Dialog(context);
        dialog.setContentView(R.layout.dialog_kategori_add);

        TextView tvBatal = (TextView) dialog.findViewById(R.id.tvBatal);
        EditText etNamaKategori = (EditText) dialog.findViewById(R.id.etNamaKategori);
        Button btSimpan = (Button) dialog.findViewById(R.id.btSimpan);

        etNamaKategori.setText(Kategori);

        tvBatal.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });

        btSimpan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ContentValues values = new ContentValues();
                values.put(DataBaseHelper.kategori, etNamaKategori.getText().toString().trim());
                dbHelper.updateData(values, idKategori);

                AndLog.ShowLog("id,kat:", String.valueOf(values));
                GlobalToast.ShowToast(context, "Kategori Berhasil Di-update!");
                dialog.dismiss();
                refreshRecyclerview();

            }
        });
        dialog.show();
    }

    void refreshRecyclerview() {
        String kodePegawai = sessionManager.getKodeUsers();
        String idToko = dbHelper.getIdTokoByKodePegawai(kodePegawai);
        Cursor cursorproduct = dbHelper.listKategori(idToko);

        ArrayList<KategoriModels> data = new ArrayList<>();

        if (cursorproduct.moveToFirst()) {
            do {
                data.add(new KategoriModels(
                        cursorproduct.getString(3), // kategori
                        cursorproduct.getString(0), // id_kategori
                        cursorproduct.getString(1)  // id_toko
                ));
            } while (cursorproduct.moveToNext());
        }
        if (data.isEmpty()) {
            GlobalToast.ShowToast(context, "No items Found in database");
        }
        cursorproduct.close();
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return arrayList.size();
    }

    public class KatHolder extends RecyclerView.ViewHolder {
        TextView tvKategori;
        ImageView btvHapus, btvEdit;

        public KatHolder(@NonNull View itemView) {
            super(itemView);
            dbHelper = new DataBaseHelper(context);

            tvKategori = itemView.findViewById(R.id.tvKategori);
            btvHapus = itemView.findViewById(R.id.btv_hapus);
            btvEdit = itemView.findViewById(R.id.btv_edit);
        }
    }
}
