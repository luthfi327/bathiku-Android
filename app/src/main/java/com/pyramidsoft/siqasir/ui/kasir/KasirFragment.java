package com.pyramidsoft.siqasir.ui.kasir;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;

import com.pyramidsoft.siqasir.R;

public class KasirFragment extends Fragment {

    private KasirViewModel kasirViewModel;

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        kasirViewModel = ViewModelProviders.of(this).get(KasirViewModel.class);
        View root = inflater.inflate(R.layout.fragment_kasir, container, false);
        final TextView textView = root.findViewById(R.id.text_slideshow);
        kasirViewModel.getText().observe(getViewLifecycleOwner(), new Observer<String>() {
            @Override
            public void onChanged(String s) {
                textView.setText(s);
            }
        });
        return root;
    }
}
