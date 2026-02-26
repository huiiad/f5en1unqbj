# ProGuard Rules for SSP SDK Internal Build

# Inherit rules from consumer-rules.pro
-include consumer-rules.pro

# Internal data models that might be accessed via reflection or need specific handling
# (Currently manual JSON parsing is used, so strictly not required, but good practice for data classes)
-keepclassmembers class com.ssp.sdk.data.** {
    <fields>;
}

# Preserve Line Numbers for Crash Reporting
-keepattributes SourceFile,LineNumberTable

# Preserve Annotations
-keepattributes *Annotation*

# Ensure OkHttp works
# -dontwarn okhttp3.**
# -dontwarn okio.**
