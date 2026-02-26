package com.ssp.sdk.ad;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.MediaController;
import android.widget.VideoView;

import com.ssp.sdk.data.VideoResponse;
import com.ssp.sdk.util.VideoCacheManager;

import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import com.ssp.sdk.network.NetworkClient;

public class SspVideoView extends FrameLayout {
    private static final String TAG = "SspVideoView";
    private VideoView videoView;
    private FrameLayout endCardContainer;
    private SspVideoAd adController;
    private VideoResponse adResponse;
    private boolean autoCache;
    private Handler progressHandler = new Handler(Looper.getMainLooper());
    private Runnable progressRunnable;
    private MediaPlayer mediaPlayer;
    private boolean isMuted = true;
    private Button muteButton;
    
    // OkHttpClient removed
    // private static final okhttp3.OkHttpClient imageClient = new okhttp3.OkHttpClient();

    public SspVideoView(Context context) {
        super(context);
        init();
    }
    
    // ... Constructors ...
    public SspVideoView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        setBackgroundColor(Color.BLACK);
        
        // Video View
        videoView = new VideoView(getContext());
        LayoutParams lp = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.gravity = Gravity.CENTER;
        addView(videoView, lp);
        
        // Mute Button
        muteButton = new Button(getContext());
        muteButton.setText("🔇"); // Default Muted
        muteButton.setBackgroundColor(Color.TRANSPARENT);
        muteButton.setTextColor(Color.WHITE);
        muteButton.setTextSize(24);
        LayoutParams muteLp = new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        muteLp.gravity = Gravity.TOP | Gravity.START;
        muteLp.setMargins(20, 20, 0, 0);
        addView(muteButton, muteLp);
        
        muteButton.setOnClickListener(v -> toggleSound());
        
        // End Card Container (Hidden initially)
        endCardContainer = new FrameLayout(getContext());
        endCardContainer.setVisibility(View.GONE);
        endCardContainer.setBackgroundColor(Color.BLACK);
        addView(endCardContainer, new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
    }
    
    // ... setAdController ...
    public void setAdController(SspVideoAd controller) {
        this.adController = controller;
        this.adResponse = (VideoResponse) controller.adResponse;
        this.autoCache = controller.isAutoCacheEnabled();
        setupVideo();
    }
    
    // ... setupVideo ...
    private void setupVideo() {
        // ... (existing video setup code) ...
        if (adResponse == null) return;

        String url = adResponse.videoUrl;
        if (url == null || url.isEmpty()) {
            url = "https://storage.googleapis.com/gvabox/media/samples/stock.mp4"; // Fallback
        }

        String finalUrl = url;
        if (autoCache) {
            if (VideoCacheManager.getInstance().isCached(getContext(), url)) {
                String localPath = VideoCacheManager.getInstance().getCachedVideoPath(getContext(), url);
                if (localPath != null) {
                    finalUrl = localPath;
                    Log.d(TAG, "Playing from cache: " + finalUrl);
                }
            } else {
                Log.d(TAG, "Cache miss or caching, playing from network");
            }
        }

        if (finalUrl.startsWith("/")) {
            videoView.setVideoPath(finalUrl);
        } else {
            videoView.setVideoURI(Uri.parse(finalUrl));
        }

        MediaController mediaController = new MediaController(getContext());
        mediaController.setAnchorView(this);
        videoView.setMediaController(mediaController);

        videoView.setOnPreparedListener(mp -> {
            this.mediaPlayer = mp;
            // Default Mute
            updateVolume();
            
            videoView.start();
            if (adController != null) {
                adController.fireImpression();
                adController.fireEvent("start");
                adController.fireEvent("creativeView");
            }
            startTracking();
        });

        videoView.setOnCompletionListener(mp -> {
            stopTracking();
            if (adController != null) {
                adController.fireEvent("complete");
            }
            showEndCard();
        });

        videoView.setOnErrorListener((mp, what, extra) -> {
            if (adController != null && adController.interactionListener != null) {
                adController.interactionListener.onAdShowFailed("Video Error: " + what);
            }
            return true;
        });

        videoView.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_UP) {
                if (adController != null) {
                    adController.fireClick();
                }
            }
            return false;
        });
    }

    private void toggleSound() {
        isMuted = !isMuted;
        updateVolume();
        if (muteButton != null) {
            muteButton.setText(isMuted ? "🔇" : "🔊");
        }
    }

    private void updateVolume() {
        if (mediaPlayer == null) return;
        if (isMuted) {
            mediaPlayer.setVolume(0f, 0f);
        } else {
            mediaPlayer.setVolume(1f, 1f);
        }
    }

    private void showEndCard() {
        if (adResponse == null || adResponse.endCard == null) return;
        
        VideoResponse.EndCard endCard = adResponse.endCard;
        endCardContainer.removeAllViews();
        endCardContainer.setVisibility(View.VISIBLE);
        
        // Render content based on type
        if ("STATIC".equalsIgnoreCase(endCard.type) && endCard.imageUrl != null) {
            ImageView imageView = new ImageView(getContext());
            imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
            endCardContainer.addView(imageView, new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            
            // Load Image
            new ImageLoadTask(endCard.imageUrl, imageView).execute();
            
            imageView.setOnClickListener(v -> handleEndCardClick(endCard));
            
        } else if ("HTML".equalsIgnoreCase(endCard.type) && endCard.htmlContent != null) {
            WebView webView = new WebView(getContext());
            webView.getSettings().setJavaScriptEnabled(true);
            webView.loadDataWithBaseURL(null, endCard.htmlContent, "text/html", "UTF-8", null);
            webView.setWebViewClient(new WebViewClient() {
                @Override
                public boolean shouldOverrideUrlLoading(WebView view, String url) {
                    handleEndCardClick(endCard, url);
                    return true;
                }
            });
            endCardContainer.addView(webView, new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        }
        
        // Close Button
        Button closeBtn = new Button(getContext());
        closeBtn.setText("X");
        closeBtn.setBackgroundColor(Color.TRANSPARENT);
        closeBtn.setTextColor(Color.WHITE);
        closeBtn.setTextSize(20);
        LayoutParams btnLp = new LayoutParams(100, 100);
        btnLp.gravity = Gravity.TOP | Gravity.END;
        btnLp.setMargins(20, 20, 20, 20);
        endCardContainer.addView(closeBtn, btnLp);
        
        closeBtn.setOnClickListener(v -> {
            destroy(); // Stop playback/tracking
            
            // Logic: Close Activity (Full Screen) or Remove View (Embedded)
            Context ctx = getContext();
            if (ctx instanceof SspFullScreenVideoActivity) {
                ((Activity) ctx).finish();
            } else if (ctx instanceof Activity) {
                 // Try to guess if we are the main content or just a view
                 // But safer to just remove self from parent
                 ViewGroup parent = (ViewGroup) getParent();
                 if (parent != null) {
                     parent.removeView(this);
                 }
            } else {
                 ViewGroup parent = (ViewGroup) getParent();
                 if (parent != null) {
                     parent.removeView(this);
                 }
            }
        });
        
        // Track view
        if (endCard.viewTrackers != null) {
            for (String url : endCard.viewTrackers) NetworkClient.sendTrackingUrl(url);
        }
    }

    private void handleEndCardClick(VideoResponse.EndCard endCard) {
        handleEndCardClick(endCard, null);
    }

    private void handleEndCardClick(VideoResponse.EndCard endCard, String overrideUrl) {
        // Track click
        if (endCard.clickTrackers != null) {
            for (String url : endCard.clickTrackers) NetworkClient.sendTrackingUrl(url);
        }
        
        String targetUrl = (overrideUrl != null) ? overrideUrl : endCard.clickUrl;
        if (targetUrl != null && !targetUrl.isEmpty()) {
            if (adController != null) adController.fireClick(targetUrl);
        }
    }
    
    // Helper for image loading
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
            }
        }
    }

    public void start() {
        if (videoView != null) videoView.start();
    }

    public void pause() {
        if (videoView != null) videoView.pause();
    }
    
    public void destroy() {
        if (videoView != null) {
            videoView.stopPlayback();
        }
        stopTracking();
    }
    
    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        stopTracking();
    }

    private void startTracking() {
        if (progressRunnable != null) return;
        progressRunnable = new Runnable() {
            @Override
            public void run() {
                if (videoView != null && videoView.isPlaying()) {
                    int duration = videoView.getDuration();
                    int current = videoView.getCurrentPosition();
                    if (duration > 0 && adController != null) {
                        float progress = (float) current / duration;
                        if (progress >= 0.25f) adController.fireEvent("firstQuartile");
                        if (progress >= 0.50f) adController.fireEvent("midpoint");
                        if (progress >= 0.75f) adController.fireEvent("thirdQuartile");
                    }
                    progressHandler.postDelayed(this, 1000);
                }
            }
        };
        progressHandler.post(progressRunnable);
    }

    private void stopTracking() {
        if (progressRunnable != null) {
            progressHandler.removeCallbacks(progressRunnable);
            progressRunnable = null;
        }
    }
}
