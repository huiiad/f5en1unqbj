package com.ssp.sdk.util;

import com.ssp.sdk.data.ReportEvent;
import com.ssp.sdk.network.NetworkClient;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class ReportManager {
    private static ReportManager instance;
    private static final String REPORT_URL = "http://sdk-sg.huiinex.com/report";

//    private static final String REPORT_URL = "http://10.33.39.19:8066/report";
    private static final int BATCH_SIZE = 10;
    private static final long FLUSH_INTERVAL_SECONDS = 300; // 5 minutes

    private final BlockingQueue<ReportEvent> eventQueue = new LinkedBlockingQueue<>();
    // Use a Set to track recently enqueued events for deduplication within a short window.
    // The key could be a hash or a combination of event + slotId + adId + timestamp(approx)
    // But since timestamps might differ slightly, maybe deduplicate by unique identifiers if available?
    // User says "same event+slotId+appId+adId".
    // We can use a simple cache with expiration or just
    // check queue?
    // Checking queue is O(N).
    // Let's use a deduplication Set that clears on flush.
    private final Set<String> pendingEventKeys = Collections.newSetFromMap(new ConcurrentHashMap<>());
    
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private long lastEventTime = 0;

    private ReportManager() {
        startScheduler();
    }

    public static synchronized ReportManager getInstance() {
        if (instance == null) {
            instance = new ReportManager();
        }
        return instance;
    }

    public void report(ReportEvent event) {
        // Generate a deduplication key
        // Key format: event_slotId_appId_adId
        // We ignore timestamp for deduplication to catch rapid duplicate firings
        String dedupKey = event.event + "_" + (event.slotId != null ? event.slotId : "") + "_" + 
                          (event.appId != null ? event.appId : "") + "_" + 
                          (event.adId != null ? event.adId : "");
                          
        // Check if this event is already pending in the queue
        if (pendingEventKeys.contains(dedupKey)) {
            android.util.Log.d("SSP_REPORT", "Duplicate event ignored: " + dedupKey);
            return;
        }

        if (eventQueue.offer(event)) {
            pendingEventKeys.add(dedupKey);
            lastEventTime = System.currentTimeMillis();
            android.util.Log.d("SSP_REPORT", "Enqueued event: " + event.event + ", Status: " + event.status + ", SlotId: " + event.slotId + ", AdId: " + event.adId);
        }
        
        if (eventQueue.size() >= BATCH_SIZE) {
            flush();
        }
    }

    private void startScheduler() {
        // Check every 30 seconds if we need to flush due to timeout
        scheduler.scheduleAtFixedRate(() -> {
            long currentTime = System.currentTimeMillis();
            if (!eventQueue.isEmpty() && (currentTime - lastEventTime >= TimeUnit.SECONDS.toMillis(FLUSH_INTERVAL_SECONDS))) {
                android.util.Log.d("SSP_REPORT", "Flushing due to timeout (" + FLUSH_INTERVAL_SECONDS + "s)");
                flush();
            }
        }, 30, 30, TimeUnit.SECONDS);
    }

    private synchronized void flush() {
        if (eventQueue.isEmpty()) return;

        List<ReportEvent> batch = new ArrayList<>();
        eventQueue.drainTo(batch);
        
        // Clear the deduplication set for the flushed events
        // Wait, if we clear it here, and the report fails, we might lose dedup protection for retries?
        // But if report fails, we don't put them back in queue (current impl).
        // If we want to support rapid re-firing of same event after a flush, clearing here is correct.
        // The dedup is mainly for "double click" or "rapid duplicate logic" scenarios.
        // Once flushed, it's safe to allow a new similar event (e.g. user clicked again later).
        for (ReportEvent event : batch) {
            String dedupKey = event.event + "_" + (event.slotId != null ? event.slotId : "") + "_" + 
                              (event.appId != null ? event.appId : "") + "_" + 
                              (event.adId != null ? event.adId : "");
            pendingEventKeys.remove(dedupKey);
        }

        if (batch.isEmpty()) return;

        JSONArray jsonArray = new JSONArray();
        for (ReportEvent event : batch) {
            JSONObject json = event.toJson();
            if (json != null) {
                jsonArray.put(json);
            }
        }

        String jsonString = jsonArray.toString();
        android.util.Log.d("SSP_REPORT", "Reporting batch (" + batch.size() + " events): " + jsonString);

        if (jsonArray.length() > 0) {
            NetworkClient.sendPostRequest(REPORT_URL, jsonString, new NetworkClient.Callback() {
                @Override
                public void onSuccess(int code, String response) {
                    android.util.Log.d("SSP_REPORT", "Report Success: Code " + code + ", Response: " + response);
                }

                @Override
                public void onError(int code, String error) {
                    android.util.Log.e("SSP_REPORT", "Report Failed: Code " + code + ", Error: " + error);
                    // Failed, maybe re-queue? For now, we drop to avoid infinite loops/memory leaks
                    // In a real robust SDK, we might persist to disk and retry later.
                }
            });
        }
    }
}
