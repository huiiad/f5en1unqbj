package com.ssp.sdk.ad;

import android.app.Activity;
import android.os.Bundle;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;

public class SspFullScreenVideoActivity extends Activity {
    public static final String EXTRA_AD_ID = "ad_slot_id";
    
    private SspVideoView videoView;
    private SspVideoAd videoAd;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        String adId = getIntent().getStringExtra(EXTRA_AD_ID);
        if (adId != null) {
            videoAd = SspVideoAd.getActiveAd(adId);
        }

        if (videoAd == null || !videoAd.isValid()) {
            finish();
            return;
        }

        FrameLayout layout = new FrameLayout(this);
        layout.setBackgroundColor(0xFF000000); // Black
        setContentView(layout);

        videoView = new SspVideoView(this);
        videoView.setAdController(videoAd);
        layout.addView(videoView, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        videoView.start();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (videoView != null) videoView.pause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (videoView != null) videoView.start();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (videoView != null) videoView.destroy();
    }
}
