package com.you.ffpanel;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public class MainActivity extends Activity {

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER);
        root.setBackgroundColor(Color.parseColor("#0b1015"));

        TextView t = new TextView(this);
        t.setText("FF PANEL");
        t.setTextColor(Color.parseColor("#22c55e"));
        t.setTextSize(26);
        t.setGravity(Gravity.CENTER);
        root.addView(t);

        TextView hint = new TextView(this);
        hint.setText("OB56 · headshot only 75%\n\n1. Allow draw over other apps\n2. Open Free Fire\n3. Tap the green FF bubble\n4. Drag the panel header to move it");
        hint.setTextColor(Color.parseColor("#7d8b99"));
        hint.setTextSize(13);
        hint.setGravity(Gravity.CENTER);
        hint.setPadding(0, 40, 0, 40);
        root.addView(hint);

        Button start = new Button(this);
        start.setText("START PANEL");
        start.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { ensureAndStart(); }
        });
        root.addView(start);

        setContentView(root);

        if (Build.VERSION.SDK_INT >= 33) {
            requestPermissions(new String[]{"android.permission.POST_NOTIFICATIONS"}, 7);
        }
        ensureAndStart();
    }

    private void ensureAndStart() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            startActivityForResult(new Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName())), 1);
            return;
        }
        startService(new Intent(this, OverlayService.class));
    }

    @Override protected void onActivityResult(int rq, int rs, Intent d) {
        super.onActivityResult(rq, rs, d);
        ensureAndStart();
    }
          }
