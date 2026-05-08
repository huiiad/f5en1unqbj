package com.ssp.sdk.data;

import org.json.JSONException;
import org.json.JSONObject;

import com.ssp.sdk.SspSdk;
import com.ssp.sdk.util.DeviceInfoManager;

public class AdRequest {
    private String adSlotId;
    private String adType; // VIDEO, BANNER, NATIVE
    private int width;
    private int height;

    public AdRequest(String adSlotId, String adType, int width, int height) {
        this.adSlotId = adSlotId;
        this.adType = adType;
        this.width = width;
        this.height = height;
    }

    public JSONObject toJson() {
        JSONObject json = new JSONObject();
        try {
            DeviceInfoManager info = DeviceInfoManager.getInstance(SspSdk.getInstance().getContext());
            
            json.put("os", info.os);
            json.put("ifa", info.getIfa());
            json.put("ip", info.getIp());
            json.put("appver", info.getAppVersion());
            json.put("deviceH", info.getScreenHeight());
            json.put("ua", info.getUserAgent());
            json.put("adType", adType);
            json.put("osv", info.getOsv());
            json.put("sourceType", "SDK");
            
            // Send exactly matching field names
            json.put("adSlotId", adSlotId);
            json.put("appId", SspSdk.getInstance().getAppId());
            
            // Log dimensions
            android.util.Log.d("SSP_SDK", "AdRequest toJson: width=" + width + ", height=" + height);
            
            // Standard fields
            json.put("width", width);
            json.put("height", height);
            
            // Remove aliases 'w' and 'h' to avoid strict JSON parsing errors on server
            // json.put("w", width);
            // json.put("h", height);

            json.put("deviceW", info.getScreenWidth());
            json.put("model", info.getDeviceModel());
            json.put("brand", info.getDeviceBrand());
            json.put("connectiontype", info.getConnectionType());
            json.put("language", info.getLanguage());
            
            // Extended location/region info
//            json.put("country", info.countryCodeIso3);
            json.put("country", "USA");
            json.put("region", info.region);
            json.put("city", info.city);
            json.put("timezone", info.timezone);
            
            if (info.latitude != null && info.longitude != null) {
                json.put("lat", info.latitude);
                json.put("lon", info.longitude);
            }
            
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return json;
    }
}
