package com.ssp.sdk;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

import com.ssp.sdk.network.NetworkClient;
import com.ssp.sdk.util.DeviceInfoManager;

import com.ssp.sdk.data.ReportEvent;
import com.ssp.sdk.util.ReportManager;

import com.ssp.sdk.util.SspConfigManager;
import org.json.JSONObject;

public class SspSdk {
    private static final String TAG = "SspSdk";
    private static final String METADATA_APP_ID = "com.ssp.sdk.APP_ID";
    
    private static volatile SspSdk instance;
    private Context context;
    private String appId;
    private volatile boolean isInitialized = false;

    private SspSdk() {}

    public static SspSdk getInstance() {
        if (instance == null) {
            synchronized (SspSdk.class) {
                if (instance == null) {
                    instance = new SspSdk();
                }
            }
        }
        return instance;
    }

    /**
     * Initialize the SDK using App ID from Manifest.
     * Requires <meta-data android:name="com.ssp.sdk.APP_ID" android:value="YOUR_APP_ID" /> in AndroidManifest.xml
     */
    public void init(Context context) {
        init(context, null);
    }

    /**
     * Initialize the SDK with a specific App ID.
     * This method is thread-safe and ensures initialization runs only once.
     * @param context Application Context
     * @param appId The App ID. If null or empty, SDK tries to read from Manifest.
     */
    public synchronized void init(Context context, String appId) {
        if (isInitialized) {
            Log.d(TAG, "SDK already initialized");
            return;
        }
        
        if (context == null) {
             Log.e(TAG, "SspSdk Initialization Failed: Context is null.");
             return;
        }
        
        this.context = context.getApplicationContext();
        
        if (TextUtils.isEmpty(appId)) {
            this.appId = getAppIdFromManifest(this.context);
        } else {
            this.appId = appId;
        }

        if (TextUtils.isEmpty(this.appId)) {
            Log.e(TAG, "SspSdk Initialization Failed: App ID is missing. Please provide it in init() or via AndroidManifest meta-data.");
            return;
        } else {
            Log.d(TAG, "SspSdk Initialization - success App ID: " + this.appId);
        }
        
        // Pre-fetch device info
        DeviceInfoManager.getInstance(this.context);
        
        isInitialized = true;
        Log.d(TAG, "SDK initialized with App ID: " + this.appId);
        
        // Report initialization
        reportInitialization();
    }
    
    private void reportInitialization() {
        // New Analytics Reporting
        ReportManager.getInstance().report(new ReportEvent(
            "init",
            200,
            this.appId,
            null, // No slotId for init
            null, // No adId for init
            null, // No adType for init
            System.currentTimeMillis(),
            "SDK Initialization"
        ));
        
        // Log all initialized parameters
        DeviceInfoManager info = DeviceInfoManager.getInstance(context);
        Log.d(TAG, info.toString());
    }

    private String getAppIdFromManifest(Context context) {
        try {
            ApplicationInfo ai = context.getPackageManager().getApplicationInfo(
                    context.getPackageName(), PackageManager.GET_META_DATA);
            Bundle bundle = ai.metaData;
            if (bundle != null) {
                // Try string first
                String id = bundle.getString(METADATA_APP_ID);
                if (id == null) {
                    // Sometimes numeric IDs might be parsed as int/float, force string
                     Object val = bundle.get(METADATA_APP_ID);
                     if (val != null) {
                         id = String.valueOf(val);
                     }
                }
                return id;
            }
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "Failed to load meta-data, NameNotFound: " + e.getMessage());
        } catch (NullPointerException e) {
            Log.e(TAG, "Failed to load meta-data, NullPointer: " + e.getMessage());
        }
        return null;
    }

    public Context getContext() {
        return context;
    }

    public String getAppId() {
        Log.i(TAG, "Call getAppId -> App ID: " + appId);
        return appId;
    }
    
    public boolean isInitialized() {
        return isInitialized;
    }
}
