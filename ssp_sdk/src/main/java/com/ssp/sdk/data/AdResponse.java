package com.ssp.sdk.data;

import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;

public class AdResponse {
    // Common fields
    public String id;
    public String impId;
    public String adType;
    public double price;
    public String currency;
    public int width;
    public int height;
    public String creativeId;
    public List<String> adomain = new ArrayList<>();
    public String billingType;
    public List<String> impTrackers = new ArrayList<>();
    public List<String> clickTrackers = new ArrayList<>();
    public String clickUrl;
    public String dealId;
    public String ext; // Assuming JSON string or null

    public static AdResponse parse(String jsonStr) {
        try {
            JSONObject json = new JSONObject(jsonStr);
            String adType = json.optString("adType");
            
            // Infer ad type if missing but distinctive fields are present
            if (adType.isEmpty()) {
                if (json.has("htmlContent")) {
                    adType = "BANNER";
                } else if (json.has("mediaFiles") || json.has("videoUrl")) {
                    adType = "VIDEO";
                } else if (json.has("title") && json.has("body")) {
                    adType = "NATIVE";
                }
            }
            
            AdResponse response;
            if ("BANNER".equalsIgnoreCase(adType)) {
                response = new BannerResponse();
            } else if ("NATIVE".equalsIgnoreCase(adType)) {
                response = new NativeResponse();
            } else if ("VIDEO".equalsIgnoreCase(adType)) {
                response = new VideoResponse();
            } else {
                response = new AdResponse(); // Fallback
            }

            parseCommon(response, json);
            
            if (response instanceof BannerResponse) {
                parseBanner((BannerResponse) response, json);
            } else if (response instanceof NativeResponse) {
                parseNative((NativeResponse) response, json);
            } else if (response instanceof VideoResponse) {
                parseVideo((VideoResponse) response, json);
            }
            
            return response;

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private static String cleanString(String input) {
        if (input == null) return null;
        return input.replace("`", "").trim();
    }

    private static void parseCommon(AdResponse response, JSONObject json) {
        response.id = cleanString(json.optString("id"));
        response.impId = cleanString(json.optString("impId"));
        response.adType = cleanString(json.optString("adType"));
        response.price = json.optDouble("price");
        response.currency = cleanString(json.optString("currency"));
        response.width = json.optInt("width");
        response.height = json.optInt("height");
        response.creativeId = cleanString(json.optString("creativeId"));
        
        JSONArray adomainArr = json.optJSONArray("adomain");
        if (adomainArr != null) {
            for(int i=0; i<adomainArr.length(); i++) response.adomain.add(cleanString(adomainArr.optString(i)));
        }
        
        response.billingType = cleanString(json.optString("billingType"));
        
        JSONArray impArr = json.optJSONArray("impTrackers");
        if (impArr != null) {
            for (int i = 0; i < impArr.length(); i++) response.impTrackers.add(cleanString(impArr.optString(i)));
        }
        
        JSONArray clickArr = json.optJSONArray("clickTrackers");
        if (clickArr != null) {
            for (int i = 0; i < clickArr.length(); i++) response.clickTrackers.add(cleanString(clickArr.optString(i)));
        }
        
        response.clickUrl = cleanString(json.optString("clickUrl"));
        if ("null".equals(response.clickUrl)) response.clickUrl = null;
        if (json.isNull("clickUrl")) response.clickUrl = null;

        response.dealId = cleanString(json.optString("dealId"));
        response.ext = cleanString(json.optString("ext"));
    }

    private static void parseBanner(BannerResponse response, JSONObject json) {
        response.htmlContent = cleanString(json.optString("htmlContent"));
    }

    private static void parseNative(NativeResponse response, JSONObject json) {
        response.title = cleanString(json.optString("title"));
        response.body = cleanString(json.optString("body"));
        response.description = cleanString(json.optString("description"));
        response.cta = cleanString(json.optString("cta"));
        if (response.cta.isEmpty()) response.cta = cleanString(json.optString("callToAction"));
        
        if (json.has("rating") && !json.isNull("rating")) {
            response.rating = json.optDouble("rating");
        }
        
        response.iconUrl = cleanString(json.optString("iconUrl"));
        response.iconWidth = json.optInt("iconWidth");
        response.iconHeight = json.optInt("iconHeight");
        
        response.imageUrl = cleanString(json.optString("imageUrl"));
        response.imageWidth = json.optInt("imageWidth");
        response.imageHeight = json.optInt("imageHeight");
        
        response.privacyUrl = cleanString(json.optString("privacyUrl"));
        
        JSONArray eventTrackersArr = json.optJSONArray("eventTrackers");
        if (eventTrackersArr != null) {
            for (int i = 0; i < eventTrackersArr.length(); i++) {
                JSONObject etJson = eventTrackersArr.optJSONObject(i);
                if (etJson != null) {
                    NativeResponse.EventTracker et = new NativeResponse.EventTracker();
                    et.event = etJson.optInt("event");
                    et.method = etJson.optInt("method");
                    et.url = cleanString(etJson.optString("url"));
                    response.eventTrackers.add(et);
                }
            }
        }
    }

    private static void parseVideo(VideoResponse response, JSONObject json) {
        response.duration = json.optInt("duration");
        response.rewarded = json.optBoolean("rewarded");
        response.renderWidth = json.optInt("renderWidth");
        response.renderHeight = json.optInt("renderHeight");
        response.skippable = json.optBoolean("skippable");
        response.placement = cleanString(json.optString("placement"));
        response.linearity = cleanString(json.optString("linearity"));
        
        JSONObject trackingEventsObj = json.optJSONObject("trackingEvents");
        if (trackingEventsObj != null) {
            java.util.Iterator<String> keys = trackingEventsObj.keys();
            while(keys.hasNext()) {
                String key = keys.next();
                JSONArray urls = trackingEventsObj.optJSONArray(key);
                if (urls != null) {
                    List<String> urlList = new ArrayList<>();
                    for(int i=0; i<urls.length(); i++) {
                        urlList.add(cleanString(urls.optString(i)));
                    }
                    response.trackingEvents.put(key, urlList);
                }
            }
        }
        
        JSONArray mediaFilesArr = json.optJSONArray("mediaFiles");
        if (mediaFilesArr != null) {
            for (int i = 0; i < mediaFilesArr.length(); i++) {
                JSONObject mfJson = mediaFilesArr.optJSONObject(i);
                if (mfJson != null) {
                    VideoResponse.MediaFile mf = new VideoResponse.MediaFile();
                    mf.url = cleanString(mfJson.optString("url"));
                    mf.type = cleanString(mfJson.optString("type"));
                    mf.delivery = cleanString(mfJson.optString("delivery"));
                    mf.width = mfJson.optInt("width");
                    mf.height = mfJson.optInt("height");
                    mf.bitrate = mfJson.optInt("bitrate");
                    response.mediaFiles.add(mf);
                }
            }
        }
        
        // Extract a valid video URL for convenience
        if (!response.mediaFiles.isEmpty()) {
            response.videoUrl = response.mediaFiles.get(0).url;
        }
        
        response.title = cleanString(json.optString("title"));
        
        JSONObject ecJson = json.optJSONObject("endCard");
        if (ecJson != null) {
            VideoResponse.EndCard ec = new VideoResponse.EndCard();
            ec.type = cleanString(ecJson.optString("type"));
            ec.imageUrl = cleanString(ecJson.optString("imageUrl"));
            ec.htmlContent = cleanString(ecJson.optString("htmlContent"));
            ec.clickUrl = cleanString(ecJson.optString("clickUrl"));
            ec.width = ecJson.optInt("width");
            ec.height = ecJson.optInt("height");
            
            JSONArray ecClick = ecJson.optJSONArray("clickTrackers");
            if (ecClick != null) {
                ec.clickTrackers = new ArrayList<>();
                for(int i=0; i<ecClick.length(); i++) ec.clickTrackers.add(cleanString(ecClick.optString(i)));
            }
            
            JSONArray ecView = ecJson.optJSONArray("viewTrackers");
            if (ecView != null) {
                ec.viewTrackers = new ArrayList<>();
                for(int i=0; i<ecView.length(); i++) ec.viewTrackers.add(cleanString(ecView.optString(i)));
            }
            
            response.endCard = ec;
        }
    }
}
