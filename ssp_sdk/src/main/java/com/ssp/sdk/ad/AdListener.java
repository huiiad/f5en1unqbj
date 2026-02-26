package com.ssp.sdk.ad;

public interface AdListener {
    void onAdLoaded();
    void onAdFailed(String errorMessage);
    void onAdImpression();
    void onAdClicked();
}
