package com.piebrowser.downloads;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.piebrowser.R;
import com.piebrowser.ui.theme.ThemeManager;

import java.io.File;
import java.util.List;

/**
 * DownloadsActivity — shows a list of all downloads with progress + open/delete options.
 */
public class DownloadsActivity extends AppCompatActivity {

    private PieDownloadManager downloadManager;
    private RecyclerView recyclerView;
    private TextView emptyView;
    private DownloadsAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeManager.applyTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_downloads);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.downloads);
        }

        downloadManager = PieDownloadManager.getInstance(this);

        recyclerView = findViewById(R.id.downloadsRecycler);
        emptyView    = findViewById(R.id.emptyDownloads);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        loadDownloads();
    }

    private void loadDownloads() {
        List<DownloadItem> items = downloadManager.getDownloads();

        if (items.isEmpty()) {
            recyclerView.setVisibility(View.GONE);
            emptyView.setVisibility(View.VISIBLE);
            return;
        }

        emptyView.setVisibility(View.GONE);
        recyclerView.setVisibility(View.VISIBLE);

        adapter = new DownloadsAdapter(items,
            // Open file
            item -> {
                File file = new File(
                    android.os.Environment.getExternalStoragePublicDirectory(
                        android.os.Environment.DIRECTORY_DOWNLOADS),
                    item.getFilename()
                );
                if (file.exists()) {
                    Uri uri = FileProvider.getUriForFile(this,
                            getPackageName() + ".fileprovider", file);
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setDataAndType(uri, getMimeType(item.getFilename()));
                    intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    startActivity(Intent.createChooser(intent, "Open with"));
                }
            },
            // Delete file
            item -> {
                downloadManager.cancelDownload(item.getId());
                loadDownloads(); // Refresh list
            }
        );

        recyclerView.setAdapter(adapter);
    }

    private String getMimeType(String filename) {
        String ext = filename.contains(".")
                ? filename.substring(filename.lastIndexOf('.') + 1).toLowerCase()
                : "";
        switch (ext) {
            case "pdf":  return "application/pdf";
            case "jpg":
            case "jpeg": return "image/jpeg";
            case "png":  return "image/png";
            case "mp4":  return "video/mp4";
            case "mp3":  return "audio/mpeg";
            case "zip":  return "application/zip";
            case "apk":  return "application/vnd.android.package-archive";
            default:     return "application/octet-stream";
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadDownloads(); // Refresh download states
    }
}
