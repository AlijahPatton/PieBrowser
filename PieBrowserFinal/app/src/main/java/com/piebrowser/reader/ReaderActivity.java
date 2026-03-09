package com.piebrowser.reader;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebView;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.piebrowser.R;
import com.piebrowser.ui.theme.ThemeManager;

/**
 * ReaderActivity — clean, distraction-free article reading.
 *
 * Features:
 * - Adjustable font size (slider)
 * - 3 background themes: White, Sepia, Dark
 * - 3 font families: Sans-Serif, Serif, Monospace
 * - Estimated read time
 * - Share article
 */
public class ReaderActivity extends AppCompatActivity {

    public static final String EXTRA_URL = "url";
    public static final String EXTRA_TITLE = "title";

    private WebView readerWebView;
    private ProgressBar loadingBar;
    private TextView statusText;

    // Reader settings
    private int fontSize = 17;
    private String bgColor = "#FFFFFF";
    private String textColor = "#212121";
    private String fontFamily = "Georgia, serif";

    private ReaderModeExtractor.ReaderContent currentContent;
    private String currentUrl;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeManager.applyTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reader);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Reader Mode");
        }

        readerWebView = findViewById(R.id.readerWebView);
        loadingBar    = findViewById(R.id.readerLoading);
        statusText    = findViewById(R.id.readerStatus);

        currentUrl = getIntent().getStringExtra(EXTRA_URL);

        if (currentUrl != null) {
            loadArticle(currentUrl);
        } else {
            statusText.setText("No URL provided");
        }
    }

    private void loadArticle(String url) {
        loadingBar.setVisibility(View.VISIBLE);
        statusText.setText("Extracting article…");

        ReaderModeExtractor.extract(url, new ReaderModeExtractor.Callback() {
            @Override
            public void onSuccess(ReaderModeExtractor.ReaderContent content) {
                currentContent = content;
                loadingBar.setVisibility(View.GONE);
                statusText.setText(content.getEstimatedReadTime() +
                        " · " + content.wordCount + " words");
                renderContent();
            }

            @Override
            public void onFailure(String error) {
                loadingBar.setVisibility(View.GONE);
                statusText.setText("Could not extract article");
                Toast.makeText(ReaderActivity.this,
                        "Reader mode failed: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void renderContent() {
        if (currentContent == null) return;
        String html = ReaderModeExtractor.buildReaderHtml(
                currentContent, fontFamily, fontSize, bgColor, textColor);
        readerWebView.loadDataWithBaseURL(currentUrl, html, "text/html", "UTF-8", null);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.reader_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            onBackPressed(); return true;
        } else if (id == R.id.reader_font_size) {
            showFontSizeDialog(); return true;
        } else if (id == R.id.reader_theme) {
            showThemeDialog(); return true;
        } else if (id == R.id.reader_font_family) {
            showFontDialog(); return true;
        } else if (id == R.id.reader_share) {
            shareArticle(); return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showFontSizeDialog() {
        View view = getLayoutInflater().inflate(R.layout.dialog_font_size, null);
        SeekBar seekBar = view.findViewById(R.id.fontSizeSeekBar);
        TextView label  = view.findViewById(R.id.fontSizeLabel);
        seekBar.setMax(20); // 12–32px range
        seekBar.setProgress(fontSize - 12);
        label.setText(fontSize + "px");
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar sb, int p, boolean u) {
                fontSize = p + 12;
                label.setText(fontSize + "px");
                renderContent();
            }
            public void onStartTrackingTouch(SeekBar sb) {}
            public void onStopTrackingTouch(SeekBar sb) {}
        });
        new AlertDialog.Builder(this).setTitle("Font Size").setView(view)
                .setPositiveButton("Done", null).show();
    }

    private void showThemeDialog() {
        String[] themes = {"White", "Sepia", "Dark"};
        new AlertDialog.Builder(this).setTitle("Reading Theme")
                .setItems(themes, (d, which) -> {
                    switch (which) {
                        case 0: bgColor = "#FFFFFF"; textColor = "#212121"; break;
                        case 1: bgColor = "#FBF0D9"; textColor = "#3B2A1A"; break;
                        case 2: bgColor = "#1A1A1A"; textColor = "#E0E0E0"; break;
                    }
                    renderContent();
                }).show();
    }

    private void showFontDialog() {
        String[] fonts = {"Serif", "Sans-Serif", "Monospace"};
        String[] values = {"Georgia, serif", "Arial, sans-serif", "Courier New, monospace"};
        new AlertDialog.Builder(this).setTitle("Font")
                .setItems(fonts, (d, which) -> {
                    fontFamily = values[which];
                    renderContent();
                }).show();
    }

    private void shareArticle() {
        if (currentUrl == null) return;
        Intent share = new Intent(Intent.ACTION_SEND);
        share.setType("text/plain");
        share.putExtra(Intent.EXTRA_TEXT, currentUrl);
        share.putExtra(Intent.EXTRA_SUBJECT,
                currentContent != null ? currentContent.title : "Shared from Pie Browser");
        startActivity(Intent.createChooser(share, "Share article"));
    }
}
