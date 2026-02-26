# ProGuard Rules for SSP SDK

# Keep public API classes and interfaces so they can be accessed by the host app
-keep class com.ssp.sdk.SspSdk {
    public static com.ssp.sdk.SspSdk getInstance();
    public *;
}

# Keep Ad Classes and their public methods
-keep class com.ssp.sdk.ad.SspBannerAd {
    public <init>(android.content.Context, java.lang.String);
    public *;
}

-keep class com.ssp.sdk.ad.SspNativeAd {
    public <init>(android.content.Context, java.lang.String);
    public *;
}

-keep class com.ssp.sdk.ad.SspVideoAd {
    public <init>(android.content.Context, java.lang.String);
    public *;
}

# Keep Listener Interfaces
-keep interface com.ssp.sdk.ad.SspLoadListener { *; }
-keep interface com.ssp.sdk.ad.SspInteractionListener { *; }

# Keep BaseAd for reference in listeners (though it's abstract, public API uses it)
-keep class com.ssp.sdk.ad.BaseAd { 
    public *;
}

# Keep Data Models if exposed (AdResponse might be used internally but BaseAd uses it)
# Generally we don't expose AdResponse directly to users in doc, but if used in signatures:
-keep class com.ssp.sdk.data.AdType { *; }

# Internal Helpers - typically we obfuscate these, but if serialization relies on reflection:
# ReportEvent uses org.json, no GSON/Jackson reflection, so safe to obfuscate.
# AdResponse uses manual JSON parsing, safe to obfuscate.

# Keep FullScreen Activity
-keep class com.ssp.sdk.ad.SspFullScreenVideoActivity { *; }

# Keep dependencies if necessary (OkHttp usually has its own consumer rules)
# -dontwarn okhttp3.**
# -dontwarn okio.**
