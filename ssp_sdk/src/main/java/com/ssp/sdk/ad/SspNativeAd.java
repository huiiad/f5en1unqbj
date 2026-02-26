package com.ssp.sdk.ad;

import android.content.Context;
import android.view.View;
import com.ssp.sdk.data.AdType;
import com.ssp.sdk.data.NativeResponse;

public class SspNativeAd extends BaseAd {

    public SspNativeAd(Context context, String adSlotId) {
        super(context, adSlotId);
    }

    @Override
    public AdType getAdType() {
        return AdType.NATIVE;
    }

    @Override
    protected int getAdWidth() {
        return 0; // Native doesn't usually specify size in request
    }

    @Override
    protected int getAdHeight() {
        return 0;
    }

    public String getTitle() {
        return (adResponse instanceof NativeResponse) ? ((NativeResponse) adResponse).title : null;
    }

    public String getBody() {
        return (adResponse instanceof NativeResponse) ? ((NativeResponse) adResponse).body : null;
    }

    public String getDescription() {
        return (adResponse instanceof NativeResponse) ? ((NativeResponse) adResponse).description : null;
    }

    public Double getRating() {
         return (adResponse instanceof NativeResponse) ? ((NativeResponse) adResponse).rating : null; 
    }

    public String getCallToAction() {
        return (adResponse instanceof NativeResponse) ? ((NativeResponse) adResponse).cta : null;
    }

    public String getImageUrl() {
        return (adResponse instanceof NativeResponse) ? ((NativeResponse) adResponse).imageUrl : null;
    }

    public String getIconUrl() {
        return (adResponse instanceof NativeResponse) ? ((NativeResponse) adResponse).iconUrl : null;
    }

    public void registerViewForInteraction(View view) {
        if (!isLoaded || adResponse == null) return;
        
        fireImpression();
        
        view.setOnClickListener(v -> {
            fireClick();
        });
    }
}
