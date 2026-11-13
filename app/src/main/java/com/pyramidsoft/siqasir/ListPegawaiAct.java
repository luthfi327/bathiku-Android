package com.pyramidsoft.siqasir;

import static com.pyramidsoft.siqasir.db.DataBaseHelper.database_name;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.GridLayoutManager;

import com.pyramidsoft.siqasir.adapter.PegawaiAdapter;
import com.pyramidsoft.siqasir.databinding.ActivityListPegawaiBinding;
import com.pyramidsoft.siqasir.db.DataBaseHelper;
import com.pyramidsoft.siqasir.model.PegawaiModels;
import com.pyramidsoft.siqasir.utils.ProgressDialog;
import com.pyramidsoft.siqasir.utils.SessionManager;

import java.util.ArrayList;

public class ListPegawaiAct extends AppCompatActivity {

    private ActivityListPegawaiBinding binding;
    private SessionManager sessionManager;
    private ProgressDialog pDialog;
    private DataBaseHelper dbHelper;
    private SQLiteDatabase mDatabase;
    private PegawaiAdapter pegawaiAdapter;
    private String jml_pegawai;

    private final BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if ("list".equals(intent.getStringExtra("refresh"))) {
                loadListPegawai();
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityListPegawaiBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setSupportActionBar(binding.toolbar);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) actionBar.setDisplayHomeAsUpEnabled(true);

        pDialog = new ProgressDialog(this);
        sessionManager = new SessionManager(this);
        dbHelper = new DataBaseHelper(this);
        mDatabase = openOrCreateDatabase(database_name, MODE_PRIVATE, null);

        binding.rvListPegawai.setHasFixedSize(true);
        binding.rvListPegawai.setLayoutManager(new GridLayoutManager(this, 1));

        binding.btTambahUser.setOnClickListener(this::onViewClicked);
        binding.btTambahPegawaiKosong.setOnClickListener(this::onViewClicked);

        // inisialisasi adapter
        pegawaiAdapter = new PegawaiAdapter(new ArrayList<>(), this);
        binding.rvListPegawai.setAdapter(pegawaiAdapter);

        binding.swipe.setOnRefreshListener(this::loadListPegawai);

        LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver, new IntentFilter("listPegawai-state"));

        loadListPegawai();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadListPegawai();
    }

    private void getRowsPegawai() {
        Cursor cursor = dbHelper.getRowsPegawai();
        if (cursor.moveToFirst()) {
            jml_pegawai = cursor.getString(cursor.getColumnIndexOrThrow("jml_pegawai"));
        }
        cursor.close();

        binding.btTambahUser.setVisibility("0".equals(jml_pegawai) ? View.GONE : View.VISIBLE);
    }

    private void loadListPegawai() {
        binding.swipe.setRefreshing(true);
        Cursor cursor = dbHelper.listPegawai();
        ArrayList<PegawaiModels> data = new ArrayList<>();

        if (cursor.moveToFirst()) {
            do {
                String kode = cursor.getString(cursor.getColumnIndexOrThrow("kode_pegawai"));
                String nama = cursor.getString(cursor.getColumnIndexOrThrow("nama_pegawai"));
                String posisi = cursor.getString(cursor.getColumnIndexOrThrow("posisi"));
                Log.d("DEBUG_CURSOR", "kode=" + kode + ", nama=" + nama + ", posisi=" + posisi);

                data.add(new PegawaiModels(kode, nama, posisi));
            } while (cursor.moveToNext());
        }

        cursor.close();

        if (data.isEmpty()) {
            binding.rvListPegawai.setVisibility(View.GONE);
            binding.llKosong.setVisibility(View.VISIBLE);
        } else {
            binding.llKosong.setVisibility(View.GONE);
            binding.rvListPegawai.setVisibility(View.VISIBLE);
            getRowsPegawai();
        }

        pegawaiAdapter.updateData(data);
        binding.swipe.setRefreshing(false);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) finish();
        return super.onOptionsItemSelected(item);
    }

    public void onViewClicked(View view) {
        if (view.getId() == R.id.btTambahUser || view.getId() == R.id.btTambahPegawaiKosong) {
            startActivity(new Intent(this, TambahPegawaiActivity.class));
        }
    }
}
