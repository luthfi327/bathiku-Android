package com.pyramidsoft.siqasir;

import static com.pyramidsoft.siqasir.db.DataBaseHelper.database_name;

import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.chivorn.smartmaterialspinner.SmartMaterialSpinner;
import com.pyramidsoft.siqasir.databinding.ActivityDataPemesananBinding;
import com.pyramidsoft.siqasir.db.DataBaseHelper;
import com.pyramidsoft.siqasir.model.KategoriModels;
import com.pyramidsoft.siqasir.utils.AndLog;
import com.pyramidsoft.siqasir.utils.DataPesanan;
import com.pyramidsoft.siqasir.utils.GlobalToast;
import com.pyramidsoft.siqasir.utils.ProgressDialog;
import com.pyramidsoft.siqasir.utils.SessionManager;

import java.util.ArrayList;

public class DataPemesananAct extends AppCompatActivity {

    private ActivityDataPemesananBinding binding;
    String[] tipeDiskon = {"Nominal", "Persen"};
    private SmartMaterialSpinner<KategoriModels> spJenisKedatangan;
    ArrayAdapter<String> jenisKAdapter;
    ArrayAdapter<String> diskonAdapter;
    SessionManager sessionManager;
    ProgressDialog pDialog;
    DataBaseHelper dbHelper;
    SQLiteDatabase mDatabase;
    String jenis_kedatangan = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityDataPemesananBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        ActionBar actionBar = getSupportActionBar();

        setSupportActionBar(binding.toolbar);

        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
        pDialog = new ProgressDialog(this);
        sessionManager = new SessionManager(this);
        dbHelper = new DataBaseHelper(this);
        mDatabase = openOrCreateDatabase(database_name, MODE_PRIVATE, null);

        diskonAdapter = new ArrayAdapter<String>(this, R.layout.spinner_item, tipeDiskon);
        diskonAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        binding.spTipeDiskon.setAdapter(diskonAdapter);

        binding.btSimpan.setOnClickListener(this::onViewClicked);
        binding.btResetData.setOnClickListener(this::onViewClicked);

        binding.spTipeDiskon.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                DataPesanan.setTipediskon(tipeDiskon[i]);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });

        initSpinview();
        setDataPelanggan();

    }

    public void initSpinview() {
        String kodePegawai = sessionManager.getKodeUsers();
        String idToko = dbHelper.getIdTokoByKodePegawai(kodePegawai);
        spJenisKedatangan = findViewById(R.id.SpinJenisKedatangan);

        ArrayList<String> ListKategori = dbHelper.spinJenisKedatangan(idToko);

        jenisKAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, ListKategori);

        jenisKAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        spJenisKedatangan.setAdapter(jenisKAdapter);

        AndLog.ShowLog("listKat", ListKategori.toString());

        spJenisKedatangan.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int position, long id) {
                jenis_kedatangan = adapterView.getItemAtPosition(position).toString();
                AndLog.ShowLog("vJenisKedatangan", jenis_kedatangan);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    public void setDataPelanggan() {
        binding.etNamaPelanggan.setText(DataPesanan.getNamaPelanggan());
        binding.etIdMemberPelanggan.setText(DataPesanan.getIdMember());
        if (!DataPesanan.getJenisKedatangan().isEmpty()) {
            int val = diskonAdapter.getPosition(DataPesanan.getJenisKedatangan());
            spJenisKedatangan.setSelection(val);
        }
        if (!DataPesanan.getTipediskon().isEmpty()) {
            int val = diskonAdapter.getPosition(DataPesanan.getTipediskon());
            binding.spTipeDiskon.setSelection(val);
        }
        binding.etDiskon.setText(DataPesanan.getDiskon());
        if (DataPesanan.isPelangganEmpty()) {
            binding.btResetData.setVisibility(View.GONE);
        } else {
            binding.btResetData.setVisibility(View.VISIBLE);
        }
    }

    public void onViewClicked(View view) {
        if (view.getId() == R.id.btSimpan) {

            if (jenis_kedatangan.isEmpty()) {
                GlobalToast.ShowToast(this, "Jenis Kedatangan Kosong");
                return;
            }

            String namaPelanggan = binding.etNamaPelanggan.getText().toString().trim();

            if (namaPelanggan.isEmpty()) {
                namaPelanggan = sessionManager.getNama();
            }

            DataPesanan.setNamaPelanggan(namaPelanggan);
            DataPesanan.setIdMember(binding.etIdMemberPelanggan.getText().toString());
            DataPesanan.setJenisKedatangan(jenis_kedatangan);
            DataPesanan.setDiskon(binding.etDiskon.getText().toString());

            Intent intent = new Intent("dataPesanan-state");
            intent.putExtra("refresh", "pesanan");
            LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
            finish();

        } else if (view.getId() == R.id.btResetData) {

            DataPesanan.resetDataPelanggan();
            finish();
        }
    }

}
