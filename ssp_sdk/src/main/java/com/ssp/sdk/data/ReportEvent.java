package com.ssp.sdk.data;

public class ReportEvent {
    public String event; // request, show, click, init
    public int status; // 200, or error code
    public String slotId; // Ad Slot ID (was adid in previous version, renaming for clarity)
    public String appId;
    public String adId; // Unique Ad Response ID (from SSP)
    public String adType; // BANNER, NATIVE, VIDEO
    public long timestamp;
    public String title; // Optional

    public ReportEvent(String event, int status, String appId, String slotId, String adId, String adType, long timestamp, String title) {
        this.event = event;
        this.status = status;
        this.appId = appId;
        this.slotId = slotId;
        this.adId = adId;
        this.adType = adType;
        this.timestamp = timestamp;
        this.title = title;
    }
    
    public org.json.JSONObject toJson() {
        try {
            org.json.JSONObject json = new org.json.JSONObject();
            json.put("event", event);
            json.put("status", status);
            json.put("appId", appId);
            json.put("slotId", slotId);
            if (adId != null) json.put("adId", adId);
            if (adType != null) json.put("adType", adType);
            json.put("timestamp", timestamp);
            if (title != null) json.put("title", title);
            return json;
        } catch (Exception e) {
            return null;
        }
    }
}
