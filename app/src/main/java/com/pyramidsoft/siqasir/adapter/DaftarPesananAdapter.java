package com.pyramidsoft.siqasir.adapter;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.RecyclerView;

import com.pyramidsoft.siqasir.HomeMainActivity;
import com.pyramidsoft.siqasir.R;
import com.pyramidsoft.siqasir.model.DaftarPesananModel;
import com.pyramidsoft.siqasir.model.ProdukPesananModel;
import com.pyramidsoft.siqasir.ui.daftarpesanan.DaftarPesananFragment;
import com.pyramidsoft.siqasir.utils.DataPesanan;
import com.pyramidsoft.siqasir.utils.DecimalsFormat;

import java.util.ArrayList;
import java.util.List;

public class DaftarPesananAdapter extends RecyclerView.Adapter<DaftarPesananAdapter.DaftarPesananHolder> {

    private ArrayList<DaftarPesananModel> arrayList;
    private Context context;
    private boolean delete = false;
    private boolean checkall = false;

    public DaftarPesananAdapter(ArrayList<DaftarPesananModel> arrayList, Context context) {
        this.arrayList = arrayList;
        this.context = context;
    }

    public void filter(String text, ArrayList<DaftarPesananModel> arrayListCopy) {
        arrayList.clear();
        if (text.isEmpty()) {
            arrayList.addAll(arrayListCopy);
        } else {
            text = text.toLowerCase();
            for (DaftarPesananModel item : arrayListCopy) {
                if (item.getNama_pembeli().toLowerCase().contains(text)) {
                    arrayList.add(item);
                }
            }
        }
    }

    public void setDelete(Boolean delete) {
        this.delete = delete;
    }

    public boolean getDelete() {
        return this.delete;
    }

    public boolean getCheckall() {
        return checkall;
    }

    public void setCheckall(boolean checkall) {
        this.checkall = checkall;
    }

    public static boolean isExist(DaftarPesananModel model) {
        for (int i = 0; i < DaftarPesananFragment.listDeletePesanan.size(); i++) {
            if (DaftarPesananFragment.listDeletePesanan.get(i).getInvoice().equals(model.getInvoice())) {
                return true;
            }
        }
        return false;
    }

    @NonNull
    @Override
    public DaftarPesananHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(context);
        View view = inflater.inflate(R.layout.item_list_daftar_pesanan, null);
        return new DaftarPesananHolder(view);
    }

    @SuppressLint("RecyclerView")
    @Override
    public void onBindViewHolder(@NonNull DaftarPesananHolder holder, int position) {
        // 1. Mode delete = tampilkan checkbox
        if (getDelete()) {
            holder.CbOption.setVisibility(View.VISIBLE);
        } else {
            holder.CbOption.setVisibility(View.GONE);
            holder.CbOption.setChecked(false);
            DaftarPesananFragment.listDeletePesanan.remove(arrayList.get(position));
        }

        // 2. Kalau checkall true → centang semua
        if (getCheckall()) {
            holder.CbOption.setChecked(true);
            if (!isExist(arrayList.get(position))) {
                DaftarPesananFragment.listDeletePesanan.add(arrayList.get(position));
            }
        } else {
            holder.CbOption.setChecked(isExist(arrayList.get(position)));
        }

        // 3. Listener checkbox
        holder.CbOption.setOnCheckedChangeListener((compoundButton, b) -> {
            if (b) {
                if (!isExist(arrayList.get(position))) {
                    DaftarPesananFragment.listDeletePesanan.add(arrayList.get(position));
                }
            } else {
                DaftarPesananFragment.listDeletePesanan.remove(arrayList.get(position));
            }

            Intent intent = new Intent("listPesanan-state");
            intent.putExtra("refresh", "addpesanan");
            LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
        });

        // 4. Set data
        holder.tvLayanan.setText(arrayList.get(position).getJenis_orderan());
        holder.tvNamaCust.setText(arrayList.get(position).getNama_pembeli());
        holder.tvQty.setText("Jumlah : " + arrayList.get(position).getJml());
        holder.tvTotalHrg.setText("Total : Rp. " +
                DecimalsFormat.priceWithoutDecimal(arrayList.get(position).getTotalharga()));

        // 5. Klik item
        holder.layout.setOnClickListener(v -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setMessage("Anda Yakin Ingin Menyelesaikan Pesanan "
                            + arrayList.get(position).getNama_pembeli() + "?")
                    .setPositiveButton("Iya", (dialog, id) -> {
                        String invoice = arrayList.get(position).getInvoice();
                        DataPesanan.DataPelanggan pelanggan = DaftarPesananFragment.getPesananEdit(invoice);
                        List<ProdukPesananModel> detailPesanan = DaftarPesananFragment.getDetailPesanan(invoice);
                        DataPesanan.setDefaultDataPesanan();
                        DataPesanan.setInvoiceEdit(invoice);
                        DataPesanan.setDataPelanggan(pelanggan);
                        DataPesanan.setArrayPesanan(detailPesanan);

                        Intent intent = new Intent("dataPesanan-state");
                        intent.putExtra("refresh", "pesanan");
                        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);

                        dialog.cancel();
                        ((HomeMainActivity) context).getSupportFragmentManager()
                                .popBackStack(null, androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE);
                    })
                    .setNegativeButton("Batal", (dialog, id) -> dialog.cancel())
                    .show();
        });
    }

    @Override
    public int getItemCount() {
        return arrayList.size();
    }

    public class DaftarPesananHolder extends RecyclerView.ViewHolder {

        TextView tvLayanan, tvNamaCust, tvQty, tvTotalHrg;
        LinearLayout layout;
        CheckBox CbOption;

        public DaftarPesananHolder(@NonNull View itemView) {
            super(itemView);

            tvLayanan = itemView.findViewById(R.id.tvLayanan);
            tvNamaCust = itemView.findViewById(R.id.tvNamaCust);
            tvQty = itemView.findViewById(R.id.tvQty);
            tvTotalHrg = itemView.findViewById(R.id.tvTotalHrg);
            layout = itemView.findViewById(R.id.layout);
            CbOption = itemView.findViewById(R.id.CbOption);

        }
    }
}
