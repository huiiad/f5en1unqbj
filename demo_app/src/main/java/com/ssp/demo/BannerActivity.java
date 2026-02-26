package com.ssp.demo;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import android.util.Log;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.ssp.sdk.ad.BaseAd;
import com.ssp.sdk.ad.SspBannerAd;
import com.ssp.sdk.ad.SspInteractionListener;
import com.ssp.sdk.ad.SspLoadListener;

public class BannerActivity extends AppCompatActivity {

    private static final String TAG = "BannerActivity";
    private FrameLayout bannerContainer;
    private SspBannerAd bannerAd;
    private TextView tvLogs;
    private Button btnShow;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_banner);
        
        if (getActionBar() != null) {
            getActionBar().setDisplayHomeAsUpEnabled(true);
        }

        bannerContainer = findViewById(R.id.banner_container);
        tvLogs = findViewById(R.id.tv_logs);
        btnShow = findViewById(R.id.btn_show_banner);

        findViewById(R.id.btn_load_banner).setOnClickListener(v -> loadBanner());
        btnShow.setOnClickListener(v -> showBanner());
        findViewById(R.id.btn_load_and_show_banner).setOnClickListener(v -> loadAndShowBanner());
    }

    private void loadBanner() {
        log("Creating BannerAd...");
        
        // 1. Create BannerAd instance (using context, slotId) - defaults to screen width
        bannerAd = new SspBannerAd(this, "slot_banner_001");
        
        // 2. Set Listeners
        bannerAd.setLoadListener(new SspLoadListener() {
            @Override
            public void onAdLoaded(BaseAd ad) {
                log("onAdLoaded: Banner is ready");
                btnShow.setEnabled(true);
                Toast.makeText(BannerActivity.this, "Ad Loaded!", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onAdFailed(String error) {
                log("onAdFailed: " + error);
                btnShow.setEnabled(false);
            }
        });
        
        bannerAd.setInteractionListener(new SspInteractionListener() {
            @Override
            public void onImpression(String adId) {
                log("onImpression: Banner displayed (ID: " + adId + ")");
            }

            @Override
            public void onAdClicked(String adId) {
                log("onAdClicked: Banner clicked (ID: " + adId + ")");
            }
            
            @Override
            public void onAdShowFailed(String error) {
                log("onAdShowFailed: " + error);
            }
        });
        
        // Enable Auto Refresh for testing
        bannerAd.setAutoRefresh(false);

        // 3. Load Ad
        log("Loading Banner Ad...");
        bannerAd.loadAd();
    }

    private void showBanner() {
        if (bannerAd != null) {
            if (bannerAd.isValid()) {
                log("Showing Banner Ad...");
                bannerContainer.removeAllViews();
                bannerContainer.addView(bannerAd);
                bannerAd.showAd();
            } else {
                log("Banner Ad is not valid (expired or not loaded)");
            }
        }
    }

    private void loadAndShowBanner() {
        log("Load & Show Banner...");
        
        if (bannerAd != null) {
            bannerAd.destroy();
        }
        bannerAd = new SspBannerAd(this, "slot_banner_001");
        bannerAd.setAutoRefresh(true);
        
        bannerContainer.removeAllViews();
        bannerContainer.addView(bannerAd);

        bannerAd.setLoadListener(new SspLoadListener() {
            @Override
            public void onAdLoaded(BaseAd ad) {
                log("onAdLoaded (Auto)");
                if (ad.isValid()) {
                    // Since ad is SspBannerAd logic wrapper if coming from BaseAd, 
                    // or we just use our local reference.
                    // For Banner, we just call showAd() on the view.
                    bannerAd.showAd();
                }
            }

            @Override
            public void onAdFailed(String error) {
                log("onAdFailed: " + error);
            }
        });
        
        bannerAd.setInteractionListener(new SspInteractionListener() {
            @Override
            public void onImpression(String adId) {
                log("onImpression (ID: " + adId + ")");
            }

            @Override
            public void onAdClicked(String adId) {
                log("onAdClicked (ID: " + adId + ")");
            }
            
            @Override
            public void onAdShowFailed(String error) {
                log("onAdShowFailed: " + error);
            }
        });

        bannerAd.loadAndShow();
    }

    private void log(String msg) {
        Log.d(TAG, msg);
        runOnUiThread(() -> tvLogs.append(msg + "\n"));
    }

    @Override
    protected void onPause() {
        super.onPause();
        // SspBannerAd now handles lifecycle automatically
    }

    @Override
    protected void onResume() {
        super.onResume();
        // SspBannerAd now handles lifecycle automatically
    }

    @Override
    protected void onDestroy() {
        // SspBannerAd now handles lifecycle automatically
        super.onDestroy();
    }
    
    @Override
    public boolean onNavigateUp() {
        finish();
        return true;
    }
}
