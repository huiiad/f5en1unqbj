package com.ssp.sdk.ad;

public interface SspLoadListener {
    void onAdLoaded(BaseAd ad);
    void onAdFailed(String error);
}
