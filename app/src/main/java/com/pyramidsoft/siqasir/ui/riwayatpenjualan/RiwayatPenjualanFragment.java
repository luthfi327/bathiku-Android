package com.pyramidsoft.siqasir.ui.riwayatpenjualan;

import static com.pyramidsoft.siqasir.db.DataBaseHelper.database_name;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;

import com.pyramidsoft.siqasir.adapter.RiwayatPenjualanAdapter;
import com.pyramidsoft.siqasir.databinding.FragmentRiwayatPenjualanBinding;
import com.pyramidsoft.siqasir.db.DataBaseHelper;
import com.pyramidsoft.siqasir.model.RiwayatPenjualanModels;
import com.pyramidsoft.siqasir.utils.GlobalToast;
import com.pyramidsoft.siqasir.utils.SessionManager;

import java.util.ArrayList;
import java.util.Calendar;

public class RiwayatPenjualanFragment extends Fragment {

    private FragmentRiwayatPenjualanBinding binding;
    private Activity activity;
    private SQLiteDatabase mDatabase;
    private SessionManager sessionManager;
    private DataBaseHelper dbHelper;
    private RiwayatPenjualanAdapter adapter;

    private final ArrayList<RiwayatPenjualanModels> arrayListCopy = new ArrayList<>();

    private String tanggalDari = "";
    private String tanggalHingga = "";

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentRiwayatPenjualanBinding.inflate(inflater, container, false);

        activity = getActivity();
        sessionManager = new SessionManager(activity);
        dbHelper = new DataBaseHelper(activity);
        mDatabase = activity.openOrCreateDatabase(database_name, Context.MODE_PRIVATE, null);

        binding.rvRiwayatPenjualan.setLayoutManager(new GridLayoutManager(activity, 1));

        binding.etRiwayatPenjualan.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void afterTextChanged(Editable s) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (adapter != null) {
                    adapter.filter(s.toString(), arrayListCopy);
                    adapter.notifyDataSetChanged();
                }
            }
        });

        binding.etTanggalDari.setOnClickListener(v -> showDatePicker(binding.etTanggalDari));
        binding.etTanggalHingga.setOnClickListener(v -> showDatePicker(binding.etTanggalHingga));

        setupClearLeft(binding.etTanggalDari);
        setupClearLeft(binding.etTanggalHingga);

        resetToToday();

        binding.swipe.setOnRefreshListener(() -> setListView());

        return binding.getRoot();
    }

    private void showDatePicker(EditText target) {
        Calendar c = Calendar.getInstance();

        new DatePickerDialog(getContext(), (view, year, month, day) -> {

            int m = month + 1;

            String display = String.format("%02d-%02d-%04d", day, m, year);
            String value   = String.format("%04d-%02d-%02d", year, m, day);

            if (target == binding.etTanggalDari) {

                if (!tanggalHingga.isEmpty() && value.compareTo(tanggalHingga) > 0) {
                    GlobalToast.ShowToast(activity, "Tanggal dari tidak boleh melebihi tanggal hingga");
                    return;
                }

                tanggalDari = value;

            } else {

                if (!tanggalDari.isEmpty() && value.compareTo(tanggalDari) < 0) {
                    GlobalToast.ShowToast(activity, "Tanggal hingga tidak boleh kurang dari tanggal dari");
                    return;
                }

                tanggalHingga = value;
            }

            target.setText(display);
            updateClearIcon();
            setListView();

        }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void resetToToday() {
        Calendar c = Calendar.getInstance();

        int y = c.get(Calendar.YEAR);
        int m = c.get(Calendar.MONTH) + 1;
        int d = c.get(Calendar.DAY_OF_MONTH);

        String display = String.format("%02d-%02d-%04d", d, m, y);
        String value   = String.format("%04d-%02d-%02d", y, m, d);

        tanggalDari = value;
        tanggalHingga = value;

        binding.etTanggalDari.setText(display);
        binding.etTanggalHingga.setText(display);

        updateClearIcon();
        setListView();
    }

    private void updateClearIcon() {
        setLeftIcon(binding.etTanggalDari, !isToday(tanggalDari));
        setLeftIcon(binding.etTanggalHingga, !isToday(tanggalHingga));
    }

    private void setLeftIcon(EditText et, boolean show) {
        et.setCompoundDrawablesWithIntrinsicBounds(
                show ? android.R.drawable.presence_offline : 0,
                0, 0, 0
        );
    }

    private boolean isToday(String value) {
        Calendar c = Calendar.getInstance();

        String today = String.format("%04d-%02d-%02d",
                c.get(Calendar.YEAR),
                c.get(Calendar.MONTH) + 1,
                c.get(Calendar.DAY_OF_MONTH)
        );

        return today.equals(value);
    }

    private void setupClearLeft(EditText et) {
        et.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_UP &&
                    et.getCompoundDrawables()[0] != null) {

                int width = et.getCompoundDrawables()[0].getBounds().width();

                if (event.getX() <= width + et.getPaddingStart()) {
                    resetToToday();
                    return true;
                }
            }
            return false;
        });
    }

    private void setListView() {

        binding.swipe.setRefreshing(true);

        String idToko = dbHelper.getIdTokoByKodePegawai(sessionManager.getKodeUsers());

        Cursor cursor = dbHelper.listRiwayatPenjualanByTanggal(
                idToko, tanggalDari, tanggalHingga
        );

        ArrayList<RiwayatPenjualanModels> data = new ArrayList<>();

        if (cursor.moveToFirst()) {

            int iInvoice = cursor.getColumnIndex("invoice");
            int iNama    = cursor.getColumnIndex("nama_pegawai");
            int iTotal   = cursor.getColumnIndex("total_jml_transaksi");
            int iJenis   = cursor.getColumnIndex("jenis_kedatangan");
            int iQty     = cursor.getColumnIndex("total_qty");

            do {
                data.add(new RiwayatPenjualanModels(
                        cursor.getString(iJenis),
                        cursor.getString(iNama),
                        cursor.getString(iQty),
                        cursor.getString(iInvoice),
                        cursor.getString(iTotal)
                ));
            } while (cursor.moveToNext());
        }

        cursor.close();

        if (data.isEmpty()) {
            binding.llKosong.setVisibility(View.VISIBLE);
            binding.swipe.setVisibility(View.GONE);
        } else {
            binding.llKosong.setVisibility(View.GONE);
            binding.swipe.setVisibility(View.VISIBLE);
        }

        arrayListCopy.clear();
        arrayListCopy.addAll(data);

        adapter = new RiwayatPenjualanAdapter(data, activity, mDatabase);
        binding.rvRiwayatPenjualan.setAdapter(adapter);

        binding.swipe.setRefreshing(false);
    }
}