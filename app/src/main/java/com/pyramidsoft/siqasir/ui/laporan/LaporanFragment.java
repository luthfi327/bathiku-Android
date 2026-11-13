package com.pyramidsoft.siqasir.ui.laporan;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.pyramidsoft.siqasir.R;
import com.pyramidsoft.siqasir.databinding.FragmentLaporanBinding;
import com.pyramidsoft.siqasir.db.DataBaseHelper;
import com.pyramidsoft.siqasir.ui.laporan.fragment.KasFragment;
import com.pyramidsoft.siqasir.ui.laporan.fragment.PenjualanFragment;
import com.pyramidsoft.siqasir.ui.laporan.fragment.StokFragment;
import com.pyramidsoft.siqasir.utils.ProgressDialog;
import com.pyramidsoft.siqasir.utils.SessionManager;

import java.util.ArrayList;

public class LaporanFragment extends Fragment {

    private FragmentLaporanBinding binding;
    Activity activity;
    SessionManager sessionManager;
    ProgressDialog pDialog;
    DataBaseHelper dbHelper;

    ArrayList<String> tokoList = new ArrayList<>();
    ArrayList<Integer> tokoIdList = new ArrayList<>();
    ArrayList<Integer> selectedTokoIndex = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentLaporanBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        activity = getActivity();
        pDialog = new ProgressDialog(activity);
        sessionManager = new SessionManager(activity);
        dbHelper = new DataBaseHelper(activity);

        VPAdapterLaporan vpAdapter = new VPAdapterLaporan(getChildFragmentManager(),
                FragmentPagerAdapter.BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT
        );
        vpAdapter.addFragment(new PenjualanFragment(), "Penjualan");
        vpAdapter.addFragment(new KasFragment(), "Kas");
        vpAdapter.addFragment(new StokFragment(), "Stok");

        binding.viewPager.setAdapter(vpAdapter);

        View.OnClickListener tabClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int position = 0;
                if (v.getId() == R.id.tabPenjualan) {
                    position = 0;
                } else if (v.getId() == R.id.tabKas) {
                    position = 1;
                } else if (v.getId() == R.id.tabStok) {
                    position = 2;
                }

                binding.viewPager.setCurrentItem(position);
                animateIndicatorToPosition(position);
                updateTabTextColor(position);
            }
        };

        binding.tabPenjualan.setOnClickListener(tabClickListener);
        binding.tabKas.setOnClickListener(tabClickListener);
        binding.tabStok.setOnClickListener(tabClickListener);

        // Setup ViewPager listener untuk SWIPE dengan animasi indicator
        binding.viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                // Animasi smooth indicator saat swipe
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

        // SET DEFAULT TAB SETELAH VIEW SELESAI RENDER
        binding.viewPager.post(new Runnable() {
            @Override
            public void run() {
                // Set default ke tab Semua
                binding.viewPager.setCurrentItem(0);
                animateIndicatorToPosition(0);
                updateTabTextColor(0);

                // Set initial indicator width
                int tabWidth = binding.viewPager.getWidth() / 3;
                ViewGroup.LayoutParams params = binding.tabIndicator.getLayoutParams();
                params.width = tabWidth - 4; // minus margin
                binding.tabIndicator.setLayoutParams(params);
            }
        });

        String hakAkses = sessionManager.getHakAkses();
        if (hakAkses != null && hakAkses.equalsIgnoreCase("Owner")) {
            binding.layoutSelectToko.setVisibility(View.VISIBLE);
            loadListToko();
            setupMultiSelectToko();
        } else {
            binding.layoutSelectToko.setVisibility(View.GONE);
        }

        return root;
    }

    // Animasi indicator saat scroll/swipe
    private void animateIndicatorOnScroll(int position, float positionOffset) {
        View tabIndicator = getView().findViewById(R.id.tabIndicator);
        if (tabIndicator == null) return;

        int tabWidth = binding.viewPager.getWidth() / 3;
        float translationX = (position + positionOffset) * tabWidth;

        tabIndicator.setTranslationX(translationX);
    }

    // Animasi indicator saat click tab
    private void animateIndicatorToPosition(int position) {
        View tabIndicator = getView().findViewById(R.id.tabIndicator);
        if (tabIndicator == null) return;

        int tabWidth = binding.viewPager.getWidth() / 3;
        float targetX = position * tabWidth;

        tabIndicator.animate()
                .translationX(targetX)
                .setDuration(250)
                .setInterpolator(new android.view.animation.DecelerateInterpolator())
                .start();
    }

    // Update text color tab
    private void updateTabTextColor(int position) {
        if (getView() == null) return;

        if (binding.tabPenjualan == null || binding.tabKas == null || binding.tabStok == null) return;

        // Reset semua text color
        binding.tabPenjualan.setTextColor(getResources().getColor(R.color.primary_navy));
        binding.tabKas.setTextColor(getResources().getColor(R.color.primary_navy));
        binding.tabStok.setTextColor(getResources().getColor(R.color.primary_navy));

        // Set selected text color
        switch (position) {
            case 0:
                binding.tabPenjualan.setTextColor(getResources().getColor(R.color.white));
                break;
            case 1:
                binding.tabKas.setTextColor(getResources().getColor(R.color.white));
                break;
            case 2:
                binding.tabStok.setTextColor(getResources().getColor(R.color.white));
                break;
        }
    }

    private void loadListToko() {
        tokoList.clear();
        tokoIdList.clear();

        tokoList.addAll(dbHelper.getAllTokoNames());
        tokoIdList.addAll(dbHelper.getAllTokoIds());

        android.util.Log.d("LaporanFragment", "Toko Names: " + tokoList.toString());
        android.util.Log.d("LaporanFragment", "Toko IDs: " + tokoIdList.toString());
    }

    private void setupMultiSelectToko() {
        binding.spinnerMultiToko.setOnClickListener(v -> {
            boolean[] checkedItems = new boolean[tokoList.size()];
            for (int i = 0; i < tokoList.size(); i++) {
                checkedItems[i] = selectedTokoIndex.contains(i);
            }

            AlertDialog.Builder builder = new AlertDialog.Builder(activity);
            builder.setTitle("Pilih Toko")
                    .setMultiChoiceItems(tokoList.toArray(new String[0]), checkedItems, (dialog, which, isChecked) -> {
                        if (isChecked) selectedTokoIndex.add(which);
                        else selectedTokoIndex.remove(Integer.valueOf(which));
                    })
                    .setPositiveButton("OK", (dialog, which) -> {
                        StringBuilder selectedNames = new StringBuilder();
                        for (int i = 0; i < selectedTokoIndex.size(); i++) {
                            selectedNames.append(tokoList.get(selectedTokoIndex.get(i)));
                            if (i < selectedTokoIndex.size() - 1) selectedNames.append(", ");
                        }
                        binding.spinnerMultiToko.setText(selectedNames.toString());
                    })
                    .setNegativeButton("Batal", (dialog, which) -> dialog.dismiss())
                    .show();
        });
    }

    public ArrayList<Integer> getSelectedTokoIds() {
        ArrayList<Integer> selectedIds = new ArrayList<>();
        android.util.Log.d("LaporanFragment", "Selected toko IDs: " + selectedIds.toString());
        for (int index : selectedTokoIndex) {
            selectedIds.add(tokoIdList.get(index));
        }
        return selectedIds;
    }
}
