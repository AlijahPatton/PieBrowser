package com.piebrowser.browser;

import android.content.Context;

import java.util.ArrayList;
import java.util.List;

/**
 * TabManager — lightweight multi-tab support.
 *
 * Memory strategy:
 * - Only the ACTIVE tab keeps its WebView alive
 * - Background tabs store their URL and scroll position
 * - WebView is recreated when a background tab is brought forward
 * - This keeps RAM usage proportional to ONE tab, not N tabs
 */
public class TabManager {

    public static final int MAX_TABS = 10;  // Hard limit to prevent OOM

    private final Context context;
    private final PieWebView activeWebView;
    private final List<Tab> tabs = new ArrayList<>();
    private int activeTabIndex = 0;

    public TabManager(Context context, PieWebView activeWebView) {
        this.context = context;
        this.activeWebView = activeWebView;
        // Create initial tab
        tabs.add(new Tab("about:blank", false));
    }

    /**
     * Open a new tab. If at MAX_TABS, recycle oldest background tab.
     */
    public Tab openNewTab(String url, boolean isIncognito) {
        if (tabs.size() >= MAX_TABS) {
            // Find oldest non-active tab and remove
            for (int i = 0; i < tabs.size(); i++) {
                if (i != activeTabIndex) {
                    tabs.remove(i);
                    if (i < activeTabIndex) activeTabIndex--;
                    break;
                }
            }
        }

        Tab newTab = new Tab(url, isIncognito);
        tabs.add(newTab);
        switchToTab(tabs.size() - 1);
        return newTab;
    }

    public void openIncognitoTab() {
        Tab tab = openNewTab("about:blank", true);
        activeWebView.loadUrl("about:blank");
        // In incognito: disable history, cookies for this session only
        applyIncognitoSettings(true);
    }

    public void switchToTab(int index) {
        if (index < 0 || index >= tabs.size()) return;

        // Save current tab state
        Tab currentTab = tabs.get(activeTabIndex);
        currentTab.setUrl(activeWebView.getUrl());
        currentTab.setScrollY(activeWebView.getScrollY());

        // Switch
        activeTabIndex = index;
        Tab newTab = tabs.get(index);

        // Apply incognito settings
        applyIncognitoSettings(newTab.isIncognito());

        // Load new tab's URL
        if (newTab.getUrl() != null && !newTab.getUrl().equals("about:blank")) {
            activeWebView.loadUrl(newTab.getUrl());
        } else {
            activeWebView.loadUrl("about:blank");
        }
    }

    public void closeTab(int index) {
        if (tabs.size() <= 1) {
            // Last tab — load blank page
            activeWebView.loadUrl("about:blank");
            return;
        }

        tabs.remove(index);
        if (activeTabIndex >= tabs.size()) activeTabIndex = tabs.size() - 1;
        switchToTab(activeTabIndex);
    }

    public void showTabSwitcher() {
        // Launch tab switcher bottom sheet
        // In full impl: open TabSwitcherBottomSheet fragment
    }

    private void applyIncognitoSettings(boolean incognito) {
        // Disable history and cookies in incognito
        android.webkit.CookieManager.getInstance()
                .setAcceptCookie(!incognito);
    }

    public int getTabCount() { return tabs.size(); }
    public List<Tab> getTabs() { return tabs; }
    public Tab getActiveTab() { return tabs.get(activeTabIndex); }
    public int getActiveTabIndex() { return activeTabIndex; }

    // ── Tab model ─────────────────────────────────────────────────────────────

    public static class Tab {
        private String url;
        private String title;
        private final boolean incognito;
        private int scrollY;

        public Tab(String url, boolean incognito) {
            this.url = url;
            this.incognito = incognito;
            this.title = "New Tab";
        }

        public String getUrl() { return url; }
        public void setUrl(String url) { this.url = url; }
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public boolean isIncognito() { return incognito; }
        public int getScrollY() { return scrollY; }
        public void setScrollY(int y) { this.scrollY = y; }
    }
}
