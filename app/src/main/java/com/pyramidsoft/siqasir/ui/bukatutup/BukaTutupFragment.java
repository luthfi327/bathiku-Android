package com.pyramidsoft.siqasir.ui.bukatutup;

import static android.content.Context.MODE_PRIVATE;
import static com.pyramidsoft.siqasir.db.DataBaseHelper.database_name;
import com.pyramidsoft.siqasir.utils.ApiKeyHttpClient;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.pyramidsoft.siqasir.BukaKasirActivity;
import com.pyramidsoft.siqasir.HomeMainActivity;
import com.pyramidsoft.siqasir.R;
import com.pyramidsoft.siqasir.adapter.JenisPembayaranAdapter;
import com.pyramidsoft.siqasir.databinding.FragmentBukatutupBinding;
import com.pyramidsoft.siqasir.db.DataBaseHelper;
import com.pyramidsoft.siqasir.model.JenisPembayaranModels;
import com.pyramidsoft.siqasir.utils.AndLog;
import com.pyramidsoft.siqasir.utils.DecimalsFormat;
import com.pyramidsoft.siqasir.utils.GlobalToast;
import com.pyramidsoft.siqasir.utils.ProgressDialog;
import com.pyramidsoft.siqasir.utils.SessionManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class BukaTutupFragment extends Fragment {

    private FragmentBukatutupBinding binding;
    private EditText mPinView;
    JenisPembayaranAdapter adapter;
    Activity activity;
    TextView tvBatal, tvPreview, toDecimal1, toDecimal2;
    Button btIya;
    String pinsandi, status_pin;

    SQLiteDatabase mDatabase;
    SessionManager sessionManager;
    ProgressDialog pDialog;
    DataBaseHelper dbHelper;

    int modal_awal = 0;
    int penjualan = 0;
    String kas_keluar = "0";
    int penerimaan_sistem = 0;
    int tunai = 0;
    String non_tunai = "0";
    int total_penerimaan_aktual = 0;
    int selisih = 0;

    EditText etTunai, etNonTunai, etKasKeluar, etKeperluan;
    TextView tvBatalTutupToko, tvBatal_kaskeluar;
    Button btSimpanTutupToko, btSimpan_kaskeluar;

    private String kode_rekapkas;
    private String enteredPin = "";

    private AlertDialog loadingDialog;

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentBukatutupBinding.inflate(inflater, container, false);
        View view = binding.getRoot();
        activity = getActivity();

        if (activity != null) {
            tvPreview = activity.findViewById(R.id.tvPreview);

        }

        pDialog = new ProgressDialog(activity);
        sessionManager = new SessionManager(activity);
        dbHelper = new DataBaseHelper(activity);
        mDatabase = activity.openOrCreateDatabase(database_name, MODE_PRIVATE, null);

        binding.tvNamaUser.setText(sessionManager.getNama());

        getPinSandi();

        binding.rvJenisPembayaran.setHasFixedSize(true);
        GridLayoutManager layoutManager = new GridLayoutManager(activity, 1, GridLayoutManager.VERTICAL, false);
        binding.rvJenisPembayaran.setLayoutManager(layoutManager);

        binding.btBukaKasir.setOnClickListener(this::onViewClicked);
        binding.btTutupKasir.setOnClickListener(this::onViewClicked);
        binding.btKasKeluar.setOnClickListener(this::onViewClicked);

        kode_rekapkas = sessionManager.getKodeRekapkas();
        dataRekapKas(kode_rekapkas);

        return view;
    }

    public void dataPenjualan(String kode_rekapkas) {

        Cursor cursorproduct = dbHelper.dataPenjualan(kode_rekapkas);

        ArrayList<JenisPembayaranModels> data = new ArrayList<>();

        if (cursorproduct.moveToFirst()) {
            do {
                data.add(new JenisPembayaranModels(
                        cursorproduct.getString(0),
                        cursorproduct.getInt(1)

                ));
            } while (cursorproduct.moveToNext());
        }
        if (data.isEmpty()) {
        }

        for (int i = 0; i < data.size(); i++) {
            penjualan += data.get(i).getTotal();
            sessionManager.setPenjualan(String.valueOf(penjualan));
        }

        cursorproduct.close();

        adapter = new JenisPembayaranAdapter(data, activity, mDatabase);

        binding.rvJenisPembayaran.setAdapter(adapter);

    }

    private void DialogForm() {
        final Dialog dialog = new Dialog(activity);
        dialog.setContentView(R.layout.dialog_question_tutupkasir);
        tvBatal = (TextView) dialog.findViewById(R.id.tvBatal);
        btIya = (Button) dialog.findViewById(R.id.btIya);

        btIya.setOnClickListener(new View.OnClickListener() {
            DialogInterface dialogInterface;

            @Override
            public void onClick(View v) {
                dialog.dismiss();
                insertRekapKasDetail();

            }
        });

        tvBatal.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
                GlobalToast.ShowToast(activity, "Batal");
            }
        });

        dialog.show();
    }

    @Override
    public void onResume() {
        super.onResume();

        dataRekapKas(kode_rekapkas);

    }

    private void dataRekapKas(String kode_rekapkas) {
        if (sessionManager.getBukaKasir().equals("close")) {

            DateFormat df = new SimpleDateFormat("EEEE, dd MMMM yyyy, HH:mm");
            String date = df.format(Calendar.getInstance().getTime());

            binding.tvDateBukaToko.setText(date);
            binding.btBukaKasir.setVisibility(View.VISIBLE);
            binding.btTutupKasir.setVisibility(View.GONE);
            binding.btTutupKasir.setVisibility(View.GONE);
            binding.btKasKeluar.setVisibility(View.GONE);
        } else {

            Cursor cursor = dbHelper.dataRekapKas(kode_rekapkas);

            if (cursor.moveToFirst()) {
                String date_awal = cursor.getString(cursor.getColumnIndexOrThrow(DataBaseHelper.date_awal));
                String total_modalawal = cursor.getString(cursor.getColumnIndexOrThrow(DataBaseHelper.total_modalawal));

                modal_awal = Integer.parseInt(total_modalawal);
                sessionManager.setModalAwal(String.valueOf(modal_awal));
                binding.tvModalAwal.setText("Rp" + DecimalsFormat.priceWithoutDecimal(total_modalawal));
                binding.tvDateBukaToko.setText(date_awal);
            }

            binding.llTutuptokoDate.setVisibility(View.GONE);
            binding.llTutuptoko.setVisibility(View.GONE);
            binding.btBukaKasir.setVisibility(View.GONE);
            binding.btTutupKasir.setVisibility(View.VISIBLE);
            binding.btKasKeluar.setVisibility(View.VISIBLE);
        }

    }

    private void getPinSandi() {

        Cursor cursor = dbHelper.getDetailPegawai(sessionManager.getKodeUsers());

        if (cursor.moveToFirst()) {
            String pin = cursor.getString(cursor.getColumnIndexOrThrow(DataBaseHelper.pin));
            pinsandi = pin;
        }

    }

    private void getkasKeluar(String kode_rekapkas) {
        pDialog.show();
        Cursor cursor = dbHelper.kasKeluar(kode_rekapkas);

        if (cursor.moveToFirst()) {
            String nominal = cursor.getString(cursor.getColumnIndexOrThrow(DataBaseHelper.nominal));

            if (nominal == null){
                sessionManager.setKasKeluar("0");
            }else {
                sessionManager.setKasKeluar(nominal);
            }
            binding.tvPengeluaranKasir.setText("Rp" + DecimalsFormat.priceWithoutDecimal(nominal));
        }

        dataPenjualan(kode_rekapkas);

    }

    private void insertRekapKasDetail() {
        final Dialog dialog = new Dialog(activity);
        dialog.setContentView(R.layout.dialog_pengisian_data_tutup_toko);
        TextView toDecimal1, toDecimal2;
        toDecimal1 = (TextView) dialog.findViewById(R.id.tvTextToDecimal1);
        toDecimal2 = (TextView) dialog.findViewById(R.id.tvTextToDecimal2);
        tvBatalTutupToko = (TextView) dialog.findViewById(R.id.tvBatal);
        etTunai = (EditText) dialog.findViewById(R.id.etTunai);
        etNonTunai = (EditText) dialog.findViewById(R.id.etNonTunai);
        btSimpanTutupToko = (Button) dialog.findViewById(R.id.btSimpan);
        etNonTunai.setText("0");
        kas_keluar = "0";
        etTunai.setText("0");

        etTunai.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() > 0) {
                    toDecimal1.setText("Rp" + DecimalsFormat.priceWithoutDecimal(etTunai.getText().toString()));
                }

            }

            @Override
            public void afterTextChanged(Editable s) {
                if (s.length() < 0) {
                    etTunai.setText("0");
                }
            }
        });

        etNonTunai.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() > 0) {
                    toDecimal2.setText("Rp" + DecimalsFormat.priceWithoutDecimal(etNonTunai.getText().toString()));
                }else {
                    etNonTunai.setText("0");
                }

            }

            @Override
            public void afterTextChanged(Editable s) {
                if (s.length() < 0) {
                    etNonTunai.setText("0");
                }
            }
        });

        tvBatalTutupToko.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });

        btSimpanTutupToko.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                sessionManager.setPenerimaanAktual(etTunai.getText().toString().trim(), etNonTunai.getText().toString().trim());

                getkasKeluar(kode_rekapkas);

                tunai = Integer.parseInt(sessionManager.getTunai());
                AndLog.ShowLog("tunai :", String.valueOf(tunai));

                non_tunai = String.valueOf(sessionManager.getNonTunai());
                AndLog.ShowLog("non_tunai", non_tunai);

                total_penerimaan_aktual = tunai + Integer.parseInt(non_tunai);

                AndLog.ShowLog("total_penerimaan_aktual :", String.valueOf(total_penerimaan_aktual));

                sessionManager.setTotalPenerimaanAktual(String.valueOf(total_penerimaan_aktual));

                try {
                    penjualan = Integer.parseInt(sessionManager.getPenjualan());
                    modal_awal = Integer.parseInt(sessionManager.getModalAwal());

                    kas_keluar = String.valueOf(sessionManager.getKasKeluar());
                    AndLog.ShowLog("kas_keluar", kas_keluar);

                    penerimaan_sistem = penjualan + modal_awal - Integer.parseInt(kas_keluar);
                    sessionManager.setPenerimaanSistem(String.valueOf(penerimaan_sistem));

                    selisih = penerimaan_sistem - total_penerimaan_aktual;
                    sessionManager.setSelisih(String.valueOf(selisih));

                } catch (NumberFormatException e) {

                }

                final BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(activity, R.style.BottomShetDialogTheme);
                View bottomSheetView = LayoutInflater.from(activity).inflate(R.layout.lsb_verify_pin, activity.findViewById(R.id.ll_bottom_sheet));

                tvPreview = bottomSheetView.findViewById(R.id.tvPreview);

                mPinView = bottomSheetView.findViewById(R.id.pin_view);
                mPinView.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD);

                mPinView.addTextChangedListener(new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                    }

                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) {
                        enteredPin = s.toString();
                        if (pinsandi != null && pinsandi.equals(enteredPin)) {
                            tvPreview.setText("benar");
                            status_pin = "benar";
                        } else {
                            tvPreview.setText("");
                            status_pin = "salah";
                        }
                    }

                    @Override
                    public void afterTextChanged(Editable s) {
                    }
                });

                bottomSheetView.findViewById(R.id.btKonfirmasi).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (status_pin.equals("salah")) {
                            GlobalToast.ShowToast(activity, "Pin Salah!");
                        } else {

                            ContentValues values2 = new ContentValues();
                            values2.put(DataBaseHelper.total_penjualan, sessionManager.getPenjualan() != null ? sessionManager.getPenjualan().trim() : "0");
                            values2.put(DataBaseHelper.total_kaskeluar, sessionManager.getKasKeluar() != null ? sessionManager.getKasKeluar().trim() : "0");
                            values2.put(DataBaseHelper.penerimaan_sistem, sessionManager.getPenerimaanSistem() != null ? sessionManager.getPenerimaanSistem().trim() : "0");
                            values2.put(DataBaseHelper.penerimaan_tunai, sessionManager.getTunai() != null ? sessionManager.getTunai().trim() : "0");
                            values2.put(DataBaseHelper.penerimaan_nontunai, sessionManager.getNonTunai() != null ? sessionManager.getNonTunai().trim() : "0");
                            values2.put(DataBaseHelper.selisih, sessionManager.getSelisih() != null ? sessionManager.getSelisih().trim() : "0");

                            dbHelper.rekapKasTutupKasir(sessionManager.getKodeRekapkas());
                            dbHelper.updateRekapKasDetail(values2, sessionManager.getKodeRekapkas());
                            sessionManager.tutupToko();
                            sessionManager.setBukaKasir("close");
                            bottomSheetDialog.dismiss();
                            pDialog.dismiss();
                            HomeMainActivity.navController.navigate(R.id.nav_tutup_kasir);
                            GlobalToast.ShowToast(activity, "Data Tutup Toko Berhasil Ditambahkan");
                            uploadToServer();
                        }
                    }
                });

                bottomSheetDialog.setContentView(bottomSheetView);
                bottomSheetDialog.show();
                dialog.dismiss();

            }
        });
        dialog.show();
    }

    private void tambahKasKeluar() {
        final Dialog dialog = new Dialog(activity);
        dialog.setContentView(R.layout.dialog_kas_keluar);
        TextView toDecimal1;
        toDecimal1 = (TextView) dialog.findViewById(R.id.tvTextToDecimal1);

        etKasKeluar = (EditText) dialog.findViewById(R.id.etKasKeluar);
        etKeperluan = (EditText) dialog.findViewById(R.id.etKeperluan);
        btSimpan_kaskeluar = (Button) dialog.findViewById(R.id.btSimpan);
        tvBatal_kaskeluar = (TextView) dialog.findViewById(R.id.tvBatal);

        etKasKeluar.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                toDecimal1.setText("Rp" + DecimalsFormat.priceWithoutDecimal(etKasKeluar.getText().toString()));
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        tvBatal_kaskeluar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });

        btSimpan_kaskeluar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String kodePegawai = sessionManager.getKodeUsers();
                String idToko = dbHelper.getIdTokoByKodePegawai(kodePegawai);

                dbHelper.insertKasKeluar(
                        sessionManager.getKodeRekapkas().trim(),
                        etKasKeluar.getText().toString().trim(),
                        etKeperluan.getText().toString().trim(),
                        idToko, // tambahan id_toko
                        kodePegawai
                );

                dialog.dismiss();
                GlobalToast.ShowToast(activity, "Kas Keluar Berhasil Ditambahkan");

            }
        });
        dialog.show();
    }

    private void flushWalToDb() {
        if (!isAdded()) return; // Fragment belum attach

        DataBaseHelper helper = new DataBaseHelper(requireContext());
        SQLiteDatabase db = helper.getWritableDatabase();

        Cursor cursor = null;
        try {
            cursor = db.rawQuery("PRAGMA wal_checkpoint(FULL);", null);
            if (cursor.moveToFirst()) {
                Log.d("WAL",
                        "checkpointed=" + cursor.getInt(0) +
                                " logSize=" + cursor.getInt(1) +
                                " frames=" + cursor.getInt(2)
                );
            }
        } catch (Exception e) {
            Log.e("WAL", "Checkpoint error", e);
        } finally {
            if (cursor != null) cursor.close();
            db.close(); // ✔️ WAJIB ditutup
        }
    }

    private static final String TAG = "UPLOAD_DB_API";

    private Object mapToJson(Object data) throws JSONException {
        if (data instanceof Map) {
            JSONObject json = new JSONObject();
            Map<?, ?> map = (Map<?, ?>) data;
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                json.put(String.valueOf(entry.getKey()), mapToJson(entry.getValue()));
            }
            return json;
        } else if (data instanceof List) {
            JSONArray array = new JSONArray();
            for (Object item : (List<?>) data) {
                array.put(mapToJson(item));
            }
            return array;
        } else {
            return data == null ? JSONObject.NULL : data;
        }
    }

    private void uploadToServer() {

        if (!isAdded()) return;

        showLoading("Mengupload database ke server...");
        flushWalToDb();

        try {
            File dbFile = requireActivity().getDatabasePath("kasir.db");
            if (!dbFile.exists()) {
                Toast.makeText(requireContext(), "Database tidak ditemukan!", Toast.LENGTH_LONG).show();
                return;
            }

            SessionManager sessionManager = new SessionManager(requireContext());
            String kodePegawai = sessionManager.getKodeUsers();
            String namaToko = sessionManager.getNamatoko();
            String kodeOwner = sessionManager.getKodeOwnerutama();

            DataBaseHelper dbHelper = new DataBaseHelper(requireContext());
            String idToko = dbHelper.getIdTokoByKodePegawai(kodePegawai);
            String pegawaiDataJson = dbHelper.getPegawaiDataJson(kodePegawai);

            String kodeFranchise = "FR-" + kodePegawai + "-" + kodeOwner;

            // ===== BACA DB =====
            byte[] dbBytes = new byte[(int) dbFile.length()];
            try (FileInputStream fis = new FileInputStream(dbFile)) {
                fis.read(dbBytes);
            }
            String dbBase64 = Base64.encodeToString(dbBytes, Base64.NO_WRAP);

            JSONObject jsonPayload = new JSONObject();
            jsonPayload.put("kode_franchise", kodeFranchise);
            jsonPayload.put("id_toko", idToko);
            jsonPayload.put("nama_toko", namaToko);
            jsonPayload.put("kode_owner", kodeOwner);
            jsonPayload.put("pegawai_data", new JSONObject(pegawaiDataJson));
            jsonPayload.put("file_name", kodeFranchise + ".db");
            jsonPayload.put("file_data", dbBase64);

            Log.d(TAG, "UPLOAD PAYLOAD:");
            Log.d(TAG, jsonPayload.toString().replace(dbBase64, "[BASE64_DB_DATA]"));

            OkHttpClient client = ApiKeyHttpClient.get();
            RequestBody requestBody = RequestBody.create(
                    jsonPayload.toString(),
                    MediaType.parse("application/json; charset=utf-8")
            );

            Request request = new Request.Builder()
                    .url("https://siqasir.awandigital.id/api/upload-db")
                    .post(requestBody)
                    .build();

            new Thread(() -> {
                try (Response response = client.newCall(request).execute()) {

                    String resBody = response.body() != null ? response.body().string() : "null";
                    Log.d(TAG, "UPLOAD RESPONSE: " + resBody);

                    if (!response.isSuccessful()) {
                        requireActivity().runOnUiThread(() -> {
                            hideLoading();
                            Toast.makeText(requireContext(), "Upload gagal: " + resBody, Toast.LENGTH_LONG).show();
                        });
                        return;
                    }

                    requireActivity().runOnUiThread(() ->
                            showLoading("Upload selesai. Merge data...")
                    );

                    // ======================
                    // MERGE
                    // ======================
                    String mergeUrl = "https://siqasir.awandigital.id/api/merge/" + kodeOwner;
                    OkHttpClient mergeClient = ApiKeyHttpClient.get();

                    try (Response mergeResponse = mergeClient.newCall(
                            new Request.Builder().url(mergeUrl).get().build()
                    ).execute()) {

                        String mergeBody = mergeResponse.body() != null
                                ? mergeResponse.body().string()
                                : "null";

                        Log.d(TAG, "MERGE RESPONSE: " + mergeBody);

                        if (!mergeResponse.isSuccessful()) {
                            requireActivity().runOnUiThread(() -> {
                                hideLoading();
                                Toast.makeText(requireContext(), "Merge gagal: " + mergeBody, Toast.LENGTH_LONG).show();
                            });
                            return;
                        }

                        requireActivity().runOnUiThread(() ->
                                showLoading("Merge selesai. Sync data...")
                        );

                        // ======================
                        // SYNC DATA
                        // ======================
                        DataBaseHelper dbHelperSync = new DataBaseHelper(requireContext());
                        Map<String, Object> syncData = dbHelperSync.getSyncData(kodeOwner);

                        JSONObject syncJson = (JSONObject) mapToJson(syncData);

                        // 🔍 LOG FULL SYNC JSON
                        Log.d("SYNC_JSON_FULL", syncJson.toString(2));

                        OkHttpClient syncClient = ApiKeyHttpClient.get();
                        RequestBody syncBody = RequestBody.create(
                                syncJson.toString(),
                                MediaType.parse("application/json; charset=utf-8")
                        );

                        Request syncRequest = new Request.Builder()
                                .url("https://siqasir.awandigital.id/api/sync/data-compare")
                                .post(syncBody)
                                .build();

                        try (Response syncResponse = syncClient.newCall(syncRequest).execute()) {

                            String syncRes = syncResponse.body() != null
                                    ? syncResponse.body().string()
                                    : "null";

                            Log.d(TAG, "SYNC RESPONSE: " + syncRes);

                            requireActivity().runOnUiThread(() -> {
                                hideLoading();
                                if (syncResponse.isSuccessful()) {
                                    Toast.makeText(requireContext(),
                                            "Upload, merge & sync berhasil",
                                            Toast.LENGTH_LONG).show();
                                } else {
                                    Toast.makeText(requireContext(),
                                            "Sync gagal: " + syncRes,
                                            Toast.LENGTH_LONG).show();
                                }
                            });
                        }
                    }

                } catch (Exception e) {
                    Log.e(TAG, "UPLOAD FLOW ERROR", e);
                    requireActivity().runOnUiThread(() -> {
                        hideLoading();
                        Toast.makeText(requireContext(),
                                "Error: " + e.getMessage(),
                                Toast.LENGTH_LONG).show();
                    });
                }
            }).start();

        } catch (Exception e) {
            Log.e(TAG, "GENERAL ERROR", e);
            hideLoading();
            Toast.makeText(requireContext(),
                    "Error: " + e.getMessage(),
                    Toast.LENGTH_LONG).show();
        }
    }

    private void showLoading(String message) {
        if (loadingDialog != null && loadingDialog.isShowing()) return;

        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setCancelable(false);

        View view = getLayoutInflater().inflate(R.layout.dialog_loading, null);
        TextView textMessage = view.findViewById(R.id.loading_text);
        textMessage.setText(message);

        builder.setView(view);
        loadingDialog = builder.create();
        loadingDialog.show();
    }

    private void hideLoading() {
        if (loadingDialog != null && loadingDialog.isShowing()) {
            loadingDialog.dismiss();
        }
    }

    public void onViewClicked(View view) {
        Intent intent;

        if (view.getId() == R.id.btBukaKasir) {
            intent = new Intent(activity, BukaKasirActivity.class);
            startActivity(intent);
        } else if (view.getId() == R.id.btTutupKasir) {
            DialogForm();
        } else if (view.getId() == R.id.btKasKeluar) {
            tambahKasKeluar();
        }
    }

}