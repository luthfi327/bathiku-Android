package com.pyramidsoft.siqasir;

import static com.pyramidsoft.siqasir.db.DataBaseHelper.database_name;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.pyramidsoft.siqasir.adapter.FilterKategoriAdapter;
import com.pyramidsoft.siqasir.adapter.ListMasterProdukAdapter;
import com.pyramidsoft.siqasir.databinding.ActivityDaftarBarangBinding;
import com.pyramidsoft.siqasir.db.DataBaseHelper;
import com.pyramidsoft.siqasir.model.KategoriModels;
import com.pyramidsoft.siqasir.model.MasterProdukModels;
import com.pyramidsoft.siqasir.model.Outlet;
import com.pyramidsoft.siqasir.utils.ProgressDialog;
import com.pyramidsoft.siqasir.utils.SessionManager;

import java.util.ArrayList;
import java.util.List;

public class ListProdukActivity extends AppCompatActivity {

    private ActivityDaftarBarangBinding binding;
    SessionManager sessionManager;
    ProgressDialog pDialog;
    DataBaseHelper dbHelper;
    SQLiteDatabase mDatabase;
    ListMasterProdukAdapter masterProduk;

    private ArrayList<MasterProdukModels> arrayListCopy = new ArrayList<>();
    public static ArrayList<KategoriModels> valuesWhere = new ArrayList<>();
    FilterKategoriAdapter filterKategoriAdapter;

    public BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String refresh = intent.getStringExtra("refresh");
            if (refresh.equals("list")) {
                setListView();
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityDaftarBarangBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);
        ActionBar actionBar = getSupportActionBar();

        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        pDialog = new ProgressDialog(ListProdukActivity.this);
        sessionManager = new SessionManager(ListProdukActivity.this);
        dbHelper = new DataBaseHelper(this);
        mDatabase = openOrCreateDatabase(database_name, MODE_PRIVATE, null);

        binding.rvListProduk.setHasFixedSize(true);
        GridLayoutManager layoutManager = new GridLayoutManager(ListProdukActivity.this, 1, GridLayoutManager.VERTICAL, false);
        binding.rvListProduk.setLayoutManager(layoutManager);

        setListView();

        binding.btTambahDaftarBrg.setOnClickListener(this::onViewClicked);
        binding.btTambahBrgKosong.setOnClickListener(this::onViewClicked);
        binding.btSearchKategori.setOnClickListener(this::onViewClicked);
        binding.btTambahTemplate.setOnClickListener(this::onViewClicked);
        binding.btExport.setOnClickListener(v -> showExportDialog());

        binding.swipe.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                setListView();
            }
        });

        binding.etPencarianProduk.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                masterProduk.filter(String.valueOf(s), arrayListCopy);
                masterProduk.notifyDataSetChanged();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        String role = sessionManager.getHakAkses();

        if (role == null || !role.equalsIgnoreCase("Owner")) {
            binding.btExport.setVisibility(View.GONE);

            // Geser tombol tambah ke kanan
            RelativeLayout.LayoutParams params =
                    (RelativeLayout.LayoutParams) binding.btTambahDaftarBrg.getLayoutParams();
            params.addRule(RelativeLayout.ALIGN_PARENT_END); // Tambah rule baru
            params.removeRule(RelativeLayout.START_OF);      // Hapus anchor lama
            binding.btTambahDaftarBrg.setLayoutParams(params);
        }

        filterKategoriAdapter = new FilterKategoriAdapter(getDataKategori(), this);
        LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver, new IntentFilter("listMasterProduk-state"));
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                this.finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void setListView() {
        binding.swipe.setRefreshing(true);
        String kodePegawai = sessionManager.getKodeUsers();
        String idToko = dbHelper.getIdTokoByKodePegawai(kodePegawai);
        Cursor cursorproduct;
        if (valuesWhere.isEmpty()) {
            cursorproduct = dbHelper.listProduk(idToko);
        } else {
            cursorproduct = dbHelper.listProdukWhereKategori(valuesWhere, idToko);
        }

        ArrayList<MasterProdukModels> data = new ArrayList<>();

        if (cursorproduct.moveToFirst()) {
            do {
                Integer stok = null;
                int stokIndex = cursorproduct.getColumnIndex("stok");
                if (!cursorproduct.isNull(stokIndex)) {
                    stok = cursorproduct.getInt(stokIndex);
                }

                data.add(new MasterProdukModels(
                        cursorproduct.getString(2),
                        cursorproduct.getString(4),
                        cursorproduct.getString(5),
                        cursorproduct.getBlob(6),
                        cursorproduct.getString(7),
                        cursorproduct.getString(8),
                        cursorproduct.getString(9),
                        cursorproduct.getString(12),
                        cursorproduct.getInt(11),
                        cursorproduct.getString(10),
                        stok
                ));

            } while (cursorproduct.moveToNext());
        }
        if (data.isEmpty()) {
            binding.llKosong.setVisibility(View.VISIBLE);
            binding.llListBarang.setVisibility(View.GONE);
            binding.btTambahDaftarBrg.setVisibility(View.GONE);
        } else {
            binding.llKosong.setVisibility(View.GONE);
            binding.llListBarang.setVisibility(View.VISIBLE);
            binding.btTambahDaftarBrg.setVisibility(View.VISIBLE);
        }

        binding.swipe.setRefreshing(false);
        cursorproduct.close();

        arrayListCopy.clear();
        arrayListCopy.addAll(data);
        masterProduk = new ListMasterProdukAdapter(data, this,mDatabase);

        binding.rvListProduk.setAdapter(masterProduk);

    }

    @Override
    protected void onResume() {
        super.onResume();
        setListView();
    }

    private ArrayList<KategoriModels> getDataKategori() {
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
            Toast.makeText(this, "Data Kategori Kosong", Toast.LENGTH_SHORT).show();
        }

        cursorproduct.close();
        return data;
    }

    public static boolean isExist(KategoriModels model) {
        for (int i = 0; i < valuesWhere.size(); i++) {
            if (valuesWhere.get(i).getKategori().equals(model.getKategori())) {
                return true;
            }
        }
        return false;
    }

    public static void addWhereValue(KategoriModels models) {
        if (!isExist(models)) {
            valuesWhere.add(models);
        }
    }

    public static void remoceWhereValue(KategoriModels models) {
        valuesWhere.remove(models);
    }

    public void setKategori() {

        final BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(ListProdukActivity.this, R.style.BottomShetDialogTheme);
        View bottomSheetView = LayoutInflater.from(ListProdukActivity.this).inflate(R.layout.lsb_kategori, ListProdukActivity.this.findViewById(R.id.ll_bottom_sheet));

        RecyclerView rvFilterKategori = bottomSheetView.findViewById(R.id.rvFilterKategori);
        Button btnTerapkanFilter = bottomSheetView.findViewById(R.id.btnTerapkanFilter);
        rvFilterKategori.setHasFixedSize(true);
        GridLayoutManager layoutManager = new GridLayoutManager(this, 1, GridLayoutManager.VERTICAL, false);
        rvFilterKategori.setLayoutManager(layoutManager);

        rvFilterKategori.setAdapter(filterKategoriAdapter);

        btnTerapkanFilter.setOnClickListener(view1 -> {
            setListView();
            bottomSheetDialog.cancel();
        });

        bottomSheetDialog.setContentView(bottomSheetView);
        bottomSheetDialog.show();

    }

    private void showTemplateDialog() {
        String[] templates = {"Barber", "Toko Buku", "Warung", "Toko Biasa"};

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Pilih Template")
                .setItems(templates, (dialog, which) -> {
                    String selected = templates[which];

                    SessionManager session = new SessionManager(this);
                    String kodePegawai = session.getKodeUsers();
                    String idToko = dbHelper.getIdTokoByKodePegawai(kodePegawai);

                    dbHelper.insertTemplate(selected, Integer.parseInt(idToko), kodePegawai);

                    Toast.makeText(this,
                            "Template " + selected + " berhasil ditambahkan!",
                            Toast.LENGTH_LONG).show();

                    setListView();
                })
                .setNegativeButton("Batal", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void showExportDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = getLayoutInflater().inflate(R.layout.dialog_export_data, null);

        CheckBox cbPilihSemuaData = view.findViewById(R.id.cbPilihSemuaData);
        CheckBox cbJenis = view.findViewById(R.id.cbJenisKedatangan);
        CheckBox cbKategori = view.findViewById(R.id.cbKategori);
        CheckBox cbProduk = view.findViewById(R.id.cbProduk);

        Spinner spTokoTujuan = view.findViewById(R.id.spTokoTujuan);
        LinearLayout llTokoPenerima = view.findViewById(R.id.llTokoPenerima);
        Button btnExport = view.findViewById(R.id.btnExportNow);

        List<Outlet> tokoList = dbHelper.getAllOutlet();

        ArrayAdapter<Outlet> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                tokoList
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spTokoTujuan.setAdapter(adapter);

        cbPilihSemuaData.setOnCheckedChangeListener((buttonView, isChecked) -> {
            cbJenis.setChecked(isChecked);
            cbKategori.setChecked(isChecked);
            cbProduk.setChecked(isChecked);
        });

        spTokoTujuan.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View v, int pos, long id) {
                llTokoPenerima.removeAllViews();

                Outlet tokoTujuan = tokoList.get(pos);

                for (Outlet toko : tokoList) {
                    if (toko.getIdToko() == tokoTujuan.getIdToko()) continue;

                    CheckBox cb = new CheckBox(ListProdukActivity.this);
                    cb.setText(toko.getNamaToko());
                    cb.setTag(toko.getIdToko());

                    // Geser checkbox sedikit ke kanan
                    LinearLayout.LayoutParams params =
                            new LinearLayout.LayoutParams(
                                    LinearLayout.LayoutParams.WRAP_CONTENT,
                                    LinearLayout.LayoutParams.WRAP_CONTENT
                            );
                    params.leftMargin = 12;
                    cb.setLayoutParams(params);

                    llTokoPenerima.addView(cb);
                }
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) { }
        });

        builder.setView(view);
        AlertDialog dialog = builder.create();
        dialog.show();

        btnExport.setOnClickListener(v -> {

            // Wajib pilih data
            if (!cbJenis.isChecked() && !cbKategori.isChecked() && !cbProduk.isChecked()) {
                Toast.makeText(this, "Pilih data yang ingin diekspor!", Toast.LENGTH_SHORT).show();
                return;
            }

            Outlet tokoTujuan = (Outlet) spTokoTujuan.getSelectedItem();
            int idTokoTujuan = tokoTujuan.getIdToko();

            ArrayList<Integer> tokoPenerima = new ArrayList<>();
            for (int i = 0; i < llTokoPenerima.getChildCount(); i++) {
                CheckBox cb = (CheckBox) llTokoPenerima.getChildAt(i);
                if (cb.isChecked()) {
                    tokoPenerima.add((Integer) cb.getTag());
                }
            }

            if (tokoPenerima.isEmpty()) {
                Toast.makeText(this, "Pilih minimal 1 toko penerima!", Toast.LENGTH_SHORT).show();
                return;
            }

            // ===========================================
            // 🔥 PANGGIL FUNGSI EXPORT BERDASARKAN CENTANG
            // ===========================================

            for (int idTokoPenerima : tokoPenerima) {

                dbHelper.copyMasterDataFiltered(
                        idTokoTujuan,
                        idTokoPenerima,
                        cbJenis.isChecked(),
                        cbKategori.isChecked(),
                        cbProduk.isChecked()
                );
            }

            Toast.makeText(this, "Export selesai!", Toast.LENGTH_LONG).show();
            dialog.dismiss();
        });
    }

    public void onViewClicked(View view) {
        Intent intent;

        int viewId = view.getId();
        if (viewId == R.id.btTambahDaftarBrg || viewId == R.id.btTambahBrgKosong) {
            intent = new Intent(ListProdukActivity.this, ProdukTokoActivity.class);
            startActivity(intent);
        } else if (viewId == R.id.btSearchKategori) {
            setKategori();
        } else if (viewId == R.id.btTambahTemplate) {
            showTemplateDialog();
        }
    }

}
