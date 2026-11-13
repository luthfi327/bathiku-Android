package com.pyramidsoft.siqasir.adapter;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.pyramidsoft.siqasir.R;
import com.pyramidsoft.siqasir.db.DataBaseHelper;
import com.pyramidsoft.siqasir.model.JenisKedatanganModels;
import com.pyramidsoft.siqasir.utils.AndLog;
import com.pyramidsoft.siqasir.utils.GlobalToast;
import com.pyramidsoft.siqasir.utils.SessionManager;

import java.util.ArrayList;

public class JenisKedatanganAdapter extends RecyclerView.Adapter<JenisKedatanganAdapter.JeniskedatanganHolder> {

    private ArrayList<JenisKedatanganModels> arrayList;
    private Context context;
    SQLiteDatabase mDatabase;
    DataBaseHelper dbHelper;
    SessionManager sessionManager;

    public JenisKedatanganAdapter(ArrayList<JenisKedatanganModels> arrayList, Context context, SQLiteDatabase mDatabase) {
        this.arrayList = arrayList;
        this.context = context;
        this.mDatabase = mDatabase;

        dbHelper = new DataBaseHelper(context);
        sessionManager = new SessionManager(context);
    }

    @NonNull
    @Override
    public JeniskedatanganHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(context);
        View view = inflater.inflate(R.layout.item_jenis_kedatangan, null);

        return new JeniskedatanganHolder(view);
    }

    @SuppressLint("RecyclerView")
    @Override
    public void onBindViewHolder(@NonNull JeniskedatanganHolder holder, int position) {
        holder.tvjenisdatang.setText(arrayList.get(position).getJenis_kedatangan());
        holder.btvHapus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder builder = new AlertDialog.Builder(context);
                builder.setTitle("Anda Yakin Menghapus " + arrayList.get(position).getJenis_kedatangan() + " ?");
                builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {

                        dbHelper.deleteJenisKedatangan(arrayList.get(position).getId_jenis_kedatangan());
                        Toast.makeText(context, "Deleted " + arrayList.get(position).getJenis_kedatangan() + " succesfully!", Toast.LENGTH_SHORT).show();
                        refreshRecyclerview();
                        arrayList.remove(position);
                        notifyDataSetChanged();
                    }
                });

                builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                    }
                });
                AlertDialog dialog = builder.create();
                dialog.show();
            }
        });

        holder.btvEdit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DialogForm(arrayList.get(position).getId_jenis_kedatangan(), arrayList.get(position).getJenis_kedatangan());
                refreshRecyclerview();
            }
        });
    }

    private void DialogForm(String id_jenis_kedatangan, String JKedatangan) {
        final Dialog dialog = new Dialog(context);
        dialog.setContentView(R.layout.dialog_jenis_kedatangan_add);

        TextView tvBatal = dialog.findViewById(R.id.tvBatal);
        EditText etJenisKedatangan = dialog.findViewById(R.id.etJenisKedatangan);
        Button btSimpan = dialog.findViewById(R.id.btSimpan);

        etJenisKedatangan.setText(JKedatangan);

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
                values.put(DataBaseHelper.jenis_kedatangan, etJenisKedatangan.getText().toString().trim());
                dbHelper.updateJenisKedatangan(values, id_jenis_kedatangan);

                AndLog.ShowLog("id,jKedatangan:", String.valueOf(values));
                GlobalToast.ShowToast(context, "Jenis Kedatangan Berhasil Di-update!");
                dialog.dismiss();

                for (int i = 0; i < arrayList.size(); i++) {
                    JenisKedatanganModels item = arrayList.get(i);
                    if (item.getId_jenis_kedatangan().equals(id_jenis_kedatangan)) {
                        item.setJenis_kedatangan(etJenisKedatangan.getText().toString().trim());
                        notifyItemChanged(i);
                        break;
                    }
                }
            }
        });
        dialog.show();
    }

    void refreshRecyclerview() {
        String kodePegawai = sessionManager.getKodeUsers();
        String idToko = dbHelper.getIdTokoByKodePegawai(kodePegawai);
        Cursor cursorproduct = dbHelper.listJenisKedatangan(idToko);

        ArrayList<JenisKedatanganModels> data = new ArrayList<>();

        if (cursorproduct.moveToFirst()) {
            do {
                data.add(new JenisKedatanganModels(
                        cursorproduct.getString(0), // id_jenis_kedatangan
                        cursorproduct.getString(1), // id_toko
                        cursorproduct.getString(2)  // jenis_kedatangan
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

    public class JeniskedatanganHolder extends RecyclerView.ViewHolder {

        TextView tvjenisdatang;
        ImageView btvHapus, btvEdit;

        public JeniskedatanganHolder(@NonNull View itemView) {
            super(itemView);
            dbHelper = new DataBaseHelper(context);
            tvjenisdatang = itemView.findViewById(R.id.tvjenisdatang);
            btvHapus = itemView.findViewById(R.id.btv_hapus);
            btvEdit = itemView.findViewById(R.id.btv_edit);
        }
    }
}