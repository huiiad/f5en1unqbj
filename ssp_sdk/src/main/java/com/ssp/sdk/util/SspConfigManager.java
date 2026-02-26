package com.ssp.sdk.util;

import org.json.JSONObject;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SspConfigManager {
    private static SspConfigManager instance;
    private final Map<String, Integer> slotRefreshIntervals = new ConcurrentHashMap<>();
    private static final int DEFAULT_REFRESH_INTERVAL = 5; // Default 5 seconds

    private SspConfigManager() {}

    public static synchronized SspConfigManager getInstance() {
        if (instance == null) {
            instance = new SspConfigManager();
        }
        return instance;
    }

    /**
     * Get refresh interval for a specific slot.
     * @param slotId The Ad Slot ID
     * @return Interval in seconds. Returns DEFAULT_REFRESH_INTERVAL if not found.
     */
    public int getRefreshInterval(String slotId) {
        if (slotId == null) return DEFAULT_REFRESH_INTERVAL;
        return slotRefreshIntervals.getOrDefault(slotId, DEFAULT_REFRESH_INTERVAL);
    }

    /**
     * Update configuration from JSON response (e.g. from Init).
     * Expected format:
     * {
     *   "slots": {
     *     "slot_banner_001": { "refreshInterval": 5 },
     *     "slot_banner_002": { "refreshInterval": 10 }
     *   }
     * }
     */
    public void updateConfig(JSONObject json) {
        if (json == null) return;
        
        try {
            JSONObject slots = json.optJSONObject("slots");
            if (slots != null) {
                java.util.Iterator<String> keys = slots.keys();
                while (keys.hasNext()) {
                    String slotId = keys.next();
                    JSONObject slotConfig = slots.optJSONObject(slotId);
                    if (slotConfig != null) {
                        int interval = slotConfig.optInt("refreshInterval", 0);
                        if (interval > 0) {
                            slotRefreshIntervals.put(slotId, interval);
                        }
                    }
                }
            }
        } catch (Exception e) {
            android.util.Log.e("SspConfigManager", "Failed to parse config: " + e.getMessage());
        }
    }
}
