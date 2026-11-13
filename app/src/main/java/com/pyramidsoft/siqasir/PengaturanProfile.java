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
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import com.chivorn.smartmaterialspinner.SmartMaterialSpinner;
import com.google.android.material.snackbar.Snackbar;
import com.pyramidsoft.siqasir.databinding.ActivityPengaturanProfileBinding;
import com.pyramidsoft.siqasir.db.DataBaseHelper;
import com.pyramidsoft.siqasir.utils.AndLog;
import com.pyramidsoft.siqasir.utils.GlobalToast;
import com.pyramidsoft.siqasir.utils.ProgressDialog;
import com.pyramidsoft.siqasir.utils.SessionManager;

import java.util.ArrayList;
import java.util.List;

public class PengaturanProfile extends AppCompatActivity {

    private ActivityPengaturanProfileBinding binding;
    SessionManager sessionManager;
    ProgressDialog pDialog;
    DataBaseHelper dbHelper;
    SQLiteDatabase mDatabase;

    private SmartMaterialSpinner<String> spJK;
    private List<String> genderList;
    String vJk, kode_pegawai;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityPengaturanProfileBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);
        ActionBar actionBar = getSupportActionBar();

        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
        pDialog = new ProgressDialog(PengaturanProfile.this);
        sessionManager = new SessionManager(PengaturanProfile.this);
        dbHelper = new DataBaseHelper(PengaturanProfile.this);
        mDatabase = openOrCreateDatabase(database_name, MODE_PRIVATE, null);

        binding.tvBuatPwd.setOnClickListener(this::onViewClicked);
        binding.tvBuatPIN.setOnClickListener(this::onViewClicked);
        binding.btBatal.setOnClickListener(this::onViewClicked);
        binding.btSimpan.setOnClickListener(this::onViewClicked);
        binding.btBukaAkses.setOnClickListener(this::onViewClicked);

        initSpinner();

        kode_pegawai = sessionManager.getKodeUsers();

        getDetailprofile();
        setDefaulActivity(false);

        binding.llAction.setVisibility(View.GONE);
        binding.btBukaAkses.setVisibility(View.VISIBLE);

    }

    private void setDefaulActivity(boolean status) {
        binding.etNamaAdmin.setEnabled(status);
        binding.spinJK.setEnabled(status);
        binding.etNoHp.setEnabled(status);
        binding.etUsername.setEnabled(status);
        binding.switchStatus.setClickable(status);
        binding.switchStatus.setChecked(status);
    }

    private void getDetailprofile() {
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

            binding.etKodePegawai.setText(kode_pegawai);
            binding.etNamaAdmin.setText(namaPegawai);
            binding.tvJk.setText(gender);
            binding.etNoHp.setText(noHp);
            binding.etHakAkses.setText(hakAkses);
            binding.etPosisi.setText(posisi);
            if (status.equals("Aktif")) {
                binding.switchStatus.setChecked(true);
            } else {
                binding.switchStatus.setChecked(false);
            }
            binding.etUsername.setText(username);
            binding.etPassword.setText(password);
            binding.etPin.setText(pin);
            pDialog.dismiss();
        }
    }

    private void initSpinner() {
        spJK = findViewById(R.id.spinJK);
        genderList = new ArrayList<>();

        genderList.add("Pria");
        genderList.add("Wanita");

        spJK.setItem(genderList);

        spJK.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
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
        String kodePegawai, namaAdmin, NoHp, Username, Pwd, Pin, HakAkses, Posisi;

        kodePegawai = sessionManager.getKodeUsers();

        HakAkses = binding.etHakAkses.getText().toString().trim();
        Posisi = binding.etPosisi.getText().toString().trim();
        namaAdmin = binding.etNamaAdmin.getText().toString().trim();
        NoHp = binding.etNoHp.getText().toString().trim();
        Username = binding.etUsername.getText().toString().trim();
        Pwd = binding.etPassword.getText().toString().trim();
        Pin = binding.etPin.getText().toString().trim();

        ContentValues values = new ContentValues();
        values.put(DataBaseHelper.nama_pegawai, namaAdmin);
        values.put(DataBaseHelper.no_hp, NoHp);
        values.put(DataBaseHelper.kode_pegawai, kodePegawai);
        values.put(DataBaseHelper.hak_akses, HakAkses);
        values.put(DataBaseHelper.posisi, Posisi);

        if (vJk != null) {
            values.put(DataBaseHelper.gender, vJk);
        } else {
            values.put(DataBaseHelper.gender, binding.tvJk.getText().toString().trim());
        }

        values.put(DataBaseHelper.username, Username);
        values.put(DataBaseHelper.password, Pwd);
        values.put(DataBaseHelper.pin, Pin);

        if (namaAdmin.equals("") ||
                NoHp.equals("") ||
                Username.equals("") ||
                Pwd.equals("") ||
                Pin.equals("")) {
            Snackbar.make(view, "Data Tidak Boleh Kosong", Snackbar.LENGTH_SHORT).show();
            pDialog.dismiss();
        } else {
            try {
                dbHelper.updateDataOwner(values, kodePegawai);
                GlobalToast.ShowToast(this, "Data Berhasil di Update!");
                setDefaulActivity(false);

                binding.llAction.setVisibility(View.GONE);
                binding.btBukaAkses.setVisibility(View.VISIBLE);
                AndLog.ShowLog("DataOwnerUpdate: ", String.valueOf(values));

                pDialog.dismiss();
            } catch (Exception e) {
                e.printStackTrace();
            }

        }

    }

    @Override
    protected void onResume() {
        super.onResume();
        getDetailprofile();
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
            intent = new Intent(PengaturanProfile.this, ResetPswdPinAdmin.class);
            intent.putExtra("reset", "password");
            intent.putExtra("kode_pegawai", kode_pegawai);
            intent.putExtra("pwd", binding.etPassword.getText().toString());
            AndLog.ShowLog("pwdGET", binding.etPassword.getText().toString());
            startActivity(intent);
        } else if (view.getId() == R.id.tvBuatPIN) {
            intent = new Intent(PengaturanProfile.this, ResetPswdPinAdmin.class);
            intent.putExtra("reset", "pin");
            intent.putExtra("kode_pegawai", kode_pegawai);
            intent.putExtra("pin", binding.etPin.getText().toString());
            startActivity(intent);
        } else if (view.getId() == R.id.btSimpan) {
            updateData(view);
        } else if (view.getId() == R.id.btBatal) {
            setDefaulActivity(false);
            binding.llAction.setVisibility(View.GONE);
            binding.btBukaAkses.setVisibility(View.VISIBLE);
        } else if (view.getId() == R.id.btBukaAkses) {
            setDefaulActivity(true);
            binding.llAction.setVisibility(View.VISIBLE);
            binding.btBukaAkses.setVisibility(View.GONE);
        }
    }

}
