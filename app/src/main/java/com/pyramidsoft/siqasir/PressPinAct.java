package com.pyramidsoft.siqasir;

import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.pyramidsoft.siqasir.databinding.ActivityPressPinBinding;

public class PressPinAct extends AppCompatActivity {

    private ActivityPressPinBinding binding;

    String number;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityPressPinBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.vbd01.setOnClickListener(this::onViewClicked);
        binding.vbd02.setOnClickListener(this::onViewClicked);
        binding.vbd03.setOnClickListener(this::onViewClicked);
        binding.vbd04.setOnClickListener(this::onViewClicked);
        binding.vbd05.setOnClickListener(this::onViewClicked);
        binding.vbd06.setOnClickListener(this::onViewClicked);
        binding.tv0.setOnClickListener(this::onViewClicked);
        binding.tv1.setOnClickListener(this::onViewClicked);
        binding.tv2.setOnClickListener(this::onViewClicked);
        binding.tv3.setOnClickListener(this::onViewClicked);
        binding.tv4.setOnClickListener(this::onViewClicked);
        binding.tv5.setOnClickListener(this::onViewClicked);
        binding.tv6.setOnClickListener(this::onViewClicked);
        binding.tv7.setOnClickListener(this::onViewClicked);
        binding.tv8.setOnClickListener(this::onViewClicked);
        binding.tv9.setOnClickListener(this::onViewClicked);
        binding.btKonfirmasi.setOnClickListener(this::onViewClicked);
        binding.tvDelete.setOnClickListener(this::onViewClicked);
        binding.tvPreview.setOnClickListener(this::onViewClicked);
        binding.tvClear.setOnClickListener(this::onViewClicked);
    }

    public void onViewClicked(View view) {
        if (view.getId() == R.id.tv_1) {
            number = "1";
        } else if (view.getId() == R.id.tv_2) {
            number = "2";
        } else if (view.getId() == R.id.tv_3) {
            number = "3";
        } else if (view.getId() == R.id.tv_4) {
            number = "4";
        } else if (view.getId() == R.id.tv_5) {
            number = "5";
        } else if (view.getId() == R.id.tv_6) {
            number = "6";
        } else if (view.getId() == R.id.tv_7) {
            number = "7";
        } else if (view.getId() == R.id.tv_8) {
            number = "8";
        } else if (view.getId() == R.id.tv_9) {
            number = "9";
        } else if (view.getId() == R.id.tv_clear) {
        } else if (view.getId() == R.id.tv_0) {
        } else if (view.getId() == R.id.tv_delete) {
        } else if (view.getId() == R.id.btKonfirmasi) {
        }
    }

}
