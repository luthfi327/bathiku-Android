package com.pyramidsoft.siqasir.adapter;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.RecyclerView;

import com.pyramidsoft.siqasir.ProdukTokoActivity;
import com.pyramidsoft.siqasir.R;
import com.pyramidsoft.siqasir.db.DataBaseHelper;
import com.pyramidsoft.siqasir.model.MasterProdukModels;
import com.pyramidsoft.siqasir.utils.DecimalsFormat;

import java.util.ArrayList;

public class ListMasterProdukAdapter extends RecyclerView.Adapter<ListMasterProdukAdapter.ListMasterPrd> {

    private ArrayList<MasterProdukModels> arrayList;
    private Context context;
    SQLiteDatabase mDatabase;
    DataBaseHelper dbHelper;
    DecimalsFormat decimalsFormat;

    public ListMasterProdukAdapter(ArrayList<MasterProdukModels> arrayList, Context context, SQLiteDatabase mDatabase) {
        this.arrayList = arrayList;
        this.context = context;
        this.mDatabase = mDatabase;
    }

    public void filter(String text, ArrayList<MasterProdukModels> arrayListCopy) {
        arrayList.clear();
        if (text.isEmpty()) {
            arrayList.addAll(arrayListCopy);
        } else {
            text = text.toLowerCase();
            for (MasterProdukModels item : arrayListCopy) {
                if (item.getNmProduk().toLowerCase().contains(text)) {
                    arrayList.add(item);
                }
            }
        }
    }

    @NonNull
    @Override
    public ListMasterPrd onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(context);
        View view = inflater.inflate(R.layout.item_list_produk, null);
        return new ListMasterPrd(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ListMasterPrd holder, @SuppressLint("RecyclerView") int position) {
        byte[] gbrProduk = arrayList.get(position).getGbrProduk();
        Bitmap bitmap = BitmapFactory.decodeByteArray(gbrProduk, 0, gbrProduk.length);
        holder.ivProduk.setImageBitmap(bitmap);

        holder.tvKodeProduk.setText(arrayList.get(position).getKdProduk());
        holder.tvNamaProduk.setText(arrayList.get(position).getNmProduk());
        holder.tvKategori.setText(arrayList.get(position).getKat());
        holder.tvHargaBeli.setText("Rp." + decimalsFormat.priceWithoutDecimal(arrayList.get(position).getHrgBeli()));
        holder.tvHargaJual.setText("Rp." + decimalsFormat.priceWithoutDecimal(arrayList.get(position).getHrgJual()));
        if (arrayList.get(position).getHrgDiskon().equals("0")) {
            holder.rel_labeldiskon.setVisibility(View.GONE);
        } else {
            holder.rel_labeldiskon.setVisibility(View.VISIBLE);
            holder.tvHargaDiskon.setText("Rp." + DecimalsFormat.priceWithoutDecimal(arrayList.get(position).getHrgDiskon()));
        }

        Integer stok = arrayList.get(position).getStok();
        if (stok != null) {
            holder.rel_labelstok.setVisibility(View.VISIBLE);
            holder.tvStok.setText(String.valueOf(stok)); // hanya angka stok
        } else {
            holder.rel_labelstok.setVisibility(View.GONE); // sembunyikan seluruh layout
        }

        if (arrayList.get(position).getFavorit() == 0) {
            holder.btv_favorit.setBackgroundResource(R.drawable.ic_favorit_false);
        } else {
            holder.btv_favorit.setBackgroundResource(R.drawable.ic_favorit_true);
        }

        holder.btv_favorit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ContentValues values = new ContentValues();
                String titledialog;
                if (arrayList.get(position).getFavorit() == 0) {
                    titledialog = "Anda Yakin Ingin Menjadikan " + arrayList.get(position).getNmProduk() + " Produk Favorit ?";
                    values.put("favorit", 1);
                } else {
                    titledialog = "Anda Yakin Ingin Menghapus " + arrayList.get(position).getNmProduk() + " Dari Produk Favorit ?";
                    values.put("favorit", 0);
                }
                androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(context);
                builder.setTitle(titledialog);

                builder.setPositiveButton("Iya", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dbHelper.updateProduk(values, arrayList.get(position).getKdProduk());

                        Intent intent = new Intent("listMasterProduk-state");
                        intent.putExtra("refresh", "list");
                        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
                    }
                });

                builder.setNegativeButton("Tidak", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.dismiss();
                    }
                });
                androidx.appcompat.app.AlertDialog dialog = builder.create();
                dialog.show();
            }
        });

        holder.btvHapus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(context);
                builder.setTitle("Anda Yakin Menghapus Produk " + arrayList.get(position).getNmProduk() + " ?");

                builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dbHelper.deleteProduk(arrayList.get(position).getKdProduk());
                        Toast.makeText(context, "Hapus" + arrayList.get(position).getNmProduk()
                                + ": " + arrayList.get(position).getKdProduk() + " sukses!", Toast.LENGTH_SHORT).show();

                        arrayList.remove(position);

                        notifyDataSetChanged();

                        Intent intent = new Intent("listMasterProduk-state");
                        intent.putExtra("refresh", "list");
                        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
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
                Intent intent = new Intent(context, ProdukTokoActivity.class);
                intent.putExtra("kode_produk", arrayList.get(position).getKdProduk());
                context.startActivity(intent);
            }
        });
    }

    @Override
    public int getItemCount() {
        return arrayList.size();
    }

    public class ListMasterPrd extends RecyclerView.ViewHolder {

        ImageView ivProduk, btvHapus, btvEdit, btv_favorit;
        RelativeLayout rel_labeldiskon, rel_labelstok;
        TextView tvNamaProduk, tvKategori, tvKodeProduk, tvHargaBeli, tvHargaJual, tvHargaDiskon, tvStok;

        public ListMasterPrd(@NonNull View itemView) {
            super(itemView);
            dbHelper = new DataBaseHelper(context);

            ivProduk = itemView.findViewById(R.id.ivProduk);
            btvHapus = itemView.findViewById(R.id.btv_hapus);
            btvEdit = itemView.findViewById(R.id.btv_edit);
            btv_favorit = itemView.findViewById(R.id.btv_favorit);
            rel_labeldiskon = itemView.findViewById(R.id.rel_labeldiskon);
            tvNamaProduk = itemView.findViewById(R.id.tvNamaProduk);
            tvKategori = itemView.findViewById(R.id.tvKategori);
            tvKodeProduk = itemView.findViewById(R.id.tvKodeProduk);
            tvHargaBeli = itemView.findViewById(R.id.tvHargaBeli);
            tvHargaJual = itemView.findViewById(R.id.tvHargaJual);
            tvHargaDiskon = itemView.findViewById(R.id.tvHargaDiskon);
            tvStok = itemView.findViewById(R.id.tvStok);
            rel_labelstok = itemView.findViewById(R.id.rel_labelstok);
        }
    }
}
