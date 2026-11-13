package com.pyramidsoft.siqasir;

import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.google.android.material.navigation.NavigationView;
import com.pyramidsoft.siqasir.databinding.ActivityHomeMainBinding;
import com.pyramidsoft.siqasir.db.DataBaseHelper;
import com.pyramidsoft.siqasir.utils.GlobalToast;
import com.pyramidsoft.siqasir.utils.ProgressDialog;
import com.pyramidsoft.siqasir.utils.SessionManager;

/**
 * HomeMainActivity
 *
 * PERUBAHAN FASE 6:
 * - Menambahkan MenuItem navOwnerDashboard yang dikontrol visibilitasnya
 *   berdasarkan hak_akses user (hanya muncul untuk Owner dan Admin).
 * - Method showOrHideOwnerMenu() menangani logika visibilitas menu baru.
 * - Handler navigasi ke nav_owner_dashboard ditambahkan di onNavigationItemSelected.
 * - nav_owner_dashboard didaftarkan di AppBarConfiguration agar tombol Back
 *   di toolbar berfungsi dengan benar.
 *
 * PERUBAHAN GAP 3:
 * - Menambahkan MenuItem navOwnerPaymentHistory (Riwayat Pembayaran).
 * - showOrHideOwnerMenu() diperluas untuk mengatur visibilitas menu baru ini.
 * - Handler navigasi ke nav_owner_payment_history ditambahkan di listener.
 * - nav_owner_payment_history didaftarkan di AppBarConfiguration.
 */
public class HomeMainActivity extends AppCompatActivity {

    private ActivityHomeMainBinding binding;

    private AppBarConfiguration mAppBarConfiguration;
    public static NavController navController;
    private NavigationView       navigationView;

    private Toolbar   toolbar;
    private ImageView ivMainLogo;
    private TextView  Header;
    private TextView  SubHeader;
    private TextView  tvCount;
    private TextView tvBatal;
    private Button   btIya;

    private String totalcart = "0";

    ProgressDialog pDialog;
    SessionManager sessionManager;
    DataBaseHelper dbHelper;

    private boolean isSalesExpanded = false;
    private boolean isExpired       = false;

    private MenuItem navReport;
    private MenuItem navSalesReport;
    private MenuItem navSalesDetail;

    // Fase 6: MenuItem Dashboard Owner
    private MenuItem navOwnerDashboard;

    // GAP 3: MenuItem Riwayat Pembayaran Owner — BARU
    private MenuItem navOwnerPaymentHistory;

    public BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String refresh = intent.getStringExtra("refresh");
            if (refresh.equals("gettotaldata")) {
                getTotalDataPesanan();
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityHomeMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.white));
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            new WindowInsetsControllerCompat(getWindow(), getWindow().getDecorView())
                    .setAppearanceLightStatusBars(true);
        }

        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        pDialog        = new ProgressDialog(HomeMainActivity.this);
        sessionManager = new SessionManager(HomeMainActivity.this);
        dbHelper       = new DataBaseHelper(HomeMainActivity.this);

        navigationView = findViewById(R.id.nav_view);
        Menu menu      = navigationView.getMenu();

        navReport             = menu.findItem(R.id.nav_report);
        navSalesReport        = menu.findItem(R.id.nav_sales_report);
        navSalesDetail        = menu.findItem(R.id.nav_sales_detail);
        navOwnerDashboard     = menu.findItem(R.id.nav_owner_dashboard);      // Fase 6
        navOwnerPaymentHistory= menu.findItem(R.id.nav_owner_payment_history);// GAP 3

        // Sembunyikan semua menu kontekstual secara default
        if (navReport != null)              navReport.setVisible(false);
        if (navSalesReport != null)         navSalesReport.setVisible(false);
        if (navSalesDetail != null)         navSalesDetail.setVisible(false);
        if (navOwnerDashboard != null)      navOwnerDashboard.setVisible(false);
        if (navOwnerPaymentHistory != null) navOwnerPaymentHistory.setVisible(false);

        tvCount = findViewById(R.id.tvCount);
        tvCount.setVisibility(View.GONE);

        View headerView = navigationView.getHeaderView(0);
        ivMainLogo = (ImageView) headerView.findViewById(R.id.ivMainLogo);
        Header     = (TextView)  headerView.findViewById(R.id.tvHeader);
        SubHeader  = (TextView)  headerView.findViewById(R.id.tvSubHeader);

        getDataToko();

        // ── AppBarConfiguration ──────────────────────────────────────────────
        // Fase 6  : nav_owner_dashboard
        // GAP 3   : nav_owner_payment_history
        // Daftar ini memastikan toolbar menampilkan hamburger (bukan Back)
        // saat fragment ini aktif sebagai top-level destination.
        mAppBarConfiguration = new AppBarConfiguration.Builder(
                R.id.nav_home,
                R.id.nav_daftar_pesanan,
                R.id.nav_riwayat_penjualan,
                R.id.nav_laporan,
                R.id.nav_pengaturan,
                R.id.nav_tutup_kasir,
                R.id.nav_like,
                R.id.nav_logout,
                R.id.nav_owner_dashboard,        // Fase 6
                R.id.nav_owner_payment_history)  // GAP 3
                .setOpenableLayout(binding.drawerLayout)
                .build();

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, binding.drawerLayout, toolbar,
                R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        binding.drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        navController = Navigation.findNavController(this, R.id.nav_host_fragment);
        NavigationUI.setupActionBarWithNavController(this, navController, mAppBarConfiguration);
        NavigationUI.setupWithNavController(navigationView, navController);

        navController.addOnDestinationChangedListener((controller, destination, arguments) -> {
            getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.white));
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                new WindowInsetsControllerCompat(getWindow(), getWindow().getDecorView())
                        .setAppearanceLightStatusBars(true);
            }
            getWindow().setNavigationBarColor(ContextCompat.getColor(this, R.color.white));
        });

        navigationView.setNavigationItemSelectedListener(item -> {
            int itemId = item.getItemId();

            if (itemId == R.id.nav_sales_data) {
                if (isExpired) {
                    GlobalToast.ShowToast(HomeMainActivity.this,
                            "Fitur laporan tidak tersedia di mode gratis");
                    return true;
                }
                isSalesExpanded = !isSalesExpanded;
                if (navReport != null)      navReport.setVisible(isSalesExpanded);
                if (navSalesReport != null) navSalesReport.setVisible(isSalesExpanded);
                if (navSalesDetail != null) navSalesDetail.setVisible(isSalesExpanded);
                return true;
            }

            if (itemId == R.id.nav_home) {
                tvCount.setVisibility(View.GONE);
                navController.navigate(R.id.nav_home);
            } else if (itemId == R.id.nav_list) {
                tvCount.setVisibility(View.GONE);
                navController.navigate(R.id.nav_daftar_pesanan);
            } else if (itemId == R.id.nav_history) {
                tvCount.setVisibility(View.GONE);
                navController.navigate(R.id.nav_riwayat_penjualan);
            } else if (itemId == R.id.nav_report) {
                tvCount.setVisibility(View.GONE);
                navController.navigate(R.id.nav_laporan);
            } else if (itemId == R.id.nav_sales_report) {
                tvCount.setVisibility(View.GONE);
                navController.navigate(R.id.nav_sales_report);
            } else if (itemId == R.id.nav_sales_detail) {
                tvCount.setVisibility(View.GONE);
                navController.navigate(R.id.nav_sales_detail);
            } else if (itemId == R.id.nav_setting) {
                tvCount.setVisibility(View.GONE);
                navController.navigate(R.id.nav_pengaturan);
            } else if (itemId == R.id.nav_open) {
                tvCount.setVisibility(View.GONE);
                navController.navigate(R.id.nav_tutup_kasir);

                // ── Fase 6: Dashboard Owner ──────────────────────────────────────
            } else if (itemId == R.id.nav_owner_dashboard) {
                tvCount.setVisibility(View.GONE);
                navController.navigate(R.id.nav_owner_dashboard);

                // ── GAP 3: Riwayat Pembayaran Owner ─────────────────────────────
            } else if (itemId == R.id.nav_owner_payment_history) {
                tvCount.setVisibility(View.GONE);
                navController.navigate(R.id.nav_owner_payment_history);

            } else if (itemId == R.id.nav_like) {
                tvCount.setVisibility(View.GONE);
                try {
                    Intent intent = new Intent(Intent.ACTION_VIEW,
                            Uri.parse("market://details?id=com.pyramidsoft.siqasir"));
                    intent.setPackage("com.android.vending");
                    startActivity(intent);
                } catch (Exception e) {
                    Intent intent = new Intent(Intent.ACTION_VIEW,
                            Uri.parse("https://play.google.com/store/apps/details?id=com.pyramidsoft.siqasir"));
                    startActivity(intent);
                }
            } else if (itemId == R.id.nav_logout) {
                tvCount.setVisibility(View.GONE);
                showLogoutDialog();
            }

            binding.drawerLayout.closeDrawers();
            return true;
        });

        LocalBroadcastManager.getInstance(this).registerReceiver(
                mMessageReceiver, new IntentFilter("dataPesanan-state"));
    }

    // =========================================================================
    // Tampilkan/sembunyikan menu Owner berdasarkan hak_akses
    //
    // FASE 6: navOwnerDashboard
    // GAP 3 : navOwnerPaymentHistory (ditambahkan)
    //
    // Dipanggil di onResume() agar selalu sinkron setelah login/logout.
    // Logika: hak_akses Owner atau Admin → tampilkan; lainnya → sembunyikan.
    // =========================================================================
    private void showOrHideOwnerMenu() {
        String hakAkses      = sessionManager.getHakAkses();
        boolean isOwnerOrAdmin = "Owner".equalsIgnoreCase(hakAkses)
                || "Admin".equalsIgnoreCase(hakAkses);

        if (navOwnerDashboard != null) {
            navOwnerDashboard.setVisible(isOwnerOrAdmin);
        }

        // GAP 3: kontrol visibilitas menu Riwayat Pembayaran
        if (navOwnerPaymentHistory != null) {
            navOwnerPaymentHistory.setVisible(isOwnerOrAdmin);
        }

        Log.d("HomeMain", "showOrHideOwnerMenu: hakAkses=" + hakAkses
                + " isOwnerOrAdmin=" + isOwnerOrAdmin);
    }

    // =========================================================================
    // Metode lama — tidak ada perubahan
    // =========================================================================

    private void updateSalesMenuUI() {
        Menu menu = navigationView.getMenu();
        MenuItem salesMenu = menu.findItem(R.id.nav_sales_data);
        if (salesMenu == null) return;

        if (isExpired) {
            salesMenu.setIcon(R.drawable.ic_lock);
            SpannableString s = new SpannableString("Data Penjualan (Terkunci)");
            s.setSpan(new ForegroundColorSpan(
                    ContextCompat.getColor(this, android.R.color.darker_gray)
            ), 0, s.length(), 0);
            salesMenu.setTitle(s);
        } else {
            salesMenu.setTitle("Data Penjualan");
        }
    }

    private void checkExpiredStatus() {
        try {
            String expDateStr = dbHelper.getExpDateToko();
            if (expDateStr == null || expDateStr.isEmpty()) {
                isExpired = false;
                return;
            }
            java.text.SimpleDateFormat sdf =
                    new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault());
            java.util.Date expDate = sdf.parse(expDateStr);
            java.util.Date today   = new java.util.Date();

            java.util.Calendar calToday = java.util.Calendar.getInstance();
            calToday.setTime(today);
            calToday.set(java.util.Calendar.HOUR_OF_DAY, 0);
            calToday.set(java.util.Calendar.MINUTE, 0);
            calToday.set(java.util.Calendar.SECOND, 0);
            calToday.set(java.util.Calendar.MILLISECOND, 0);

            java.util.Calendar calExp = java.util.Calendar.getInstance();
            calExp.setTime(expDate);
            calExp.set(java.util.Calendar.HOUR_OF_DAY, 0);
            calExp.set(java.util.Calendar.MINUTE, 0);
            calExp.set(java.util.Calendar.SECOND, 0);
            calExp.set(java.util.Calendar.MILLISECOND, 0);

            isExpired = calExp.getTimeInMillis() < calToday.getTimeInMillis();
            Log.d("EXP_CHECK", "Expired: " + isExpired);

        } catch (Exception e) {
            e.printStackTrace();
            isExpired = false;
        }
    }

    private void showLogoutDialog() {
        final Dialog dialog = new Dialog(HomeMainActivity.this);
        dialog.setContentView(R.layout.dialog_lock_screen);
        tvBatal = dialog.findViewById(R.id.tvBatal);
        btIya   = dialog.findViewById(R.id.btIya);
        btIya.setOnClickListener(v -> {
            sessionManager.logOuts();
            Intent intent = new Intent(HomeMainActivity.this, PilihKaryawan.class);
            finishAffinity();
            startActivity(intent);
            dialog.dismiss();
            GlobalToast.ShowToast(HomeMainActivity.this, "Kunci Layar");
        });
        tvBatal.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private void getDataToko() {
        String kodePegawai = sessionManager.getKodeUsers();
        String idToko      = dbHelper.getIdTokoByKodePegawai(kodePegawai);
        Cursor cursor      = dbHelper.dataToko(idToko);
        if (cursor.moveToFirst()) {
            String email    = cursor.getString(cursor.getColumnIndexOrThrow(DataBaseHelper.tb_toko_email));
            String namaToko = cursor.getString(cursor.getColumnIndexOrThrow(DataBaseHelper.tb_toko_nama_toko));
            byte[] ivLogoByte = cursor.getBlob(5);
            Bitmap bitmap   = BitmapFactory.decodeByteArray(ivLogoByte, 0, ivLogoByte.length);
            ivMainLogo.setImageBitmap(bitmap);
            Header.setText(namaToko);
            SubHeader.setText(email);
        }
        cursor.close();
    }

    private void getTotalDataPesanan() {
        totalcart = dbHelper.getTotalPesananAll();
        invalidateOptionsMenu();
    }

    @Override
    protected void onResume() {
        super.onResume();
        getTotalDataPesanan();
        getDataToko();
        checkExpiredStatus();
        updateSalesMenuUI();
        showOrHideOwnerMenu(); // Fase 6 + GAP 3
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem menuItem = menu.findItem(R.id.nav_list_order);
        if (menuItem == null) {
            Log.e("Error", "Menu item is null");
        } else {
            View actionView = menuItem.getActionView();
            if (actionView == null) {
                Log.e("Error", "Action view is null");
            } else {
                TextView textCartItemCount = actionView.findViewById(R.id.notif_count_pesanan);
                if (totalcart.equals("0")) {
                    textCartItemCount.setVisibility(View.GONE);
                } else {
                    textCartItemCount.setText(totalcart);
                    textCartItemCount.setVisibility(View.VISIBLE);
                }
            }
        }
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment);
        return NavigationUI.navigateUp(navController, mAppBarConfiguration)
                || super.onSupportNavigateUp();
    }
}