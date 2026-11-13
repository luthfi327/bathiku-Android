package com.pyramidsoft.siqasir.kategori;

import static com.pyramidsoft.siqasir.db.DataBaseHelper.database_name;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.Fragment;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.pyramidsoft.siqasir.ProdukTokoActivity;
import com.pyramidsoft.siqasir.R;
import com.pyramidsoft.siqasir.adapter.ListProdukAtHomeAdapter;
import com.pyramidsoft.siqasir.databinding.FragmentHomeDiskonBinding;
import com.pyramidsoft.siqasir.db.DataBaseHelper;
import com.pyramidsoft.siqasir.model.ListProdukAtHomeModel;
import com.pyramidsoft.siqasir.ui.home.HomeMainFragment;
import com.pyramidsoft.siqasir.utils.SessionManager;

import java.util.ArrayList;

public class HomeDiskonFragment extends Fragment {
    private FragmentHomeDiskonBinding binding;
    private ArrayList<ListProdukAtHomeModel> arrayListCopy = new ArrayList<>();
    DataBaseHelper dbHelper;
    SQLiteDatabase mDatabase;
    ListProdukAtHomeAdapter adapter;
    Activity activity;
    SessionManager sessionManager;

    public BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String refresh = intent.getStringExtra("refresh");
            switch (refresh) {
                case "dataproduk":
                    getDataProduk();
                    break;
                case "finditem":
                    String find = intent.getStringExtra("find");
                    Log.i("find", "onReceive: " + find);
                    adapter.filter(find, arrayListCopy);
                    adapter.notifyDataSetChanged();
                    break;
            }
        }
    };

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentHomeDiskonBinding.inflate(inflater, container, false);
        View view = binding.getRoot();

        activity = getActivity();
        dbHelper = new DataBaseHelper(getActivity());
        mDatabase = getActivity().openOrCreateDatabase(database_name, Context.MODE_PRIVATE, null);

        sessionManager = new SessionManager(getActivity());

        binding.listProduk.setHasFixedSize(true);
        setupGridLayout();

        getDataProduk();

        binding.swipe.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                getDataProduk();
            }
        });

        binding.emptylayout.findViewById(R.id.btnTambahBarang).setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), ProdukTokoActivity.class);
            startActivity(intent);
        });

        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(mMessageReceiver, new IntentFilter("filterkategori-state"));
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        getDataProduk();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        setupGridLayout();
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
    }

    private void setupGridLayout() {
        int spanCount = getSpanCountBasedOnScreenSize();
        GridLayoutManager layoutManager = new GridLayoutManager(getActivity(), spanCount, GridLayoutManager.VERTICAL, false);
        binding.listProduk.setLayoutManager(layoutManager);
    }

    private int getSpanCountBasedOnScreenSize() {
        int orientation = getResources().getConfiguration().orientation;

        int screenWidth = getResources().getDisplayMetrics().widthPixels;
        int dpWidth = (int) (screenWidth / getResources().getDisplayMetrics().density);

        if (dpWidth >= 600) {
            return orientation == Configuration.ORIENTATION_LANDSCAPE ? 4 : 3;
        } else {
            return orientation == Configuration.ORIENTATION_LANDSCAPE ? 3 : 2;
        }
    }

    public void getDataProduk() {
        String kodePegawai = sessionManager.getKodeUsers();
        String idToko = dbHelper.getIdTokoByKodePegawai(kodePegawai);
        Cursor cursorproduct;
        if (HomeMainFragment.valuesWhere.isEmpty()) {
            cursorproduct = dbHelper.listProdukDiskon(idToko);
        } else {
            cursorproduct = dbHelper.listProdukDiskonWhereKategori(HomeMainFragment.valuesWhere, idToko);
        }

        ArrayList<ListProdukAtHomeModel> data = new ArrayList<>();

        if (cursorproduct.moveToFirst()) {
            do {
                data.add(new ListProdukAtHomeModel(
                        cursorproduct.getString(2),
                        cursorproduct.getString(4),
                        cursorproduct.getString(12),
                        cursorproduct.getString(9),
                        cursorproduct.getString(5),
                        cursorproduct.getBlob(6),
                        cursorproduct.isNull(13) ? null : cursorproduct.getInt(13)
                ));
            } while (cursorproduct.moveToNext());
        }

        if (data.isEmpty()) {
            binding.emptylayout.setVisibility(View.VISIBLE);
            binding.listProduk.setVisibility(View.GONE);
        } else {
            binding.emptylayout.setVisibility(View.GONE);
            binding.listProduk.setVisibility(View.VISIBLE);
        }

        cursorproduct.close();

        arrayListCopy.clear();
        arrayListCopy.addAll(data);
        adapter = new ListProdukAtHomeAdapter(data, getActivity());

        binding.listProduk.setAdapter(adapter);
        if (binding.swipe.isRefreshing()) binding.swipe.setRefreshing(false);

    }

}