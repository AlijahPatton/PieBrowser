# 🌐 PieBrowser — Lightweight Android Browser

A **tiny, fast, customizable** Android browser built on the system WebView (Chromium engine).  
Supports **Android 5.0+** (API 21) • Targets ~4–6MB APK size • Low RAM usage

---

## ✨ Features

| Feature | Details |
|---|---|
| 🛡️ Ad & Tracker Blocker | Blocks 40+ ad networks before they touch your RAM |
| 🎨 7 Themes | Light, Dark, AMOLED, Midnight Blue, Forest, Sunset, System |
| 👆 Swipe Gestures | Right=Back, Left=Forward, Pull=Reload |
| 📥 Downloads | Progress tracking, open/delete, file type detection |
| 🔒 Privacy | Block 3rd-party cookies, Do Not Track, Incognito tabs |
| 🔍 Search Engines | DuckDuckGo (default), Google, Bing, Brave, Startpage |
| 📑 Tabs | Up to 10 tabs with memory-efficient lazy loading |
| 🔖 Bookmarks | Local SQLite storage, no cloud required |
| 📐 Text Size | Adjustable 50%–200% per user preference |
| 🌙 Force Dark | Forces dark mode on any website (Android 10+) |
| 🖥️ Desktop Mode | Toggle desktop user agent per-tab |
| 🔎 Find in Page | Built-in text search |

---

## 🚀 Getting Started (Step-by-Step for Beginners)

### Step 1: Install Android Studio
1. Download **Android Studio** from https://developer.android.com/studio
2. Install it and run the setup wizard — let it download the Android SDK

### Step 2: Open the Project
1. Open Android Studio
2. Click **"Open"** → select the `PieBrowser` folder
3. Wait for Gradle to sync (this downloads dependencies, takes 2–5 mins first time)

### Step 3: Add Missing Drawable Resources
The project needs icon files. The easiest way:

**Option A — Use Material Icons (recommended):**
1. Right-click `app/src/main/res` → **New → Vector Asset**
2. Click "Clip Art" and search for each icon below
3. Name them exactly as listed:

| File name | Icon to search |
|---|---|
| `ic_arrow_back.xml` | arrow_back |
| `ic_arrow_forward.xml` | arrow_forward |
| `ic_home.xml` | home |
| `ic_tabs.xml` | tab |
| `ic_more_vert.xml` | more_vert |
| `ic_refresh.xml` | refresh |
| `ic_lock.xml` | lock |

**Option B — Add to `res/drawable/` manually** using any 24dp vector XML.

### Step 4: Add Missing Layouts
Create these simple XML layout files in `app/src/main/res/layout/`:

**`activity_settings.xml`** — just a FrameLayout:
```xml
<?xml version="1.0" encoding="utf-8"?>
<FrameLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:id="@+id/settings_container"
    android:layout_width="match_parent"
    android:layout_height="match_parent" />
```

**`activity_downloads.xml`** — RecyclerView + empty state:
```xml
<?xml version="1.0" encoding="utf-8"?>
<RelativeLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent">

    <androidx.recyclerview.widget.RecyclerView
        android:id="@+id/downloadsRecycler"
        android:layout_width="match_parent"
        android:layout_height="match_parent" />

    <TextView
        android:id="@+id/emptyDownloads"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:layout_centerInParent="true"
        android:text="No downloads yet"
        android:textSize="16sp"
        android:visibility="gone" />
</RelativeLayout>
```

**`item_download.xml`** — single download row:
```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:orientation="vertical"
    android:padding="16dp">

    <TextView android:id="@+id/download_filename"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:textStyle="bold" />

    <ProgressBar android:id="@+id/download_progress_bar"
        style="@android:style/Widget.ProgressBar.Horizontal"
        android:layout_width="match_parent"
        android:layout_height="8dp"
        android:max="100" />

    <LinearLayout android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="horizontal">

        <TextView android:id="@+id/download_progress_text"
            android:layout_width="0dp" android:layout_height="wrap_content"
            android:layout_weight="1" />

        <TextView android:id="@+id/download_status"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content" />

        <ImageButton android:id="@+id/download_open"
            android:layout_width="40dp" android:layout_height="40dp"
            android:src="@android:drawable/ic_menu_view"
            android:background="?attr/selectableItemBackgroundBorderless" />

        <ImageButton android:id="@+id/download_delete"
            android:layout_width="40dp" android:layout_height="40dp"
            android:src="@android:drawable/ic_menu_delete"
            android:background="?attr/selectableItemBackgroundBorderless" />
    </LinearLayout>
</LinearLayout>
```

### Step 5: Add App Launcher Icons
1. Right-click `app/src/main/res` → **New → Image Asset**
2. Choose "Launcher Icons (Adaptive and Legacy)"
3. Set a name and pick any background color + foreground icon
4. Click **Finish** — this creates `mipmap/ic_launcher` automatically

### Step 6: Build and Run
1. Connect your Android phone (enable USB debugging in Developer Options)  
   OR use the **Android Emulator** (AVD Manager → Create Virtual Device)
2. Click the green **▶ Run** button in Android Studio
3. The app will build and install!

---

## 🧠 Architecture Overview

```
PieBrowser/
├── browser/
│   ├── BrowserActivity.java   ← Main screen, hosts everything
│   ├── PieWebView.java       ← Custom WebView with ad blocking
│   ├── BrowserSettings.java   ← All settings (SharedPreferences)
│   ├── TabManager.java        ← Low-RAM multi-tab system
│   ├── BookmarkManager.java   ← SQLite bookmarks via Room
│   └── BrowserGestureDetector.java  ← Swipe gestures
├── adblock/
│   └── AdBlockEngine.java     ← Domain + keyword ad blocker
├── downloads/
│   ├── PieDownloadManager.java  ← Wraps Android DownloadManager
│   ├── DownloadsActivity.java    ← Downloads list screen
│   └── DownloadsAdapter.java     ← RecyclerView adapter
├── settings/
│   └── SettingsActivity.java  ← Full settings screen
└── ui/theme/
    └── ThemeManager.java      ← 7 color themes
```

---

## 💡 Why Low RAM?

1. **System WebView** — no bundled browser engine (saves ~30–60MB)
2. **Ad blocking at network level** — blocked requests never reach memory
3. **Lazy tab loading** — background tabs don't keep WebViews alive
4. **WebView paused** when app goes to background (stops JS, timers)
5. **Minification + shrinking** in release builds removes unused code

---

## 🛠️ Expanding the Project

### Add More Blocked Domains
Edit `AdBlockEngine.java` → add entries to `DEFAULT_BLOCKED_DOMAINS`

### Load a Full EasyList
In `AdBlockEngine`, add a method to read from `assets/adblock_rules.txt`:
```java
// In assets/adblock_rules.txt — one domain per line
ads.example.com
tracker.example.org
```

### Add Extension / Userscript Support
In `PieWebView.init()`, inject JavaScript after page load:
```java
webView.evaluateJavascript(loadUserScript(), null);
```

### Custom Homepage
Change default in `BrowserSettings.java`:
```java
return prefs(ctx).getString(KEY_HOME_PAGE, "https://your-homepage.com");
```

---

## 📦 Building a Release APK

1. In Android Studio: **Build → Generate Signed Bundle/APK**
2. Choose **APK**
3. Create a new keystore (or use existing)
4. Select **release** build variant
5. Your APK will be in `app/release/app-release.apk`

The release APK will be ~4–7MB thanks to:
- `minifyEnabled true` (removes unused code)
- `shrinkResources true` (removes unused assets)
- ProGuard optimization

---

## 📋 Requirements

- **Android Studio** Hedgehog (2023.1.1) or newer
- **JDK 11+** (bundled with Android Studio)
- **Android device/emulator** running Android 5.0+ (API 21)
- Internet connection for first Gradle sync

---

*Built with ❤️ — lightweight by design*
