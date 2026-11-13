package com.pyramidsoft.siqasir;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.pyramidsoft.siqasir.adapter.OutletAdapter;
import com.pyramidsoft.siqasir.db.DataBaseHelper;
import com.pyramidsoft.siqasir.model.Outlet;

import java.util.List;

public class ListOutletActivity extends AppCompatActivity {

    private Toolbar toolbar;
    private RecyclerView rvListOutlet;
    private SwipeRefreshLayout swipe;
    private LinearLayout llKosong;
    private View btTambahOutlet, btTambahOutletKosong;
    private int maxOutlet = 1;
    private int jumlahOutletSaatIni = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list_outlet);

        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }

        rvListOutlet = findViewById(R.id.rvListOutlet);
        swipe = findViewById(R.id.swipe);
        llKosong = findViewById(R.id.ll_Kosong);
        btTambahOutlet = findViewById(R.id.btTambahOutlet);
        btTambahOutletKosong = findViewById(R.id.btTambahOutletKosong);
        rvListOutlet.setLayoutManager(new LinearLayoutManager(this));


        swipe.setOnRefreshListener(() -> {
            loadDataOutlet();
        });

        btTambahOutlet.setOnClickListener(v -> {
            if (bolehTambahOutlet()) {
                Intent intent = new Intent(ListOutletActivity.this, TambahOutletActivity.class);
                startActivity(intent);
            }
        });

        btTambahOutletKosong.setOnClickListener(v -> {
            if (bolehTambahOutlet()) {
                Intent intent = new Intent(ListOutletActivity.this, TambahOutletActivity.class);
                startActivity(intent);
            }
        });

        loadDataOutlet();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadDataOutlet();
    }

    private boolean isExpired(String expDate) {
        if (expDate == null || expDate.isEmpty()) return true;

        try {
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            sdf.setLenient(false);

            java.util.Date exp = sdf.parse(expDate);
            java.util.Date now = new java.util.Date();

            return now.after(exp);
        } catch (Exception e) {
            e.printStackTrace();
            return true;
        }
    }

    private void loadDataOutlet() {
        swipe.setRefreshing(true);

        DataBaseHelper dbHelper = new DataBaseHelper(this);
        List<Outlet> outletList = dbHelper.getAllOutlet();
        jumlahOutletSaatIni = outletList.size();
        int maxTokoDb = dbHelper.getMaxToko();
        String expDate = dbHelper.getExpiredDate();

        boolean expired = isExpired(expDate);

        if (expired) {
            maxOutlet = 1;
        } else {
            maxOutlet = maxTokoDb;
        }

        if (outletList.size() > 0) {
            rvListOutlet.setVisibility(View.VISIBLE);
            llKosong.setVisibility(View.GONE);
            rvListOutlet.setAdapter(new OutletAdapter(outletList));
        } else {
            rvListOutlet.setVisibility(View.GONE);
            llKosong.setVisibility(View.VISIBLE);
        }

        if (expired || outletList.size() >= maxOutlet) {

            btTambahOutlet.setEnabled(false);
            btTambahOutletKosong.setEnabled(false);

            btTambahOutlet.setAlpha(1f);
            btTambahOutletKosong.setAlpha(1f);

            btTambahOutlet.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#BDBDBD")));
            btTambahOutletKosong.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#BDBDBD")));


            ((androidx.appcompat.widget.AppCompatButton) btTambahOutlet)
                    .setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_lock, 0, 0, 0);

            ((androidx.appcompat.widget.AppCompatButton) btTambahOutletKosong)
                    .setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_lock, 0, 0, 0);

            ((androidx.appcompat.widget.AppCompatButton) btTambahOutlet).setText("Terkunci");
            ((androidx.appcompat.widget.AppCompatButton) btTambahOutletKosong).setText("Terkunci");

        } else {

            btTambahOutlet.setEnabled(true);
            btTambahOutletKosong.setEnabled(true);

            btTambahOutlet.setAlpha(1f);
            btTambahOutletKosong.setAlpha(1f);

            btTambahOutlet.setBackgroundResource(R.drawable.theme_toolbar_btn_adduser);
            btTambahOutletKosong.setBackgroundResource(R.drawable.theme_toolbar_btn_adduser);

            ((androidx.appcompat.widget.AppCompatButton) btTambahOutlet)
                    .setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_baseline_add_24, 0, 0, 0);

            ((androidx.appcompat.widget.AppCompatButton) btTambahOutletKosong)
                    .setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_baseline_add_24, 0, 0, 0);

            ((androidx.appcompat.widget.AppCompatButton) btTambahOutlet).setText("Tambah");
            ((androidx.appcompat.widget.AppCompatButton) btTambahOutletKosong).setText("Tambah");
        }

        swipe.setRefreshing(false);
    }

    private boolean bolehTambahOutlet() {

        if (jumlahOutletSaatIni >= maxOutlet) {

            android.widget.Toast.makeText(this,
                    "Batas maksimal outlet: " + maxOutlet,
                    android.widget.Toast.LENGTH_LONG).show();

            return false;
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
