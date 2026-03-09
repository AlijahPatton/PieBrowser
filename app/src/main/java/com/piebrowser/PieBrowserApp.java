package com.piebrowser;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.os.Build;
import androidx.multidex.MultiDexApplication;

/**
 * PieBrowser Application class
 * Initializes app-wide components on startup
 */
public class PieBrowserApp extends MultiDexApplication {

    public static final String DOWNLOAD_CHANNEL_ID = "downloads";
    private static PieBrowserApp instance;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        createNotificationChannels();
    }

    public static PieBrowserApp getInstance() {
        return instance;
    }

    /**
     * Create notification channels for Android 8.0+
     */
    private void createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel downloadChannel = new NotificationChannel(
                DOWNLOAD_CHANNEL_ID,
                "Downloads",
                NotificationManager.IMPORTANCE_LOW
            );
            downloadChannel.setDescription("File download progress");

            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(downloadChannel);
            }
        }
    }
}
