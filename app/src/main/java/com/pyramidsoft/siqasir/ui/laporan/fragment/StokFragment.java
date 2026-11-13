package com.pyramidsoft.siqasir.ui.laporan.fragment;

import android.app.Activity;
import android.database.Cursor;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.pyramidsoft.siqasir.adapter.StokAdapter;
import com.pyramidsoft.siqasir.databinding.FragmentStokBinding;
import com.pyramidsoft.siqasir.db.DataBaseHelper;
import com.pyramidsoft.siqasir.model.StokModel;
import com.pyramidsoft.siqasir.ui.laporan.LaporanFragment;
import com.pyramidsoft.siqasir.utils.SessionManager;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Locale;

public class StokFragment extends Fragment {

    private FragmentStokBinding binding;
    private Activity activity;
    private SessionManager sessionManager;
    private DataBaseHelper dbHelper;

    private final ArrayList<StokModel> list = new ArrayList<>();
    private StokAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {

        binding = FragmentStokBinding.inflate(inflater, container, false);
        activity = requireActivity();

        sessionManager = new SessionManager(activity);
        dbHelper = new DataBaseHelper(activity);

        binding.rvStok.setLayoutManager(
                new LinearLayoutManager(activity, LinearLayoutManager.VERTICAL, false)
        );

        adapter = new StokAdapter(list);
        binding.rvStok.setAdapter(adapter);

        loadData();

        binding.swipe.setOnRefreshListener(() -> {
            loadData();
            binding.swipe.setRefreshing(false);
        });

        return binding.getRoot();
    }

    private String formatRupiah(int value) {
        return NumberFormat
                .getInstance(new Locale("in", "ID"))
                .format(value);
    }

    private void loadData() {
        list.clear();

        ArrayList<Integer> tokoIds = new ArrayList<>();
        LaporanFragment parentFragment = (LaporanFragment) getParentFragment();

        if (parentFragment != null) {
            tokoIds = parentFragment.getSelectedTokoIds();
        }

        if (tokoIds.isEmpty()) {
            String idToko = dbHelper.getIdTokoByKodePegawai(
                    sessionManager.getKodeUsers()
            );
            if (idToko != null) {
                tokoIds.add(Integer.parseInt(idToko));
            }
        }

        Cursor cur = dbHelper.listStokProduk(tokoIds);
        int no = 1;

        while (cur.moveToNext()) {
            String kode = cur.getString(0);
            String nama = cur.getString(1);

            String qty;
            int totalHpp;

            if (cur.isNull(2)) {
                qty = "--";
                totalHpp = cur.isNull(3) ? 0 : cur.getInt(3);
            } else {
                int stok = cur.getInt(2);
                int hargaBeli = cur.isNull(3) ? 0 : cur.getInt(3);
                qty = String.valueOf(stok);
                totalHpp = stok * hargaBeli;
            }

            list.add(new StokModel(
                    no++,
                    kode,
                    nama,
                    qty,
                    formatRupiah(totalHpp)
            ));
        }

        cur.close();
        adapter.notifyDataSetChanged();
    }
}
