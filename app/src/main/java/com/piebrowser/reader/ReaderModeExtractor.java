package com.piebrowser.reader;

import android.content.Context;
import android.os.AsyncTask;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

/**
 * ReaderModeExtractor — strips a webpage down to just its article content.
 *
 * Uses Jsoup to:
 * 1. Parse the HTML
 * 2. Remove ads, navbars, sidebars, footers, scripts
 * 3. Extract the main article body
 * 4. Return clean HTML for the reader view
 */
public class ReaderModeExtractor {

    public interface Callback {
        void onSuccess(ReaderContent content);
        void onFailure(String error);
    }

    public static class ReaderContent {
        public final String title;
        public final String byline;
        public final String content;
        public final String siteName;
        public final int wordCount;

        public ReaderContent(String title, String byline,
                             String content, String siteName, int wordCount) {
            this.title = title;
            this.byline = byline;
            this.content = content;
            this.siteName = siteName;
            this.wordCount = wordCount;
        }

        public String getEstimatedReadTime() {
            int minutes = Math.max(1, wordCount / 200);
            return minutes + " min read";
        }
    }

    /**
     * Extract readable content from a URL asynchronously.
     */
    public static void extract(String url, Callback callback) {
        new AsyncTask<Void, Void, ReaderContent>() {
            private String error;

            @Override
            protected ReaderContent doInBackground(Void... voids) {
                try {
                    Document doc = Jsoup.connect(url)
                            .userAgent("Mozilla/5.0 (compatible; PieBrowser/1.0)")
                            .timeout(10000)
                            .get();

                    return parseDocument(doc);
                } catch (Exception e) {
                    error = e.getMessage();
                    return null;
                }
            }

            @Override
            protected void onPostExecute(ReaderContent result) {
                if (result != null) callback.onSuccess(result);
                else callback.onFailure(error != null ? error : "Failed to extract content");
            }
        }.execute();
    }

    private static ReaderContent parseDocument(Document doc) {
        // Remove noise elements
        doc.select("script, style, nav, header, footer, aside, " +
                   ".ad, .ads, .advertisement, .sidebar, .social, " +
                   ".comments, .comment, .related, .popup, " +
                   "[class*='share'], [class*='banner'], " +
                   "[id*='sidebar'], [id*='ad'], [id*='footer']").remove();

        // Extract metadata
        String title = extractTitle(doc);
        String byline = extractByline(doc);
        String siteName = extractSiteName(doc);

        // Find main content
        String content = extractMainContent(doc);
        int wordCount = countWords(content);

        return new ReaderContent(title, byline, content, siteName, wordCount);
    }

    private static String extractTitle(Document doc) {
        // Try OpenGraph title first
        Element ogTitle = doc.selectFirst("meta[property=og:title]");
        if (ogTitle != null) return ogTitle.attr("content");

        // Then <title>
        String title = doc.title();
        // Remove site name from title (e.g. "Article Name - Site Name")
        if (title.contains(" - ")) title = title.substring(0, title.lastIndexOf(" - ")).trim();
        if (title.contains(" | ")) title = title.substring(0, title.lastIndexOf(" | ")).trim();
        return title;
    }

    private static String extractByline(Document doc) {
        String[] authorSelectors = {
            "[rel=author]", ".author", ".byline", "[itemprop=author]",
            "meta[name=author]", ".writer", ".journalist"
        };
        for (String sel : authorSelectors) {
            Element el = doc.selectFirst(sel);
            if (el != null) {
                String text = el.tagName().equals("meta")
                        ? el.attr("content") : el.text();
                if (!text.isEmpty()) return text;
            }
        }
        return "";
    }

    private static String extractSiteName(Document doc) {
        Element og = doc.selectFirst("meta[property=og:site_name]");
        return og != null ? og.attr("content") : "";
    }

    private static String extractMainContent(Document doc) {
        // Priority order for article containers
        String[] contentSelectors = {
            "article", "[role=main]", "main",
            ".post-content", ".article-content", ".entry-content",
            ".story-body", ".article-body", ".content-body",
            "#content", "#main-content", ".main-content"
        };

        for (String sel : contentSelectors) {
            Element el = doc.selectFirst(sel);
            if (el != null && el.text().length() > 200) {
                return cleanHtml(el);
            }
        }

        // Fallback: find the div with the most paragraph text
        return cleanHtml(findLargestTextBlock(doc));
    }

    private static Element findLargestTextBlock(Document doc) {
        Elements divs = doc.select("div, section");
        Element best = doc.body();
        int maxLen = 0;
        for (Element div : divs) {
            int len = div.text().length();
            if (len > maxLen) {
                maxLen = len;
                best = div;
            }
        }
        return best;
    }

    private static String cleanHtml(Element el) {
        // Keep only safe/readable tags
        el.select("img, figure, figcaption, p, h1, h2, h3, h4, h5, h6, " +
                  "blockquote, ul, ol, li, strong, em, a, code, pre").tagName("keep");
        // The rest get unwrapped
        return el.outerHtml();
    }

    private static int countWords(String html) {
        String text = Jsoup.parse(html).text();
        return text.split("\\s+").length;
    }

    /**
     * Wrap extracted content in a styled HTML page for display in WebView.
     */
    public static String buildReaderHtml(ReaderContent content,
                                          String fontFamily,
                                          int fontSize,
                                          String bgColor,
                                          String textColor) {
        return "<!DOCTYPE html><html><head>" +
            "<meta name='viewport' content='width=device-width,initial-scale=1'>" +
            "<style>" +
            "  body { background:" + bgColor + "; color:" + textColor + ";" +
            "         font-family:" + fontFamily + "; font-size:" + fontSize + "px;" +
            "         line-height:1.7; margin:0; padding:16px 20px; max-width:680px;" +
            "         margin:0 auto; }" +
            "  h1 { font-size:1.6em; margin-bottom:8px; line-height:1.3; }" +
            "  .byline { color:#888; font-size:0.9em; margin-bottom:4px; }" +
            "  .meta { color:#888; font-size:0.85em; margin-bottom:24px; }" +
            "  hr { border:none; border-top:1px solid #ccc; margin:20px 0; }" +
            "  img { max-width:100%; height:auto; border-radius:8px; }" +
            "  blockquote { border-left:4px solid #ccc; margin:0; padding-left:16px;" +
            "                color:#666; font-style:italic; }" +
            "  a { color:#1976D2; text-decoration:none; }" +
            "  p { margin:0 0 1em 0; }" +
            "  code { background:#f4f4f4; padding:2px 6px; border-radius:4px;" +
            "          font-size:0.9em; }" +
            "</style></head><body>" +
            "<h1>" + content.title + "</h1>" +
            (content.byline.isEmpty() ? "" : "<div class='byline'>By " + content.byline + "</div>") +
            "<div class='meta'>" + content.siteName +
              (content.siteName.isEmpty() ? "" : " · ") +
              content.getEstimatedReadTime() + "</div>" +
            "<hr>" +
            content.content +
            "</body></html>";
    }
}
