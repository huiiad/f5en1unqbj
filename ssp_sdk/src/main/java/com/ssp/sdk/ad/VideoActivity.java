package com.ssp.sdk.ad;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.view.MotionEvent;
import android.view.View;
import android.widget.MediaController;
import android.widget.VideoView;
import android.widget.FrameLayout;
import android.widget.TextView;
import java.util.HashMap;
import java.util.Map;

import com.ssp.sdk.network.NetworkClient;

public class VideoActivity extends Activity {
    private VideoView videoView;
    private String videoUrl;
    private String clickUrl;
    private static Map<String, String[]> trackingEvents; // Static for simplicity in passing complex data
    private boolean isFirstQuartileFired = false;
    private boolean isMidpointFired = false;
    private boolean isThirdQuartileFired = false;
    private boolean isImpressionFired = false;
    private Handler handler = new Handler();

    public static void launch(Context context, String url, String click, Map<String, String[]> events) {
        trackingEvents = events;
        Intent intent = new Intent(context, VideoActivity.class);
        intent.putExtra("videoUrl", url);
        intent.putExtra("clickUrl", click);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        FrameLayout layout = new FrameLayout(this);
        layout.setBackgroundColor(0xFF000000);
        
        videoView = new VideoView(this);
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT, 
            FrameLayout.LayoutParams.WRAP_CONTENT
        );
        params.gravity = android.view.Gravity.CENTER;
        layout.addView(videoView, params);

        // Close button
        TextView closeBtn = new TextView(this);
        closeBtn.setText("X");
        closeBtn.setTextColor(0xFFFFFFFF);
        closeBtn.setTextSize(24);
        closeBtn.setPadding(30, 30, 30, 30);
        closeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        FrameLayout.LayoutParams closeParams = new FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT, 
            FrameLayout.LayoutParams.WRAP_CONTENT
        );
        closeParams.gravity = android.view.Gravity.TOP | android.view.Gravity.END;
        layout.addView(closeBtn, closeParams);

        setContentView(layout);

        videoUrl = getIntent().getStringExtra("videoUrl");
        clickUrl = getIntent().getStringExtra("clickUrl");

        videoView.setVideoPath(videoUrl);
        videoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                mp.start();
                fireEvent("start");
                fireEvent("creativeView");
                startProgressTracker();
            }
        });

        videoView.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                fireEvent("complete");
                finish();
            }
        });

        videoView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_UP) {
                    handleClick();
                }
                return true;
            }
        });
    }

    private void startProgressTracker() {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (videoView != null && videoView.isPlaying()) {
                    int current = videoView.getCurrentPosition();
                    int duration = videoView.getDuration();
                    if (duration > 0) {
                        int progress = (current * 100) / duration;
                        
                        if (progress >= 25 && !isFirstQuartileFired) {
                            isFirstQuartileFired = true;
                            fireEvent("firstQuartile");
                        }
                        if (progress >= 50 && !isMidpointFired) {
                            isMidpointFired = true;
                            fireEvent("midpoint");
                        }
                        if (progress >= 75 && !isThirdQuartileFired) {
                            isThirdQuartileFired = true;
                            fireEvent("thirdQuartile");
                        }
                    }
                    handler.postDelayed(this, 1000);
                }
            }
        }, 1000);
    }

    private void fireEvent(String eventName) {
        if (trackingEvents != null && trackingEvents.containsKey(eventName)) {
            String[] urls = trackingEvents.get(eventName);
            if (urls != null) {
                for (String url : urls) {
                    NetworkClient.sendTrackingUrl(url);
                }
            }
        }
    }

    private void handleClick() {
        if (clickUrl != null && !clickUrl.isEmpty()) {
            try {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(clickUrl));
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
            } catch (Exception e) {}
        }
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacksAndMessages(null);
    }
}
