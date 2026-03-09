package com.piebrowser.browser;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.webkit.WebView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.textfield.TextInputEditText;
import com.piebrowser.R;
import com.piebrowser.downloads.DownloadsActivity;
import com.piebrowser.settings.SettingsActivity;
import com.piebrowser.ui.theme.ThemeManager;

/**
 * BrowserActivity — the heart of PieBrowser.
 * Hosts the WebView, address bar, navigation controls, and bottom menu.
 */
public class BrowserActivity extends AppCompatActivity {

    private static final int PERMISSION_REQUEST_STORAGE = 1001;

    private PieWebView webView;
    private TextInputEditText addressBar;
    private ProgressBar progressBar;
    private BottomNavigationView bottomNav;
    private TabManager tabManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Apply saved theme before setContentView
        ThemeManager.applyTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_browser);

        initViews();
        initWebView();
        initAddressBar();
        initBottomNav();
        initGestureDetector();
        handleIncomingIntent(getIntent());

        // Request storage permission for downloads (Android < 10)
        requestStoragePermission();
    }

    private void initViews() {
        webView    = findViewById(R.id.webView);
        addressBar = findViewById(R.id.addressBar);
        progressBar = findViewById(R.id.progressBar);
        bottomNav  = findViewById(R.id.bottomNav);
    }

    private void initWebView() {
        tabManager = new TabManager(this, webView);

        webView.setProgressListener(progress -> {
            progressBar.setProgress(progress);
            progressBar.setVisibility(progress < 100 ? View.VISIBLE : View.GONE);
        });

        webView.setUrlChangedListener(url -> {
            if (addressBar != null) {
                addressBar.setText(url);
                addressBar.clearFocus();
            }
        });

        // Load default home page
        webView.loadUrl(getHomeUrl());
    }

    private void initAddressBar() {
        addressBar.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_GO
                    || (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                navigateToInput();
                return true;
            }
            return false;
        });

        addressBar.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) addressBar.selectAll();
        });
    }

    private void initBottomNav() {
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_back) {
                if (webView.canGoBack()) webView.goBack();
                return true;
            } else if (id == R.id.nav_forward) {
                if (webView.canGoForward()) webView.goForward();
                return true;
            } else if (id == R.id.nav_home) {
                webView.loadUrl(getHomeUrl());
                return true;
            } else if (id == R.id.nav_tabs) {
                tabManager.showTabSwitcher();
                return true;
            } else if (id == R.id.nav_menu) {
                showBrowserMenu();
                return true;
            }
            return false;
        });
    }

    private void initGestureDetector() {
        BrowserGestureDetector gestureDetector = new BrowserGestureDetector(this);
        gestureDetector.setOnSwipeLeftListener(() -> {
            if (webView.canGoForward()) webView.goForward();
        });
        gestureDetector.setOnSwipeRightListener(() -> {
            if (webView.canGoBack()) webView.goBack();
        });
        gestureDetector.setOnSwipeDownListener(() -> webView.reload());
        webView.setGestureDetector(gestureDetector);
    }

    private void navigateToInput() {
        String input = addressBar.getText() != null ? addressBar.getText().toString().trim() : "";
        if (input.isEmpty()) return;

        // Hide keyboard
        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        if (imm != null) imm.hideSoftInputFromWindow(addressBar.getWindowToken(), 0);

        // Detect if it's a URL or a search query
        String url = resolveInput(input);
        webView.loadUrl(url);
    }

    /**
     * Smart input resolver: URL vs. search query
     */
    private String resolveInput(String input) {
        if (input.startsWith("http://") || input.startsWith("https://")) {
            return input;
        }
        // Looks like a domain (e.g. "google.com")
        if (!input.contains(" ") && input.contains(".")) {
            return "https://" + input;
        }
        // Treat as search query
        String searchEngine = BrowserSettings.getSearchEngine(this);
        return searchEngine + Uri.encode(input);
    }

    private String getHomeUrl() {
        return BrowserSettings.getHomePage(this);
    }

    private void showBrowserMenu() {
        BrowserMenuBottomSheet menu = new BrowserMenuBottomSheet(this);
        menu.setOnItemClickListener(action -> {
            switch (action) {
                case SETTINGS:
                    startActivity(new Intent(this, SettingsActivity.class));
                    break;
                case DOWNLOADS:
                    startActivity(new Intent(this, DownloadsActivity.class));
                    break;
                case BOOKMARK_PAGE:
                    bookmarkCurrentPage();
                    break;
                case SHARE_PAGE:
                    sharePage();
                    break;
                case FIND_IN_PAGE:
                    webView.showFindInPage();
                    break;
                case DESKTOP_SITE:
                    webView.toggleDesktopSite();
                    break;
                case NEW_INCOGNITO_TAB:
                    tabManager.openIncognitoTab();
                    break;
            }
        });
        menu.show(getSupportFragmentManager(), "browser_menu");
    }

    private void bookmarkCurrentPage() {
        String url = webView.getUrl();
        String title = webView.getTitle();
        if (url != null) {
            BookmarkManager.save(this, title, url);
            Toast.makeText(this, R.string.bookmark_saved, Toast.LENGTH_SHORT).show();
        }
    }

    private void sharePage() {
        String url = webView.getUrl();
        if (url != null) {
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            shareIntent.putExtra(Intent.EXTRA_TEXT, url);
            startActivity(Intent.createChooser(shareIntent, getString(R.string.share_via)));
        }
    }

    private void handleIncomingIntent(Intent intent) {
        if (intent != null && Intent.ACTION_VIEW.equals(intent.getAction())) {
            Uri data = intent.getData();
            if (data != null) webView.loadUrl(data.toString());
        }
    }

    private void requestStoragePermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        PERMISSION_REQUEST_STORAGE);
            }
        }
    }

    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        webView.onPause();  // Pause JS to save battery/RAM
    }

    @Override
    protected void onResume() {
        super.onResume();
        webView.onResume();
        ThemeManager.applyTheme(this); // Re-apply in case settings changed
    }

    @Override
    protected void onDestroy() {
        webView.destroy(); // Free WebView memory
        super.onDestroy();
    }
}
