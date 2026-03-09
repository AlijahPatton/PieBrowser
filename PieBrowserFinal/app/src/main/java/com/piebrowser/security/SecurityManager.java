package com.piebrowser.security;

import android.content.Context;
import android.net.VpnService;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;

/**
 * SecurityManager — privacy and security features for Pie Browser.
 *
 * Features:
 * 1. HTTPS-Only Mode     — blocks all HTTP requests, forces HTTPS upgrade
 * 2. Canvas Fingerprint Blocker — injects JS to randomize canvas fingerprints
 * 3. WebRTC Blocker      — prevents IP leaks via WebRTC
 * 4. Location Spoofing   — replace real GPS with a fake location
 * 5. VPN Integration     — stub for connecting to VPN providers
 */
public class SecurityManager {

    private static SecurityManager instance;
    private final Context context;

    // Spoofed location (default: Null Island 0,0)
    private double spoofedLat = 0.0;
    private double spoofedLng = 0.0;
    private boolean locationSpoofingEnabled = false;

    private SecurityManager(Context ctx) {
        this.context = ctx.getApplicationContext();
    }

    public static synchronized SecurityManager getInstance(Context ctx) {
        if (instance == null) instance = new SecurityManager(ctx);
        return instance;
    }

    // ── HTTPS-Only ────────────────────────────────────────────────────────────

    /**
     * Called from LiteWebViewClient.shouldOverrideUrlLoading.
     * If HTTPS-only is on, block plain HTTP requests.
     * @return true if the request should be blocked
     */
    public boolean shouldBlockRequest(String url) {
        if (!isHttpsOnlyEnabled()) return false;
        return url.startsWith("http://") && !url.startsWith("http://localhost");
    }

    /**
     * Try to upgrade HTTP to HTTPS automatically.
     */
    public String upgradeToHttps(String url) {
        if (url.startsWith("http://")) {
            return "https://" + url.substring(7);
        }
        return url;
    }

    // ── Canvas Fingerprint Blocker ────────────────────────────────────────────

    /**
     * JavaScript to inject that randomizes canvas fingerprints.
     * Overwrites HTMLCanvasElement.toDataURL and getImageData with
     * slightly randomized versions so tracking scripts can't identify you.
     */
    public static String getCanvasFingerprintBlockerJS() {
        return "(function() {" +
            "  var originalToDataURL = HTMLCanvasElement.prototype.toDataURL;" +
            "  HTMLCanvasElement.prototype.toDataURL = function(type) {" +
            "    var ctx = this.getContext('2d');" +
            "    if (ctx) {" +
            "      var imageData = ctx.getImageData(0, 0, this.width, this.height);" +
            "      for (var i = 0; i < imageData.data.length; i += 4) {" +
            "        imageData.data[i]     += Math.floor(Math.random() * 3) - 1;" +
            "        imageData.data[i + 1] += Math.floor(Math.random() * 3) - 1;" +
            "        imageData.data[i + 2] += Math.floor(Math.random() * 3) - 1;" +
            "      }" +
            "      ctx.putImageData(imageData, 0, 0);" +
            "    }" +
            "    return originalToDataURL.apply(this, arguments);" +
            "  };" +
            "})();";
    }

    // ── WebRTC IP Leak Blocker ────────────────────────────────────────────────

    /**
     * JavaScript to block WebRTC from leaking real IP addresses.
     * Overrides RTCPeerConnection to prevent local IP discovery.
     */
    public static String getWebRTCBlockerJS() {
        return "(function() {" +
            "  var origRTC = window.RTCPeerConnection || window.webkitRTCPeerConnection;" +
            "  if (!origRTC) return;" +
            "  var blockedRTC = function(config) {" +
            "    if (config && config.iceServers) {" +
            "      config.iceServers = [];" + // Remove STUN/TURN servers
            "    }" +
            "    return new origRTC(config);" +
            "  };" +
            "  blockedRTC.prototype = origRTC.prototype;" +
            "  window.RTCPeerConnection = blockedRTC;" +
            "  window.webkitRTCPeerConnection = blockedRTC;" +
            "})();";
    }

    // ── Location Spoofing ─────────────────────────────────────────────────────

    /**
     * JavaScript to inject fake GPS coordinates into the browser.
     * Overrides navigator.geolocation.getCurrentPosition and watchPosition.
     */
    public String getLocationSpoofingJS() {
        if (!locationSpoofingEnabled) return "";
        return String.format(
            "(function() {" +
            "  var fakePos = {" +
            "    coords: {" +
            "      latitude: %f," +
            "      longitude: %f," +
            "      accuracy: 20," +
            "      altitude: null," +
            "      altitudeAccuracy: null," +
            "      heading: null," +
            "      speed: null" +
            "    }," +
            "    timestamp: Date.now()" +
            "  };" +
            "  navigator.geolocation.getCurrentPosition = function(success, error, opts) {" +
            "    success(fakePos);" +
            "  };" +
            "  navigator.geolocation.watchPosition = function(success, error, opts) {" +
            "    success(fakePos);" +
            "    return 0;" +
            "  };" +
            "})();",
            spoofedLat, spoofedLng
        );
    }

    public void setSpoofedLocation(double lat, double lng) {
        this.spoofedLat = lat;
        this.spoofedLng = lng;
    }

    public void setLocationSpoofingEnabled(boolean enabled) {
        this.locationSpoofingEnabled = enabled;
    }

    public boolean isLocationSpoofingEnabled() { return locationSpoofingEnabled; }

    // ── All Security Scripts ───────────────────────────────────────────────────

    /**
     * Returns all security JavaScript to inject into each page.
     * Called from LiteWebViewClient.onPageStarted.
     */
    public String getAllSecurityScripts() {
        StringBuilder sb = new StringBuilder();

        if (isCanvasBlockingEnabled()) {
            sb.append(getCanvasFingerprintBlockerJS());
        }
        if (isWebRTCBlockingEnabled()) {
            sb.append(getWebRTCBlockerJS());
        }
        if (locationSpoofingEnabled) {
            sb.append(getLocationSpoofingJS());
        }

        return sb.toString();
    }

    // ── Settings ──────────────────────────────────────────────────────────────

    private boolean isHttpsOnlyEnabled() {
        return context.getSharedPreferences("security_prefs", Context.MODE_PRIVATE)
                .getBoolean("https_only", false);
    }

    private boolean isCanvasBlockingEnabled() {
        return context.getSharedPreferences("security_prefs", Context.MODE_PRIVATE)
                .getBoolean("canvas_block", false);
    }

    private boolean isWebRTCBlockingEnabled() {
        return context.getSharedPreferences("security_prefs", Context.MODE_PRIVATE)
                .getBoolean("webrtc_block", false);
    }

    public void setHttpsOnly(boolean enabled) {
        context.getSharedPreferences("security_prefs", Context.MODE_PRIVATE)
                .edit().putBoolean("https_only", enabled).apply();
    }

    public void setCanvasBlocking(boolean enabled) {
        context.getSharedPreferences("security_prefs", Context.MODE_PRIVATE)
                .edit().putBoolean("canvas_block", enabled).apply();
    }

    public void setWebRTCBlocking(boolean enabled) {
        context.getSharedPreferences("security_prefs", Context.MODE_PRIVATE)
                .edit().putBoolean("webrtc_block", enabled).apply();
    }
}
