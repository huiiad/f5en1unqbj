package com.ssp.sdk.data;

import java.util.ArrayList;
import java.util.List;

public class NativeResponse extends AdResponse {
    public String title;
    public String body;
    public String description;
    public String cta; // callToAction
    public Double rating;
    public String iconUrl;
    public int iconWidth;
    public int iconHeight;
    public String imageUrl;
    public int imageWidth;
    public int imageHeight;
    public String privacyUrl;
    public List<EventTracker> eventTrackers = new ArrayList<>();

    public static class EventTracker {
        public int event;
        public int method;
        public String url;
    }
}
