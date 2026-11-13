package com.pyramidsoft.siqasir;

import static com.pyramidsoft.siqasir.db.DataBaseHelper.database_name;

import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.pyramidsoft.siqasir.adapter.KategoriAdp;
import com.pyramidsoft.siqasir.databinding.ActivityKategoriBinding;
import com.pyramidsoft.siqasir.db.DataBaseHelper;
import com.pyramidsoft.siqasir.model.KategoriModels;
import com.pyramidsoft.siqasir.model.Outlet;
import com.pyramidsoft.siqasir.utils.AndLog;
import com.pyramidsoft.siqasir.utils.GlobalToast;
import com.pyramidsoft.siqasir.utils.ProgressDialog;
import com.pyramidsoft.siqasir.utils.SessionManager;

import java.util.ArrayList;
import java.util.List;

public class KategoriActivity extends AppCompatActivity {

    private ActivityKategoriBinding binding;
    EditText etNamaKategori;
    TextView tvBatal;
    Button btSimpan;

    SessionManager sessionManager;
    ProgressDialog pDialog;
    DataBaseHelper dbHelper;
    SQLiteDatabase mDatabase;
    KategoriAdp kategoriAdp;

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
        binding = ActivityKategoriBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);
        ActionBar actionBar = getSupportActionBar();

        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        pDialog = new ProgressDialog(KategoriActivity.this);
        sessionManager = new SessionManager(KategoriActivity.this);
        dbHelper = new DataBaseHelper(this);
        mDatabase = openOrCreateDatabase(database_name, MODE_PRIVATE, null);

        binding.listKategori.setHasFixedSize(true);
        GridLayoutManager layoutManager = new GridLayoutManager(KategoriActivity.this, 1, GridLayoutManager.VERTICAL, false);
        binding.listKategori.setLayoutManager(layoutManager);

        binding.btTambahkategori.setOnClickListener(this::onViewClicked);
        binding.btTambahKategoriKosong.setOnClickListener(this::onViewClicked);
        binding.btTambahTemplate.setOnClickListener(this::onViewClicked);
        binding.btExport.setOnClickListener(v -> showExportDialog());

        setListView();

        binding.swipe.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                setListView();
            }
        });

        String role = sessionManager.getHakAkses();

        if (role == null || !role.equalsIgnoreCase("Owner")) {
            binding.btExport.setVisibility(View.GONE);

            // Geser tombol tambah ke kanan
            RelativeLayout.LayoutParams params =
                    (RelativeLayout.LayoutParams) binding.btTambahkategori.getLayoutParams();
            params.addRule(RelativeLayout.ALIGN_PARENT_END); // Tambah rule baru
            params.removeRule(RelativeLayout.START_OF);      // Hapus anchor lama
            binding.btTambahkategori.setLayoutParams(params);
        }

        LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver, new IntentFilter("listMasterKategori-state"));
    }

    public void setListView() {
        binding.swipe.setRefreshing(true);
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
            binding.swipe.setVisibility(View.GONE);
            binding.llKosong.setVisibility(View.VISIBLE);
            binding.btTambahkategori.setVisibility(View.GONE);
        } else {
            binding.swipe.setVisibility(View.VISIBLE);
            binding.llKosong.setVisibility(View.GONE);
            binding.btTambahkategori.setVisibility(View.VISIBLE);
        }

        binding.swipe.setRefreshing(false);
        cursorproduct.close();

        kategoriAdp = new KategoriAdp(data, this, mDatabase);

        binding.listKategori.setAdapter(kategoriAdp);
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

    private void DialogForm() {
        final Dialog dialog = new Dialog(KategoriActivity.this);
        dialog.setContentView(R.layout.dialog_kategori_add);

        tvBatal = (TextView) dialog.findViewById(R.id.tvBatal);
        etNamaKategori = (EditText) dialog.findViewById(R.id.etNamaKategori);
        btSimpan = (Button) dialog.findViewById(R.id.btSimpan);

        tvBatal.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });

        btSimpan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                TambahKategori();
                GlobalToast.ShowToast(KategoriActivity.this, "Kategori Berhasil Ditambahkan");
                etNamaKategori.setText("");
                etNamaKategori.setFocusable(true);

                setListView();

            }
        });
        dialog.show();
    }

    private void TambahKategori() {
        pDialog.show();

        String etKategori = etNamaKategori.getText().toString().trim();
        String kodePegawai = sessionManager.getKodeUsers();

        String idToko = dbHelper.getIdTokoByKodePegawai(kodePegawai);

        ContentValues values = new ContentValues();
        values.put(DataBaseHelper.kode_pegawai, kodePegawai);
        values.put(DataBaseHelper.kategori, etKategori);

        if (idToko != null) {
            values.put(DataBaseHelper.id_toko, idToko);
        } else {
            AndLog.ShowLog("TambahKategori", "id_toko null untuk kode pegawai: " + kodePegawai);
        }

        if (etKategori.equals("")) {
            GlobalToast.ShowToast(this, "Data tidak Boleh Kosong");
            pDialog.dismiss();

        } else {
            dbHelper.tambahKategori(values);
            GlobalToast.ShowToast(this, "Data Berhasil Tersimpan");
            AndLog.ShowLog("dataUsers: ", String.valueOf(values));
            pDialog.dismiss();
        }
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

                    CheckBox cb = new CheckBox(KategoriActivity.this);
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
        int id = view.getId();
        if (id == R.id.btTambahkategori || id == R.id.btTambahKategoriKosong) {
            DialogForm();
        } else if (id == R.id.btTambahTemplate) {
            showTemplateDialog();
        }
    }
}
