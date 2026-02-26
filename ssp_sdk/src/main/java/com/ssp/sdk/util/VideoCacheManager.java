package com.ssp.sdk.util;

import android.content.Context;
import android.util.Log;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.security.MessageDigest;
import java.util.HashSet;
import java.util.Set;
import java.net.HttpURLConnection;
import java.net.URL;

public class VideoCacheManager {
    private static final String TAG = "VideoCacheManager";
    private static VideoCacheManager instance;
    private Set<String> downloadingUrls = new HashSet<>();

    private VideoCacheManager() {}

    public static synchronized VideoCacheManager getInstance() {
        if (instance == null) {
            instance = new VideoCacheManager();
        }
        return instance;
    }

    public void cacheVideo(Context context, String url) {
        if (url == null || url.isEmpty()) return;
        if (isCached(context, url)) return;
        if (downloadingUrls.contains(url)) return;

        downloadingUrls.add(url);
        new Thread(() -> {
            HttpURLConnection connection = null;
            try {
                Log.d(TAG, "Starting cache for: " + url);
                URL urlObj = new URL(url);
                connection = (HttpURLConnection) urlObj.openConnection();
                connection.setConnectTimeout(15000);
                connection.setReadTimeout(15000);
                connection.connect();

                if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                    File file = getCacheFile(context, url);
                    File tempFile = new File(file.getAbsolutePath() + ".tmp");
                    
                    try (InputStream is = connection.getInputStream();
                         FileOutputStream fos = new FileOutputStream(tempFile)) {
                        byte[] buffer = new byte[8192];
                        int read;
                        while ((read = is.read(buffer)) != -1) {
                            fos.write(buffer, 0, read);
                        }
                        fos.flush();
                    }
                    
                    // Rename temp to final
                    if (tempFile.renameTo(file)) {
                        Log.d(TAG, "Cache success: " + file.getAbsolutePath());
                    } else {
                        Log.e(TAG, "Failed to rename cache file");
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Cache failed for " + url, e);
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
                downloadingUrls.remove(url);
            }
        }).start();
    }

    public boolean isCached(Context context, String url) {
        File file = getCacheFile(context, url);
        return file != null && file.exists() && file.length() > 0;
    }
    
    public boolean isCaching(String url) {
        return downloadingUrls.contains(url);
    }

    public String getCachedVideoPath(Context context, String url) {
        if (isCached(context, url)) {
            return getCacheFile(context, url).getAbsolutePath();
        }
        return null;
    }

    private File getCacheFile(Context context, String url) {
        try {
            String filename = md5(url) + ".mp4";
            return new File(context.getCacheDir(), filename);
        } catch (Exception e) {
            return null;
        }
    }

    private String md5(String s) {
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            digest.update(s.getBytes());
            byte[] messageDigest = digest.digest();
            StringBuilder hexString = new StringBuilder();
            for (byte b : messageDigest) {
                String h = Integer.toHexString(0xFF & b);
                while (h.length() < 2) h = "0" + h;
                hexString.append(h);
            }
            return hexString.toString();
        } catch (Exception e) {
            return String.valueOf(s.hashCode());
        }
    }
}
