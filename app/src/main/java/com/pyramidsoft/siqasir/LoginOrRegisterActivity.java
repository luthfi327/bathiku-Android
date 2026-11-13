package com.pyramidsoft.siqasir;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

public class LoginOrRegisterActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login_or_register);

        Button btnLoginOnline = findViewById(R.id.btnLoginOnline);
        Button btnLogin = findViewById(R.id.btnLogin);
        Button btnRegister = findViewById(R.id.btnRegister);

        btnLoginOnline.setOnClickListener(v -> {
            startActivity(new Intent(this, OnlineLoginPageActivity.class));
        });

        btnLogin.setOnClickListener(v -> {
            startActivity(new Intent(this, LoginPageActivity.class));
        });

        btnRegister.setOnClickListener(v -> {
            startActivity(new Intent(this, RegistrasiToko.class));
        });
    }
}
