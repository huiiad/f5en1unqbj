package com.ssp.demo;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.ssp.sdk.ad.SspInteractionListener;
import com.ssp.sdk.ad.SspLoadListener;
import com.ssp.sdk.ad.SspVideoAd;

public class VideoActivity extends Activity {

    private static final String TAG = "VideoActivity";
    private FrameLayout videoContainer;
    private TextView tvLogs;
    private Button btnShowView;
    private Button btnShowFull;
    private SspVideoAd videoAd;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        if (getActionBar() != null) {
            getActionBar().setDisplayHomeAsUpEnabled(true);
        }

        // Root Layout
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(32, 32, 32, 32);
        
        // Buttons
        LinearLayout btnLayout = new LinearLayout(this);
        btnLayout.setOrientation(LinearLayout.HORIZONTAL);
        
        Button btnLoad = new Button(this);
        btnLoad.setText("Load Video");
        btnLoad.setOnClickListener(v -> loadVideoAd());
        btnLayout.addView(btnLoad, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        btnShowView = new Button(this);
        btnShowView.setText("Show View");
        btnShowView.setEnabled(false);
        btnShowView.setOnClickListener(v -> showVideoAdView());
        btnLayout.addView(btnShowView, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        
        btnShowFull = new Button(this);
        btnShowFull.setText("Show Full");
        btnShowFull.setEnabled(false);
        btnShowFull.setOnClickListener(v -> showVideoAdFull());
        btnLayout.addView(btnShowFull, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        
        root.addView(btnLayout);

        // Container for Ad
        videoContainer = new FrameLayout(this);
        videoContainer.setBackgroundColor(Color.BLACK);
        LinearLayout.LayoutParams containerParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 800);
        containerParams.topMargin = 32;
        containerParams.bottomMargin = 32;
        root.addView(videoContainer, containerParams);

        // Logs
        TextView tvLabel = new TextView(this);
        tvLabel.setText("Logs:");
        root.addView(tvLabel);

        ScrollView scrollView = new ScrollView(this);
        tvLogs = new TextView(this);
        scrollView.addView(tvLogs);
        root.addView(scrollView, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        setContentView(root);
    }

    private void loadVideoAd() {
        log("Loading Video Ad...");
        btnShowView.setEnabled(false);
        btnShowFull.setEnabled(false);
        if (videoAd != null) {
            videoAd = null;
        }
        videoContainer.removeAllViews(); // Clear previous

        videoAd = new SspVideoAd(this, "slot_video_001");
        videoAd.enableAutoCache(true); // Test caching too
        videoAd.setLoadListener(new com.ssp.sdk.ad.SspLoadListener() {
            @Override
            public void onAdLoaded(com.ssp.sdk.ad.BaseAd ad) {
                log("Video Ad Loaded");
                btnShowView.setEnabled(true);
                btnShowFull.setEnabled(true);
                Toast.makeText(VideoActivity.this, "Video Ready", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onAdFailed(String error) {
                log("Video Ad Failed: " + error);
                btnShowView.setEnabled(false);
                btnShowFull.setEnabled(false);
            }
        });
        
        videoAd.setInteractionListener(new com.ssp.sdk.ad.SspInteractionListener() {
            @Override
            public void onImpression(String adId) {
                log("Video Ad Impression (Started) ID: " + adId);
            }

            @Override
            public void onAdClicked(String adId) {
                log("Video Ad Clicked ID: " + adId);
            }
            
            @Override
            public void onAdShowFailed(String error) {
                log("Video Show Failed: " + error);
            }
        });
        
        videoAd.loadAd();
    }

    private void showVideoAdView() {
        if (videoAd != null) {
            log("Showing Video Ad (View Mode)...");
            videoAd.show(videoContainer);
        }
    }
    
    private void showVideoAdFull() {
        if (videoAd != null) {
            log("Showing Video Ad (FullScreen Mode)...");
            videoAd.showAd(this);
        }
    }

    private void log(String msg) {
        Log.d(TAG, msg);
        runOnUiThread(() -> tvLogs.append(msg + "\n"));
    }
    
    @Override
    public boolean onNavigateUp() {
        finish();
        return true;
    }
}