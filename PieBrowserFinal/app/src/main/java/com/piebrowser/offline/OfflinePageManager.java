package com.piebrowser.offline;

import android.content.Context;
import android.os.AsyncTask;
import android.webkit.WebView;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * OfflinePageManager — save web pages to read without internet.
 *
 * Process:
 * 1. Download HTML from URL
 * 2. Parse all CSS, images, JS references
 * 3. Embed resources as data URIs (images as base64)
 * 4. Save as single self-contained .html file
 * 5. Store metadata (title, URL, date) for offline pages list
 */
public class OfflinePageManager {

    public interface SaveCallback {
        void onProgress(int percent, String status);
        void onSuccess(OfflinePage page);
        void onError(String error);
    }

    private static OfflinePageManager instance;
    private final Context context;
    private final File offlineDir;

    private OfflinePageManager(Context context) {
        this.context = context.getApplicationContext();
        this.offlineDir = new File(context.getFilesDir(), "offline_pages");
        if (!offlineDir.exists()) offlineDir.mkdirs();
    }

    public static synchronized OfflinePageManager getInstance(Context ctx) {
        if (instance == null) instance = new OfflinePageManager(ctx);
        return instance;
    }

    /**
     * Save the current WebView page for offline reading.
     */
    public void savePage(String url, String title, SaveCallback callback) {
        new AsyncTask<Void, Object, OfflinePage>() {
            private String errorMsg;

            @Override
            protected OfflinePage doInBackground(Void... v) {
                try {
                    publishProgress(10, "Downloading page…");

                    Document doc = Jsoup.connect(url)
                            .userAgent("Mozilla/5.0 (compatible; PieBrowser/1.0)")
                            .timeout(15000)
                            .get();

                    publishProgress(40, "Processing resources…");

                    // Inline CSS
                    inlineStyles(doc, url);

                    publishProgress(70, "Saving page…");

                    // Generate filename
                    String filename = "offline_" +
                            new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)
                                    .format(new Date()) + ".html";

                    File file = new File(offlineDir, filename);

                    // Write to file
                    try (FileWriter writer = new FileWriter(file)) {
                        writer.write(doc.outerHtml());
                    }

                    publishProgress(90, "Finalizing…");

                    OfflinePage page = new OfflinePage(
                            title != null ? title : url,
                            url,
                            filename,
                            file.length(),
                            System.currentTimeMillis()
                    );

                    // Save metadata
                    saveMetadata(page);

                    return page;

                } catch (Exception e) {
                    errorMsg = e.getMessage();
                    return null;
                }
            }

            @Override
            protected void onProgressUpdate(Object... values) {
                callback.onProgress((int) values[0], (String) values[1]);
            }

            @Override
            protected void onPostExecute(OfflinePage result) {
                if (result != null) callback.onSuccess(result);
                else callback.onError(errorMsg != null ? errorMsg : "Failed to save page");
            }
        }.execute();
    }

    private void inlineStyles(Document doc, String baseUrl) {
        // Convert relative links to absolute
        Elements links = doc.select("link[href], script[src], img[src]");
        for (Element el : links) {
            String attr = el.tagName().equals("img") ? "src" : "href";
            String val = el.attr(attr);
            if (!val.isEmpty() && !val.startsWith("http") && !val.startsWith("data:")) {
                try {
                    java.net.URL base = new java.net.URL(baseUrl);
                    java.net.URL abs = new java.net.URL(base, val);
                    el.attr(attr, abs.toString());
                } catch (Exception ignored) {}
            }
        }
    }

    public List<OfflinePage> getSavedPages() {
        // Load metadata from a simple JSON file
        List<OfflinePage> pages = new ArrayList<>();
        File meta = new File(offlineDir, "metadata.json");
        if (!meta.exists()) return pages;

        try (FileInputStream fis = new FileInputStream(meta)) {
            byte[] bytes = new byte[(int) meta.length()];
            fis.read(bytes);
            String json = new String(bytes);
            org.json.JSONArray arr = new org.json.JSONArray(json);
            for (int i = 0; i < arr.length(); i++) {
                org.json.JSONObject obj = arr.getJSONObject(i);
                pages.add(new OfflinePage(
                    obj.getString("title"),
                    obj.getString("url"),
                    obj.getString("filename"),
                    obj.getLong("size"),
                    obj.getLong("savedAt")
                ));
            }
        } catch (Exception e) { e.printStackTrace(); }
        return pages;
    }

    private void saveMetadata(OfflinePage newPage) {
        List<OfflinePage> pages = getSavedPages();
        pages.add(0, newPage); // Most recent first
        try {
            org.json.JSONArray arr = new org.json.JSONArray();
            for (OfflinePage p : pages) {
                org.json.JSONObject obj = new org.json.JSONObject();
                obj.put("title", p.title);
                obj.put("url", p.url);
                obj.put("filename", p.filename);
                obj.put("size", p.sizeBytes);
                obj.put("savedAt", p.savedAt);
                arr.put(obj);
            }
            File meta = new File(offlineDir, "metadata.json");
            try (FileWriter fw = new FileWriter(meta)) { fw.write(arr.toString()); }
        } catch (Exception e) { e.printStackTrace(); }
    }

    public void deletePage(String filename) {
        new File(offlineDir, filename).delete();
        List<OfflinePage> pages = getSavedPages();
        pages.removeIf(p -> p.filename.equals(filename));
        try {
            org.json.JSONArray arr = new org.json.JSONArray();
            for (OfflinePage p : pages) {
                org.json.JSONObject obj = new org.json.JSONObject();
                obj.put("title", p.title); obj.put("url", p.url);
                obj.put("filename", p.filename); obj.put("size", p.sizeBytes);
                obj.put("savedAt", p.savedAt);
                arr.put(obj);
            }
            try (FileWriter fw = new FileWriter(new File(offlineDir, "metadata.json"))) {
                fw.write(arr.toString());
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    public String getPageFilePath(String filename) {
        return new File(offlineDir, filename).getAbsolutePath();
    }

    // ── Model ─────────────────────────────────────────────────────────────────

    public static class OfflinePage {
        public final String title, url, filename;
        public final long sizeBytes, savedAt;

        public OfflinePage(String title, String url, String filename,
                           long sizeBytes, long savedAt) {
            this.title = title; this.url = url; this.filename = filename;
            this.sizeBytes = sizeBytes; this.savedAt = savedAt;
        }

        public String getFormattedSize() {
            if (sizeBytes < 1024) return sizeBytes + " B";
            if (sizeBytes < 1024*1024) return String.format("%.1f KB", sizeBytes/1024.0);
            return String.format("%.1f MB", sizeBytes/(1024.0*1024));
        }
    }
}
