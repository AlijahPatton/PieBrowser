package com.piebrowser.downloads;

/**
 * Model class representing a single download.
 */
public class DownloadItem {

    public enum Status { QUEUED, RUNNING, PAUSED, COMPLETED, FAILED, CANCELLED }

    private final long id;
    private final String filename;
    private final String url;
    private Status status;
    private long bytesDownloaded;
    private long totalBytes;

    public DownloadItem(long id, String filename, String url, Status status) {
        this.id = id;
        this.filename = filename;
        this.url = url;
        this.status = status;
    }

    public long getId() { return id; }
    public String getFilename() { return filename; }
    public String getUrl() { return url; }
    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }
    public long getBytesDownloaded() { return bytesDownloaded; }
    public void setBytesDownloaded(long bytes) { this.bytesDownloaded = bytes; }
    public long getTotalBytes() { return totalBytes; }
    public void setTotalBytes(long total) { this.totalBytes = total; }

    /**
     * Returns download progress as a 0–100 int, or -1 if unknown.
     */
    public int getProgressPercent() {
        if (totalBytes <= 0) return -1;
        return (int) ((bytesDownloaded * 100L) / totalBytes);
    }

    /**
     * Human-readable size string, e.g. "4.2 MB / 10 MB"
     */
    public String getProgressString() {
        if (totalBytes <= 0) return formatBytes(bytesDownloaded);
        return formatBytes(bytesDownloaded) + " / " + formatBytes(totalBytes);
    }

    private String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024));
        return String.format("%.2f GB", bytes / (1024.0 * 1024 * 1024));
    }
}
