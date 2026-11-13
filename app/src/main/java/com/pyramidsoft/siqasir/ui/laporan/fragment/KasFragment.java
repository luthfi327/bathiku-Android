package com.pyramidsoft.siqasir.ui.laporan.fragment;

import static com.pyramidsoft.siqasir.db.DataBaseHelper.database_name;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.DatePicker;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;

import com.pyramidsoft.siqasir.R;
import com.pyramidsoft.siqasir.adapter.KasAdapter;
import com.pyramidsoft.siqasir.databinding.FragmentKasBinding;
import com.pyramidsoft.siqasir.db.DataBaseHelper;
import com.pyramidsoft.siqasir.model.KasModel;
import com.pyramidsoft.siqasir.ui.laporan.LaporanFragment;
import com.pyramidsoft.siqasir.utils.ProgressDialog;
import com.pyramidsoft.siqasir.utils.SessionManager;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;

public class KasFragment extends Fragment {

    private FragmentKasBinding binding;
    SessionManager sessionManager;
    ProgressDialog pDialog;
    DataBaseHelper dbHelper;
    SQLiteDatabase mDatabase;
    Activity activity;
    KasAdapter adapter;

    private SimpleDateFormat dateFormatter;
    private DatePickerDialog datePickerDialog;
    private String tanggal1, tanggal2;

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentKasBinding.inflate(inflater, container, false);
        View view = binding.getRoot();

        activity = getActivity();
        dateFormatter = new SimpleDateFormat("dd-MM-yy", new Locale("id", "ID"));

        pDialog = new ProgressDialog(activity);
        sessionManager = new SessionManager(activity);
        dbHelper = new DataBaseHelper(activity);
        mDatabase = activity.openOrCreateDatabase(database_name, Context.MODE_PRIVATE, null);

        binding.rvKas.setHasFixedSize(true);
        GridLayoutManager layoutManager = new GridLayoutManager(activity, 1, GridLayoutManager.VERTICAL, false);
        binding.rvKas.setLayoutManager(layoutManager);

        binding.swipe.setOnRefreshListener(() -> {
            if (tanggal1 != null && tanggal2 != null) {
                listLaporanKas(tanggal1, tanggal2);
            } else {
                binding.swipe.setRefreshing(false);
            }
        });

        binding.tvTgl1.setOnClickListener(this::onViewClicked);
        binding.tvTgl2.setOnClickListener(this::onViewClicked);

        return view;
    }

    public void listLaporanKas(String tgl1, String tgl2) {
        binding.swipe.setRefreshing(true);

        ArrayList<Integer> tokoIds = new ArrayList<>();
        if (getParentFragment() instanceof LaporanFragment) {
            LaporanFragment parent = (LaporanFragment) getParentFragment();
            tokoIds = parent.getSelectedTokoIds();
        }

        if (tokoIds == null || tokoIds.isEmpty()) {
            tokoIds = new ArrayList<>();
            String kodePegawai = sessionManager.getKodeUsers();
            String idToko = dbHelper.getIdTokoByKodePegawai(kodePegawai);
            if (idToko != null && !idToko.isEmpty()) {
                tokoIds.add(Integer.parseInt(idToko));
            }
        }


        Cursor cursorproduct = dbHelper.listLaporanKas(tgl1, tgl2, tokoIds);

        ArrayList<KasModel> data = new ArrayList<>();

        if (cursorproduct != null && cursorproduct.moveToFirst()) {
            do {
                data.add(new KasModel(
                        cursorproduct.getString(0),
                        cursorproduct.getString(1),
                        cursorproduct.getString(2)
                ));
            } while (cursorproduct.moveToNext());
            cursorproduct.close();
        }

        if (data.isEmpty()) {
            Toast.makeText(activity, "Laporan Kas tidak ditemukan", Toast.LENGTH_SHORT).show();
        }

        adapter = new KasAdapter(data, activity, mDatabase);
        binding.rvKas.setAdapter(adapter);

        binding.swipe.setRefreshing(false);
    }

    private void showDateDialog1() {

        Calendar newCalendar = Calendar.getInstance();

        datePickerDialog = new DatePickerDialog(activity, new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                Calendar newDate = Calendar.getInstance();
                newDate.set(year, monthOfYear, dayOfMonth);

                tanggal1 = dateFormatter.format(newDate.getTime());
                binding.tvTgl1.setText(dateFormatter.format(newDate.getTime()));
            }
        }, newCalendar.get(Calendar.YEAR), newCalendar.get(Calendar.MONTH), newCalendar.get(Calendar.DAY_OF_MONTH));

        datePickerDialog.show();
    }

    private void showDateDialog2() {

        Calendar newCalendar = Calendar.getInstance();

        datePickerDialog = new DatePickerDialog(activity, new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                Calendar newDate = Calendar.getInstance();
                newDate.set(year, monthOfYear, dayOfMonth);

                tanggal2 = dateFormatter.format(newDate.getTime());
                binding.tvTgl2.setText(dateFormatter.format(newDate.getTime()));

                listLaporanKas(tanggal1, tanggal2);
            }
        },newCalendar.get(Calendar.YEAR), newCalendar.get(Calendar.MONTH), newCalendar.get(Calendar.DAY_OF_MONTH));

        datePickerDialog.show();
    }

    public void onViewClicked(View view) {
        if (view.getId() == R.id.tvTgl1) {
            showDateDialog1();
        } else if (view.getId() == R.id.tvTgl2) {
            showDateDialog2();
        }
    }

}
