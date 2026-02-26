package com.ssp.sdk.data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class VideoResponse extends AdResponse {
    public int duration;
    public boolean rewarded;
    public int renderWidth;
    public int renderHeight;
    public boolean skippable;
    public String placement;
    public String linearity;
    public Map<String, List<String>> trackingEvents = new HashMap<>();
    public List<MediaFile> mediaFiles = new ArrayList<>();
    public String videoUrl; // Extracted from mediaFiles for convenience
    public String title;
    public EndCard endCard;

    public static class EndCard {
        public String type; // STATIC, HTML
        public String imageUrl; // For STATIC
        public String htmlContent; // For HTML
        public String clickUrl;
        public List<String> clickTrackers;
        public List<String> viewTrackers;
        public Integer width;
        public Integer height;
    }

    public static class MediaFile {
        public String url;
        public String type;
        public String delivery;
        public int width;
        public int height;
        public int bitrate;
    }
}
