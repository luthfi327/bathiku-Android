package com.pyramidsoft.siqasir;

import static com.pyramidsoft.siqasir.db.DataBaseHelper.database_name;

import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import com.chivorn.smartmaterialspinner.SmartMaterialSpinner;
import com.google.android.material.snackbar.Snackbar;
import com.pyramidsoft.siqasir.databinding.ActivityUpdateDataPegawaiBinding;
import com.pyramidsoft.siqasir.db.DataBaseHelper;
import com.pyramidsoft.siqasir.utils.AndLog;
import com.pyramidsoft.siqasir.utils.GlobalToast;
import com.pyramidsoft.siqasir.utils.ProgressDialog;
import com.pyramidsoft.siqasir.utils.SessionManager;

import java.util.ArrayList;
import java.util.List;

public class UpdateDataPegawaiActivity extends AppCompatActivity {

    private ActivityUpdateDataPegawaiBinding binding;
    SessionManager sessionManager;
    ProgressDialog pDialog;
    DataBaseHelper dbHelper;
    SQLiteDatabase mDatabase;
    private SmartMaterialSpinner<String> spJk;
    private SmartMaterialSpinner<String> spPosisi;
    private SmartMaterialSpinner<String> spHakAkses;

    private List<String> genderList;
    private List<String> posisiList;
    private List<String> hakAksesList;

    private String id_toko;

    String vJk, vPosisi, vHakAkses, UOV, statusUsers, dataUser, kode_pegawai;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityUpdateDataPegawaiBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);
        ActionBar actionBar = getSupportActionBar();

        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        pDialog = new ProgressDialog(UpdateDataPegawaiActivity.this);
        sessionManager = new SessionManager(UpdateDataPegawaiActivity.this);
        dbHelper = new DataBaseHelper(UpdateDataPegawaiActivity.this);
        mDatabase = openOrCreateDatabase(database_name, MODE_PRIVATE, null);

        initSpinner();

        binding.tvBuatPwd.setOnClickListener(this::onViewClicked);
        binding.tvBuatPIN.setOnClickListener(this::onViewClicked);
        binding.btSimpan.setOnClickListener(this::onViewClicked);
        binding.btBatal.setOnClickListener(this::onViewClicked);

        kode_pegawai = getIntent().getStringExtra("kode_pegawai");
        getDetailprofile(kode_pegawai);

    }

    private void getDetailprofile(String kode_pegawai) {
        pDialog.show();

        Cursor cursor = dbHelper.getDetailPegawai(kode_pegawai);

        if (cursor.moveToFirst()) {
            String namaPegawai = cursor.getString(cursor.getColumnIndexOrThrow(DataBaseHelper.nama_pegawai));
            String gender = cursor.getString(cursor.getColumnIndexOrThrow(DataBaseHelper.gender));
            String noHp = cursor.getString(cursor.getColumnIndexOrThrow(DataBaseHelper.no_hp));
            String hakAkses = cursor.getString(cursor.getColumnIndexOrThrow(DataBaseHelper.hak_akses));
            String posisi = cursor.getString(cursor.getColumnIndexOrThrow(DataBaseHelper.posisi));
            String status = cursor.getString(cursor.getColumnIndexOrThrow(DataBaseHelper.status));
            String username = cursor.getString(cursor.getColumnIndexOrThrow(DataBaseHelper.username));
            String password = cursor.getString(cursor.getColumnIndexOrThrow(DataBaseHelper.password));
            String pin = cursor.getString(cursor.getColumnIndexOrThrow(DataBaseHelper.pin));
            id_toko = cursor.getString(cursor.getColumnIndexOrThrow(DataBaseHelper.id_toko));

            binding.etnamaPegawai.setText(namaPegawai);
            binding.tvJk.setText(gender);
            binding.etNoHp.setText(noHp);
            binding.etHakAkses.setText(hakAkses);
            binding.etPosisi.setText(posisi);
            binding.etUsername.setText(username);
            binding.etPassword.setText(password);
            binding.etPin.setText(pin);

            if (status.equals("Aktif")) {
                binding.switchStatus.setChecked(true);
            } else {
                binding.switchStatus.setChecked(false);
            }

            Cursor tokoCursor = dbHelper.getTokoById(id_toko);
            if (tokoCursor.moveToFirst()) {
                String namaToko = tokoCursor.getString(tokoCursor.getColumnIndexOrThrow("nama_toko"));
                binding.etNamaOutlet.setText(namaToko);
            }
            tokoCursor.close();

            pDialog.dismiss();
        }
        cursor.close();
    }


    private void initSpinner() {

        spJk = findViewById(R.id.spinJK);

        genderList = new ArrayList<>();

        genderList.add("Pria");
        genderList.add("Wanita");

        spJk.setItem(genderList);

        spJk.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int position, long id) {
                vJk = genderList.get(position);
                AndLog.ShowLog("vJk", vJk);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
            }
        });

    }

    private void updateData(View view) {

        pDialog.show();
        String namaPegawai, NoHP, Username, Pwd, Pin, HakAkses, Posisi;

        HakAkses = binding.etHakAkses.getText().toString().trim();
        Posisi = binding.etPosisi.getText().toString().trim();
        namaPegawai = binding.etnamaPegawai.getText().toString().trim();
        NoHP = binding.etNoHp.getText().toString().trim();
        Username = binding.etUsername.getText().toString().trim();
        Pwd = binding.etPassword.getText().toString().trim();
        Pin = binding.etPin.getText().toString().trim();

        ContentValues values = new ContentValues();
        values.put(DataBaseHelper.nama_pegawai, namaPegawai);
        values.put(DataBaseHelper.kode_pegawai, kode_pegawai);
        values.put(DataBaseHelper.hak_akses, HakAkses);
        values.put(DataBaseHelper.posisi, Posisi);

        if (vJk != null) {
            values.put(DataBaseHelper.gender, vJk);
        } else {
            values.put(DataBaseHelper.gender, binding.tvJk.getText().toString().trim());
        }

        values.put(DataBaseHelper.no_hp, NoHP);

        if (binding.switchStatus.isChecked()) {
            values.put(DataBaseHelper.status, "Aktif");
        } else {
            values.put(DataBaseHelper.status, "Tidak Aktif");
        }

        values.put(DataBaseHelper.username, Username);
        values.put(DataBaseHelper.password, Pwd);
        values.put(DataBaseHelper.pin, Pin);

        if (namaPegawai.equals("") ||
                NoHP.equals("") ||
                Username.equals("") ||
                Pwd.equals("") ||
                Pin.equals("")) {
            Snackbar.make(view, "Data Tidak Boleh Kosong", Snackbar.LENGTH_SHORT).show();

            pDialog.dismiss();
        } else {
            try {
                dbHelper.updateDataOwner(values, kode_pegawai);
                GlobalToast.ShowToast(this, "Data Berhasil di Update!");
                AndLog.ShowLog("dataPegawaiUpdate: ", String.valueOf(values));
                finish();
                pDialog.dismiss();

            } catch (Exception e) {
                e.printStackTrace();
            }

        }

    }

    @Override
    protected void onResume() {
        super.onResume();
        getDetailprofile(kode_pegawai);
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
        Intent intent;
        if (view.getId() == R.id.tvBuatPwd) {
            intent = new Intent(UpdateDataPegawaiActivity.this, ResetPswdPinAdmin.class);
            intent.putExtra("reset", "password");
            intent.putExtra("kode_pegawai", kode_pegawai);
            intent.putExtra("pwd", binding.etPassword.getText().toString());
            startActivity(intent);
        } else if (view.getId() == R.id.tvBuatPIN) {
            intent = new Intent(UpdateDataPegawaiActivity.this, ResetPswdPinAdmin.class);
            intent.putExtra("reset", "pin");
            intent.putExtra("kode_pegawai", kode_pegawai);
            intent.putExtra("pin", binding.etPin.getText().toString());
            startActivity(intent);
        } else if (view.getId() == R.id.btSimpan) {
            updateData(view);
        } else if (view.getId() == R.id.btBatal) {
            this.finish();
        }
    }

}
