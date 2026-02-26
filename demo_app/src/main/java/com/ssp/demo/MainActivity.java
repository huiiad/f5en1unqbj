package com.ssp.demo;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.ssp.sdk.SspSdk;
import com.ssp.sdk.ad.SspBannerAd;
import com.ssp.sdk.ad.BaseAd;
import com.ssp.sdk.ad.SspLoadListener;
import com.ssp.sdk.ad.SspInteractionListener;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "DemoApp";
    private TextView tvLog;
    private FrameLayout mainBannerContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvLog = findViewById(R.id.tv_log);
        mainBannerContainer = findViewById(R.id.main_banner_container);

        // Initialization Buttons
        findViewById(R.id.btn_init).setOnClickListener(v -> initSdkManual());
        findViewById(R.id.btn_init_manifest).setOnClickListener(v -> initSdkManifest());

        // Navigation Buttons
        findViewById(R.id.btn_banner_activity).setOnClickListener(v -> {
            startActivity(new Intent(this, BannerActivity.class));
        });
        findViewById(R.id.btn_native_activity).setOnClickListener(v -> {
            startActivity(new Intent(this, NativeActivity.class));
        });
        findViewById(R.id.btn_video_activity).setOnClickListener(v -> {
            startActivity(new Intent(this, VideoActivity.class));
        });
        
        // Auto-load bottom banner ad if SDK is ready, or wait for init
        // For demo, we trigger it after manual init or immediately if manifest init works
    }

    private void log(String message) {
        Log.d(TAG, message);
        tvLog.append(message + "\n");
    }

    private void initSdkManual() {
        SspSdk.getInstance().init(this, getPackageName());
        log("SDK Initialized with Manual ID: " + SspSdk.getInstance().getAppId());
        loadBottomBanner();
    }

    private void initSdkManifest() {
        SspSdk.getInstance().init(this);
        log("SDK Initialized with Manifest ID: " + SspSdk.getInstance().getAppId());
        loadBottomBanner();
    }

    private void loadBottomBanner() {
        if (mainBannerContainer.getChildCount() > 1) {
            // Already loaded (textView is index 0)
            return;
        }
        
        log("Loading Main Bottom Banner...");
        SspBannerAd bannerAd = new SspBannerAd(this, "slot_banner_001");
        
        // Add to view hierarchy immediately for loadAndShow or simple load
        mainBannerContainer.removeAllViews();
        mainBannerContainer.addView(bannerAd);
        
        bannerAd.setLoadListener(new SspLoadListener() {
            @Override
            public void onAdLoaded(BaseAd ad) {
                log("Main Banner Loaded");
            }

            @Override
            public void onAdFailed(String error) {
                log("Main Banner Failed: " + error);
            }
        });
        
        bannerAd.setInteractionListener(new SspInteractionListener() {
            @Override
            public void onImpression(String adId) {
                log("Main Banner Impression (ID: " + adId + ")");
            }

            @Override
            public void onAdClicked(String adId) {
                log("Main Banner Clicked (ID: " + adId + ")");
            }
            
            @Override
            public void onAdShowFailed(String error) {
                log("Main Banner Show Failed: " + error);
            }
        });
        
        bannerAd.loadAndShow();
    }
}