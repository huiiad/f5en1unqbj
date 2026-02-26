package com.ssp.sdk.ad;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;

import java.util.logging.Logger;

import com.ssp.sdk.SspSdk;
import com.ssp.sdk.data.AdRequest;
import com.ssp.sdk.data.AdResponse;
import com.ssp.sdk.data.AdType;
import com.ssp.sdk.data.ReportEvent;
import com.ssp.sdk.data.NativeResponse;
import com.ssp.sdk.data.VideoResponse;
import com.ssp.sdk.network.NetworkClient;
import com.ssp.sdk.util.ReportManager;

public abstract class BaseAd {
    protected Context context;
    protected String adSlotId;
    protected SspLoadListener loadListener;
    protected SspInteractionListener interactionListener;
    protected AdResponse adResponse;
    protected boolean isLoaded = false;
    protected long loadTime = 0;
    protected Handler handler = new Handler(Looper.getMainLooper());

    // Placeholder URL - in a real SDK this would be a config
    // Ensure this points to your real SSP service. Use 10.0.2.2 for Android Emulator accessing localhost.
    private static final String SSP_URL = "https://sdk-sg.huiinex.com/ssp/bid";
//    private static final String SSP_URL = "http://10.33.39.19:8066/ssp/bid";

    private static final Logger log = Logger.getLogger(BaseAd.class.getName());
    public BaseAd(Context context, String adSlotId) {
        this.context = context;
        this.adSlotId = adSlotId;
    }

    public void setLoadListener(SspLoadListener listener) {
        this.loadListener = listener;
    }
    
    public void setInteractionListener(SspInteractionListener listener) {
        this.interactionListener = listener;
    }

    // ... (Listeners) ...

    public abstract AdType getAdType();
    
    // Abstract method to get width/height for request
    protected abstract int getAdWidth();
    protected abstract int getAdHeight();

    public boolean isValid() {
        return isLoaded && (System.currentTimeMillis() - loadTime < 3600000); // 1 hour validity
    }

    public void loadAd() {
        if (!SspSdk.getInstance().isInitialized()) {
            if (loadListener != null) loadListener.onAdFailed("SDK not initialized");
            reportRequest(1001); // SDK not initialized error code
            return;
        }

        AdRequest request = new AdRequest(adSlotId, getAdType().name(), getAdWidth(), getAdHeight());
        String jsonBody = request.toJson().toString();

        
        // Use POST method for SSP requests with redirect support
        NetworkClient.resolveUrlWithPost(SSP_URL, jsonBody, new NetworkClient.ResolveUrlCallback() {
            @Override
            public void onResolved(int responseCode, String finalUrl, int redirectCount, String responseBody) {
                // Log redirect information for click tracking
                log.info("SSP request completed - Code: " + responseCode + ", Final URL: " + finalUrl + "\n  responseBody: " + responseBody);

                // Handle the response based on status code
                if (responseCode >= 200 && responseCode < 300) {
                    // Check if body is present
                    if (responseBody != null && !responseBody.isEmpty()) {
                        handleResponse(responseBody);
                        reportRequest(200);
                    } else {
                        // Empty response
                        log.severe("No ad fill (empty response)");
                        if (loadListener != null) {
                            loadListener.onAdFailed("No ad fill (empty response)");
                        }
                        reportRequest(204); // No content
                    }
                } else {
                    // Handle error cases
                    if (loadListener != null) {
                        loadListener.onAdFailed("SSP request failed with code: " + responseCode);
                    }
                    reportRequest(responseCode);
                }
            }

            @Override
            public void onFailure(String error) {
                // FAILURE: Do NOT use mock data as requested
                android.util.Log.e("SSP_SDK", "SSP request failed: " + error);
                if (loadListener != null) loadListener.onAdFailed("Network failure: " + error);
                reportRequest(1002); // Network failure
            }
        });
    }

    private void handleResponse(String responseStr) {
        adResponse = AdResponse.parse(responseStr);
        if (adResponse != null) {
            isLoaded = true;
            loadTime = System.currentTimeMillis();
            onAdLoaded(); // Hook for subclasses
            if (loadListener != null) loadListener.onAdLoaded(this);
        } else {
            if (loadListener != null) loadListener.onAdFailed("Failed to parse response");
            reportRequest(1003); // Parse error
        }
    }
    
    // Hook for subclasses to perform actions after load but before listener notification
    protected void onAdLoaded() {
        // Default no-op
    }
    
    protected void fireImpression() {
        if (adResponse != null && adResponse.impTrackers != null) {
            for (String url : adResponse.impTrackers) {
                NetworkClient.sendTrackingUrl(url);
            }
        }
        reportShow();
        if (interactionListener != null) {
            String id = (adResponse != null) ? adResponse.id : null;
            interactionListener.onImpression(id);
        }
    }
    
    protected void fireClick() {
        fireClick(null);
    }

    protected void fireClick(String overrideClickUrl) {
        reportClick();
        if (adResponse != null) {
            // ... (Click handling logic) ...
            // 1. Send click tracking URLs
            if (adResponse.clickTrackers != null) {
                for (String url : adResponse.clickTrackers) {
                    NetworkClient.sendTrackingUrl(url);
                }
            }
            
            // 2. Determine target URL: use override if provided, otherwise fallback to response.clickUrl
            String targetUrl = overrideClickUrl;
            if (targetUrl == null || targetUrl.isEmpty()) {
                targetUrl = adResponse.clickUrl;
            }
            
            // 3. Handle Click URL resolution and opening
            if (targetUrl != null && !targetUrl.isEmpty()) {
                final String urlToResolve = targetUrl;
                NetworkClient.resolveClickUrl(urlToResolve, new NetworkClient.ResolveUrlCallback() {
                    @Override
                    public void onResolved(int responseCode, String finalUrl, int redirectCount, String responseBody) {
                        // Log for reporting
                        android.util.Log.d("SSP_SDK", "Click resolved: Code=" + responseCode + ", Redirects=" + redirectCount + ", FinalURL=" + finalUrl);
                        
                        // Open final URL in system browser
                        if (finalUrl != null && !finalUrl.isEmpty()) {
                            try {
                                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(finalUrl));
                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                context.startActivity(intent);
                            } catch (Exception e) {
                                android.util.Log.e("SSP_SDK", "Failed to open URL: " + e.getMessage());
                            }
                        }
                    }

                    @Override
                    public void onFailure(String error) {
                         android.util.Log.e("SSP_SDK", "Click resolution failed: " + error);
                         // Fallback: try to open original URL
                         try {
                            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(urlToResolve));
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            context.startActivity(intent);
                         } catch (Exception e) {
                             e.printStackTrace();
                         }
                    }
                });
            }
        }
        if (interactionListener != null) {
            String id = (adResponse != null) ? adResponse.id : null;
            interactionListener.onAdClicked(id);
        }
    }
    
    private void reportRequest(int status) {
        String appId = SspSdk.getInstance().getAppId();
        String adId = (adResponse != null) ? adResponse.id : null;
        
        ReportManager.getInstance().report(new ReportEvent(
            "request",
            status,
            appId,
            adSlotId,
            adId,
            getAdType().name(), // Added adType
            System.currentTimeMillis(),
            getTitle()
        ));
    }

    private void reportShow() {
        String appId = SspSdk.getInstance().getAppId();
        String adId = (adResponse != null) ? adResponse.id : null;
        
        ReportManager.getInstance().report(new ReportEvent(
            "show",
            200,
            appId,
            adSlotId,
            adId,
            getAdType().name(), // Added adType
            System.currentTimeMillis(),
            getTitle()
        ));
    }

    private void reportClick() {
        String appId = SspSdk.getInstance().getAppId();
        String adId = (adResponse != null) ? adResponse.id : null;
        
        ReportManager.getInstance().report(new ReportEvent(
            "click",
            200,
            appId,
            adSlotId,
            adId,
            getAdType().name(), // Added adType
            System.currentTimeMillis(),
            getTitle()
        ));
    }
    
    private String getTitle() {
        if (adResponse == null) return null;
        if (adResponse instanceof NativeResponse) return ((NativeResponse) adResponse).title;
        if (adResponse instanceof VideoResponse) return ((VideoResponse) adResponse).title;
        // Banner usually doesn't have a title field in standard response unless extending
        return null;
    }
}
