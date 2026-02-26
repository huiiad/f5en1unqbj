package com.ssp.sdk.util;

import android.content.Context;
import android.os.Build;
import android.util.DisplayMetrics;
import android.util.Log;
import android.webkit.WebSettings;
import android.provider.Settings;
import android.view.WindowManager;
import android.view.WindowMetrics;
import android.view.Display;
import android.graphics.Rect;
import android.text.TextUtils;

import java.util.Locale;
import java.util.UUID;

import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.location.Location;
import android.location.LocationManager;
import android.content.pm.PackageManager;
import android.Manifest;

public class DeviceInfoManager {
    private static String Tag = DeviceInfoManager.class.getSimpleName();
    private static DeviceInfoManager instance;
    private Context context;

    // Cached values
    public int deviceW;
    public int deviceH;
    public String ua;
    public String os = "android";
    public String osv;
    public String brand;
    public String model;
    public String ifa; // Identifier for Advertising
    public String ip = "";
    public int connectionType;
    public Double latitude;
    public Double longitude;
    public String language;

    // Extended Location Info
    public String region;
    public String city;
    public String country;
    public String timezone;
    public String countryCodeIso3;

    // ISO 3166-1 alpha-2 to alpha-3 mapping (partial, common countries)
    private static final java.util.Map<String, String> ISO2_TO_ISO3 = new java.util.HashMap<>();

    static {
        ISO2_TO_ISO3.put("US", "USA");
        ISO2_TO_ISO3.put("CN", "CHN");
        ISO2_TO_ISO3.put("GB", "GBR");
        ISO2_TO_ISO3.put("JP", "JPN");
        ISO2_TO_ISO3.put("DE", "DEU");
        ISO2_TO_ISO3.put("FR", "FRA");
        ISO2_TO_ISO3.put("KR", "KOR");
        ISO2_TO_ISO3.put("IN", "IND");
        ISO2_TO_ISO3.put("BR", "BRA");
        ISO2_TO_ISO3.put("RU", "RUS");
        ISO2_TO_ISO3.put("CA", "CAN");
        ISO2_TO_ISO3.put("IT", "ITA");
        ISO2_TO_ISO3.put("AU", "AUS");
        ISO2_TO_ISO3.put("ES", "ESP");
        ISO2_TO_ISO3.put("MX", "MEX");
        ISO2_TO_ISO3.put("ID", "IDN");
        ISO2_TO_ISO3.put("TR", "TUR");
        ISO2_TO_ISO3.put("SA", "SAU");
        ISO2_TO_ISO3.put("CH", "CHE");
        ISO2_TO_ISO3.put("SE", "SWE");
        ISO2_TO_ISO3.put("NL", "NLD");
        ISO2_TO_ISO3.put("TW", "TWN");
        ISO2_TO_ISO3.put("HK", "HKG");
        ISO2_TO_ISO3.put("SG", "SGP");
        ISO2_TO_ISO3.put("MY", "MYS");
        ISO2_TO_ISO3.put("TH", "THA");
        ISO2_TO_ISO3.put("VN", "VNM");
    }

    private DeviceInfoManager(Context context) {
        this.context = context.getApplicationContext();
        Log.i(Tag, "DeviceInfoManager: Initializing...");
        fetchDeviceInfo();
    }

    public static synchronized DeviceInfoManager getInstance(Context context) {
        if (instance == null) {
            instance = new DeviceInfoManager(context);
        }
        return instance;
    }

    public int getScreenWidth() {
        return deviceW;
    }

    public int getScreenHeight() {
        return deviceH;
    }

    public String getAppVersion() {
        try {
            return context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionName;
        } catch (Exception e) {
            return "1.0";
        }
    }

    public String getUserAgent() {
        return ua;
    }

    public String getOsv() {
        return osv;
    }

    public String getDeviceBrand() {
        return brand;
    }

    public String getDeviceModel() {
        return model;
    }

    public int getConnectionType() {
        return connectionType;
    }

    public String getIfa() {
        return ifa;
    }

    public String getIp() {
        return ip;
    }

    public String getLanguage() {
        return language;
    }

    private void fetchDeviceInfo() {
        Log.d(Tag, "DeviceInfoManager: Fetching device info...");
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (wm != null) {
                try {
                    WindowMetrics windowMetrics = wm.getCurrentWindowMetrics();
                    Rect bounds = windowMetrics.getBounds();
                    this.deviceW = bounds.width();
                    this.deviceH = bounds.height();
                } catch (Exception e) {
                    // Fallback to display metrics if WindowMetrics fails (e.g. strict context context)
                    DisplayMetrics metrics = context.getResources().getDisplayMetrics();
                    this.deviceW = metrics.widthPixels;
                    this.deviceH = metrics.heightPixels;
                }
            } else {
                DisplayMetrics metrics = context.getResources().getDisplayMetrics();
                this.deviceW = metrics.widthPixels;
                this.deviceH = metrics.heightPixels;
            }
        } else {
            DisplayMetrics metrics = new DisplayMetrics();
            if (wm != null) {
                Display display = wm.getDefaultDisplay();
                display.getRealMetrics(metrics);
            } else {
                metrics = context.getResources().getDisplayMetrics();
            }
            this.deviceW = metrics.widthPixels;
            this.deviceH = metrics.heightPixels;
        }

        Log.d(Tag, "DeviceInfoManager: Screen Size: " + deviceW + " x " + deviceH);

        this.osv = Build.VERSION.RELEASE;
        this.brand = Build.BRAND;
        this.model = Build.MODEL;


        try {
            // Note: This must be called on UI thread usually, but for SDK init we might be on main thread.
            // If called from background, this might fail or need a Looper.
            // For safety, we can try-catch or use a fallback.
            this.ua = WebSettings.getDefaultUserAgent(context);
        } catch (Exception e) {
            this.ua = "Mozilla/5.0 (Linux; Android " + osv + "; " + model + ")";
        }


        String gmsId = getGmsIfa(context);
        DeviceInfoManager self = this;
        GAIDHelper.getGAID(context, new GAIDHelper.GAIDCallback() {
            @Override
            public void onGAIDReceived(String gaid) {
                self.ifa = gaid;
                Log.d(Tag, "DeviceInfoManager:Received GMS IFA: " + ifa);
                Log.d(Tag, "DeviceInfoManager: ALL: " + self.toString());
            }

            @Override
            public void onFailure(Exception e) {
                Log.e(Tag, "Failed to get GMS IFA", e);
            }
        });
        if (!TextUtils.isEmpty(gmsId)) {
            this.ifa = gmsId;
            Log.i(Tag, "DeviceInfoManager: GMS ID: " + this.ifa);
        } else {
            String androidId = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
            Log.i(Tag, "DeviceInfoManager: Android ID - Secure.ANDROID_ID: " + androidId);
            if (!TextUtils.isEmpty(androidId)) {
                this.ifa = UUID.nameUUIDFromBytes(androidId.getBytes()).toString();
                Log.i(Tag, "DeviceInfoManager: Android ID - UUIDFromBytes: " + this.ifa);
            } else {
                this.ifa = UUID.randomUUID().toString();
                Log.i(Tag, "DeviceInfoManager: Android ID - Random: " + this.ifa);
            }
        }


        this.ip = IpUtils.getLocalIp(context);
        this.connectionType = getConnectionType(context);

        try {
            this.language = Locale.getDefault().getLanguage();
            this.country = Locale.getDefault().getCountry();
        } catch (Exception e) {
            this.language = "en";
        }

        fetchLocation();
        fetchIpApiInfo();
    }

    private String convertIso2ToIso3(String iso2) {
        if (iso2 == null) return null;
        if (iso2.length() == 3) return iso2;

        // Try Locale conversion first
        try {
            Locale l = new Locale("", iso2);
            Log.i(Tag, "DeviceInfoManager: Locale ISO3 for " + iso2 + " is " + l.getISO3Country());
            return l.getISO3Country();
        } catch (Exception e) {
            // Fallback to manual map
            return ISO2_TO_ISO3.get(iso2.toUpperCase());
        }
    }

    private void fetchIpApiInfo() {
        Log.i(Tag, "DeviceInfoManager: Fetching IP API info...");
        new Thread(() -> {
            boolean success = fetchGeoInfo("https://ipapi.co/json/", true);
            if (!success) {
                Log.w(Tag, "ipapi.co failed (429 or error), trying fallback ipwho.is...");
                fetchGeoInfo("https://ipwho.is/", false);
            }
        }).start();
    }

    private boolean fetchGeoInfo(String urlStr, boolean isIpApiCo) {
        try {
            java.net.URL url = new java.net.URL(urlStr);
            java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);

            int responseCode = conn.getResponseCode();
            if (responseCode == 200) {
                java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(conn.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();

                org.json.JSONObject json = new org.json.JSONObject(response.toString());
                Log.i(Tag, "DeviceInfoManager: Geo API Response (" + urlStr + "): " + response.toString());

                // Check success for ipwho.is
                if (!isIpApiCo && json.has("success") && !json.optBoolean("success")) {
                    return false;
                }

                // Update fields
                if (json.has("region")) this.region = json.optString("region");
                if (json.has("city")) this.city = json.optString("city");

                String ipCountryName = isIpApiCo ? json.optString("country_name") : json.optString("country");

                // Prioritize system Locale for country, only use IP-based country if system locale failed
                // Or if they match (to update other fields)
                boolean updateCountry = false;

                if (TextUtils.isEmpty(this.country) || "US".equals(this.country)) {
                    updateCountry = true;
                } else if (ipCountryName != null && !ipCountryName.equalsIgnoreCase(this.country)) {
                    updateCountry = true;
                }

                if (updateCountry && !TextUtils.isEmpty(ipCountryName)) {
                    this.country = ipCountryName;
                }

                if (json.has("timezone")) this.timezone = json.optString("timezone");

                String ipCountryCode = isIpApiCo ? json.optString("country_code_iso3") : null;
                // ipwho.is uses "country_code" (2 letters)
                if (ipCountryCode == null && json.has("country_code")) {
                    ipCountryCode = json.optString("country_code");
                }

                if (json.has("country_code_iso3")) {
                    this.countryCodeIso3 = json.optString("country_code_iso3");
                } else if (ipCountryCode != null) {
                    // Convert 2-letter to 3-letter if needed
                    this.countryCodeIso3 = convertIso2ToIso3(ipCountryCode);
                    if (this.countryCodeIso3 == null)
                        this.countryCodeIso3 = ipCountryCode; // Fallback
                } else if (updateCountry && this.country != null) {
                    // updated country but no code, leave it
                }

                // Ensure we always have a 3-letter code if possible
                if (this.countryCodeIso3 != null && this.countryCodeIso3.length() == 2) {
                    this.countryCodeIso3 = convertIso2ToIso3(this.countryCodeIso3);
                }

                // Update Lat/Lon if missing
                if (this.latitude == null && json.has("latitude")) {
                    this.latitude = json.optDouble("latitude");
                }
                if (this.longitude == null && json.has("longitude")) {
                    this.longitude = json.optDouble("longitude");
                }

                Log.d(Tag, "Updated device info from geo api: " + this.toString());
                return true;
            } else {
                Log.w(Tag, "Geo API returned code: " + responseCode);
            }
        } catch (Exception e) {
            Log.e(Tag, "Failed to fetch geo info from " + urlStr + ": " + e.getMessage());
        }
        return false;
    }

    private int getConnectionType(Context context) {
        int type = 0; // Default to 0 (Mobile) or handle as unknown? 
        // ConnectivityManager.TYPE_MOBILE = 0
        // ConnectivityManager.TYPE_WIFI = 1
        // We will try to return these values.

        try {
            ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            if (cm == null) {
                return -1; // Unknown or No Network
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                android.net.Network network = cm.getActiveNetwork();
                if (network == null) {
                    return -1;
                }
                NetworkCapabilities capabilities = cm.getNetworkCapabilities(network);
                if (capabilities == null) {
                    return -1;
                }

                if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                    return 1; // ConnectivityManager.TYPE_WIFI
                } else if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
                    return 0; // ConnectivityManager.TYPE_MOBILE
                } else if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) {
                    return 9; // ConnectivityManager.TYPE_ETHERNET
                }
            } else {
                NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
                if (activeNetwork != null && activeNetwork.isConnected()) {
                    return activeNetwork.getType();
                } else {
                    return -1;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return -1;
    }

    private void fetchLocation() {
        Log.i(Tag, "Fetching location");
        try {
            boolean fineGranted = context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
            boolean coarseGranted = context.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
            if (!fineGranted && !coarseGranted) {
                return;
            }

            LocationManager lm = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
            if (lm == null) {
                return;
            }
            Location loc = null;
            try {
                loc = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            } catch (SecurityException ignored) {
            }
            if (loc == null) {
                try {
                    loc = lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                } catch (SecurityException ignored) {
                }
            }
            if (loc != null) {
                latitude = loc.getLatitude();
                longitude = loc.getLongitude();
            }
        } catch (Throwable t) {
            // Ignore and leave latitude/longitude as null
        }
    }

    private String getGmsIfa(Context context) {
        try {
//            Class<?> clientClass = Class.forName("com.google.android.gms.ads.identifier.AdvertisingIdClient");
//            java.lang.reflect.Method getInfoMethod = clientClass.getMethod("getAdvertisingIdInfo", Context.class);
//            Object info = getInfoMethod.invoke(null, context);
//            if (info != null) {
//                java.lang.reflect.Method getIdMethod = info.getClass().getMethod("getId");
//                Object id = getIdMethod.invoke(info);
//                Log.d(Tag, "DeviceInfoManager: GMS IFA: " + id);
//                if (id instanceof String) {
//                    return (String) id;
//                }
//            } else {
            GAIDHelper.getGAID(context, new GAIDHelper.GAIDCallback() {
                @Override
                public void onGAIDReceived(String gaid) {
                    ifa = gaid;
                    Log.d(Tag, "DeviceInfoManager: GMS IFA: " + ifa);
                }

                @Override
                public void onFailure(Exception e) {
                    Log.e(Tag, "Failed to get GMS IFA", e);
                }
            });
//            }
        } catch (Throwable t) {
            Log.e(Tag, "Throwable ,Failed to get GMS IFA", t);
        }
        return ifa;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("========== DeviceInfoManager ==========\n");
        sb.append("OS: ").append(os).append("\n");
        sb.append("OS Version: ").append(osv).append("\n");
        sb.append("Device: ").append(brand).append(" ").append(model).append("\n");
        sb.append("Screen Size: ").append(deviceW).append("x").append(deviceH).append("\n");
        sb.append("Language: ").append(language).append("\n");
        sb.append("Country: ").append(country).append("\n");
        sb.append("Region: ").append(region).append("\n");
        sb.append("City: ").append(city).append("\n");
        sb.append("Timezone: ").append(timezone).append("\n");
        sb.append("ISO3: ").append(countryCodeIso3).append("\n");
        sb.append("User Agent: ").append(ua).append("\n");
        sb.append("IFA: ").append(ifa).append("\n");
        sb.append("IP: ").append(ip).append("\n");
        sb.append("Connection Type: ").append(connectionType).append("\n");
        if (latitude != null && longitude != null) {
            sb.append("Location: ").append(latitude).append(", ").append(longitude).append("\n");
        } else {
            sb.append("Location: null\n");
        }
        sb.append("=======================================");
        return sb.toString();
    }
}
