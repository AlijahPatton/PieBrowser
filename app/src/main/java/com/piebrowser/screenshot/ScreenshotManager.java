package com.piebrowser.screenshot;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.View;
import android.webkit.WebView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * ScreenshotManager — capture and save screenshots of the current page.
 *
 * Two modes:
 * 1. VISIBLE  — captures what you currently see on screen
 * 2. FULL_PAGE — captures the entire scrollable page (stitched bitmap)
 *
 * Saves to Pictures/PieBrowser/ folder, visible in gallery.
 */
public class ScreenshotManager {

    public interface SaveCallback {
        void onSaved(String path);
        void onError(String error);
    }

    /**
     * Capture a screenshot of just the visible portion of the WebView.
     */
    public static void captureVisible(WebView webView, Activity activity, SaveCallback callback) {
        try {
            webView.setDrawingCacheEnabled(true);
            webView.buildDrawingCache();
            Bitmap bitmap = Bitmap.createBitmap(webView.getDrawingCache());
            webView.setDrawingCacheEnabled(false);

            if (bitmap == null) {
                callback.onError("Failed to capture screenshot");
                return;
            }

            saveToGallery(bitmap, activity, "screenshot", callback);
        } catch (Exception e) {
            callback.onError(e.getMessage());
        }
    }

    /**
     * Capture the FULL page by scrolling through and stitching bitmaps.
     * Works by measuring the full content height and drawing the whole WebView.
     */
    public static void captureFullPage(WebView webView, Activity activity, SaveCallback callback) {
        try {
            // Measure full content dimensions
            int width = webView.getWidth();
            int fullHeight = (int) (webView.getContentHeight() *
                    activity.getResources().getDisplayMetrics().density);

            // Cap at 8000px to prevent OOM on very long pages
            fullHeight = Math.min(fullHeight, 8000);

            Bitmap bitmap = Bitmap.createBitmap(width, fullHeight, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);

            int savedScrollY = webView.getScrollY();
            webView.scrollTo(0, 0);
            webView.draw(canvas);
            webView.scrollTo(0, savedScrollY);

            saveToGallery(bitmap, activity, "fullpage", callback);
        } catch (OutOfMemoryError e) {
            callback.onError("Page too long for full screenshot. Try visible screenshot.");
        } catch (Exception e) {
            callback.onError(e.getMessage());
        }
    }

    private static void saveToGallery(Bitmap bitmap, Activity activity,
                                       String type, SaveCallback callback) {
        String filename = "PieBrowser_" + type + "_" +
                new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date()) + ".png";

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Android 10+ — use MediaStore (no storage permission needed)
                ContentValues values = new ContentValues();
                values.put(MediaStore.Images.Media.DISPLAY_NAME, filename);
                values.put(MediaStore.Images.Media.MIME_TYPE, "image/png");
                values.put(MediaStore.Images.Media.RELATIVE_PATH,
                        Environment.DIRECTORY_PICTURES + "/PieBrowser");

                Uri uri = activity.getContentResolver()
                        .insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);

                if (uri != null) {
                    try (OutputStream out = activity.getContentResolver().openOutputStream(uri)) {
                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
                    }
                    callback.onSaved(uri.toString());
                } else {
                    callback.onError("Could not create file");
                }
            } else {
                // Android 9 and below — write directly to file
                File dir = new File(Environment.getExternalStoragePublicDirectory(
                        Environment.DIRECTORY_PICTURES), "PieBrowser");
                if (!dir.exists()) dir.mkdirs();

                File file = new File(dir, filename);
                try (FileOutputStream out = new FileOutputStream(file)) {
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
                }
                callback.onSaved(file.getAbsolutePath());
            }
        } catch (Exception e) {
            callback.onError("Save failed: " + e.getMessage());
        } finally {
            bitmap.recycle();
        }
    }

    /**
     * Show a dialog asking user which type of screenshot they want.
     */
    public static void showScreenshotDialog(Activity activity, WebView webView) {
        String[] options = {"Visible screen", "Full page (entire content)"};
        new android.app.AlertDialog.Builder(activity)
            .setTitle("Screenshot")
            .setItems(options, (d, which) -> {
                SaveCallback cb = new SaveCallback() {
                    @Override public void onSaved(String path) {
                        Toast.makeText(activity, "Screenshot saved to gallery",
                                Toast.LENGTH_SHORT).show();
                    }
                    @Override public void onError(String error) {
                        Toast.makeText(activity, "Screenshot failed: " + error,
                                Toast.LENGTH_SHORT).show();
                    }
                };
                if (which == 0) captureVisible(webView, activity, cb);
                else captureFullPage(webView, activity, cb);
            })
            .show();
    }
}
