package com.pyramidsoft.siqasir;

import static com.pyramidsoft.siqasir.db.DataBaseHelper.database_name;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.GridLayoutManager;

import com.pyramidsoft.siqasir.adapter.PesananProdukAdapter;
import com.pyramidsoft.siqasir.databinding.ActivityBuatPesananBinding;
import com.pyramidsoft.siqasir.db.DataBaseHelper;
import com.pyramidsoft.siqasir.model.ProdukPesananModel;
import com.pyramidsoft.siqasir.utils.DataPesanan;
import com.pyramidsoft.siqasir.utils.DecimalsFormat;
import com.pyramidsoft.siqasir.utils.GlobalToast;
import com.pyramidsoft.siqasir.utils.ProgressDialog;
import com.pyramidsoft.siqasir.utils.SessionManager;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.Random;

public class BuatPesananAct extends AppCompatActivity {

    private ActivityBuatPesananBinding binding;
    private ArrayList<ProdukPesananModel> list = new ArrayList<>();
    PesananProdukAdapter produkAdapter;
    Calendar c = Calendar.getInstance();
    DataBaseHelper dbHelper;
    SQLiteDatabase mDatabase;
    SessionManager sessionManager;
    ProgressDialog pDialog;

    String invoice;

    public BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String refresh = intent.getStringExtra("refresh");
            switch (refresh) {
                case "pesanan":
                    getDataPesanan();
                    break;
                case "reset":
                    resetAdapter();
                    getDataPesanan();
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityBuatPesananBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setSupportActionBar(binding.toolbar);

        binding.rvListPesanan.setHasFixedSize(true);
        GridLayoutManager layoutManager = new GridLayoutManager(this, 1, GridLayoutManager.VERTICAL, false);
        binding.rvListPesanan.setLayoutManager(layoutManager);

        pDialog = new ProgressDialog(this);
        sessionManager = new SessionManager(this);
        mDatabase = openOrCreateDatabase(database_name, MODE_PRIVATE, null);
        dbHelper = new DataBaseHelper(this);

        binding.IvClose.setOnClickListener(this::onViewClicked);
        binding.tvBatal.setOnClickListener(this::onViewClicked);
        binding.btSimpan.setOnClickListener(this::onViewClicked);
        binding.tvBtTambahItem.setOnClickListener(this::onViewClicked);
        binding.btByrSkrg.setOnClickListener(this::onViewClicked);
        binding.tvBtDataPesanan.setOnClickListener(this::onViewClicked);
        binding.btfilledplgn.setOnClickListener(this::onViewClicked);

        SimpleDateFormat df = new SimpleDateFormat("dd MMMM yyyy", new Locale("id", "ID"));
        String formattedDate = df.format(c.getTime());
        binding.tvTglOrder.setText(formattedDate);
        produkAdapter = new PesananProdukAdapter(DataPesanan.getArrayPesanan(), this);
        binding.rvListPesanan.setAdapter(produkAdapter);

        LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver, new IntentFilter("dataPesanan-state"));
    }

    private void resetAdapter() {
        produkAdapter = new PesananProdukAdapter(DataPesanan.getArrayPesanan(), this);
        binding.rvListPesanan.setAdapter(produkAdapter);
    }

    private void getDataPesanan() {
        if (DataPesanan.getIdMember().isEmpty()) {
            binding.tvnamanmemberplgn.setText(DataPesanan.getNamaPelanggan());
        } else if (DataPesanan.getNamaPelanggan().isEmpty()) {
            binding.tvnamanmemberplgn.setText(DataPesanan.getIdMember());
        } else {
            binding.tvnamanmemberplgn.setText(DataPesanan.getNamaPelanggan() + "  -  " + DataPesanan.getIdMember());
        }
        binding.tvjenisdatang.setText(DataPesanan.getJenisKedatangan());

        if (DataPesanan.getArrayPesanan().size() == 0) {
            binding.tvLblPesanan.setText("Pesanan");
            binding.tvTotalHrg.setText("Rp. 0");
        } else {
            binding.tvLblPesanan.setText("Pesanan (" + DataPesanan.getArrayPesanan().size() + ")");
            binding.tvTotalHrg.setText("Rp. " + DecimalsFormat.priceWithoutDecimal(DataPesanan.getTotalHargaPesanan()));
        }

        if (DataPesanan.isDiskonEmpty()) {
            binding.tvDiskon.setVisibility(View.GONE);
        } else {
            binding.tvDiskon.setVisibility(View.VISIBLE);
            if (DataPesanan.getTipediskon().equals("Nominal")) {
                binding.tvDiskon.setText("- Rp. " + DataPesanan.getDiskon());
            } else {
                binding.tvDiskon.setText("-" + DataPesanan.getDiskon() + "%");
            }
        }

        if (DataPesanan.isPelangganEmpty()) {
            binding.tvBtDataPesanan.setVisibility(View.VISIBLE);
            binding.btfilledplgn.setVisibility(View.GONE);
        } else {
            binding.tvBtDataPesanan.setVisibility(View.GONE);
            binding.btfilledplgn.setVisibility(View.VISIBLE);
        }

        if (DataPesanan.getArrayPesanan().size() == 0) {
            binding.btByrSkrg.setVisibility(View.GONE);
        } else {
            binding.btByrSkrg.setVisibility(View.VISIBLE);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        fillDefaultPelangganIfEmpty();
        getDataPesanan();

        if (DataPesanan.getInvoiceEdit().isEmpty()) {
            String timestamp = new SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(new Date());
            String randomStr = getRandomString(6);
            invoice = "INVC-" + timestamp + randomStr;
        } else {
            invoice = DataPesanan.getInvoiceEdit();
        }

        binding.tvCodeCounter.setText(invoice);
        binding.tvNamaUser.setText(sessionManager.getNama());
    }

    public void onViewClicked(View view) {
        Intent intent;

        if (view.getId() == R.id.IvClose || view.getId() == R.id.tvBtTambahItem || view.getId() == R.id.tvBatal) {
            finish();

        } else if (view.getId() == R.id.btSimpan) {
            if (DataPesanan.isPelangganEmpty()) {
                GlobalToast.ShowToast(this, "Data Pelanggan Kosong");
            } else {
                if (!DataPesanan.getInvoiceEdit().isEmpty()) {
                    dbHelper.deleteDataPenjualan(invoice);
                }
                String idToko = dbHelper.getIdTokoByKodePegawai(sessionManager.getKodeUsers());
                dbHelper.insertDataPesanan(invoice, sessionManager.getKodeUsers(), idToko);
                dbHelper.insetDataDetailPesanan(invoice);
                DataPesanan.setDefaultDataPesanan();
                intent = new Intent("datapesanan-state");
                intent.putExtra("refresh", "reset");
                intent.putExtra("refresh", "gettotaldata");
                LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
                finish();
            }
        } else if (view.getId() == R.id.btByrSkrg) {
            if (DataPesanan.isPelangganEmpty()) {
                GlobalToast.ShowToast(this, "Data Pelanggan Kosong");
            } else {
                intent = new Intent(BuatPesananAct.this, PembayaranAct.class);
                intent.putExtra("invoice", invoice);
                startActivity(intent);
                finish();
            }
        } else if (view.getId() == R.id.tvBtDataPesanan || view.getId() == R.id.btfilledplgn) {
            intent = new Intent(BuatPesananAct.this, DataPemesananAct.class);
            startActivity(intent);
        }
    }

    private static String getRandomString(int i) {
        final String character = "1234567890ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        StringBuilder result = new StringBuilder();
        while (i > 0) {
            Random random = new Random();
            result.append(character.charAt(random.nextInt(character.length())));
            i--;
        }
        return result.toString();
    }

    private String getDefaultJenisKedatangan() {
        String kodePegawai = sessionManager.getKodeUsers();
        String idToko = dbHelper.getIdTokoByKodePegawai(kodePegawai);

        ArrayList<String> list = dbHelper.spinJenisKedatangan(idToko);
        if (list != null && !list.isEmpty()) {
            return list.get(0);
        }
        return "";
    }

    private void fillDefaultPelangganIfEmpty() {

        if (!DataPesanan.isPelangganEmpty()) return;

        String namaKasir = sessionManager.getNama();
        DataPesanan.setNamaPelanggan(namaKasir);

        DataPesanan.setIdMember("");

        String jenisKedatangan = getDefaultJenisKedatangan();
        DataPesanan.setJenisKedatangan(jenisKedatangan);

        DataPesanan.setDiskon("");
        DataPesanan.setTipediskon("");
    }

}
