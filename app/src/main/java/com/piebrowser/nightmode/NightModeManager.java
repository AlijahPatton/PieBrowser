package com.piebrowser.nightmode;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.PixelFormat;
import android.os.Build;
import android.view.View;
import android.view.WindowManager;

/**
 * NightModeManager — applies a warm eye-care overlay to reduce blue light.
 *
 * Three modes:
 * - OFF         — no filter
 * - WARM        — light amber tint (reduces blue light ~30%)
 * - NIGHT       — stronger amber (reduces ~60%)
 * - DEEP_NIGHT  — very strong amber + dimming
 *
 * Also injects CSS to invert colors on dark-mode-unsupported websites.
 */
public class NightModeManager {

    private static final String PREFS = "night_mode_prefs";
    private static final String KEY_MODE = "night_mode";
    private static final String KEY_INTENSITY = "blue_light_intensity";

    public enum Mode { OFF, WARM, NIGHT, DEEP_NIGHT }

    private static View overlayView;

    /**
     * Apply or remove the eye-care overlay on an Activity.
     */
    public static void applyOverlay(Activity activity) {
        Mode mode = getMode(activity);
        removeOverlay(activity);

        if (mode == Mode.OFF) return;

        int color = getOverlayColor(mode);

        overlayView = new View(activity);
        overlayView.setBackgroundColor(color);

        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                : WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE |
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        );

        try {
            WindowManager wm = (WindowManager) activity.getSystemService(Context.WINDOW_SERVICE);
            if (wm != null) wm.addView(overlayView, params);
        } catch (Exception e) {
            // Permission not granted — gracefully ignore
        }
    }

    public static void removeOverlay(Activity activity) {
        if (overlayView != null) {
            try {
                WindowManager wm = (WindowManager) activity.getSystemService(Context.WINDOW_SERVICE);
                if (wm != null) wm.removeView(overlayView);
            } catch (Exception e) {
                // Already removed
            }
            overlayView = null;
        }
    }

    /**
     * JavaScript to inject into WebView for additional dark mode.
     * Inverts colors on white-background pages.
     */
    public static String getDarkModeJS() {
        return "javascript:(function(){" +
            "  var style = document.createElement('style');" +
            "  style.innerHTML = 'html { filter: invert(90%) hue-rotate(180deg) !important; }' +" +
            "                    'img, video, canvas, svg { filter: invert(100%) hue-rotate(180deg) !important; }';" +
            "  document.head.appendChild(style);" +
            "})();";
    }

    private static int getOverlayColor(Mode mode) {
        switch (mode) {
            case WARM:       return 0x20FF8C00; // 12% opacity amber
            case NIGHT:      return 0x40FF6600; // 25% opacity amber
            case DEEP_NIGHT: return 0x65FF4500; // 40% opacity deep amber
            default:         return 0x00000000;
        }
    }

    public static void setMode(Context ctx, Mode mode) {
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
           .edit().putString(KEY_MODE, mode.name()).apply();
    }

    public static Mode getMode(Context ctx) {
        String name = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getString(KEY_MODE, Mode.OFF.name());
        try { return Mode.valueOf(name); }
        catch (Exception e) { return Mode.OFF; }
    }

    public static String getModeDisplayName(Mode mode) {
        switch (mode) {
            case OFF:        return "Off";
            case WARM:       return "Warm (light blue light filter)";
            case NIGHT:      return "Night (strong blue light filter)";
            case DEEP_NIGHT: return "Deep Night (maximum filter)";
            default:         return mode.name();
        }
    }
}
