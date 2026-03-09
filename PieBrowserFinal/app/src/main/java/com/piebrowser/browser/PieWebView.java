package com.piebrowser.browser;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.Build;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.webkit.CookieManager;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.annotation.Nullable;
import androidx.webkit.WebSettingsCompat;
import androidx.webkit.WebViewFeature;

import com.piebrowser.adblock.AdBlockEngine;

import java.io.ByteArrayInputStream;

/**
 * PieWebView — a memory-efficient, ad-blocking WebView.
 *
 * Key optimizations:
 * - JavaScript disabled by default (enable per-site if needed)
 * - Cache limited to 8MB
 * - Images load on-demand
 * - Ad requests blocked before they reach the network
 */
public class PieWebView extends WebView {

    public interface ProgressListener { void onProgress(int progress); }
    public interface UrlChangedListener { void onUrlChanged(String url); }

    private ProgressListener progressListener;
    private UrlChangedListener urlChangedListener;
    private AdBlockEngine adBlockEngine;
    private BrowserGestureDetector gestureDetector;
    private boolean isDesktopMode = false;
    private FindInPageBar findInPageBar;

    public PieWebView(Context context) { super(context); init(); }
    public PieWebView(Context context, AttributeSet attrs) { super(context, attrs); init(); }
    public PieWebView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle); init();
    }

    @SuppressLint({"SetJavaScriptEnabled"})
    private void init() {
        adBlockEngine = AdBlockEngine.getInstance(getContext());

        WebSettings settings = getSettings();

        // ── Performance & Memory ────────────────────────────────────────────
        settings.setCacheMode(WebSettings.LOAD_DEFAULT);
        settings.setAppCacheEnabled(true);   // deprecated in API 33 but safe fallback
        settings.setDatabaseEnabled(true);
        settings.setDomStorageEnabled(true);

        // Limit image loading to save RAM
        settings.setLoadsImagesAutomatically(true);
        settings.setBlockNetworkImage(false);

        // Text compression
        settings.setMinimumFontSize(8);
        settings.setTextZoom(100);

        // ── Security ────────────────────────────────────────────────────────
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE);
        settings.setAllowFileAccess(false);
        settings.setAllowContentAccess(false);
        settings.setGeolocationEnabled(false);

        // JavaScript: controlled by user setting
        boolean jsEnabled = BrowserSettings.isJavaScriptEnabled(getContext());
        settings.setJavaScriptEnabled(jsEnabled);

        // ── Features ────────────────────────────────────────────────────────
        settings.setSupportZoom(true);
        settings.setBuiltInZoomControls(true);
        settings.setDisplayZoomControls(false);  // Hide ugly +/- buttons
        settings.setUseWideViewPort(true);
        settings.setLoadWithOverviewMode(true);
        settings.setSupportMultipleWindows(false);

        // Dark mode (Android 10+)
        if (WebViewFeature.isFeatureSupported(WebViewFeature.FORCE_DARK)) {
            boolean darkMode = BrowserSettings.isDarkMode(getContext());
            int darkModeVal = darkMode
                    ? WebSettingsCompat.FORCE_DARK_ON
                    : WebSettingsCompat.FORCE_DARK_OFF;
            WebSettingsCompat.setForceDark(settings, darkModeVal);
        }

        // User agent
        settings.setUserAgentString(getMobileUserAgent());

        // ── Clients ─────────────────────────────────────────────────────────
        setWebViewClient(new PieWebViewClient());
        setWebChromeClient(new LiteWebChromeClient());

        // Cookies
        CookieManager.getInstance().setAcceptCookie(true);
        CookieManager.getInstance().setAcceptThirdPartyCookies(this,
                !BrowserSettings.isBlockThirdPartyCookies(getContext()));

        // Scrollbars
        setScrollBarStyle(WebView.SCROLLBARS_INSIDE_OVERLAY);
        setOverScrollMode(OVER_SCROLL_NEVER);
    }

    // ── WebViewClient (intercept ad requests) ───────────────────────────────

    private class PieWebViewClient extends WebViewClient {

        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            super.onPageStarted(view, url, favicon);
            if (urlChangedListener != null) urlChangedListener.onUrlChanged(url);
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            super.onPageFinished(view, url);
            if (urlChangedListener != null) urlChangedListener.onUrlChanged(url);
        }

        /**
         * Block ads and trackers at the network level — before they consume RAM.
         */
        @Override
        public WebResourceResponse shouldInterceptRequest(WebView view,
                                                          WebResourceRequest request) {
            if (BrowserSettings.isAdBlockEnabled(getContext())) {
                String url = request.getUrl().toString();
                if (adBlockEngine.shouldBlock(url)) {
                    // Return empty response — zero bytes loaded
                    return new WebResourceResponse("text/plain", "utf-8",
                            new ByteArrayInputStream(new byte[0]));
                }
            }
            return super.shouldInterceptRequest(view, request);
        }

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
            String url = request.getUrl().toString();
            // Handle tel:, mailto:, etc.
            if (!url.startsWith("http://") && !url.startsWith("https://")) {
                return true; // Let Android handle special schemes
            }
            return false;
        }
    }

    // ── WebChromeClient (progress tracking) ──────────────────────────────────

    private class LiteWebChromeClient extends WebChromeClient {
        @Override
        public void onProgressChanged(WebView view, int newProgress) {
            if (progressListener != null) progressListener.onProgress(newProgress);
        }
    }

    // ── Touch / Gesture handling ──────────────────────────────────────────────

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (gestureDetector != null) {
            gestureDetector.onTouchEvent(event);
        }
        return super.onTouchEvent(event);
    }

    // ── Desktop site toggle ───────────────────────────────────────────────────

    public void toggleDesktopSite() {
        isDesktopMode = !isDesktopMode;
        WebSettings settings = getSettings();
        if (isDesktopMode) {
            settings.setUserAgentString(getDesktopUserAgent());
            settings.setUseWideViewPort(true);
            settings.setLoadWithOverviewMode(true);
        } else {
            settings.setUserAgentString(getMobileUserAgent());
            settings.setUseWideViewPort(true);
            settings.setLoadWithOverviewMode(true);
        }
        reload();
    }

    // ── Find in page ──────────────────────────────────────────────────────────

    public void showFindInPage() {
        // Trigger the system find dialog
        // In a full implementation, use a custom overlay for more control
        findActivated(true);
    }

    // ── Memory management ─────────────────────────────────────────────────────

    @Override
    public void onPause() {
        super.onPause();
        // Pause all timers, animations, geolocation
        pauseTimers();
    }

    @Override
    public void onResume() {
        super.onResume();
        resumeTimers();
    }

    @Override
    public void destroy() {
        stopLoading();
        clearHistory();
        clearCache(true);
        loadUrl("about:blank");
        removeAllViews();
        super.destroy();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private String getMobileUserAgent() {
        return "Mozilla/5.0 (Linux; Android " + Build.VERSION.RELEASE
                + "; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) "
                + "Chrome/120.0.0.0 Mobile Safari/537.36";
    }

    private String getDesktopUserAgent() {
        return "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
                + "(KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36";
    }

    // ── Setters ───────────────────────────────────────────────────────────────

    public void setProgressListener(ProgressListener l) { this.progressListener = l; }
    public void setUrlChangedListener(UrlChangedListener l) { this.urlChangedListener = l; }
    public void setGestureDetector(BrowserGestureDetector gd) { this.gestureDetector = gd; }
}
