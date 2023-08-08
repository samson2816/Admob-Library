package com.example.andmoduleads.activity;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.ads.control.ads.AdUtils;
import com.ads.control.funtion.AdCallback;
import com.ads.control.util.AppConstant;
import com.example.andmoduleads.R;
import com.google.android.gms.ads.AdSize;

public class TestSplash extends AppCompatActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
    }
}
