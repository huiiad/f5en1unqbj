package com.ssp.sdk.util;

import android.content.Context;
import com.google.android.gms.ads.identifier.AdvertisingIdClient;
import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import java.io.IOException;

public class GAIDHelper {

    public interface GAIDCallback {
        void onGAIDReceived(String gaid);
        void onFailure(Exception e);
    }

    public static void getGAID(final Context context, final GAIDCallback callback) {
        // 必须在子线程中执行
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    // 调用 Google 官方接口获取信息
                    AdvertisingIdClient.Info adInfo = AdvertisingIdClient.getAdvertisingIdInfo(context);

                    if (adInfo != null) {
                        final String gaid = adInfo.getId();
                        // 检查用户是否限制了广告追踪 (Optional)
                        boolean isLAT = adInfo.isLimitAdTrackingEnabled();

                        // 回调到主线程或处理数据
                        if (callback != null) {
                            callback.onGAIDReceived(gaid);
                        }
                    }
                } catch (IOException | GooglePlayServicesRepairableException | GooglePlayServicesNotAvailableException e) {
                    if (callback != null) {
                        callback.onFailure(e);
                    }
                }
            }
        }).start();
    }
}