package com.pyramidsoft.siqasir.ui;

import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

public abstract class BaseFragment extends Fragment {

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        requireActivity().getWindow().setStatusBarColor(
                ContextCompat.getColor(requireContext(), android.R.color.white)
        );

        addFooter(view);
    }

    private void addFooter(@NonNull View rootView) {
        if (!(rootView instanceof ViewGroup)) return;

        ViewGroup root = (ViewGroup) rootView;

        // Cek apakah footer sudah ada
        if (root.findViewWithTag("footer_siqasir") != null) return;

        TextView footer = new TextView(requireContext());
        footer.setTag("footer_siqasir");
        footer.setText("Bathiku by PyramidSoft");
        footer.setTextSize(14f);
        footer.setTextColor(0xFF000000); // Hitam
        footer.setGravity(Gravity.CENTER);
        footer.setPadding(0, 16, 0, 16);

        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
        );
        params.gravity = Gravity.BOTTOM;
        footer.setLayoutParams(params);

        // Bungkus root view dengan FrameLayout supaya footer bisa overlay
        FrameLayout frame = new FrameLayout(requireContext());
        frame.setLayoutParams(new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
        ));

        root.removeAllViews();
        frame.addView(root);
        frame.addView(footer);

        if (root.getParent() instanceof ViewGroup) {
            ViewGroup parent = (ViewGroup) root.getParent();
            parent.removeView(root);
            parent.addView(frame);
        }
    }
}
