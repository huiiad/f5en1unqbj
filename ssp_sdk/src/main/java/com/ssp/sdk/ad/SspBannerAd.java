package com.ssp.sdk.ad;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;

import android.util.DisplayMetrics;
import android.view.WindowManager;

import com.ssp.sdk.data.AdResponse;
import com.ssp.sdk.data.BannerResponse;
import com.ssp.sdk.data.AdType;
import com.ssp.sdk.util.DeviceInfoManager;

import android.webkit.WebChromeClient;
import android.os.Message;

import com.ssp.sdk.util.SspConfigManager;

import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleEventObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.annotation.NonNull;
import android.os.Handler;
import android.os.Looper;

public class SspBannerAd extends FrameLayout implements LifecycleEventObserver {

    private BannerAdLogic adLogic;
    private int width;
    private int height;
    private WebView webView;
    private SspLoadListener loadListener;
    private SspInteractionListener interactionListener;
    private boolean autoRefresh = false;
    private int refreshInterval = 5; // Default 5 seconds, overridden by config
    private Handler refreshHandler = new Handler(Looper.getMainLooper());
    private String adSlotId; // Store slot ID for config lookup
    private int retryCount = 0;
    private static final int MAX_RETRY_COUNT = 5;

    private Runnable refreshRunnable = new Runnable() {
        @Override
        public void run() {
            if (autoRefresh && adLogic != null) {
                adLogic.loadAd();
            }
        }
    };

    public SspBannerAd(Context context, String adSlotId, int width, int height) {
        super(context);
        this.width = width;
        this.height = height;
        Log.d("SSP_SDK", "SspBannerAd: Constructor with size: " + width + "x" + height);
        init(context, adSlotId);
    }

    // ... Constructors ...

    public SspBannerAd(Context context, String adSlotId) {
        super(context);
        // Calculate default dimensions using DeviceInfoManager
        DeviceInfoManager deviceInfo = DeviceInfoManager.getInstance(context);
        this.width = deviceInfo.getScreenWidth();
        Log.d("SSP_SDK", "SspBannerAd: Default Constructor. Device width: " + this.width);

        if (this.width <= 0) {
            // Fallback if device info failed
            DisplayMetrics displayMetrics = new DisplayMetrics();
            WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
            if (wm != null) {
                wm.getDefaultDisplay().getMetrics(displayMetrics);
                this.width = displayMetrics.widthPixels;
            }
            if (this.width <= 0) {
                this.width = 320;
            }
        }

        this.height = (this.width * 9) / 16;
        if (this.height <= 0) this.height = 50;

        Log.d("SSP_SDK", "SspBannerAd: Calculated default size: " + this.width + "x" + this.height);

        init(context, adSlotId);
    }

    public SspBannerAd(Context context) {
        super(context);
    }

    public SspBannerAd(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public SspBannerAd(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    private void init(Context context, String adSlotId) {
        this.adSlotId = adSlotId;
        adLogic = new BannerAdLogic(context, adSlotId);
        // Proxy internal listeners to external
        adLogic.setLoadListener(new SspLoadListener() {
            @Override
            public void onAdLoaded(BaseAd ad) {
                // Fetch refresh interval from Config Manager (NOT AdResponse)
                int configInterval = SspConfigManager.getInstance().getRefreshInterval(SspBannerAd.this.adSlotId);
                if (configInterval > 0) {
                    refreshInterval = configInterval;
                } else {
                    refreshInterval = 5; // Fallback default
                }
                
                // Note: Auto-refresh logic is now moved to showAd() to ensure it starts only after first display.
                
                if (loadListener != null) {
                    loadListener.onAdLoaded(adLogic);
                    // Standard load + show flow: Auto-refresh starts ONLY after showAd() is called manually.
                    // But wait, if user calls showAd(), showAd() will start timer.
                    // Here we don't start timer automatically.
                }
                
                // If this is a subsequent load triggered by auto-refresh loop (not the first manual load),
                // AND we are in Standard Mode (not loadAndShow mode which handles this itself),
                // AND the view is already attached (webView != null),
                // THEN we must update the UI automatically to keep the loop going.
                if (!isAutoMode && autoRefresh && webView != null) {
                    showAd();
                }
                
                retryCount = 0; // Reset retry count on success
            }

            @Override
            public void onAdFailed(String error) {
                if (loadListener != null) loadListener.onAdFailed(error);
                
                // Standard load + show flow (Standard Mode): 
                // If load fails, we do NOT auto-retry unless autoRefresh was already active (in a loop).
                // "autoRefresh && webView != null" means we have shown at least once (or were in a loop).
                // "isAutoMode" is false here (or true if set by loadAndShow, but loadAndShow overrides this listener anyway).
                
                if (autoRefresh && webView != null) { 
                    if (retryCount < MAX_RETRY_COUNT) {
                        retryCount++;
                        startRefresh();
                    } else {
                        Log.e("SSP_SDK", "Max retry count reached. Stopping auto-refresh.");
                        stopRefresh();
                    }
                }
            }
        });

        adLogic.setInteractionListener(new SspInteractionListener() {
            @Override
            public void onImpression(String adId) {
                if (interactionListener != null) interactionListener.onImpression(adId);
            }

            @Override
            public void onAdClicked(String adId) {
                if (interactionListener != null) interactionListener.onAdClicked(adId);
            }

            @Override
            public void onAdShowFailed(String error) {
                if (interactionListener != null) interactionListener.onAdShowFailed(error);
            }
        });

        // Automatically bind to lifecycle if context is a LifecycleOwner
        LifecycleOwner lifecycleOwner = getLifecycleOwner(context);
        if (lifecycleOwner != null) {
            lifecycleOwner.getLifecycle().addObserver(this);
            Log.d("SSP_SDK", "SspBannerAd: Auto-bound to lifecycle of " + lifecycleOwner);
        }
    }

    private LifecycleOwner getLifecycleOwner(Context context) {
        if (context instanceof LifecycleOwner) {
            return (LifecycleOwner) context;
        }
        if (context instanceof android.content.ContextWrapper) {
            Context baseContext = ((android.content.ContextWrapper) context).getBaseContext();
            if (baseContext != context) { // Prevent infinite recursion
                return getLifecycleOwner(baseContext);
            }
        }
        return null;
    }

    public void setAutoRefresh(boolean enable) {
        this.autoRefresh = enable;
        Log.i("SSP_SDK", "Auto-refresh: " + enable);
        if (!enable) {
            stopRefresh();
        } else if (isValid() && webView != null) {
            // If already loaded AND shown (webView exists), start timer
            startRefresh();
        }
    }

    private void startRefresh() {
        stopRefresh(); // Ensure single task
        Log.d("SSP_SDK", "autoRefresh = " + autoRefresh +", Scheduling refresh in " + refreshInterval + " seconds");
        if (autoRefresh) {
            refreshHandler.postDelayed(refreshRunnable, refreshInterval * 1000L);
        }
    }

    private void stopRefresh() {
        if (refreshHandler != null) {
            refreshHandler.removeCallbacks(refreshRunnable);
        }
    }

    public void pause() {
        Log.i("SSP_SDK", "SspBannerAd paused");
        stopRefresh();
    }

    public void resume() {
        Log.i("SSP_SDK", "SspBannerAd resumed. AutoRefresh: " + autoRefresh + ", View attached: " + (webView != null));
        if (autoRefresh && webView != null) {
            startRefresh();
        }
    }

    public void setLoadListener(SspLoadListener listener) {
        this.loadListener = listener;
    }

    public void setInteractionListener(SspInteractionListener listener) {
        this.interactionListener = listener;
    }
    
    public boolean isValid() {
        return adLogic != null && adLogic.isValid();
    }

    public void loadAd() {
        if (adLogic != null) {
            adLogic.loadAd();
        }
    }

    public void showAd() {
        if (adLogic == null || !adLogic.isValid()) {
            String msg = (adLogic == null) ? "Ad not initialized" : "Ad expired or not loaded";
            if (loadListener != null) loadListener.onAdFailed(msg); // Or interaction listener?
            // Usually show failure goes to interaction listener if show is explicit
            if (interactionListener != null) interactionListener.onAdShowFailed(msg);
            return;
        }

        // Trigger auto-refresh timer only when showAd is called
        // We do this BEFORE creating/manipulating view to ensure timer starts reliably
        if (autoRefresh) {
            startRefresh();
        }
        
        // Create WebView if not exists, otherwise reuse
        if (webView == null) {
            webView = new WebView(getContext());
            webView.getSettings().setJavaScriptEnabled(true);
            webView.getSettings().setSupportMultipleWindows(true);
            webView.getSettings().setJavaScriptCanOpenWindowsAutomatically(true);

            webView.setWebChromeClient(new WebChromeClient() {
                @Override
                public boolean onCreateWindow(WebView view, boolean isDialog, boolean isUserGesture, Message resultMsg) {
                    WebView.WebViewTransport transport = (WebView.WebViewTransport) resultMsg.obj;
                    WebView tempWebView = new WebView(getContext());
                    tempWebView.setWebViewClient(new WebViewClient() {
                        @Override
                        public boolean shouldOverrideUrlLoading(WebView view, String url) {
                            adLogic.fireClick(url);
                            return true;
                        }
                    });
                    transport.setWebView(tempWebView);
                    resultMsg.sendToTarget();
                    return true;
                }
            });

            webView.setWebViewClient(new WebViewClient() {
                @Override
                public void onPageFinished(WebView view, String url) {
                    super.onPageFinished(view, url);
                    // Fire impression when content is loaded and view is likely visible
                    adLogic.fireImpression();
                }

                @Override
                public boolean shouldOverrideUrlLoading(WebView view, String url) {
                    // Intercept click
                    adLogic.fireClick(url);
                    return true;
                }
            });
            
            // Add WebView to this FrameLayout
            int adW = this.width > 0 ? this.width : ViewGroup.LayoutParams.MATCH_PARENT;
            int adH = this.height > 0 ? this.height : ViewGroup.LayoutParams.MATCH_PARENT;
            addView(webView, new ViewGroup.LayoutParams(adW, adH));
        } else {
            // If reusing, maybe clear history or stop loading?
            webView.stopLoading();
        }

        // Load content
        if (adLogic.getAdResponse() instanceof BannerResponse) {
            BannerResponse response = (BannerResponse) adLogic.getAdResponse();
            // Clean content (remove backticks if present from copy-paste data)
            String cleanContent = response.htmlContent.replace("`", "");
            // Use loadDataWithBaseURL for better robustness with HTML fragments
            webView.loadDataWithBaseURL(null, cleanContent, "text/html", "UTF-8", null);
        } else {
            if (loadListener != null) loadListener.onAdFailed("Invalid ad response type");
        }
    }

    // Flag to track if we are in "Auto Mode" (loadAndShow)
    private boolean isAutoMode = false;

    public void loadAndShow() {
        isAutoMode = true; // Set flag
        if (adLogic != null) {
            // Chain listeners
            final SspLoadListener userLoadListener = this.loadListener;

            // Updating 'this.loadListener' here does NOT update the reference held by adLogic's listener.
            adLogic.setLoadListener(new SspLoadListener() {
                @Override
                public void onAdLoaded(BaseAd ad) {
                    // Update internal interval in case it changed
                    int configInterval = SspConfigManager.getInstance().getRefreshInterval(SspBannerAd.this.adSlotId);
                    if (configInterval > 0) refreshInterval = configInterval;
                    
                    retryCount = 0; // Reset retry on success
                    
                    showAd(); // Auto show
                    if (userLoadListener != null) userLoadListener.onAdLoaded(ad);
                }
                
                @Override
                public void onAdFailed(String error) {
                    if (userLoadListener != null) userLoadListener.onAdFailed(error);
                    
                    // Logic for loadAndShow: If load fails, we retry immediately if autoRefresh is on.
                    if (autoRefresh) {
                         if (retryCount < MAX_RETRY_COUNT) {
                            retryCount++;
                            startRefresh();
                        } else {
                            Log.e("SSP_SDK", "Max retry count reached (loadAndShow). Stopping auto-refresh.");
                            stopRefresh();
                        }
                    }
                }
            });
            
            adLogic.loadAd();
        }
    }

    public void destroy() {
        stopRefresh();
        if (webView != null) {
            removeView(webView);
            webView.destroy();
            webView = null;
        }
        // BaseAd doesn't have destroy, but we can clear references
        adLogic = null;
    }

    @Override
    public void onStateChanged(@NonNull LifecycleOwner source, @NonNull Lifecycle.Event event) {
        Log.d("SSP_SDK", "SspBannerAd: Lifecycle state changed: " + event);
        if (event == Lifecycle.Event.ON_PAUSE) {
            pause();
        } else if (event == Lifecycle.Event.ON_RESUME) {
            resume();
        } else if (event == Lifecycle.Event.ON_DESTROY) {
            destroy();
            source.getLifecycle().removeObserver(this);
        }
    }

    // Inner class to reuse BaseAd logic
    private class BannerAdLogic extends BaseAd {
        public BannerAdLogic(Context context, String adSlotId) {
            super(context, adSlotId);
        }

        @Override
        public AdType getAdType() {
            return AdType.BANNER;
        }

        @Override
        protected int getAdWidth() {
            return SspBannerAd.this.width;
        }

        @Override
        protected int getAdHeight() {
            return SspBannerAd.this.height;
        }

        // Expose protected members to outer class
        public boolean isLoaded() {
            return super.isLoaded;
        }

        public AdResponse getAdResponse() {
            return super.adResponse;
        }

        @Override
        public void fireImpression() {
            super.fireImpression();
        }

        @Override
        public void fireClick() {
            super.fireClick();
        }

        public void fireClick(String url) {
            super.fireClick(url);
        }
    }
}