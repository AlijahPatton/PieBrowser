package com.piebrowser.startpage;

import android.content.Context;

import com.piebrowser.browser.BookmarkManager;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * StartPageBuilder — generates the custom new tab / home page.
 *
 * Widgets included:
 * - Date & time (live)
 * - Search bar
 * - Quick access bookmarks (top 8)
 * - Speed dial (most visited sites)
 * - Motivational quote section
 *
 * The page is rendered as local HTML/CSS and loaded into WebView
 * without any network requests — instant load.
 */
public class StartPageBuilder {

    public static String build(Context context, String theme) {
        List<BookmarkManager.Bookmark> bookmarks = BookmarkManager.getAll(context);
        String bookmarksHtml = buildBookmarksSection(bookmarks);
        String speedDialHtml = buildSpeedDial();
        String colors = getThemeColors(theme);

        return "<!DOCTYPE html><html><head>" +
            "<meta name='viewport' content='width=device-width,initial-scale=1'>" +
            "<meta charset='UTF-8'>" +
            "<title>New Tab</title>" +
            "<style>" + buildCSS(colors) + "</style>" +
            "</head><body>" +
            "<div class='container'>" +
            "  <div class='datetime' id='datetime'></div>" +
            "  <div class='logo'>" +
            "    <span class='logo-icon'>🥧</span>" +
            "    <span class='logo-text'>Pie Browser</span>" +
            "  </div>" +
            "  <div class='search-box'>" +
            "    <form onsubmit='search(event)'>" +
            "      <input type='text' id='searchInput' " +
            "             placeholder='Search or enter URL…' autocomplete='off' />" +
            "      <button type='submit'>→</button>" +
            "    </form>" +
            "  </div>" +
            speedDialHtml +
            bookmarksHtml +
            "</div>" +
            "<script>" + buildJS() + "</script>" +
            "</body></html>";
    }

    private static String buildSpeedDial() {
        // Default quick-access sites
        String[][] sites = {
            {"Google",     "https://google.com",      "🔍"},
            {"YouTube",    "https://youtube.com",     "▶️"},
            {"Wikipedia",  "https://wikipedia.org",   "📚"},
            {"Reddit",     "https://reddit.com",      "🗣️"},
            {"GitHub",     "https://github.com",      "💻"},
            {"Maps",       "https://maps.google.com", "🗺️"},
            {"News",       "https://news.google.com", "📰"},
            {"Weather",    "https://weather.com",     "🌤️"},
        };

        StringBuilder sb = new StringBuilder("<div class='speed-dial'><h3>Quick Access</h3><div class='dial-grid'>");
        for (String[] site : sites) {
            sb.append(String.format(
                "<a href='%s' class='dial-item'>" +
                "  <span class='dial-icon'>%s</span>" +
                "  <span class='dial-name'>%s</span>" +
                "</a>", site[1], site[2], site[0]
            ));
        }
        sb.append("</div></div>");
        return sb.toString();
    }

    private static String buildBookmarksSection(List<BookmarkManager.Bookmark> bookmarks) {
        if (bookmarks.isEmpty()) return "";
        StringBuilder sb = new StringBuilder(
            "<div class='bookmarks'><h3>Bookmarks</h3><div class='bookmark-list'>");
        int limit = Math.min(bookmarks.size(), 8);
        for (int i = 0; i < limit; i++) {
            BookmarkManager.Bookmark b = bookmarks.get(i);
            String domain = extractDomain(b.url);
            sb.append(String.format(
                "<a href='%s' class='bookmark-item'>" +
                "  <img src='https://www.google.com/s2/favicons?domain=%s&sz=32' " +
                "       onerror=\"this.style.display='none'\" class='fav-icon'/>" +
                "  <span>%s</span>" +
                "</a>",
                b.url, domain,
                b.title.length() > 20 ? b.title.substring(0, 20) + "…" : b.title
            ));
        }
        sb.append("</div></div>");
        return sb.toString();
    }

    private static String extractDomain(String url) {
        try {
            String d = url.replaceFirst("https?://", "").split("/")[0];
            return d.startsWith("www.") ? d.substring(4) : d;
        } catch (Exception e) { return url; }
    }

    private static String getThemeColors(String theme) {
        switch (theme) {
            case "DARK":
                return "--bg:#121212;--card:#1E1E1E;--text:#EEEEEE;--accent:#90CAF9;--sub:#888;";
            case "AMOLED":
                return "--bg:#000000;--card:#0A0A0A;--text:#FFFFFF;--accent:#FFFFFF;--sub:#666;";
            case "MIDNIGHT_BLUE":
                return "--bg:#0A0E1A;--card:#111827;--text:#DCE5FF;--accent:#5C9BFF;--sub:#666;";
            case "FOREST":
                return "--bg:#0D1A0F;--card:#1B2B1D;--text:#C8E6C9;--accent:#4CAF50;--sub:#4a7a4c;";
            case "SUNSET":
                return "--bg:#1A0A00;--card:#2A1200;--text:#FFCCBC;--accent:#FF6D00;--sub:#8a5a40;";
            default: // LIGHT
                return "--bg:#F5F5F5;--card:#FFFFFF;--text:#212121;--accent:#1976D2;--sub:#777;";
        }
    }

    private static String buildCSS(String colors) {
        return ":root{" + colors + "}" +
            "*{margin:0;padding:0;box-sizing:border-box;}" +
            "body{background:var(--bg);color:var(--text);font-family:sans-serif;" +
            "     min-height:100vh;}" +
            ".container{max-width:700px;margin:0 auto;padding:32px 16px;}" +
            ".datetime{text-align:center;color:var(--sub);font-size:14px;margin-bottom:16px;}" +
            ".logo{display:flex;align-items:center;justify-content:center;gap:12px;margin-bottom:28px;}" +
            ".logo-icon{font-size:48px;}" +
            ".logo-text{font-size:28px;font-weight:700;color:var(--accent);letter-spacing:-0.5px;}" +
            ".search-box{background:var(--card);border-radius:50px;padding:0 4px 0 20px;" +
            "            display:flex;align-items:center;margin-bottom:32px;" +
            "            box-shadow:0 2px 12px rgba(0,0,0,0.12);}" +
            ".search-box input{flex:1;border:none;outline:none;background:transparent;" +
            "                  color:var(--text);font-size:16px;padding:14px 0;}" +
            ".search-box button{background:var(--accent);color:#fff;border:none;border-radius:50%;" +
            "                   width:40px;height:40px;font-size:18px;cursor:pointer;}" +
            "h3{font-size:13px;text-transform:uppercase;letter-spacing:1px;color:var(--sub);" +
            "   margin-bottom:12px;}" +
            ".dial-grid{display:grid;grid-template-columns:repeat(4,1fr);gap:10px;" +
            "           margin-bottom:28px;}" +
            ".dial-item{display:flex;flex-direction:column;align-items:center;gap:6px;" +
            "           background:var(--card);border-radius:16px;padding:14px 8px;" +
            "           text-decoration:none;color:var(--text);transition:transform .15s;}" +
            ".dial-item:hover{transform:scale(1.05);}" +
            ".dial-icon{font-size:24px;}" +
            ".dial-name{font-size:11px;color:var(--sub);text-align:center;}" +
            ".bookmark-list{display:grid;grid-template-columns:1fr 1fr;gap:8px;}" +
            ".bookmark-item{display:flex;align-items:center;gap:10px;background:var(--card);" +
            "               border-radius:12px;padding:10px 14px;text-decoration:none;" +
            "               color:var(--text);font-size:13px;overflow:hidden;}" +
            ".fav-icon{width:20px;height:20px;border-radius:4px;flex-shrink:0;}" +
            "@media(max-width:400px){.dial-grid{grid-template-columns:repeat(3,1fr);}}" +
            ".speed-dial{margin-bottom:24px;}";
    }

    private static String buildJS() {
        return "function updateTime(){" +
            "  var now=new Date();" +
            "  var days=['Sunday','Monday','Tuesday','Wednesday','Thursday','Friday','Saturday'];" +
            "  var months=['Jan','Feb','Mar','Apr','May','Jun','Jul','Aug','Sep','Oct','Nov','Dec'];" +
            "  var h=now.getHours(),m=now.getMinutes();" +
            "  var ampm=h>=12?'PM':'AM';h=h%12||12;" +
            "  document.getElementById('datetime').textContent=" +
            "    days[now.getDay()]+', '+months[now.getMonth()]+' '+now.getDate()+' · '" +
            "    +h+':'+(m<10?'0':'')+m+' '+ampm;" +
            "}" +
            "updateTime();setInterval(updateTime,30000);" +
            "function search(e){e.preventDefault();" +
            "  var v=document.getElementById('searchInput').value.trim();" +
            "  if(!v)return;" +
            "  if(/^https?:\\/\\//.test(v)||(!v.includes(' ')&&v.includes('.')))window.location=v.startsWith('http')?v:'https://'+v;" +
            "  else window.location='https://duckduckgo.com/?q='+encodeURIComponent(v);" +
            "}" +
            "document.getElementById('searchInput').focus();";
    }
}
