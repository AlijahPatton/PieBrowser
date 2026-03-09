# PieBrowser ProGuard Rules

# Keep Room entities and DAOs
-keep class com.piebrowser.browser.BookmarkManager$Bookmark { *; }
-keep interface com.piebrowser.browser.BookmarkManager$BookmarkDao { *; }

# Keep WebView interfaces
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}

# Keep settings keys
-keep class com.piebrowser.browser.BrowserSettings { *; }

# Prevent stripping of AdBlock rules
-keep class com.piebrowser.adblock.AdBlockEngine { *; }

# Room
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**

# Material Components
-keep class com.google.android.material.** { *; }
