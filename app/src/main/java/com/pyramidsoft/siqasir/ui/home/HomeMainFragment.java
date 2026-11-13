package com.pyramidsoft.siqasir.ui.home;

import static com.pyramidsoft.siqasir.db.DataBaseHelper.database_name;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.view.menu.ActionMenuItemView;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.lifecycle.ViewModelProvider;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.tabs.TabLayout;
import com.pyramidsoft.siqasir.BuatPesananAct;
import com.pyramidsoft.siqasir.BukaKasirActivity;
import com.pyramidsoft.siqasir.HomeMainActivity;
import com.pyramidsoft.siqasir.PaketLanggananActivity;
import com.pyramidsoft.siqasir.R;
import com.pyramidsoft.siqasir.adapter.FilterKategoriAtHomeAdapter;
import com.pyramidsoft.siqasir.databinding.FragmentHomeMainBinding;
import com.pyramidsoft.siqasir.db.DataBaseHelper;
import com.pyramidsoft.siqasir.kategori.HomeDiskonFragment;
import com.pyramidsoft.siqasir.kategori.HomeFavoritFragment;
import com.pyramidsoft.siqasir.kategori.HomeSemuaFragment;
import com.pyramidsoft.siqasir.kategori.VPAdapter;
import com.pyramidsoft.siqasir.model.KategoriModels;
import com.pyramidsoft.siqasir.utils.DataPesanan;
import com.pyramidsoft.siqasir.utils.DecimalsFormat;
import com.pyramidsoft.siqasir.utils.GlobalToast;
import com.pyramidsoft.siqasir.utils.ProgressDialog;
import com.pyramidsoft.siqasir.utils.SessionManager;

import org.json.JSONObject;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class HomeMainFragment extends Fragment {

    private FragmentHomeMainBinding binding;
    private HomeMainViewModel homeMainViewModel;
    Handler handler = new Handler();
    Runnable runnable;
    Activity activity;
    String totalcart = "0";
    private TabLayout tabLayout;
    private ViewPager viewPager;

    private Handler qrHandler = new Handler();
    private Runnable qrRunnable;
    private long sisaMillisQr = 0;
    private boolean isQrActive = false;

    SQLiteDatabase mDatabase;
    SessionManager sessionManager;
    ProgressDialog pDialog;
    DataBaseHelper dbHelper;
    public static ArrayList<KategoriModels> valuesWhere = new ArrayList<>();
    FilterKategoriAtHomeAdapter adapter;

    public BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String refresh = intent.getStringExtra("refresh");
            switch (refresh) {
                case "pesanan" :
                    checkDataPesanan();
                    break;
                case "gettotaldata" :
                    getTotalDataPesanan();
                    break;
            }
        }
    };

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentHomeMainBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        activity = getActivity();
        setHasOptionsMenu(true);
        homeMainViewModel = new ViewModelProvider(requireActivity()).get(HomeMainViewModel.class);

        TextView tabSemua = root.findViewById(R.id.tabSemua);
        TextView tabDiskon = root.findViewById(R.id.tabDiskon);
        TextView tabFavorit = root.findViewById(R.id.tabFavorit);
        View tabIndicator = root.findViewById(R.id.tabIndicator);
        viewPager = root.findViewById(R.id.viewPager);

        VPAdapter vpAdapter = new VPAdapter(getChildFragmentManager(),
                FragmentPagerAdapter.BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT);
        vpAdapter.addFragment(new HomeSemuaFragment(), "Semua");
        vpAdapter.addFragment(new HomeDiskonFragment(), "Diskon");
        vpAdapter.addFragment(new HomeFavoritFragment(), "Favorit");

        viewPager.setAdapter(vpAdapter);

        View.OnClickListener tabClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int position = 0;
                if (v.getId() == R.id.tabSemua) {
                    position = 0;
                } else if (v.getId() == R.id.tabDiskon) {
                    position = 1;
                } else if (v.getId() == R.id.tabFavorit) {
                    position = 2;
                }

                viewPager.setCurrentItem(position);
                animateIndicatorToPosition(position);
                updateTabTextColor(position);
                binding.etPencarianProduk.setText("");
            }
        };

        tabSemua.setOnClickListener(tabClickListener);
        tabDiskon.setOnClickListener(tabClickListener);
        tabFavorit.setOnClickListener(tabClickListener);

        viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                animateIndicatorOnScroll(position, positionOffset);
            }

            @Override
            public void onPageSelected(int position) {
                updateTabTextColor(position);
            }

            @Override
            public void onPageScrollStateChanged(int state) {
            }
        });

        viewPager.post(new Runnable() {
            @Override
            public void run() {
                viewPager.setCurrentItem(0);
                animateIndicatorToPosition(0);
                updateTabTextColor(0);

                int tabWidth = viewPager.getWidth() / 3;
                ViewGroup.LayoutParams params = tabIndicator.getLayoutParams();
                params.width = tabWidth - 4;
                tabIndicator.setLayoutParams(params);
            }
        });

        binding.btSearchKategori.setOnClickListener(this::onViewClicked);
        binding.relProduksi.setOnClickListener(this::onViewClicked);
        binding.relBukakasir.setOnClickListener(this::onViewClicked);

        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(mMessageReceiver,
                new IntentFilter("dataPesanan-state"));

        pDialog = new ProgressDialog(activity);
        sessionManager = new SessionManager(activity);
        dbHelper = new DataBaseHelper(activity);
        mDatabase = activity.openOrCreateDatabase(database_name, Context.MODE_PRIVATE, null);

        adapter = new FilterKategoriAtHomeAdapter(getDataKategori(), activity);

        binding.etPencarianProduk.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) { }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                Intent intent2 = new Intent("filterkategori-state");
                intent2.putExtra("refresh", "finditem");
                intent2.putExtra("find", String.valueOf(charSequence));
                LocalBroadcastManager.getInstance(activity).sendBroadcast(intent2);
            }

            @Override
            public void afterTextChanged(Editable editable) { }
        });

        getDataToko();
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(mMessageReceiver,
                new IntentFilter("datapesanan-state"));

        return root;
    }

    private void checkExpDateToko() {
        if (activity == null || binding == null) return;

        if (isQrActive) {
            binding.bannerExpWarning.setVisibility(View.GONE);
            return;
        }

        String expDateStr = dbHelper.getExpDateToko();

        try {
            SimpleDateFormat sdf = new SimpleDateFormat(
                    "yyyy-MM-dd HH:mm:ss",
                    Locale.getDefault()
            );

            Date expDate = sdf.parse(expDateStr);
            Date today = new Date();

            Calendar calToday = Calendar.getInstance();
            calToday.set(Calendar.HOUR_OF_DAY, 0);
            calToday.set(Calendar.MINUTE, 0);
            calToday.set(Calendar.SECOND, 0);
            calToday.set(Calendar.MILLISECOND, 0);

            Calendar calExp = Calendar.getInstance();
            calExp.setTime(expDate);
            calExp.set(Calendar.HOUR_OF_DAY, 0);
            calExp.set(Calendar.MINUTE, 0);
            calExp.set(Calendar.SECOND, 0);
            calExp.set(Calendar.MILLISECOND, 0);

            long diffMillis = calExp.getTimeInMillis() - calToday.getTimeInMillis();
            long diffDays = TimeUnit.MILLISECONDS.toDays(diffMillis);

            if (diffDays >= 0 && diffDays <= 7) {
                showExpWarning(diffDays);
            } else if (diffDays < 0) {
                showExpiredWarning();
            } else {
                binding.bannerExpWarning.setVisibility(View.GONE);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void showExpWarning(long sisaHari) {
        binding.bannerExpWarning.setVisibility(View.VISIBLE);

        String pesan;
        if (sisaHari == 0) {
            pesan = "Masa aktif toko berakhir hari ini";
        } else {
            pesan = "Masa aktif toko akan berakhir dalam " + sisaHari + " hari";
        }

        binding.tvExpWarning.setText(pesan);

        binding.bannerExpWarning.setBackgroundColor(
                getResources().getColor(android.R.color.holo_orange_light)
        );

        if ("owner".equalsIgnoreCase(sessionManager.getHakAkses())) {
            binding.btnPerpanjang.setVisibility(View.VISIBLE);

            binding.btnPerpanjang.setOnClickListener(v -> {
                startActivity(new Intent(activity, PaketLanggananActivity.class));
            });
        } else {
            binding.btnPerpanjang.setVisibility(View.GONE);
        }
    }

    private void showExpiredWarning() {

        binding.bannerExpWarning.setVisibility(View.VISIBLE);

        binding.tvExpWarning.setText(
                "Masa Aktif Habis, Masuk Mode Gratis Beberapa Fitur Dibatasi"
        );

        if ("owner".equalsIgnoreCase(sessionManager.getHakAkses())) {
            binding.btnPerpanjang.setVisibility(View.VISIBLE);

            binding.btnPerpanjang.setOnClickListener(v -> {
                startActivity(new Intent(activity, PaketLanggananActivity.class));
            });
        } else {
            binding.btnPerpanjang.setVisibility(View.GONE);
        }
    }

    private void animateIndicatorOnScroll(int position, float positionOffset) {
        View tabIndicator = getView().findViewById(R.id.tabIndicator);
        if (tabIndicator == null) return;

        int tabWidth = viewPager.getWidth() / 3;
        float translationX = (position + positionOffset) * tabWidth;

        tabIndicator.setTranslationX(translationX);
    }

    private void animateIndicatorToPosition(int position) {
        View tabIndicator = getView().findViewById(R.id.tabIndicator);
        if (tabIndicator == null) return;

        int tabWidth = viewPager.getWidth() / 3;
        float targetX = position * tabWidth;

        tabIndicator.animate()
                .translationX(targetX)
                .setDuration(250)
                .setInterpolator(new android.view.animation.DecelerateInterpolator())
                .start();
    }

    private void updateTabTextColor(int position) {
        if (getView() == null) return;

        TextView tabSemua = getView().findViewById(R.id.tabSemua);
        TextView tabDiskon = getView().findViewById(R.id.tabDiskon);
        TextView tabFavorit = getView().findViewById(R.id.tabFavorit);

        if (tabSemua == null || tabDiskon == null || tabFavorit == null) return;

        tabSemua.setTextColor(getResources().getColor(R.color.primary_navy));
        tabDiskon.setTextColor(getResources().getColor(R.color.primary_navy));
        tabFavorit.setTextColor(getResources().getColor(R.color.primary_navy));

        switch (position) {
            case 0:
                tabSemua.setTextColor(getResources().getColor(R.color.white));
                break;
            case 1:
                tabDiskon.setTextColor(getResources().getColor(R.color.white));
                break;
            case 2:
                tabFavorit.setTextColor(getResources().getColor(R.color.white));
                break;
        }
    }

    private void getTotalDataPesanan() {
        totalcart = dbHelper.getTotalPesananAll();
        activity.invalidateOptionsMenu();
    }

    private void getDataToko() {
        String kodePegawai = sessionManager.getKodeUsers();
        String idToko = dbHelper.getIdTokoByKodePegawai(kodePegawai);
        Cursor cursor = dbHelper.dataToko(idToko);

        if (cursor != null && cursor.moveToFirst()) {
            String email = cursor.getString(cursor.getColumnIndexOrThrow(DataBaseHelper.tb_toko_email));
            String namaToko = cursor.getString(cursor.getColumnIndexOrThrow(DataBaseHelper.tb_toko_nama_toko));

            int logoIndex = cursor.getColumnIndex("logo");
            if (logoIndex != -1 && !cursor.isNull(logoIndex)) {
                byte[] ivLogoByte = cursor.getBlob(logoIndex);
                Bitmap bitmap = BitmapFactory.decodeByteArray(ivLogoByte, 0, ivLogoByte.length);
                binding.IvLogo.setImageBitmap(bitmap);
            }
            binding.tvNamaOutlet.setText(namaToko);
            binding.tvEmail.setText(email);
        }
        if (cursor != null) {
            cursor.close();
        }
    }

    private void checkPendingQr() {

        if (!"owner".equalsIgnoreCase(sessionManager.getHakAkses())) return;

        String kodeOwner = sessionManager.getKodeOwnerutama();

        String url = "https://siqasir.awandigital.id/api/qris/status?kode_owner=" + kodeOwner;

        Log.d("QR_CHECK", "Request URL: " + url);

        okhttp3.Request request = new okhttp3.Request.Builder()
                .url(url)
                .get()
                .build();

        new okhttp3.OkHttpClient().newCall(request).enqueue(new okhttp3.Callback() {

            @Override
            public void onFailure(okhttp3.Call call, IOException e) {
            }

            @Override
            public void onResponse(okhttp3.Call call, okhttp3.Response response) throws IOException {
                String res = response.body().string();
                try {

                    JSONObject obj = new JSONObject(res);

                    String status = obj.optString("status", "");
                    String expiredAt = obj.optString("expired_at", "");

                    if ("PENDING".equals(status)) {
                        long sisa = getRemainingTimeNormal(expiredAt);
                        if (sisa > 0) {
                            sisaMillisQr = sisa;
                            isQrActive = true;
                            requireActivity().runOnUiThread(() -> {
                                binding.bannerExpWarning.setVisibility(View.GONE);
                                showQrBanner();
                                startQrCountdown();
                            });
                        } else {
                            isQrActive = false;
                            requireActivity().runOnUiThread(() -> hideQrBanner());
                        }
                    } else {
                        isQrActive = false;

                        requireActivity().runOnUiThread(() -> hideQrBanner());
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private long getRemainingTimeNormal(String expiredAt){

        try {

            Date expDate;

            if (expiredAt.contains(".")) {
                java.text.SimpleDateFormat sdf =
                        new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault());
                expDate = sdf.parse(expiredAt);
            } else {
                java.text.SimpleDateFormat sdf =
                        new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
                expDate = sdf.parse(expiredAt);
            }

            if (expDate == null) {
                Log.e("QR_TIME", "Parse NULL!");
                return 0;
            }

            long now = System.currentTimeMillis();
            long diff = expDate.getTime() - now;

            return Math.max(diff, 0);

        } catch (Exception e) {
            Log.e("QR_TIME", "Parse gagal: " + e.getMessage());
            return 0;
        }
    }

    private void showQrBanner() {

        binding.bannerQrPending.setVisibility(View.VISIBLE);

        binding.btnBayarQr.setOnClickListener(v -> {
            startActivity(new Intent(getActivity(), PaketLanggananActivity.class));
        });
    }

    private void hideQrBanner() {
        binding.bannerQrPending.setVisibility(View.GONE);
    }

    private void startQrCountdown() {

        if (qrRunnable != null) {
            qrHandler.removeCallbacks(qrRunnable);
        }

        qrRunnable = new Runnable() {
            @Override
            public void run() {

                if (sisaMillisQr <= 0) {
                    hideQrBanner();
                    return;
                }

                long menit = sisaMillisQr / 60000;
                long detik = (sisaMillisQr % 60000) / 1000;

                binding.tvQrWarning.setText(
                        "Selesaikan pembayaran QR (" +
                                String.format("%02d:%02d", menit, detik) + ")"
                );

                sisaMillisQr -= 1000;

                qrHandler.postDelayed(this, 1000);
            }
        };

        qrHandler.post(qrRunnable);
    }

    @Override
    public void onResume() {
        super.onResume();

        checkDataPesanan();
        checkPendingQr();
        checkExpDateToko();

        binding.tvNamaUser.setText("Buka Kasir " + sessionManager.getNama());

        DateFormat df = new SimpleDateFormat("EEEE, dd MMMM yyyy, HH:mm", new Locale("id", "ID"));

        String date = df.format(Calendar.getInstance().getTime());
        binding.tvDateTime.setText(date);
        binding.linearLayout.setEnabled(false);
    }

    public void checkDataPesanan() {
        if (DataPesanan.getArrayPesanan().isEmpty()) {

            if (sessionManager.getBukaKasir().equals("open")) {
                binding.relProduksi.setVisibility(View.GONE);
                binding.relBukakasir.setVisibility(View.GONE);
                binding.relDefault.setVisibility(View.VISIBLE);
            } else if (sessionManager.getBukaKasir().equals("close")) {
                binding.relDefault.setVisibility(View.GONE);
                binding.relBukakasir.setVisibility(View.VISIBLE);
            }

        } else {

            if (sessionManager.getBukaKasir().equals("open")) {
                binding.relDefault.setVisibility(View.GONE);
                binding.relBukakasir.setVisibility(View.GONE);
                binding.relProduksi.setVisibility(View.VISIBLE);
            } else if (sessionManager.getBukaKasir().equals("close")) {
                GlobalToast.ShowToast(activity, "Anda Harus Buka Toko Terlebih Dahulu.");
            }

        }
        binding.tvQtyP.setText(String.valueOf(DataPesanan.getArrayPesanan().size()));
        binding.tvTotalHrgP.setText("Rp. " + DecimalsFormat.priceWithoutDecimal(DataPesanan.getTotalHargaPesanan()));
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        inflater.inflate(R.menu.list_pesanan, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public void onPrepareOptionsMenu (@NonNull Menu menu) {
        super.onPrepareOptionsMenu(menu);

        MenuItem menuItem = menu.findItem(R.id.nav_list_order);
        View actionView = menuItem.getActionView();

        if (actionView instanceof ActionMenuItemView) {
            ActionMenuItemView actionMenuItemView = (ActionMenuItemView) actionView;
            actionMenuItemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onOptionsItemSelected(menuItem);
                }
            });
            TextView textCartItemCount = actionMenuItemView.findViewById(R.id.notif_count_pesanan);
            if (totalcart.equals("0")) {
                textCartItemCount.setText(totalcart);
                textCartItemCount.setVisibility(View.GONE);
            } else {
                textCartItemCount.setText(totalcart);
                textCartItemCount.setVisibility(View.VISIBLE);
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.nav_list_order) {
            HomeMainActivity.navController.navigate(R.id.nav_daftar_pesanan);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private ArrayList<KategoriModels> getDataKategori() {
        String kodePegawai = sessionManager.getKodeUsers();
        String idToko = dbHelper.getIdTokoByKodePegawai(kodePegawai);
        Cursor cursorproduct = dbHelper.listKategori(idToko);

        ArrayList<KategoriModels> data = new ArrayList<>();

        if (cursorproduct.moveToFirst()) {
            do {
                data.add(new KategoriModels(
                        cursorproduct.getString(3),
                        cursorproduct.getString(0),
                        cursorproduct.getString(1)
                ));
            } while (cursorproduct.moveToNext());
        }
        if (data.isEmpty()) {

        }

        cursorproduct.close();
        return data;
    }

    public static boolean isExist(KategoriModels model) {
        for (int i = 0; i < valuesWhere.size(); i++) {
            if (valuesWhere.get(i).getKategori().equals(model.getKategori())) {
                return true;
            }
        }
        return false;
    }

    public static void addWhereValue(KategoriModels models) {
        if (!isExist(models)) {
            valuesWhere.add(models);
        }
    }

    public static void remoceWhereValue(KategoriModels models) {
        valuesWhere.remove(models);
    }

    public void onViewClicked(View view) {
        Intent intent;

        if (view.getId() == R.id.btSearchKategori) {
            final BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(activity, R.style.BottomShetDialogTheme);
            View bottomSheetView = LayoutInflater.from(activity).inflate(R.layout.lsb_kategori, activity.findViewById(R.id.ll_bottom_sheet));
            RecyclerView rvFilterKategori = bottomSheetView.findViewById(R.id.rvFilterKategori);
            Button btnTerapkanFilter = bottomSheetView.findViewById(R.id.btnTerapkanFilter);
            rvFilterKategori.setHasFixedSize(true);
            GridLayoutManager layoutManager = new GridLayoutManager(activity, 1, GridLayoutManager.VERTICAL, false);

            rvFilterKategori.setLayoutManager(layoutManager);

            rvFilterKategori.setAdapter(adapter);

            btnTerapkanFilter.setOnClickListener(view1 -> {
                Intent intent2 = new Intent("filterkategori-state");
                intent2.putExtra("refresh", "dataproduk");
                LocalBroadcastManager.getInstance(activity).sendBroadcast(intent2);
                bottomSheetDialog.cancel();
            });

            bottomSheetDialog.setContentView(bottomSheetView);
            bottomSheetDialog.show();
        } else if (view.getId() == R.id.rel_produksi) {
            intent = new Intent(activity, BuatPesananAct.class);
            startActivity(intent);
        } else if (view.getId() == R.id.rel_bukakasir) {
            intent = new Intent(activity, BukaKasirActivity.class);
            startActivity(intent);
        }
    }

}