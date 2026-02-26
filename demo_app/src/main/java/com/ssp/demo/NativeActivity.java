package com.ssp.demo;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.ssp.sdk.ad.SspInteractionListener;
import com.ssp.sdk.ad.SspLoadListener;
import com.ssp.sdk.ad.SspNativeAd;
import com.ssp.sdk.ad.BaseAd;

public class NativeActivity extends Activity {

    private static final String TAG = "NativeActivity";
    private FrameLayout nativeAdContainer;
    private TextView tvLogs;
    private SspNativeAd nativeAd;

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
        
        // Load Button
        Button btnLoad = new Button(this);
        btnLoad.setText("Load Native Ad");
        btnLoad.setOnClickListener(v -> loadNativeAd());
        root.addView(btnLoad, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        // Container for Ad
        nativeAdContainer = new FrameLayout(this);
        nativeAdContainer.setBackgroundColor(Color.LTGRAY);
        LinearLayout.LayoutParams containerParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 800);
        containerParams.topMargin = 32;
        containerParams.bottomMargin = 32;
        root.addView(nativeAdContainer, containerParams);

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

    private void loadNativeAd() {
        log("Loading Native Ad...");
        if (nativeAd != null) {
            nativeAd = null; // Reset
        }
        nativeAdContainer.removeAllViews();

        nativeAd = new SspNativeAd(this, "slot_native_001");
        
        nativeAd.setLoadListener(new SspLoadListener() {
            @Override
            public void onAdLoaded(BaseAd ad) {
                log("Native Ad Loaded");
                renderNativeAd();
            }

            @Override
            public void onAdFailed(String error) {
                log("Native Ad Failed: " + error);
                Toast.makeText(NativeActivity.this, "Failed: " + error, Toast.LENGTH_SHORT).show();
            }
        });
        
        nativeAd.setInteractionListener(new SspInteractionListener() {
            @Override
            public void onImpression(String adId) {
                log("Native Ad Impression (ID: " + adId + ")");
            }

            @Override
            public void onAdClicked(String adId) {
                log("Native Ad Clicked (ID: " + adId + ")");
                Toast.makeText(NativeActivity.this, "Ad Clicked!", Toast.LENGTH_SHORT).show();
            }
            
            @Override
            public void onAdShowFailed(String error) {
                log("Native Ad Show Failed: " + error);
            }
        });
        
        nativeAd.loadAd();
    }

    private void renderNativeAd() {
        if (nativeAd == null) return;

        // Create Native Ad Layout
        LinearLayout adView = new LinearLayout(this);
        adView.setOrientation(LinearLayout.VERTICAL);
        adView.setPadding(20, 20, 20, 20);
        adView.setBackgroundColor(Color.WHITE);

        // Icon + Title + Body
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        
        ImageView icon = new ImageView(this);
        icon.setBackgroundColor(Color.GRAY); // Placeholder
        
        // Simple image loader task since we don't have Glide/Picasso in this environment
        String iconUrl = nativeAd.getIconUrl();
        if (iconUrl != null && !iconUrl.isEmpty()) {
            new ImageLoadTask(iconUrl, icon).execute();
        }

        LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(150, 150);
        iconParams.rightMargin = 20;
        header.addView(icon, iconParams);
        
        LinearLayout textContainer = new LinearLayout(this);
        textContainer.setOrientation(LinearLayout.VERTICAL);
        
        TextView title = new TextView(this);
        title.setText(nativeAd.getTitle());
        title.setTextSize(18);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        textContainer.addView(title);
        
        TextView body = new TextView(this);
        body.setText(nativeAd.getBody());
        textContainer.addView(body);
        
        header.addView(textContainer);
        adView.addView(header);

        // Main Image
        ImageView mainImage = new ImageView(this);
        mainImage.setBackgroundColor(Color.DKGRAY); // Placeholder
        
        String imageUrl = nativeAd.getImageUrl();
        if (imageUrl != null && !imageUrl.isEmpty()) {
            new ImageLoadTask(imageUrl, mainImage).execute();
        }

        LinearLayout.LayoutParams imgParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 400);
        imgParams.topMargin = 20;
        imgParams.bottomMargin = 20;
        adView.addView(mainImage, imgParams);
        
        // CTA Button
        Button cta = new Button(this);
        cta.setText(nativeAd.getCallToAction());
        adView.addView(cta, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        // Add to container
        nativeAdContainer.addView(adView);

        // Register for interaction
        nativeAd.registerViewForInteraction(cta);
    }
    
    // Use standard HttpURLConnection for image loading to avoid OkHttp dependency
    // private static final okhttp3.OkHttpClient imageClient = new okhttp3.OkHttpClient();

    // Simple AsyncTask for loading images using HttpURLConnection
    private static class ImageLoadTask extends android.os.AsyncTask<Void, Void, android.graphics.Bitmap> {
        private String url;
        private ImageView imageView;

        public ImageLoadTask(String url, ImageView imageView) {
            this.url = url;
            this.imageView = imageView;
        }

        @Override
        protected android.graphics.Bitmap doInBackground(Void... voids) {
            java.net.HttpURLConnection connection = null;
            try {
                java.net.URL urlObj = new java.net.URL(url);
                connection = (java.net.HttpURLConnection) urlObj.openConnection();
                connection.setDoInput(true);
                connection.connect();
                java.io.InputStream input = connection.getInputStream();
                return android.graphics.BitmapFactory.decodeStream(input);
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(android.graphics.Bitmap result) {
            if (result != null && imageView != null) {
                imageView.setImageBitmap(result);
                imageView.setBackgroundColor(Color.TRANSPARENT);
            }
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