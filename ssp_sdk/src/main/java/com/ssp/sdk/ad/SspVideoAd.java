package com.ssp.sdk.ad;

import android.content.Context;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Handler;
import android.view.ViewGroup;
import android.widget.VideoView;
import android.widget.MediaController;
import android.app.Activity;
import android.content.Intent;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.List;

import com.ssp.sdk.data.AdType;
import com.ssp.sdk.data.VideoResponse;
import com.ssp.sdk.network.NetworkClient;
import com.ssp.sdk.util.VideoCacheManager;

public class SspVideoAd extends BaseAd {
    
    // Registry for full screen activity to access the ad object
    private static final Map<String, SspVideoAd> activeAds = new HashMap<>();

    public static SspVideoAd getActiveAd(String adSlotId) {
        return activeAds.get(adSlotId);
    }

    private Set<String> firedEvents = new HashSet<>();
    private Handler progressHandler = new Handler();
    // VideoView logic moved to SspVideoView
    private Runnable progressRunnable;
    private boolean autoCache = false;

    public SspVideoAd(Context context, String adSlotId) {
        super(context, adSlotId);
        // Register self (in a real app, manage lifecycle carefully to avoid leaks)
        // For simplicity we key by adSlotId, but multiple ads with same slotId is possible.
        // A unique ID (UUID) would be better.
        // But the demo uses adSlotId. We'll assume unique for now or use object hash.
    }
    
    // Helper to register using a unique key if needed, or just overwrite for now.
    private void registerActiveAd() {
        activeAds.put(adSlotId, this);
    }

    public void enableAutoCache(boolean enable) {
        this.autoCache = enable;
    }
    
    public boolean isAutoCacheEnabled() {
        return autoCache;
    }

    public boolean isLoaded() {
        return isLoaded && adResponse != null;
    }
    
    @Override
    protected void onAdLoaded() {
        super.onAdLoaded();
        registerActiveAd();
        if (autoCache && adResponse instanceof VideoResponse) {
            VideoResponse response = (VideoResponse) adResponse;
            String url = response.videoUrl;
            if (url != null && !url.isEmpty()) {
                VideoCacheManager.getInstance().cacheVideo(context, url);
            }
        }
    }
    
    @Override
    public AdType getAdType() {
        return AdType.VIDEO;
    }

    @Override
    protected int getAdWidth() {
        return 1080; // Example
    }

    @Override
    protected int getAdHeight() {
        return 1920; // Example
    }

    /**
     * Get the Ad View for embedding in your own layout.
     * @return SspVideoView
     */
    public SspVideoView getAdView() {
        if (!isLoaded || adResponse == null) return null;
        SspVideoView view = new SspVideoView(context);
        view.setAdController(this);
        return view;
    }
    
    /**
     * Show the video ad in a full screen activity.
     * @param activity The current activity
     */
    public void showAd(Activity activity) {
        if (!isValid()) {
             if (interactionListener != null) interactionListener.onAdShowFailed("Ad expired or not loaded");
             return;
        }
        
        Intent intent = new Intent(activity, SspFullScreenVideoActivity.class);
        intent.putExtra(SspFullScreenVideoActivity.EXTRA_AD_ID, adSlotId);
        activity.startActivity(intent);
    }

    /**
     * Show the video ad in the provided container (View Card mode).
     */
    public void show(ViewGroup container) {
        if (!isValid()) {
             if (interactionListener != null) interactionListener.onAdShowFailed("Ad expired or not loaded");
             return;
        }
        
        container.removeAllViews();
        SspVideoView view = getAdView();
        if (view != null) {
            container.addView(view, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            view.start();
        } else {
             if (interactionListener != null) interactionListener.onAdShowFailed("Failed to create ad view");
        }
    }

    protected void startTracking() {
        // ... (implementation same as before but using the view passed or just callback?)
        // Tracking logic needs reference to the current VideoView?
        // SspVideoView handles playback.
        // We can expose a generic "getCurrentPosition" from SspVideoView?
        // OR SspVideoView calls "updateProgress(current, duration)" on controller?
        
        // Simpler: SspVideoView handles the progress checking loop and calls fireEvent directly?
        // Or SspVideoAd maintains the loop but needs to ask SspVideoView for progress.
        // But SspVideoAd might be detached from view (if multiple views?).
        // Usually 1 ad = 1 view.
        // Let's make SspVideoView handle the progress loop since it owns the MediaPlayer.
        // So startTracking/stopTracking here might just be empty or deprecated hooks?
        // Ah, in previous code startTracking was private in SspVideoAd.
        // Now I moved the view logic to SspVideoView.
        // I should move the tracking logic to SspVideoView too, OR SspVideoAd provides the 'fireEvent' capability.
        // Let's move tracking loop to SspVideoView.
        // So SspVideoAd just exposes fireEvent.
    }
    
    protected void stopTracking() {
        // No-op if moved to view
    }

    protected void fireEvent(String eventName) {
        if (firedEvents.contains(eventName)) return;
        
        firedEvents.add(eventName);
        if (adResponse instanceof VideoResponse) {
             VideoResponse response = (VideoResponse) adResponse;
             if (response.trackingEvents != null && response.trackingEvents.containsKey(eventName)) {
                List<String> urls = response.trackingEvents.get(eventName);
                if (urls != null) {
                    for (String url : urls) {
                        NetworkClient.sendTrackingUrl(url);
                    }
                }
            }
        }
    }
}
