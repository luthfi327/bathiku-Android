package com.pyramidsoft.siqasir;

import static com.pyramidsoft.siqasir.db.DataBaseHelper.database_name;

import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.pyramidsoft.siqasir.databinding.ActivityBukaKasirBinding;
import com.pyramidsoft.siqasir.db.DataBaseHelper;
import com.pyramidsoft.siqasir.utils.AndLog;
import com.pyramidsoft.siqasir.utils.GlobalToast;
import com.pyramidsoft.siqasir.utils.ProgressDialog;
import com.pyramidsoft.siqasir.utils.SessionManager;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.Random;

public class BukaKasirActivity extends AppCompatActivity {

    private ActivityBukaKasirBinding binding;
    SQLiteDatabase mDatabase;
    SessionManager sessionManager;
    ProgressDialog pDialog;
    DataBaseHelper dbHelper;
    String time, tgl_buka, kdTglBuka, dateAwal;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityBukaKasirBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        pDialog = new ProgressDialog(BukaKasirActivity.this);
        sessionManager = new SessionManager(BukaKasirActivity.this);
        dbHelper = new DataBaseHelper(BukaKasirActivity.this);
        mDatabase = openOrCreateDatabase(database_name, MODE_PRIVATE, null);

        binding.IvClose.setOnClickListener(this::onViewClicked);
        binding.btMasuk.setOnClickListener(this::onViewClicked);

        SimpleDateFormat df = new SimpleDateFormat("EEEE, dd MMMM yyyy, HH:mm", new Locale("id", "ID"));
        dateAwal = df.format(Calendar.getInstance().getTime());

        SimpleDateFormat waktu = new SimpleDateFormat("HH:mm", new Locale("id", "ID"));
        time = waktu.format(Calendar.getInstance().getTime());

        SimpleDateFormat df_tgl_buka = new SimpleDateFormat("dd-MM-yyyy", new Locale("id", "ID"));
        tgl_buka = df_tgl_buka.format(Calendar.getInstance().getTime());

        SimpleDateFormat df_kode_tgl_buka = new SimpleDateFormat("ddMMyy", new Locale("id", "ID"));
        kdTglBuka = df_kode_tgl_buka.format(Calendar.getInstance().getTime());

        binding.tvNamaUser.setText(sessionManager.getNama());
        binding.tvDateBukaToko.setText(dateAwal);

        setupRupiahFormatter();
    }

    private static String getRandomString(int i) {
        final String character = "1234567890ABCDEFGHIKLMNOPQRSTUVWXYZ";
        StringBuilder result = new StringBuilder();
        while (i > 0) {
            Random random = new Random();
            result.append(character.charAt(random.nextInt(character.length())));
            i--;
        }
        return result.toString();
    }

    private void bukaKasir() {
        pDialog.show();

        String modal_awal = binding.etModalAwal.getText()
                .toString()
                .replace(".", "")
                .trim();
        String kode_rekapkas = "BKTOKO" + kdTglBuka + getRandomString(3);
        String kodePegawai = sessionManager.getKodeUsers();
        String idToko = dbHelper.getIdTokoByKodePegawai(kodePegawai);

        ContentValues values = new ContentValues();
        values.put(DataBaseHelper.kode_pegawai, sessionManager.getKodeUsers());
        values.put(DataBaseHelper.id_toko, idToko);
        values.put(DataBaseHelper.kode_rekapkas, kode_rekapkas);
        values.put(DataBaseHelper.status_rekap_kas, "Terbuka");
        values.put(DataBaseHelper.waktu_buka, time);
        values.put(DataBaseHelper.waktu_tutup, "-");
        values.put(DataBaseHelper.tgl_buka, tgl_buka);
        values.put(DataBaseHelper.tgl_tutup, "-");
        values.put(DataBaseHelper.date_awal, dateAwal);
        values.put(DataBaseHelper.date_tutup, "-");

        ContentValues values2 = new ContentValues();
        values2.put(DataBaseHelper.kode_rekapkas, kode_rekapkas);
        values2.put(DataBaseHelper.total_modalawal, modal_awal);

        if (modal_awal.equals("")) {

            GlobalToast.ShowToast(this, "Data Tidak Boleh Kosong");
            pDialog.dismiss();

        } else {

            dbHelper.bukaKasir(values);
            dbHelper.insertModalAwal(values2);
            sessionManager.setKodeRekapkas(kode_rekapkas);
            AndLog.ShowLog("dataRekapkas: ", String.valueOf(values));

            pDialog.dismiss();

        }
    }

    private void setupRupiahFormatter() {
        binding.etModalAwal.addTextChangedListener(new android.text.TextWatcher() {

            private String current = "";

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(android.text.Editable s) {
                if (!s.toString().equals(current)) {
                    binding.etModalAwal.removeTextChangedListener(this);

                    String cleanString = s.toString().replaceAll("[^\\d]", "");

                    if (!cleanString.isEmpty()) {
                        long parsed = Long.parseLong(cleanString);
                        java.text.NumberFormat formatter =
                                java.text.NumberFormat.getInstance(new Locale("id", "ID"));
                        formatter.setMaximumFractionDigits(0);
                        String formatted = formatter.format(parsed);

                        current = formatted;
                        binding.etModalAwal.setText(formatted);
                        binding.etModalAwal.setSelection(formatted.length());
                    } else {
                        current = "";
                    }

                    binding.etModalAwal.addTextChangedListener(this);
                }
            }
        });
    }

    public void onViewClicked(View view) {
        int viewId = view.getId();

        if (viewId == R.id.IvClose) {
            finish();

        } else if (viewId == R.id.btMasuk) {

            String modalAwal = binding.etModalAwal.getText()
                    .toString()
                    .replace(".", "")
                    .trim();

            if (modalAwal.isEmpty() || modalAwal.equals("0")) {
                binding.etModalAwal.setError("Modal awal wajib diisi");
                binding.etModalAwal.requestFocus();
                return;
            }

            sessionManager.setBukaKasir("open");
            bukaKasir();
            GlobalToast.ShowToast(this, "Buka Toko");
            finish();
        }
    }


}
