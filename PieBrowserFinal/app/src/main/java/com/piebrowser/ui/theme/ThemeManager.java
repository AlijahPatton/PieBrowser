package com.piebrowser.ui.theme;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;

import com.piebrowser.R;

/**
 * ThemeManager controls the visual theme of PieBrowser.
 *
 * Available themes:
 * - LIGHT       — Clean white interface
 * - DARK        — True dark theme (OLED-friendly, saves battery)
 * - AMOLED      — Pure black (#000000) for OLED screens
 * - MIDNIGHT_BLUE — Dark blue accent theme
 * - FOREST      — Green nature theme
 * - SUNSET      — Warm orange/red theme
 * - SYSTEM      — Follows Android system light/dark setting
 */
public class ThemeManager {

    private static final String PREFS = "theme_prefs";
    private static final String KEY_THEME = "selected_theme";

    public enum Theme {
        SYSTEM, LIGHT, DARK, AMOLED, MIDNIGHT_BLUE, FOREST, SUNSET
    }

    public static void applyTheme(Activity activity) {
        Theme theme = getSavedTheme(activity);
        int styleRes = getStyleRes(activity, theme);
        activity.setTheme(styleRes);
    }

    public static int getStyleRes(Context context, Theme theme) {
        switch (theme) {
            case LIGHT:         return R.style.Theme_PieBrowser_Light;
            case DARK:          return R.style.Theme_PieBrowser_Dark;
            case AMOLED:        return R.style.Theme_PieBrowser_Amoled;
            case MIDNIGHT_BLUE: return R.style.Theme_PieBrowser_MidnightBlue;
            case FOREST:        return R.style.Theme_PieBrowser_Forest;
            case SUNSET:        return R.style.Theme_PieBrowser_Sunset;
            case SYSTEM:
            default:
                return isSystemDark(context)
                        ? R.style.Theme_PieBrowser_Dark
                        : R.style.Theme_PieBrowser_Light;
        }
    }

    public static void saveTheme(Context context, Theme theme) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_THEME, theme.name())
                .apply();
    }

    public static Theme getSavedTheme(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String name = prefs.getString(KEY_THEME, Theme.SYSTEM.name());
        try {
            return Theme.valueOf(name);
        } catch (IllegalArgumentException e) {
            return Theme.SYSTEM;
        }
    }

    private static boolean isSystemDark(Context context) {
        int nightModeFlags = context.getResources().getConfiguration().uiMode
                & android.content.res.Configuration.UI_MODE_NIGHT_MASK;
        return nightModeFlags == android.content.res.Configuration.UI_MODE_NIGHT_YES;
    }

    /**
     * Returns display name for each theme (for settings UI).
     */
    public static String getThemeDisplayName(Theme theme) {
        switch (theme) {
            case SYSTEM:        return "Follow System";
            case LIGHT:         return "Light";
            case DARK:          return "Dark";
            case AMOLED:        return "AMOLED Black";
            case MIDNIGHT_BLUE: return "Midnight Blue";
            case FOREST:        return "Forest";
            case SUNSET:        return "Sunset";
            default:            return theme.name();
        }
    }
}
