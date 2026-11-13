package com.pyramidsoft.siqasir;

import android.os.Bundle;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import com.pyramidsoft.siqasir.databinding.ActivityBantuanBinding;

public class BantuanActivity extends AppCompatActivity {

    private ActivityBantuanBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityBantuanBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot()); // ⬅️ WAJIB INI

        setSupportActionBar(binding.toolbar);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setTitle(""); // biar gak double title
        }

        binding.toolbar.setNavigationOnClickListener(v -> finish());
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                this.finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

}
