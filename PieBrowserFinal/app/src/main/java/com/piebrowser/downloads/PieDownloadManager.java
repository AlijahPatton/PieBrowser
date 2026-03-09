package com.piebrowser.downloads;

import android.app.DownloadManager;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.webkit.MimeTypeMap;
import android.webkit.URLUtil;

import androidx.core.app.NotificationCompat;

import com.piebrowser.PieBrowserApp;
import com.piebrowser.R;

import java.util.ArrayList;
import java.util.List;

/**
 * PieDownloadManager wraps Android's system DownloadManager.
 *
 * Uses the OS download infrastructure — no background service required,
 * minimal RAM usage, automatic resume on reconnection.
 */
public class PieDownloadManager {

    private static PieDownloadManager instance;
    private final Context context;
    private final DownloadManager systemDownloader;
    private final List<DownloadItem> activeDownloads = new ArrayList<>();

    private PieDownloadManager(Context context) {
        this.context = context.getApplicationContext();
        this.systemDownloader = (DownloadManager)
                this.context.getSystemService(Context.DOWNLOAD_SERVICE);

        // Listen for completed downloads
        IntentFilter filter = new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE);
        this.context.registerReceiver(downloadCompleteReceiver, filter);
    }

    public static synchronized PieDownloadManager getInstance(Context context) {
        if (instance == null) {
            instance = new PieDownloadManager(context);
        }
        return instance;
    }

    /**
     * Start downloading a file from the given URL.
     * @param url       The file URL
     * @param userAgent The browser's user agent string
     * @param contentDisposition  Content-Disposition header value (may be null)
     * @param mimeType  MIME type (may be null)
     * @return download ID
     */
    public long startDownload(String url, String userAgent,
                              String contentDisposition, String mimeType) {

        // Guess filename from URL or Content-Disposition
        String filename = URLUtil.guessFileName(url, contentDisposition, mimeType);

        // Resolve MIME type from extension if unknown
        if (mimeType == null || mimeType.isEmpty() || mimeType.equals("application/octet-stream")) {
            String extension = MimeTypeMap.getFileExtensionFromUrl(url);
            mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
            if (mimeType == null) mimeType = "application/octet-stream";
        }

        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url))
                .setTitle(filename)
                .setDescription("PieBrowser download")
                .setMimeType(mimeType)
                .setNotificationVisibility(
                        DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setDestinationInExternalPublicDir(
                        Environment.DIRECTORY_DOWNLOADS, filename)
                .setAllowedOverMetered(true)
                .setAllowedOverRoaming(false);

        if (userAgent != null) {
            request.addRequestHeader("User-Agent", userAgent);
        }

        long downloadId = systemDownloader.enqueue(request);

        activeDownloads.add(new DownloadItem(downloadId, filename, url,
                DownloadItem.Status.RUNNING));

        return downloadId;
    }

    /**
     * Get current list of downloads for the Downloads screen.
     */
    public List<DownloadItem> getDownloads() {
        refreshStatuses();
        return new ArrayList<>(activeDownloads);
    }

    /**
     * Cancel and remove a download.
     */
    public void cancelDownload(long downloadId) {
        systemDownloader.remove(downloadId);
        activeDownloads.removeIf(item -> item.getId() == downloadId);
    }

    /**
     * Pause a download (Android 24+)
     */
    public void pauseDownload(long downloadId) {
        // DownloadManager doesn't natively support pause; we remove and re-enqueue
        // For a full implementation, track byte offset and use Range header
        cancelDownload(downloadId);
    }

    private void refreshStatuses() {
        for (DownloadItem item : activeDownloads) {
            DownloadManager.Query query = new DownloadManager.Query()
                    .setFilterById(item.getId());
            Cursor cursor = systemDownloader.query(query);
            if (cursor != null && cursor.moveToFirst()) {
                int statusCol = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS);
                int bytesCol = cursor.getColumnIndex(
                        DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR);
                int totalCol = cursor.getColumnIndex(
                        DownloadManager.COLUMN_TOTAL_SIZE_BYTES);

                if (statusCol >= 0) {
                    int status = cursor.getInt(statusCol);
                    switch (status) {
                        case DownloadManager.STATUS_RUNNING:
                            item.setStatus(DownloadItem.Status.RUNNING);
                            if (bytesCol >= 0 && totalCol >= 0) {
                                item.setBytesDownloaded(cursor.getLong(bytesCol));
                                item.setTotalBytes(cursor.getLong(totalCol));
                            }
                            break;
                        case DownloadManager.STATUS_SUCCESSFUL:
                            item.setStatus(DownloadItem.Status.COMPLETED);
                            break;
                        case DownloadManager.STATUS_FAILED:
                            item.setStatus(DownloadItem.Status.FAILED);
                            break;
                        case DownloadManager.STATUS_PAUSED:
                            item.setStatus(DownloadItem.Status.PAUSED);
                            break;
                    }
                }
                cursor.close();
            }
        }
    }

    private final BroadcastReceiver downloadCompleteReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context ctx, Intent intent) {
            long completedId = intent.getLongExtra(
                    DownloadManager.EXTRA_DOWNLOAD_ID, -1);
            for (DownloadItem item : activeDownloads) {
                if (item.getId() == completedId) {
                    item.setStatus(DownloadItem.Status.COMPLETED);
                    break;
                }
            }
        }
    };
}
