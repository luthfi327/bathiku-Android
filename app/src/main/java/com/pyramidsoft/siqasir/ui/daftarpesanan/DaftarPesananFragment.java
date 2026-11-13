package com.pyramidsoft.siqasir.ui.daftarpesanan;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.pyramidsoft.siqasir.R;
import com.pyramidsoft.siqasir.adapter.DaftarPesananAdapter;
import com.pyramidsoft.siqasir.adapter.FilterDaftarPesananAdapter;
import com.pyramidsoft.siqasir.databinding.FragmentDaftarPesananBinding;
import com.pyramidsoft.siqasir.db.DataBaseHelper;
import com.pyramidsoft.siqasir.model.DaftarPesananModel;
import com.pyramidsoft.siqasir.model.JenisKedatanganModels;
import com.pyramidsoft.siqasir.model.ProdukPesananModel;
import com.pyramidsoft.siqasir.utils.DataPesanan;
import com.pyramidsoft.siqasir.utils.GlobalToast;
import com.pyramidsoft.siqasir.utils.SessionManager;

import java.util.ArrayList;
import java.util.List;

public class DaftarPesananFragment extends Fragment {

    public FragmentDaftarPesananBinding binding;
    Activity activity;
    TextView tvCount;
    FrameLayout framePilihSemua, frameBatal;
    ImageButton imageButton;
    SessionManager sessionManager;

    public static DataBaseHelper dbHelper;
    public static ArrayList<DaftarPesananModel> listDeletePesanan = new ArrayList<>();
    DaftarPesananAdapter adapter;
    public static ArrayList<JenisKedatanganModels> valuesWhere = new ArrayList<>();
    FilterDaftarPesananAdapter filterAdapter;

    ArrayList<DaftarPesananModel> arrayListCopy = new ArrayList<>();
    public BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String refresh = intent.getStringExtra("refresh");
            switch (refresh) {
                case "addpesanan":
                    checkstatedelete();
                    break;
                case "finditem":
                    String find = intent.getStringExtra("find");
                    Log.i("find", "onReceive: " + find);
                    adapter.filter(find,arrayListCopy);
                    adapter.notifyDataSetChanged();
                    break;
            }

        }
    };

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentDaftarPesananBinding.inflate(inflater, container, false);
        View root = binding.getRoot();
        activity = getActivity();
        dbHelper = new DataBaseHelper(activity);
        setHasOptionsMenu(true);

        sessionManager = new SessionManager(getActivity());

        filterAdapter = new FilterDaftarPesananAdapter(getDataJenisKedatangan(), activity);

        binding.rvDaftarPesanan.setHasFixedSize(true);
        GridLayoutManager layoutManager = new GridLayoutManager(getActivity(), 1, GridLayoutManager.VERTICAL, false);
        binding.rvDaftarPesanan.setLayoutManager(layoutManager);

        tvCount = activity.findViewById(R.id.tvCount);
        tvCount.setText(dbHelper.getTotalPesananAll());

        getDataPesanan();

        binding.btSearchKategori.setOnClickListener(this::onViewClicked);
        binding.bthapusdftr.setOnClickListener(this::onViewClicked);

        binding.swipe.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                getDataPesanan();
            }
        });

        binding.etPencarianPesanan.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                Intent intent2 = new Intent("listPesanan-state");
                intent2.putExtra("refresh", "finditem");
                intent2.putExtra("find", String.valueOf(s));
                LocalBroadcastManager.getInstance(activity).sendBroadcast(intent2);
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
        LocalBroadcastManager.getInstance(activity).registerReceiver(mMessageReceiver,
                new IntentFilter("listPesanan-state"));
        return root;
    }

    private void checkstatedelete() {
        if (adapter.getDelete()) {
            if (listDeletePesanan.isEmpty()) {
                binding.layoutbelow.setVisibility(View.GONE);
            } else {
                binding.layoutbelow.setVisibility(View.VISIBLE);
            }
            imageButton.setVisibility(View.GONE);
            framePilihSemua.setVisibility(View.VISIBLE);
            frameBatal.setVisibility(View.VISIBLE);
        } else {
            imageButton.setVisibility(View.VISIBLE);
            framePilihSemua.setVisibility(View.GONE);
            frameBatal.setVisibility(View.GONE);
            binding.layoutbelow.setVisibility(View.GONE);
        }
    }

    private void getDataPesanan() {
        Cursor cursorpesanan;
        if (valuesWhere.isEmpty()) {
            String kodePegawai = sessionManager.getKodeUsers();
            String idToko = dbHelper.getIdTokoByKodePegawai(kodePegawai);
            cursorpesanan = dbHelper.listPesanan(idToko);
        } else {
            cursorpesanan = dbHelper.listPesananWhereJenisKedatangan(valuesWhere);
        }

        ArrayList<DaftarPesananModel> data = new ArrayList<>();

        if (cursorpesanan.moveToFirst()) {
            do {
                data.add(new DaftarPesananModel(
                        cursorpesanan.getString(15),
                        cursorpesanan.getString(14),
                        dbHelper.getTotalPesananPerINV(cursorpesanan.getString(4)),
                        "Meja 2",
                        cursorpesanan.getString(16),
                        cursorpesanan.getString(4)
                ));
            } while (cursorpesanan.moveToNext());
        }

        if (data.isEmpty()) {
            binding.llKosong.setVisibility(View.VISIBLE);
            binding.rvDaftarPesanan.setVisibility(View.GONE);
        } else {
            binding.llKosong.setVisibility(View.GONE);
            binding.rvDaftarPesanan.setVisibility(View.VISIBLE);
        }
        cursorpesanan.close();

        arrayListCopy.clear();
        arrayListCopy.addAll(data);
        adapter = new DaftarPesananAdapter(data, activity);
        binding.rvDaftarPesanan.setAdapter(adapter);
        if (binding.swipe.isRefreshing()) binding.swipe.setRefreshing(false);
    }

    public static DataPesanan.DataPelanggan getPesananEdit(String inv) {
        Cursor cursorproduct = dbHelper.getPesananWhereInvoice(inv);

        Log.d("DaftarPesananFragment", "getPesananEdit() dipanggil dengan invoice: " + inv);
        if (cursorproduct == null) {
            Log.e("DaftarPesananFragment", "Cursor null! Kemungkinan query gagal dijalankan.");
            return null;
        }

        Log.d("DaftarPesananFragment", "Jumlah kolom: " + cursorproduct.getColumnCount());
        for (int i = 0; i < cursorproduct.getColumnCount(); i++) {
            Log.d("DaftarPesananFragment", "Kolom[" + i + "]: " + cursorproduct.getColumnName(i));
        }

        DataPesanan.DataPelanggan data = new DataPesanan.DataPelanggan("", "", "", "", "");

        if (cursorproduct.moveToFirst()) {
            try {
                String nama = cursorproduct.getString(14);
                String kode = cursorproduct.getString(13);
                String telp = cursorproduct.getString(15);
                String alamat = cursorproduct.getString(12);
                String jenisKedatangan = cursorproduct.getString(11);

                data = new DataPesanan.DataPelanggan(nama, kode, telp, alamat, jenisKedatangan);

                Log.d("DaftarPesananFragment", "Data ditemukan:");
                Log.d("DaftarPesananFragment", "Nama: " + nama);
                Log.d("DaftarPesananFragment", "Kode: " + kode);
                Log.d("DaftarPesananFragment", "Telp: " + telp);
                Log.d("DaftarPesananFragment", "Alamat: " + alamat);
                Log.d("DaftarPesananFragment", "Jenis Kedatangan: " + jenisKedatangan);

            } catch (Exception e) {
                Log.e("DaftarPesananFragment", "Gagal membaca data dari cursor: " + e.getMessage(), e);
            }
        } else {
            Log.w("DaftarPesananFragment", "Cursor kosong! Tidak ada data pesanan untuk invoice: " + inv);
        }

        cursorproduct.close();
        return data;
    }

    public static List<ProdukPesananModel> getDetailPesanan(String inv) {
        Log.d("DaftarPesananFragment", "getDetailPesanan() dipanggil dengan invoice: " + inv);

        Cursor cursorproduct = dbHelper.getPesananDetailWhereInvoice(inv);
        ArrayList<ProdukPesananModel> data = new ArrayList<>();

        if (cursorproduct == null) {
            Log.e("DaftarPesananFragment", "Cursor tb_pesanan_detail null!");
            return data;
        }

        Log.d("DaftarPesananFragment", "Jumlah kolom tb_pesanan_detail: " + cursorproduct.getColumnCount());
        for (int i = 0; i < cursorproduct.getColumnCount(); i++) {
            Log.d("DaftarPesananFragment", "Kolom[" + i + "]: " + cursorproduct.getColumnName(i));
        }

        if (cursorproduct.moveToFirst()) {
            do {
                String kdProduk = cursorproduct.getString(3); // kolom kode_produk
                Log.d("DaftarPesananFragment", "Memproses produk dengan kode: " + kdProduk);

                Cursor cursorGetDiskon = dbHelper.getDetailProduk(kdProduk);
                if (cursorGetDiskon != null && cursorGetDiskon.moveToFirst()) {
                    try {
                        String hrgDiskon = cursorGetDiskon.getString(11); // harga diskon
                        String hrgsale = cursorGetDiskon.getString(8);    // harga normal

                        ProdukPesananModel produk = new ProdukPesananModel(
                                kdProduk,
                                cursorproduct.getString(4), // nama_produk
                                cursorproduct.getString(5), // harga_produk
                                cursorproduct.getString(7), // qty
                                hrgDiskon,
                                hrgsale,
                                cursorproduct.getString(6)  // total_harga
                        );

                        data.add(produk);

                        Log.d("DaftarPesananFragment", "Detail produk ditambahkan: " +
                                "Nama=" + cursorproduct.getString(4) +
                                ", Harga=" + cursorproduct.getString(5) +
                                ", Qty=" + cursorproduct.getString(7) +
                                ", Diskon=" + hrgDiskon +
                                ", HargaNormal=" + hrgsale +
                                ", Total=" + cursorproduct.getString(6));

                    } catch (Exception e) {
                        Log.e("DaftarPesananFragment", "Gagal membaca data produk: " + e.getMessage(), e);
                    }
                } else {
                    Log.w("DaftarPesananFragment", "Cursor tb_produk kosong untuk kode: " + kdProduk);
                }

                if (cursorGetDiskon != null) cursorGetDiskon.close();

            } while (cursorproduct.moveToNext());
        } else {
            Log.w("DaftarPesananFragment", "Cursor tb_pesanan_detail kosong untuk invoice: " + inv);
        }

        cursorproduct.close();
        Log.d("DaftarPesananFragment", "Jumlah produk di list: " + data.size());

        return data;
    }

    private ArrayList<JenisKedatanganModels> getDataJenisKedatangan() {
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

        }

        cursorproduct.close();
        return data;
    }

    public static boolean isExist(JenisKedatanganModels model) {
        for (int i = 0; i < valuesWhere.size(); i++) {
            if (valuesWhere.get(i).getJenis_kedatangan().equals(model.getJenis_kedatangan())) {
                return true;
            }
        }
        return false;
    }

    public static void addWhereValue(JenisKedatanganModels models) {
        if (!isExist(models)) {
            valuesWhere.add(models);
        }
    }

    public static void removeWhereValue(JenisKedatanganModels models) {
        valuesWhere.remove(models);
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        inflater.inflate(R.menu.hapus_pesanan, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public void onPrepareOptionsMenu(@NonNull Menu menu) {
        final MenuItem item = menu.findItem(R.id.nav_semua);
        View rootView = item.getActionView();

        framePilihSemua = (FrameLayout) rootView.findViewById(R.id.nav_pilih_semua);
        frameBatal = (FrameLayout) rootView.findViewById(R.id.nav_batal);
        imageButton = (ImageButton) rootView.findViewById(R.id.deleteIcon);

        framePilihSemua.setOnClickListener(view -> {
            adapter.setCheckall(true);
            adapter.notifyDataSetChanged();
        });

        frameBatal.setOnClickListener(view -> {
            adapter.setDelete(false);
            adapter.setCheckall(false);
            adapter.notifyDataSetChanged();
            checkstatedelete();
        });

        imageButton.setOnClickListener(view -> {
            if (!dbHelper.getTotalPesananAll().equals("0")) {
                adapter.setDelete(true);
                adapter.notifyDataSetChanged();
                checkstatedelete();
            } else {
            }
        });

        super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.nav_semua) {
            GlobalToast.ShowToast(activity, "pilih semua");
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void refreshTotalPesanan() {
        tvCount.setText(dbHelper.getTotalPesananAll());

        Intent intent = new Intent("dataPesanan-state");
        intent.putExtra("refresh", "gettotaldata");
        LocalBroadcastManager.getInstance(activity).sendBroadcast(intent);
    }

    public void onViewClicked(View view) {
            if (view.getId() == R.id.btSearchKategori) {
                final BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(activity, R.style.BottomShetDialogTheme);
                View bottomSheetView = LayoutInflater.from(activity).inflate(R.layout.lsb_kategori_pesanan, activity.findViewById(R.id.ll_bottom_sheet));

                RecyclerView rvFilterJenisKedatangan = bottomSheetView.findViewById(R.id.rvFilterJenisKedatangan);
                Button btnTerapkanFilter = bottomSheetView.findViewById(R.id.btnTerapkanFilter);
                rvFilterJenisKedatangan.setHasFixedSize(true);
                GridLayoutManager layoutManager = new GridLayoutManager(activity, 1, GridLayoutManager.VERTICAL, false);

                rvFilterJenisKedatangan.setLayoutManager(layoutManager);

                rvFilterJenisKedatangan.setAdapter(filterAdapter);

                btnTerapkanFilter.setOnClickListener(view1 -> {
                    getDataPesanan();
                    bottomSheetDialog.cancel();
                });

                bottomSheetDialog.setContentView(bottomSheetView);
                bottomSheetDialog.show();

        } else if (view.getId() == R.id.bthapusdftr) {
            AlertDialog.Builder builder = new AlertDialog.Builder(activity);
            builder.setMessage("Anda Yakin Ingin Menghapus Data Pesanan Yang Dipilih?").setPositiveButton("Iya", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int id) {
                    dbHelper.deleteDataDetailPenjualan(listDeletePesanan);
                    adapter.setDelete(false);
                    adapter.notifyDataSetChanged();
                    getDataPesanan();
                    checkstatedelete();
                    refreshTotalPesanan();
                }
            }).setNegativeButton("Batal", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int id) {
                    dialog.cancel();
                }
            });
            builder.create();
            builder.show();
        }
    }

}