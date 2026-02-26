package com.ssp.sdk.network;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class NetworkClient {
    private static final String TAG = "NetworkClient";
    private static final int TIMEOUT_MS = 10000; // 10 seconds
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());
    private static final ExecutorService resolveExecutor = Executors.newSingleThreadExecutor();

    public interface Callback {
        void onSuccess(int code, String response);
        void onError(int code, String error);
    }

    public interface ResolveUrlCallback {
        void onResolved(int responseCode, String finalUrl, int redirectCount, String responseBody);
        void onFailure(String error);
    }

    public static void resolveClickUrl(final String url, final ResolveUrlCallback callback) {
        if (url == null || url.isEmpty()) {
            if (callback != null) callback.onFailure("URL is empty");
            return;
        }
        resolveUrlWithGet(url, callback);
    }

    public static void resolveUrlWithGet(final String url, final ResolveUrlCallback callback) {
        resolveExecutor.execute(() -> {
            String currentUrl = url;
            int redirectCount = 0;
            int finalCode = -1;
            String finalBody = null;
            
            try {
                Log.d("SSP_REDIRECT", "Starting resolution for: " + url);
                
                while (redirectCount < 5) {
                    HttpURLConnection connection = null;
                    try {
                        URL urlObj = new URL(currentUrl);
                        connection = (HttpURLConnection) urlObj.openConnection();
                        connection.setRequestMethod("GET");
                        connection.setConnectTimeout(TIMEOUT_MS);
                        connection.setReadTimeout(TIMEOUT_MS);
                        connection.setInstanceFollowRedirects(false); // Manual redirect handling
                        connection.connect();
                        
                        int code = connection.getResponseCode();
                        finalCode = code;
                        
                        Log.d("SSP_REDIRECT", "Hop " + (redirectCount + 1) + ": " + code + " -> " + currentUrl);
                        
                        if (code >= 200 && code < 300) {
                            // Success
                            // Only read body if content length is small or JSON? 
                            // For consistency with previous logic, we don't read body for click tracking usually, 
                            // but if this is used for config fetch, we MUST read body.
                            // Let's try to read body if it's available.
                            finalBody = readResponseBody(connection);
                            break;
                        } else if (code >= 300 && code < 400) {
                            String location = connection.getHeaderField("Location");
                            if (location == null || location.isEmpty()) {
                                break;
                            }
                            // Handle relative URL
                            URL base = new URL(currentUrl);
                            currentUrl = new URL(base, location).toString();
                            redirectCount++;
                        } else {
                            break;
                        }
                    } finally {
                        if (connection != null) {
                            connection.disconnect();
                        }
                    }
                }
                
                final String finalResult = currentUrl;
                final int codeResult = finalCode;
                final int countResult = redirectCount;
                final String bodyResult = finalBody;
                
                if (callback != null) {
                    mainHandler.post(() -> callback.onResolved(codeResult, finalResult, countResult, bodyResult));
                }
                
            } catch (Exception e) {
                if (callback != null) {
                    mainHandler.post(() -> callback.onFailure(e.getMessage()));
                }
            }
        });
    }

    public static void sendPostRequest(final String urlStr, final String jsonBody, final Callback callback) {
        resolveExecutor.execute(() -> {
            HttpURLConnection connection = null;
            try {
                URL url = new URL(urlStr);
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setConnectTimeout(TIMEOUT_MS);
                connection.setReadTimeout(TIMEOUT_MS);
                connection.setDoInput(true);
                connection.setDoOutput(true);
                connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
                connection.setRequestProperty("Accept", "application/json");

                // Write body
                try (OutputStream os = connection.getOutputStream()) {
                    byte[] input = jsonBody.getBytes(StandardCharsets.UTF_8);
                    os.write(input, 0, input.length);
                }

                int code = connection.getResponseCode();
                String responseBody = null;
                
                if (code >= 200 && code < 300) {
                    responseBody = readResponseBody(connection);
                    final String finalResp = responseBody;
                    if (callback != null) {
                        mainHandler.post(() -> callback.onSuccess(code, finalResp));
                    }
                } else {
                    responseBody = readErrorStream(connection);
                    if (callback != null) {
                        mainHandler.post(() -> callback.onError(code, "HTTP Error: " + code));
                    }
                }

            } catch (Exception e) {
                if (callback != null) {
                    mainHandler.post(() -> callback.onError(-1, e.getMessage()));
                }
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
        });
    }

    public static void sendTrackingUrl(final String urlStr) {
        if (urlStr == null || urlStr.isEmpty()) return;
        final String cleanUrl = urlStr.replace("`", "").trim();

        resolveExecutor.execute(() -> {
            HttpURLConnection connection = null;
            try {
                URL url = new URL(cleanUrl);
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(TIMEOUT_MS);
                connection.setReadTimeout(TIMEOUT_MS);
                connection.connect();
                // Just trigger request
                int code = connection.getResponseCode();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
        });
    }

    public static void sendTrackingUrls(java.util.List<String> urls) {
        if (urls == null || urls.isEmpty()) return;
        for (String url : urls) {
            sendTrackingUrl(url);
        }
    }

    public static void sendTrackingUrls(String[] urls) {
        if (urls == null || urls.length == 0) return;
        for (String url : urls) {
            sendTrackingUrl(url);
        }
    }

    /**
     * Resolve a URL that may involve POST redirects (307/308).
     * Preserves the original request method and body across redirects.
     */
    public static void resolveUrlWithPost(final String url, final String jsonBody, final ResolveUrlCallback callback) {
         resolveUrlWithPostInternal(url, jsonBody, 5, callback);
    }
    
    private static void resolveUrlWithPostInternal(final String urlStr, final String jsonBody, final int maxRedirects, final ResolveUrlCallback callback) {
        if (urlStr == null || urlStr.isEmpty()) {
            if (callback != null) callback.onFailure("URL is empty");
            return;
        }

        resolveExecutor.execute(() -> {
            String currentUrl = urlStr.replace("`", "").trim();
            String currentBody = jsonBody;
            int redirectCount = 0;
            int finalCode = -1;
            String method = (jsonBody == null) ? "GET" : "POST";
            String finalBody = null;
            
            try {
                Log.d("SSP_REDIRECT", "Starting POST resolution for: " + urlStr);
                
                while (redirectCount <= maxRedirects) {
                    HttpURLConnection connection = null;
                    try {
                        URL url = new URL(currentUrl);
                        connection = (HttpURLConnection) url.openConnection();
                        connection.setRequestMethod(method);
                        connection.setConnectTimeout(TIMEOUT_MS);
                        connection.setReadTimeout(TIMEOUT_MS);
                        connection.setInstanceFollowRedirects(false);
                        
                        if ("POST".equals(method) && currentBody != null) {
                            connection.setDoOutput(true);
                            connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
                            try (OutputStream os = connection.getOutputStream()) {
                                byte[] input = currentBody.getBytes(StandardCharsets.UTF_8);
                                os.write(input, 0, input.length);
                            }
                        }

                        int code = connection.getResponseCode();
                        finalCode = code;
                        
                        Log.d("SSP_REDIRECT", "Hop " + (redirectCount + 1) + " [" + method + "]: " + code + " -> " + currentUrl);

                        if (code >= 200 && code < 300) {
                            finalBody = readResponseBody(connection);
                            break;
                        } else if (code == 307 || code == 308) {
                            String location = connection.getHeaderField("Location");
                            if (location == null || location.isEmpty()) break;
                            
                            URL base = new URL(currentUrl);
                            currentUrl = new URL(base, location).toString();
                            // Keep POST method and body
                            redirectCount++;
                        } else if (code >= 300 && code < 400) {
                            String location = connection.getHeaderField("Location");
                            if (location == null || location.isEmpty()) break;
                            
                            URL base = new URL(currentUrl);
                            currentUrl = new URL(base, location).toString();
                            method = "GET"; // Downgrade to GET
                            currentBody = null;
                            redirectCount++;
                        } else {
                            break;
                        }
                    } finally {
                        if (connection != null) {
                            connection.disconnect();
                        }
                    }
                }

                final String finalResult = currentUrl;
                final int codeResult = finalCode;
                final int countResult = redirectCount;
                final String bodyResult = finalBody;
                
                if (callback != null) {
                    mainHandler.post(() -> callback.onResolved(codeResult, finalResult, countResult, bodyResult));
                }
                
            } catch (Exception e) {
                if (callback != null) {
                    mainHandler.post(() -> callback.onFailure(e.getMessage()));
                }
            }
        });
    }

    private static String readResponseBody(HttpURLConnection connection) throws IOException {
        try (InputStream is = connection.getInputStream();
             BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            StringBuilder response = new StringBuilder();
            String responseLine;
            while ((responseLine = br.readLine()) != null) {
                response.append(responseLine.trim());
            }
            return response.toString();
        }
    }
    
    private static String readErrorStream(HttpURLConnection connection) {
        try (InputStream is = connection.getErrorStream();
             BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            if (is == null) return null;
            StringBuilder response = new StringBuilder();
            String responseLine;
            while ((responseLine = br.readLine()) != null) {
                response.append(responseLine.trim());
            }
            return response.toString();
        } catch (Exception e) {
            return null;
        }
    }
}
