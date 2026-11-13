package com.pyramidsoft.siqasir;

import static com.pyramidsoft.siqasir.db.DataBaseHelper.database_name;

import android.app.Dialog;
import android.content.ContentValues;
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
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.pyramidsoft.siqasir.adapter.JenisKedatanganAdapter;
import com.pyramidsoft.siqasir.databinding.ActivityMasterJenisKedatanganBinding;
import com.pyramidsoft.siqasir.db.DataBaseHelper;
import com.pyramidsoft.siqasir.model.JenisKedatanganModels;
import com.pyramidsoft.siqasir.model.Outlet;
import com.pyramidsoft.siqasir.utils.AndLog;
import com.pyramidsoft.siqasir.utils.GlobalToast;
import com.pyramidsoft.siqasir.utils.ProgressDialog;
import com.pyramidsoft.siqasir.utils.SessionManager;

import java.util.ArrayList;
import java.util.List;

public class MasterJenisKedatanganActivity extends AppCompatActivity {

    private ActivityMasterJenisKedatanganBinding binding;
    EditText etJenisKedatangan;
    TextView tvBatal;
    Button btSimpan;

    SessionManager sessionManager;
    ProgressDialog pDialog;
    DataBaseHelper dbHelper;
    SQLiteDatabase mDatabase;

    JenisKedatanganAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMasterJenisKedatanganBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);
        ActionBar actionBar = getSupportActionBar();

        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        pDialog = new ProgressDialog(this);
        sessionManager = new SessionManager(this);
        dbHelper = new DataBaseHelper(this);
        mDatabase = openOrCreateDatabase(database_name, MODE_PRIVATE, null);

        binding.btTambah.setOnClickListener(this::onViewClicked);
        binding.btTambahJKKosong.setOnClickListener(this::onViewClicked);
        binding.btTambahTemplate.setOnClickListener(this::onViewClicked);
        binding.btExport.setOnClickListener(v -> showExportDialog());

        binding.rvMasterJenisBrg.setHasFixedSize(true);
        GridLayoutManager layoutManager = new GridLayoutManager(this, 1, GridLayoutManager.VERTICAL, false);
        binding.rvMasterJenisBrg.setLayoutManager(layoutManager);

        setListView();

        String role = sessionManager.getHakAkses();

        if (role == null || !role.equalsIgnoreCase("Owner")) {
            binding.btExport.setVisibility(View.GONE);

            // Geser tombol tambah ke kanan
            RelativeLayout.LayoutParams params =
                    (RelativeLayout.LayoutParams) binding.btTambah.getLayoutParams();
            params.addRule(RelativeLayout.ALIGN_PARENT_END); // Tambah rule baru
            params.removeRule(RelativeLayout.START_OF);      // Hapus anchor lama
            binding.btTambah.setLayoutParams(params);
        }

        binding.swipe.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                setListView();
            }
        });
    }

    public void setListView() {
        binding.swipe.setRefreshing(true);
        String kodePegawai = sessionManager.getKodeUsers();
        String idToko = dbHelper.getIdTokoByKodePegawai(kodePegawai);
        Cursor cursorproduct = dbHelper.listJenisKedatangan(idToko);

        ArrayList<JenisKedatanganModels> data = new ArrayList<>();

        if (cursorproduct.moveToFirst()) {
            do {
                data.add(new JenisKedatanganModels(
                        cursorproduct.getString(0),
                        cursorproduct.getString(1),
                        cursorproduct.getString(2)
                ));
            } while (cursorproduct.moveToNext());
        }

        if (data.isEmpty()) {
            binding.swipe.setVisibility(View.GONE);
            binding.llKosong.setVisibility(View.VISIBLE);
            binding.btTambah.setVisibility(View.GONE);
        } else {
            binding.swipe.setVisibility(View.VISIBLE);
            binding.llKosong.setVisibility(View.GONE);
            binding.btTambah.setVisibility(View.VISIBLE);
        }

        binding.swipe.setRefreshing(false);
        cursorproduct.close();

        adapter = new JenisKedatanganAdapter(data, this, mDatabase);
        binding.rvMasterJenisBrg.setAdapter(adapter);
    }

    private void DialogForm() {
        final Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_jenis_kedatangan_add);

        tvBatal = (TextView) dialog.findViewById(R.id.tvBatal);
        etJenisKedatangan = (EditText) dialog.findViewById(R.id.etJenisKedatangan);
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
                TambahJenisKedatangan();
                etJenisKedatangan.setText("");
                etJenisKedatangan.setFocusable(true);

                setListView();
            }
        });
        dialog.show();
    }

    private void TambahJenisKedatangan () {
        pDialog.show();

        String jenis_kedatangan = etJenisKedatangan.getText().toString().trim();
        String kodePegawai = sessionManager.getKodeUsers();
        String idToko = dbHelper.getIdTokoByKodePegawai(kodePegawai);

        ContentValues values = new ContentValues();
        values.put(DataBaseHelper.jenis_kedatangan, jenis_kedatangan);
        values.put("kode_pegawai", kodePegawai);

        if (idToko != null) {
            values.put(DataBaseHelper.id_toko, idToko);
        } else {
            AndLog.ShowLog("TambahJenisKedatangan", "id_toko null untuk kode pegawai: " + kodePegawai);
        }

        if (jenis_kedatangan.equals("")) {
            GlobalToast.ShowToast(this, "Data Tidak Boleh Kosong");
        } else {
            dbHelper.tambahJenisKedatangan(values);
            GlobalToast.ShowToast(this, "Data Berhasil Tersimpan");
            AndLog.ShowLog("dataUsers: ", String.valueOf(values));
        }

        pDialog.dismiss();
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

                    CheckBox cb = new CheckBox(MasterJenisKedatanganActivity.this);
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

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                this.finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void onViewClicked(View view) {
        int id = view.getId();

        if (id == R.id.btTambah || id == R.id.btTambahJKKosong) {
            DialogForm();
        } else if (id == R.id.btTambahTemplate) {
            showTemplateDialog();
        }
    }

}