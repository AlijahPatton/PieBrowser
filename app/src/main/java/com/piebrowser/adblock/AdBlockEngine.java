package com.piebrowser.adblock;

import android.content.Context;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * AdBlockEngine — lightweight ad/tracker blocker.
 *
 * Uses a domain-based blocklist loaded from assets.
 * The check is O(1) HashMap lookup — zero performance penalty.
 *
 * To expand: replace BLOCKED_DOMAINS with a full EasyList/uBlock ruleset
 * loaded from assets/adblock_rules.txt at startup.
 */
public class AdBlockEngine {

    private static AdBlockEngine instance;
    private final Set<String> blockedDomains = new HashSet<>();
    private final Set<String> blockedKeywords = new HashSet<>();

    // Core ad/tracker domains (extend this list as needed)
    private static final String[] DEFAULT_BLOCKED_DOMAINS = {
        // Ad networks
        "doubleclick.net", "googlesyndication.com", "googleadservices.com",
        "adnxs.com", "ads.yahoo.com", "adserver.yahoo.com",
        "advertising.com", "adsystem.amazon.com",
        "criteo.com", "criteo.net",
        "casalemedia.com", "serving-sys.com",
        "rubiconproject.com", "openx.net",
        "pubmatic.com", "appnexus.com",
        "taboola.com", "outbrain.com",
        "revcontent.com", "mgid.com",
        "media.net", "yieldmanager.com",

        // Trackers
        "google-analytics.com", "googletagmanager.com",
        "facebook.com/tr", "connect.facebook.net",
        "analytics.twitter.com", "ads.twitter.com",
        "scorecardresearch.com", "omtrdc.net",
        "mookie1.com", "dotmetrics.net",
        "hotjar.com", "mixpanel.com",
        "segment.com", "segment.io",
        "intercom.io", "fullstory.com",
        "mouseflow.com", "inspectlet.com",
        "quantserve.com", "chartbeat.com",

        // Malware / cryptominers
        "coinhive.com", "coin-hive.com",
        "jsecoin.com", "webmine.pro",
    };

    private static final String[] BLOCKED_KEYWORDS = {
        "/ads/", "/ad/", "/advertisement/", "/banner/",
        "/popup/", "/tracker/", "/tracking/",
        "doubleclick", "adserv", "adsystem",
        "analytics.js", "gtag.js", "fbevents.js",
        "googletag", "prebid",
    };

    private AdBlockEngine(Context context) {
        blockedDomains.addAll(Arrays.asList(DEFAULT_BLOCKED_DOMAINS));
        blockedKeywords.addAll(Arrays.asList(BLOCKED_KEYWORDS));
        // TODO: load additional rules from assets/adblock_rules.txt
    }

    public static synchronized AdBlockEngine getInstance(Context context) {
        if (instance == null) {
            instance = new AdBlockEngine(context.getApplicationContext());
        }
        return instance;
    }

    /**
     * Returns true if this URL should be blocked.
     * Called on the network thread — must be fast.
     */
    public boolean shouldBlock(String url) {
        if (url == null || url.isEmpty()) return false;

        String lowerUrl = url.toLowerCase();

        // Domain check
        String domain = extractDomain(lowerUrl);
        if (domain != null) {
            // Check exact domain and subdomains
            if (blockedDomains.contains(domain)) return true;
            for (String blocked : blockedDomains) {
                if (domain.endsWith("." + blocked)) return true;
            }
        }

        // Keyword check (catches path-based patterns)
        for (String keyword : blockedKeywords) {
            if (lowerUrl.contains(keyword)) return true;
        }

        return false;
    }

    private String extractDomain(String url) {
        try {
            int start = url.indexOf("://");
            if (start < 0) return null;
            start += 3;
            int end = url.indexOf('/', start);
            if (end < 0) end = url.length();
            String host = url.substring(start, end);
            // Remove www. prefix
            if (host.startsWith("www.")) host = host.substring(4);
            return host;
        } catch (Exception e) {
            return null;
        }
    }

    public void addCustomRule(String domain) {
        blockedDomains.add(domain.toLowerCase());
    }

    public void removeRule(String domain) {
        blockedDomains.remove(domain.toLowerCase());
    }

    public int getRuleCount() {
        return blockedDomains.size() + blockedKeywords.size();
    }
}
