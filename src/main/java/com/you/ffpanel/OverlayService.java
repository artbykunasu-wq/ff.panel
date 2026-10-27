package com.you.ffpanel;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.FrameLayout;
import android.widget.TextView;

public class OverlayService extends Service {

    private WindowManager wm;
    private final Handler ui = new Handler(Looper.getMainLooper());
    private TextView bubble;
    private FrameLayout panel;
    private WindowManager.LayoutParams bp, pp;
    private boolean open = false;

    @Override public IBinder onBind(Intent i) { return null; }

    @Override public int onStartCommand(Intent i, int f, int s) { return START_STICKY; }

    @Override public void onCreate() {
        super.onCreate();
        startForeground(1, buildNotification());
        wm = (WindowManager) getSystemService(WINDOW_SERVICE);
        addBubble();
    }

    private int winType() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                : WindowManager.LayoutParams.TYPE_PHONE;
    }

    private int dp(float v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }

    private Notification buildNotification() {
        String ch = "ffpanel";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel c = new NotificationChannel(
                    ch, "FF Panel", NotificationManager.IMPORTANCE_LOW);
            ((NotificationManager) getSystemService(NOTIFICATION_SERVICE))
                    .createNotificationChannel(c);
        }
        int fl = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                ? PendingIntent.FLAG_IMMUTABLE : 0;
        PendingIntent pi = PendingIntent.getActivity(this, 0,
                new Intent(this, MainActivity.class), fl);

        Notification.Builder b = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? new Notification.Builder(this, ch)
                : new Notification.Builder(this);

        return b.setContentTitle("FF Panel running")
                .setContentText("Tap the FF bubble to open the macro")
                .setSmallIcon(android.R.drawable.ic_menu_compass)
                .setContentIntent(pi)
                .setOngoing(true)
                .build();
    }

    private void addBubble() {
        bubble = new TextView(this);
        bubble.setText("FF");
        bubble.setTextColor(Color.parseColor("#06120a"));
        bubble.setTextSize(13);
        bubble.setTypeface(null, Typeface.BOLD);
        bubble.setGravity(Gravity.CENTER);

        GradientDrawable g = new GradientDrawable();
        g.setShape(GradientDrawable.OVAL);
        g.setColor(Color.parseColor("#22c55e"));
        g.setStroke(dp(2), Color.parseColor("#0b1015"));
        bubble.setBackground(g);

        bp = new WindowManager.LayoutParams(dp(46), dp(46), winType(),
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT);
        bp.gravity = Gravity.TOP | Gravity.START;
        bp.x = dp(12);
        bp.y = dp(180);
        wm.addView(bubble, bp);

        bubble.setOnTouchListener(new View.OnTouchListener() {
            float dx, dy; int sx, sy; boolean moved;
            @Override public boolean onTouch(View v, MotionEvent e) {
                switch (e.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN:
                        dx = e.getRawX(); dy = e.getRawY();
                        sx = bp.x; sy = bp.y; moved = false;
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        if (Math.abs(e.getRawX() - dx) > dp(5)
                                || Math.abs(e.getRawY() - dy) > dp(5)) moved = true;
                        bp.x = sx + (int) (e.getRawX() - dx);
                        bp.y = sy + (int) (e.getRawY() - dy);
                        wm.updateViewLayout(bubble, bp);
                        return true;
                    case MotionEvent.ACTION_UP:
                        if (!moved) toggle();
                        return true;
                }
                return false;
            }
        });
    }

    private void toggle() { if (open) closePanel(); else openPanel(); }

    private void openPanel() {
        if (panel != null) return;

        panel = new FrameLayout(this);
        GradientDrawable g = new GradientDrawable();
        g.setCornerRadius(dp(14));
        g.setColor(Color.parseColor("#0b1015"));
        g.setStroke(dp(1), Color.parseColor("#1e2b36"));
        panel.setBackground(g);

        WebView web = new WebView(this);
        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setAllowFileAccess(true);
        web.setBackgroundColor(Color.TRANSPARENT);
        web.setVerticalScrollBarEnabled(false);
        web.addJavascriptInterface(new Bridge(), "Android");
        web.loadUrl("file:///android_asset/index.html");

        panel.addView(web, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, dp(560)));

        pp = new WindowManager.LayoutParams(dp(320), dp(564), winType(),
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                        | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT);
        pp.gravity = Gravity.TOP | Gravity.START;
        pp.x = dp(10);
        pp.y = dp(60);

        wm.addView(panel, pp);
        open = true;
    }

    private void closePanel() {
        if (panel != null) {
            try { wm.removeView(panel); } catch (Exception ignored) {}
            panel = null;
        }
        open = false;
    }

    public class Bridge {
        @JavascriptInterface public void drag(final int dx, final int dy) {
            ui.post(new Runnable() { @Override public void run() {
                if (pp == null || panel == null) return;
                pp.x += dx; pp.y += dy;
                wm.updateViewLayout(panel, pp);
            }});
        }
        @JavascriptInterface public void close() {
            ui.post(new Runnable() { @Override public void run() { closePanel(); }});
        }
    }

    @Override public void onDestroy() {
        super.onDestroy();
        closePanel();
        if (bubble != null) { try { wm.removeView(bubble); } catch (Exception ignored) {} }
    }
          }
