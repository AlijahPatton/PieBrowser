package com.piebrowser.browser;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * BrowserSettings — centralized settings for PieBrowser.
 *
 * All settings are stored in SharedPreferences.
 * Access any setting via static methods.
 */
public class BrowserSettings {

    private static final String PREFS = "browser_settings";

    // Keys
    public static final String KEY_HOME_PAGE         = "home_page";
    public static final String KEY_SEARCH_ENGINE     = "search_engine";
    public static final String KEY_JAVASCRIPT        = "javascript_enabled";
    public static final String KEY_ADBLOCK           = "adblock_enabled";
    public static final String KEY_DARK_MODE         = "dark_mode";
    public static final String KEY_BLOCK_COOKIES     = "block_third_party_cookies";
    public static final String KEY_SAVE_HISTORY      = "save_history";
    public static final String KEY_DO_NOT_TRACK      = "do_not_track";
    public static final String KEY_LOAD_IMAGES       = "load_images";
    public static final String KEY_TEXT_SIZE         = "text_size";
    public static final String KEY_GESTURE_BACK      = "gesture_back_forward";
    public static final String KEY_EXTENSIONS_ENABLED = "extensions_enabled";
    public static final String KEY_DEFAULT_DOWNLOAD_PATH = "download_path";

    // Search engine URLs
    public static final String SEARCH_GOOGLE    = "https://www.google.com/search?q=";
    public static final String SEARCH_DUCKDUCKGO= "https://duckduckgo.com/?q=";
    public static final String SEARCH_BING      = "https://www.bing.com/search?q=";
    public static final String SEARCH_BRAVE     = "https://search.brave.com/search?q=";
    public static final String SEARCH_STARTPAGE = "https://www.startpage.com/search?q=";

    // ── Getters ───────────────────────────────────────────────────────────────

    public static String getHomePage(Context ctx) {
        return prefs(ctx).getString(KEY_HOME_PAGE, "https://start.duckduckgo.com");
    }

    public static String getSearchEngine(Context ctx) {
        return prefs(ctx).getString(KEY_SEARCH_ENGINE, SEARCH_DUCKDUCKGO);
    }

    public static boolean isJavaScriptEnabled(Context ctx) {
        return prefs(ctx).getBoolean(KEY_JAVASCRIPT, true);
    }

    public static boolean isAdBlockEnabled(Context ctx) {
        return prefs(ctx).getBoolean(KEY_ADBLOCK, true);
    }

    public static boolean isDarkMode(Context ctx) {
        return prefs(ctx).getBoolean(KEY_DARK_MODE, false);
    }

    public static boolean isBlockThirdPartyCookies(Context ctx) {
        return prefs(ctx).getBoolean(KEY_BLOCK_COOKIES, false);
    }

    public static boolean isSaveHistoryEnabled(Context ctx) {
        return prefs(ctx).getBoolean(KEY_SAVE_HISTORY, true);
    }

    public static boolean isDoNotTrackEnabled(Context ctx) {
        return prefs(ctx).getBoolean(KEY_DO_NOT_TRACK, false);
    }

    public static boolean isLoadImagesEnabled(Context ctx) {
        return prefs(ctx).getBoolean(KEY_LOAD_IMAGES, true);
    }

    public static int getTextSize(Context ctx) {
        return prefs(ctx).getInt(KEY_TEXT_SIZE, 100);  // 100 = default (100%)
    }

    public static boolean isGestureNavigationEnabled(Context ctx) {
        return prefs(ctx).getBoolean(KEY_GESTURE_BACK, true);
    }

    public static boolean areExtensionsEnabled(Context ctx) {
        return prefs(ctx).getBoolean(KEY_EXTENSIONS_ENABLED, false);
    }

    // ── Setters ───────────────────────────────────────────────────────────────

    public static void setHomePage(Context ctx, String url) {
        prefs(ctx).edit().putString(KEY_HOME_PAGE, url).apply();
    }

    public static void setSearchEngine(Context ctx, String url) {
        prefs(ctx).edit().putString(KEY_SEARCH_ENGINE, url).apply();
    }

    public static void setJavaScriptEnabled(Context ctx, boolean enabled) {
        prefs(ctx).edit().putBoolean(KEY_JAVASCRIPT, enabled).apply();
    }

    public static void setAdBlockEnabled(Context ctx, boolean enabled) {
        prefs(ctx).edit().putBoolean(KEY_ADBLOCK, enabled).apply();
    }

    public static void setDarkMode(Context ctx, boolean enabled) {
        prefs(ctx).edit().putBoolean(KEY_DARK_MODE, enabled).apply();
    }

    public static void setTextSize(Context ctx, int size) {
        prefs(ctx).edit().putInt(KEY_TEXT_SIZE, size).apply();
    }

    public static void setDoNotTrack(Context ctx, boolean enabled) {
        prefs(ctx).edit().putBoolean(KEY_DO_NOT_TRACK, enabled).apply();
    }

    // ── Reset ─────────────────────────────────────────────────────────────────

    public static void resetToDefaults(Context ctx) {
        prefs(ctx).edit().clear().apply();
    }

    // ── Private helper ────────────────────────────────────────────────────────

    private static SharedPreferences prefs(Context ctx) {
        return ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }
}
