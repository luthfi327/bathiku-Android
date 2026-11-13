package com.pyramidsoft.siqasir.adapter;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.pyramidsoft.siqasir.ListOutletActivity;
import com.pyramidsoft.siqasir.R;
import com.pyramidsoft.siqasir.TambahOutletActivity;
import com.pyramidsoft.siqasir.db.DataBaseHelper;
import com.pyramidsoft.siqasir.model.Outlet;

import java.util.List;

public class OutletAdapter extends RecyclerView.Adapter<OutletAdapter.ViewHolder> {

    private List<Outlet> outletList;

    public OutletAdapter(List<Outlet> outletList) {
        this.outletList = outletList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_outlet, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Outlet outlet = outletList.get(position);
        Context context = holder.itemView.getContext();

        // Nama + label Utama untuk data pertama
        if (position == 0) {
            holder.tvNama.setText(outlet.getNamaToko() + " (Utama)");
        } else {
            holder.tvNama.setText(outlet.getNamaToko());
        }

        holder.tvAlamat.setText(outlet.getKota() + ", " + outlet.getProvinsi());
        holder.tvEmail.setText(outlet.getEmail());
        holder.tvTelp.setText(outlet.getNoTelepon());

        // Logo
        if (outlet.getLogo() != null && outlet.getLogo().length > 0) {
            holder.ivLogo.setImageBitmap(BitmapFactory.decodeByteArray(outlet.getLogo(), 0, outlet.getLogo().length));
        } else {
            holder.ivLogo.setImageResource(R.drawable.outlet_kosong);
        }

        if (position == 0) {
            // Toko utama → sembunyikan tombol edit & hapus
            holder.ivEdit.setVisibility(View.GONE);
            holder.ivHapus.setVisibility(View.GONE);
        } else {
            // Tombol edit
            holder.ivEdit.setVisibility(View.VISIBLE);
            holder.ivEdit.setOnClickListener(v -> {
                Intent intent = new Intent(context, TambahOutletActivity.class);
                intent.putExtra("edit_outlet_id", outlet.getIdToko());
                context.startActivity(intent);
            });

            // Tombol hapus
            holder.ivHapus.setVisibility(View.VISIBLE);
            holder.ivHapus.setOnClickListener(v -> {
                new AlertDialog.Builder(context)
                        .setTitle("Hapus Outlet")
                        .setMessage("Apakah Anda yakin ingin menghapus outlet ini?")
                        .setPositiveButton("Hapus", (dialog, which) -> {
                            DataBaseHelper dbHelper = new DataBaseHelper(context);
                            dbHelper.deleteOutlet(outlet.getIdToko());
                            Toast.makeText(context, "Outlet dihapus", Toast.LENGTH_SHORT).show();

                            if (context instanceof ListOutletActivity) {
                                ((ListOutletActivity) context).recreate(); // refresh list
                            }
                        })
                        .setNegativeButton("Batal", null)
                        .show();
            });
        }
    }

    @Override
    public int getItemCount() {
        return outletList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivLogo, ivEdit, ivHapus;
        TextView tvNama, tvAlamat, tvEmail, tvTelp;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivLogo = itemView.findViewById(R.id.ivLogoOutlet);
            ivEdit = itemView.findViewById(R.id.ivEditOutlet);
            ivHapus = itemView.findViewById(R.id.ivHapusOutlet);
            tvNama = itemView.findViewById(R.id.tvNamaOutlet);
            tvAlamat = itemView.findViewById(R.id.tvAlamatOutlet);
            tvEmail = itemView.findViewById(R.id.tvEmailOutlet);
            tvTelp = itemView.findViewById(R.id.tvTelpOutlet);
        }
    }
}
