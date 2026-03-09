package com.piebrowser.floating;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.IBinder;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageButton;

import androidx.annotation.Nullable;

import com.piebrowser.R;

/**
 * FloatingBubbleService — a mini browser in a floating draggable bubble.
 *
 * Shows a floating icon on screen (like a chat bubble).
 * Tap it to expand a mini WebView overlay.
 * Drag to reposition anywhere.
 * Works over other apps (requires SYSTEM_ALERT_WINDOW permission).
 *
 * Use cases:
 * - Browse while watching a video
 * - Quick lookup without leaving your current app
 * - Floating calculator / translator
 */
public class FloatingBubbleService extends Service {

    private WindowManager windowManager;
    private View bubbleView;
    private View expandedView;
    private boolean isExpanded = false;

    @Override
    public void onCreate() {
        super.onCreate();
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        createBubble();
    }

    private void createBubble() {
        LayoutInflater inflater = LayoutInflater.from(this);

        // ── Floating bubble icon ──────────────────────────────────────────────
        bubbleView = new View(this);
        bubbleView.setBackgroundResource(R.drawable.bubble_background);

        WindowManager.LayoutParams bubbleParams = new WindowManager.LayoutParams(
            80, 80,
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                : WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        );
        bubbleParams.gravity = Gravity.TOP | Gravity.START;
        bubbleParams.x = 30;
        bubbleParams.y = 300;

        // Make bubble draggable
        bubbleView.setOnTouchListener(new DragTouchListener(bubbleParams));
        bubbleView.setOnClickListener(v -> toggleExpanded());

        windowManager.addView(bubbleView, bubbleParams);

        // ── Expanded mini browser ─────────────────────────────────────────────
        expandedView = createExpandedView();
        expandedView.setVisibility(View.GONE);

        WindowManager.LayoutParams expandedParams = new WindowManager.LayoutParams(
            (int)(getResources().getDisplayMetrics().widthPixels * 0.9f),
            (int)(getResources().getDisplayMetrics().heightPixels * 0.6f),
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                : WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        );
        expandedParams.gravity = Gravity.CENTER;

        windowManager.addView(expandedView, expandedParams);
    }

    private View createExpandedView() {
        // Create the mini browser panel programmatically
        android.widget.LinearLayout layout = new android.widget.LinearLayout(this);
        layout.setOrientation(android.widget.LinearLayout.VERTICAL);
        layout.setBackgroundColor(0xFF1E1E1E);

        // Toolbar
        android.widget.LinearLayout toolbar = new android.widget.LinearLayout(this);
        toolbar.setBackgroundColor(0xFF2C2C2C);
        toolbar.setPadding(12, 8, 12, 8);
        toolbar.setGravity(Gravity.CENTER_VERTICAL);

        android.widget.EditText urlBar = new android.widget.EditText(this);
        urlBar.setHint("Search or enter URL…");
        urlBar.setHintTextColor(0xFF888888);
        urlBar.setTextColor(0xFFFFFFFF);
        urlBar.setBackground(null);
        urlBar.setSingleLine(true);
        android.widget.LinearLayout.LayoutParams urlParams =
                new android.widget.LinearLayout.LayoutParams(0,
                        android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        urlBar.setLayoutParams(urlParams);

        ImageButton closeBtn = new ImageButton(this);
        closeBtn.setImageResource(android.R.drawable.ic_menu_close_clear_cancel);
        closeBtn.setBackgroundColor(0x00000000);
        closeBtn.setOnClickListener(v -> toggleExpanded());

        toolbar.addView(urlBar);
        toolbar.addView(closeBtn);

        // Mini WebView
        WebView miniWebView = new WebView(this);
        miniWebView.setLayoutParams(new android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT));
        miniWebView.getSettings().setJavaScriptEnabled(true);
        miniWebView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                urlBar.setText(url);
            }
        });
        miniWebView.loadUrl("https://duckduckgo.com");

        urlBar.setOnEditorActionListener((v, actionId, event) -> {
            String input = urlBar.getText().toString().trim();
            if (!input.isEmpty()) {
                String url = input.startsWith("http") ? input
                        : input.contains(".") && !input.contains(" ")
                        ? "https://" + input
                        : "https://duckduckgo.com/?q=" + android.net.Uri.encode(input);
                miniWebView.loadUrl(url);
            }
            return true;
        });

        layout.addView(toolbar);
        layout.addView(miniWebView);

        return layout;
    }

    private void toggleExpanded() {
        isExpanded = !isExpanded;
        expandedView.setVisibility(isExpanded ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (bubbleView != null) windowManager.removeView(bubbleView);
        if (expandedView != null) windowManager.removeView(expandedView);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) { return null; }

    public static void start(Context context) {
        context.startService(new Intent(context, FloatingBubbleService.class));
    }

    public static void stop(Context context) {
        context.stopService(new Intent(context, FloatingBubbleService.class));
    }

    // ── Drag listener ─────────────────────────────────────────────────────────

    private class DragTouchListener implements View.OnTouchListener {
        private final WindowManager.LayoutParams params;
        private int initialX, initialY;
        private float initialTouchX, initialTouchY;
        private boolean isDragging = false;

        DragTouchListener(WindowManager.LayoutParams params) {
            this.params = params;
        }

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    initialX = params.x;
                    initialY = params.y;
                    initialTouchX = event.getRawX();
                    initialTouchY = event.getRawY();
                    isDragging = false;
                    return true;
                case MotionEvent.ACTION_MOVE:
                    float dx = event.getRawX() - initialTouchX;
                    float dy = event.getRawY() - initialTouchY;
                    if (Math.abs(dx) > 5 || Math.abs(dy) > 5) isDragging = true;
                    params.x = initialX + (int) dx;
                    params.y = initialY + (int) dy;
                    windowManager.updateViewLayout(bubbleView, params);
                    return true;
                case MotionEvent.ACTION_UP:
                    if (!isDragging) v.performClick();
                    return true;
            }
            return false;
        }
    }
}
