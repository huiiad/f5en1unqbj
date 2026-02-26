package com.ssp.sdk.ad;

public interface SspInteractionListener {
    void onImpression(String adId);
    void onAdClicked(String adId);
    void onAdShowFailed(String error);
}
