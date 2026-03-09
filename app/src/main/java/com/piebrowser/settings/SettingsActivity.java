package com.piebrowser.settings;

import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.SeekBarPreference;
import androidx.preference.SwitchPreferenceCompat;

import com.piebrowser.R;
import com.piebrowser.adblock.AdBlockEngine;
import com.piebrowser.browser.BrowserSettings;
import com.piebrowser.ui.theme.ThemeManager;

/**
 * SettingsActivity — all user preferences in one place.
 *
 * Sections:
 * 1. General   — home page, search engine
 * 2. Privacy   — ad block, JavaScript, cookies, DNT
 * 3. Appearance — theme picker, text size, dark web pages
 * 4. Gestures  — enable/disable swipe navigation
 * 5. Downloads — default save location
 * 6. Extensions — toggle extension support
 * 7. About     — version, licenses, reset
 */
public class SettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeManager.applyTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.settings);
        }

        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.settings_container, new SettingsFragment())
                .commit();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // ── Settings Fragment ────────────────────────────────────────────────────

    public static class SettingsFragment extends PreferenceFragmentCompat {

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.preferences, rootKey);

            setupSearchEnginePref();
            setupThemePref();
            setupAdBlockPref();
            setupTextSizePref();
            setupClearDataPref();
            setupResetPref();
        }

        private void setupSearchEnginePref() {
            ListPreference searchPref = findPreference("search_engine_pref");
            if (searchPref == null) return;
            searchPref.setOnPreferenceChangeListener((pref, newValue) -> {
                BrowserSettings.setSearchEngine(requireContext(), (String) newValue);
                return true;
            });
        }

        private void setupThemePref() {
            ListPreference themePref = findPreference("theme_pref");
            if (themePref == null) return;
            themePref.setOnPreferenceChangeListener((pref, newValue) -> {
                ThemeManager.Theme theme = ThemeManager.Theme.valueOf((String) newValue);
                ThemeManager.saveTheme(requireContext(), theme);
                requireActivity().recreate(); // Apply immediately
                return true;
            });
        }

        private void setupAdBlockPref() {
            SwitchPreferenceCompat adBlockPref = findPreference("adblock_enabled");
            if (adBlockPref == null) return;
            // Show rule count in summary
            int ruleCount = AdBlockEngine.getInstance(requireContext()).getRuleCount();
            adBlockPref.setSummary("Block ads & trackers (" + ruleCount + " rules)");
        }

        private void setupTextSizePref() {
            SeekBarPreference textSizePref = findPreference("text_size_pref");
            if (textSizePref == null) return;
            textSizePref.setOnPreferenceChangeListener((pref, newValue) -> {
                BrowserSettings.setTextSize(requireContext(), (int) newValue);
                return true;
            });
        }

        private void setupClearDataPref() {
            Preference clearPref = findPreference("clear_data");
            if (clearPref == null) return;
            clearPref.setOnPreferenceClickListener(pref -> {
                android.webkit.WebStorage.getInstance().deleteAllData();
                android.webkit.CookieManager.getInstance().removeAllCookies(null);
                Toast.makeText(requireContext(), "Browsing data cleared",
                        Toast.LENGTH_SHORT).show();
                return true;
            });
        }

        private void setupResetPref() {
            Preference resetPref = findPreference("reset_settings");
            if (resetPref == null) return;
            resetPref.setOnPreferenceClickListener(pref -> {
                BrowserSettings.resetToDefaults(requireContext());
                Toast.makeText(requireContext(), "Settings reset to defaults",
                        Toast.LENGTH_SHORT).show();
                requireActivity().recreate();
                return true;
            });
        }
    }
}
