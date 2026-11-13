package com.pyramidsoft.siqasir;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.pyramidsoft.siqasir.adapter.ListDiskonProdukAtPembayaran;
import com.pyramidsoft.siqasir.adapter.ListProdukAtPembayaranAdapter;
import com.pyramidsoft.siqasir.databinding.ActivityPembayaranBinding;
import com.pyramidsoft.siqasir.db.DataBaseHelper;
import com.pyramidsoft.siqasir.utils.AndLog;
import com.pyramidsoft.siqasir.utils.DataPesanan;
import com.pyramidsoft.siqasir.utils.DecimalsFormat;
import com.pyramidsoft.siqasir.utils.SessionManager;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.Random;

public class PembayaranAct extends AppCompatActivity {

    private ActivityPembayaranBinding binding;
    SessionManager sessionManager;
    Calendar c = Calendar.getInstance();
    String nilaiBayar = "0";
    String jenisBayar = "";
    String kembalian = "0";
    DataBaseHelper dbHelper;
    String invoice;

    public BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String refresh = intent.getStringExtra("refresh");
            if (refresh.equals("pesanan")) {
                getDataPesanan();
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityPembayaranBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        sessionManager = new SessionManager(PembayaranAct.this);

        ActionBar actionBar = getSupportActionBar();

        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        dbHelper = new DataBaseHelper(this);

        binding.tvBtDetailPemesanan.setOnClickListener(this::onViewClicked);
        binding.tvBtCash.setOnClickListener(this::onViewClicked);
        binding.tvBtEdc.setOnClickListener(this::onViewClicked);
        binding.tvBtTrfBank.setOnClickListener(this::onViewClicked);
        binding.tvBtVoucher.setOnClickListener(this::onViewClicked);
        binding.btBayar.setOnClickListener(this::onViewClicked);

        binding.etUangBayar.addTextChangedListener(new TextWatcher() {
            private String current = "";

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                if (!s.toString().equals(current)) {
                    binding.etUangBayar.removeTextChangedListener(this);

                    try {
                        String cleanString = s.toString().replaceAll("[^0-9]", "");
                        if (cleanString.isEmpty()) cleanString = "0";

                        long parsed = Long.parseLong(cleanString);

                        // format ribuan saja, tanpa "Rp"
                        DecimalFormatSymbols symbols = new DecimalFormatSymbols(new Locale("id", "ID"));
                        symbols.setGroupingSeparator('.');
                        DecimalFormat formatter = new DecimalFormat("#,###", symbols);

                        String formatted = formatter.format(parsed);

                        current = formatted;
                        binding.etUangBayar.setText(formatted);
                        binding.etUangBayar.setSelection(formatted.length());

                        nilaiBayar = cleanString; // angka murni untuk hitung kembalian

                        binding.relPembayaran.setBackgroundResource(R.drawable.bg_active);

                    } catch (Exception e) {}

                    binding.etUangBayar.addTextChangedListener(this);
                }

                setKembalian();
            }
        });


        LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver, new IntentFilter("dataPesanan-state"));

        if (getIntent().getStringExtra("invoice") != null) {
            invoice = getIntent().getStringExtra("invoice");
            AndLog.ShowLog("invoice", invoice);
        }

        binding.btn2k.setOnClickListener(v -> isiUangBayar(2000));
        binding.btn5k.setOnClickListener(v -> isiUangBayar(5000));
        binding.btn10k.setOnClickListener(v -> isiUangBayar(10000));
        binding.btn20k.setOnClickListener(v -> isiUangBayar(20000));
        binding.btn50k.setOnClickListener(v -> isiUangBayar(50000));
        binding.btn100k.setOnClickListener(v -> isiUangBayar(100000));
        binding.btnPas.setOnClickListener(v -> isiUangPas());
        binding.btnClear.setOnClickListener(v -> clearUangBayar());

    }

    private void setKembalian() {
        int totalpesanan = 0;
        if (DataPesanan.isDiskonEmpty()) {
            totalpesanan = Integer.parseInt(DataPesanan.getTotalHargaPesanan());
        } else {
            totalpesanan = Integer.parseInt(DataPesanan.getTotalHargawithDiskon());
        }
        int intKembalian = Integer.parseInt(
                nilaiBayar.replaceAll("[^0-9]", "")
        ) - totalpesanan;
        kembalian = String.valueOf(intKembalian);
        binding.tvUangKembalian.setText("Rp. " + DecimalsFormat.priceWithoutDecimal(String.valueOf(intKembalian)));
    }

    private void getDataPesanan() {
        if (DataPesanan.isDiskonEmpty()) {
            binding.tvTotalHrg.setText("Rp. " + DecimalsFormat.priceWithoutDecimal(DataPesanan.getTotalHargaPesanan()));
        } else {
            binding.tvTotalHrg.setText("Rp. " + DecimalsFormat.priceWithoutDecimal(DataPesanan.getTotalHargawithDiskon()));
        }
    }

    private void isiUangBayar(int amount) {
        try {
            long currentValue = Long.parseLong(
                    nilaiBayar.replaceAll("[^0-9]", "")
            );

            long newValue = currentValue + amount;

            binding.etUangBayar.setText(String.valueOf(newValue));

            binding.etUangBayar.setSelection(binding.etUangBayar.getText().length());

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void isiUangPas() {
        int totalPesanan = 0;
        if (DataPesanan.isDiskonEmpty()) {
            totalPesanan = Integer.parseInt(DataPesanan.getTotalHargaPesanan());
        } else {
            totalPesanan = Integer.parseInt(DataPesanan.getTotalHargawithDiskon());
        }
        binding.etUangBayar.setText(String.valueOf(totalPesanan));
        binding.etUangBayar.setSelection(binding.etUangBayar.getText().length());
    }

    private String cleanNumber(String value) {
        if (value == null) return "0";
        return value.replace(".", "").replace(",", "");
    }

    private void clearUangBayar() {
        binding.etUangBayar.setText("");
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

    @Override
    protected void onResume() {
        super.onResume();
        getDataPesanan();
        setKembalian();
    }

    public void onViewClicked(View view) {
        if (view.getId() == R.id.tvBtDetailPemesanan) {
            final BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(PembayaranAct.this, R.style.BottomShetDialogTheme);
            View bottomSheetView = LayoutInflater.from(PembayaranAct.this).inflate(R.layout.lsb_detail_pesanan, PembayaranAct.this.findViewById(R.id.sv_detail_pesanan));
            bottomSheetDialog.setContentView(bottomSheetView);

            TextView tvTglOrder = bottomSheetDialog.findViewById(R.id.tvTglOrder);
            TextView tvkodeCounter = bottomSheetDialog.findViewById(R.id.tvkodeCounter);
            TextView tvNamaPegawai = bottomSheetDialog.findViewById(R.id.tvNamaPegawai);
            TextView tvIdMember = bottomSheetDialog.findViewById(R.id.tvIdMember);
            TextView tvLblPesanan = bottomSheetDialog.findViewById(R.id.tvLblPesanan);
            RecyclerView rvListPesanan = bottomSheetDialog.findViewById(R.id.rvListPesanan);
            TextView tvSubTotal = bottomSheetDialog.findViewById(R.id.tvSubTotal);
            RecyclerView rvListDiskonProduk = bottomSheetDialog.findViewById(R.id.rvListDiskonProduk);
            TextView tvDiskon = bottomSheetDialog.findViewById(R.id.tvDiskon);
            TextView tvTotalBayar = bottomSheetDialog.findViewById(R.id.tvTotalBayar);
            RelativeLayout layoutIdMember = bottomSheetDialog.findViewById(R.id.layoutIdMember);

            ListProdukAtPembayaranAdapter adapter = new ListProdukAtPembayaranAdapter(this, DataPesanan.getArrayPesanan());
            ListDiskonProdukAtPembayaran adapterDiskon = new ListDiskonProdukAtPembayaran(this, DataPesanan.getArrayDiskonProduk());
            SimpleDateFormat df = new SimpleDateFormat("dd MMMM yyyy", new Locale("id", "ID"));
            String formattedDate = df.format(c.getTime());

            rvListPesanan.setHasFixedSize(true);
            GridLayoutManager layoutManager = new GridLayoutManager(this, 1,
                    GridLayoutManager.VERTICAL, false);
            rvListPesanan.setLayoutManager(layoutManager);
            rvListPesanan.setAdapter(adapter);

            rvListDiskonProduk.setHasFixedSize(true);
            GridLayoutManager layoutManager2 = new GridLayoutManager(this, 1,
                    GridLayoutManager.VERTICAL, false);
            rvListDiskonProduk.setLayoutManager(layoutManager2);
            rvListDiskonProduk.setAdapter(adapterDiskon);


            tvNamaPegawai.setText(sessionManager.getNama());
            tvkodeCounter.setText(invoice);

            tvLblPesanan.setText("Pesanan (" + DataPesanan.getArrayPesanan().size() + ")");
            tvTglOrder.setText(formattedDate);
            if (DataPesanan.isPelangganEmpty()) {
                layoutIdMember.setVisibility(View.GONE);
            } else {
                tvIdMember.setText(DataPesanan.getIdMember());
                layoutIdMember.setVisibility(View.VISIBLE);
            }
            tvSubTotal.setText("Rp. " + DecimalsFormat.priceWithoutDecimal(DataPesanan.getTotalHargaSaleProduk()));

            if (DataPesanan.isDiskonEmpty()) {
                tvTotalBayar.setText("Rp. " + DecimalsFormat.priceWithoutDecimal(DataPesanan.getTotalHargaPesanan()));
            } else {
                if (DataPesanan.getTipediskon().equals("Persen")) {
                    tvDiskon.setText("-" + DataPesanan.getDiskon() + "%");
                    tvTotalBayar.setText("Rp. " + DecimalsFormat.priceWithoutDecimal(DataPesanan.getTotalHargawithDiskon()));
                } else if (DataPesanan.getTipediskon().equals("Nominal")) {
                    tvDiskon.setText("-Rp. " + DecimalsFormat.priceWithoutDecimal(DataPesanan.getDiskon()));
                    tvTotalBayar.setText("Rp. " + DecimalsFormat.priceWithoutDecimal(DataPesanan.getTotalHargawithDiskon()));
                }
            }

            bottomSheetDialog.show();

        } else if (view.getId() == R.id.tvBtCash) {
            binding.tvBtCash.setBackgroundResource(R.drawable.rounded_paymetode_active);
            binding.tvBtCash.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_active_16_px_cash, 0, R.drawable.ic_24_px_check, 0);
            binding.tvBtCash.setTextColor(Color.parseColor("#13b03f"));

            binding.tvBtEdc.setBackgroundResource(R.drawable.rounded_white);
            binding.tvBtEdc.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_idle_16_px_edc, 0, 0, 0);
            binding.tvBtEdc.setTextColor(Color.parseColor("#000000"));

            binding.tvBtTrfBank.setBackgroundResource(R.drawable.rounded_white);
            binding.tvBtTrfBank.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_idle_16_px_bank, 0, 0, 0);
            binding.tvBtTrfBank.setTextColor(Color.parseColor("#000000"));

            binding.tvBtVoucher.setBackgroundResource(R.drawable.rounded_white);
            binding.tvBtVoucher.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_idle_16_px_voucher, 0, 0, 0);
            binding.tvBtVoucher.setTextColor(Color.parseColor("#000000"));

            sessionManager.setNamaMenuActive("Cash");
            jenisBayar = "Cash";

        } else if (view.getId() == R.id.tvBtEdc) {
            binding.tvBtEdc.setBackgroundResource(R.drawable.rounded_paymetode_active);
            binding.tvBtEdc.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_active_16_px_edc, 0, R.drawable.ic_24_px_check, 0);
            binding.tvBtEdc.setTextColor(Color.parseColor("#13b03f"));

            binding.tvBtCash.setBackgroundResource(R.drawable.rounded_white);
            binding.tvBtCash.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_idle_16_px_cash, 0, 0, 0);
            binding.tvBtCash.setTextColor(Color.parseColor("#000000"));

            binding.tvBtTrfBank.setBackgroundResource(R.drawable.rounded_white);
            binding.tvBtTrfBank.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_idle_16_px_bank, 0, 0, 0);
            binding.tvBtTrfBank.setTextColor(Color.parseColor("#000000"));

            binding.tvBtVoucher.setBackgroundResource(R.drawable.rounded_white);
            binding.tvBtVoucher.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_idle_16_px_voucher, 0, 0, 0);
            binding.tvBtVoucher.setTextColor(Color.parseColor("#000000"));

            sessionManager.setNamaMenuActive("EDC");
            jenisBayar = "EDC";

        } else if (view.getId() == R.id.tvBtTrfBank) {
            binding.tvBtTrfBank.setBackgroundResource(R.drawable.rounded_paymetode_active);
            binding.tvBtTrfBank.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_active_16_px_bank, 0, R.drawable.ic_24_px_check, 0);
            binding.tvBtTrfBank.setTextColor(Color.parseColor("#13b03f"));

            binding.tvBtCash.setBackgroundResource(R.drawable.rounded_white);
            binding.tvBtCash.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_idle_16_px_cash, 0, 0, 0);
            binding.tvBtCash.setTextColor(Color.parseColor("#000000"));

            binding.tvBtEdc.setBackgroundResource(R.drawable.rounded_white);
            binding.tvBtEdc.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_idle_16_px_edc, 0, 0, 0);
            binding.tvBtEdc.setTextColor(Color.parseColor("#000000"));

            binding.tvBtVoucher.setBackgroundResource(R.drawable.rounded_white);
            binding.tvBtVoucher.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_idle_16_px_voucher, 0, 0, 0);
            binding.tvBtVoucher.setTextColor(Color.parseColor("#000000"));

            sessionManager.setNamaMenuActive("Trf");
            jenisBayar = "Trf";

        } else if (view.getId() == R.id.tvBtVoucher) {
            binding.tvBtVoucher.setBackgroundResource(R.drawable.rounded_paymetode_active);
            binding.tvBtVoucher.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_active_16_px_voucher, 0, R.drawable.ic_24_px_check, 0);
            binding.tvBtVoucher.setTextColor(Color.parseColor("#13b03f"));

            binding.tvBtCash.setBackgroundResource(R.drawable.rounded_white);
            binding.tvBtCash.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_idle_16_px_cash, 0, 0, 0);
            binding.tvBtCash.setTextColor(Color.parseColor("#000000"));

            //edc
            binding.tvBtEdc.setBackgroundResource(R.drawable.rounded_white);
            binding.tvBtEdc.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_idle_16_px_edc, 0, 0, 0);
            binding.tvBtEdc.setTextColor(Color.parseColor("#000000"));

            //trf
            binding.tvBtTrfBank.setBackgroundResource(R.drawable.rounded_white);
            binding.tvBtTrfBank.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_idle_16_px_bank, 0, 0, 0);
            binding.tvBtTrfBank.setTextColor(Color.parseColor("#000000"));

            sessionManager.setNamaMenuActive("Voucher");
            jenisBayar = "Voucher";

        } else if (view.getId() == R.id.btBayar) {

            String dibayarView = binding.etUangBayar.getText().toString();

            String dibayarClean = cleanNumber(dibayarView);
            String kembalianClean = cleanNumber(kembalian);

            int intKembalian = Integer.parseInt(kembalianClean);

            if (intKembalian >= 0 && !jenisBayar.isEmpty()) {

                if (!DataPesanan.getInvoiceEdit().isEmpty()) {
                    dbHelper.deleteDataPenjualan(invoice);
                }

                String idToko = dbHelper.getIdTokoByKodePegawai(
                        sessionManager.getKodeUsers()
                );

                dbHelper.insertDataPenjualan(
                        sessionManager.getKodeRekapkas(),
                        sessionManager.getKodeUsers(),
                        invoice,
                        jenisBayar,
                        dibayarClean,
                        kembalianClean,
                        idToko
                );

                dbHelper.insetDataDetailPenjualan(invoice);

                Intent intent = new Intent(
                        PembayaranAct.this,
                        PembayaranSuksesAct.class
                );
                intent.putExtra("invoice", invoice);
                intent.putExtra("dibayar", dibayarView); // ⬅️ ada titik
                intent.putExtra("kembalian", kembalian); // ⬅️ ada titik

                startActivity(intent);
                finish();
            }
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

}
