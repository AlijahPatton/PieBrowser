package com.piebrowser.browser;

import android.content.Context;

import androidx.room.Dao;
import androidx.room.Database;
import androidx.room.Entity;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.PrimaryKey;
import androidx.room.Query;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import java.util.List;

/**
 * BookmarkManager — saves and retrieves browser bookmarks using Room DB.
 *
 * Stored locally in a compact SQLite database.
 * No cloud sync by default (privacy first).
 */
public class BookmarkManager {

    private static BookmarkDatabase db;

    public static void save(Context context, String title, String url) {
        getDb(context).bookmarkDao().insert(
                new Bookmark(title != null ? title : url, url,
                        System.currentTimeMillis())
        );
    }

    public static List<Bookmark> getAll(Context context) {
        return getDb(context).bookmarkDao().getAll();
    }

    public static void delete(Context context, String url) {
        getDb(context).bookmarkDao().deleteByUrl(url);
    }

    public static boolean isBookmarked(Context context, String url) {
        return getDb(context).bookmarkDao().existsByUrl(url) > 0;
    }

    private static BookmarkDatabase getDb(Context context) {
        if (db == null) {
            db = Room.databaseBuilder(context.getApplicationContext(),
                    BookmarkDatabase.class, "bookmarks.db")
                    .allowMainThreadQueries()  // OK for simple bookmark queries
                    .build();
        }
        return db;
    }

    // ── Entity ────────────────────────────────────────────────────────────────

    @Entity(tableName = "bookmarks")
    public static class Bookmark {
        @PrimaryKey(autoGenerate = true)
        public int id;
        public String title;
        public String url;
        public long createdAt;

        public Bookmark(String title, String url, long createdAt) {
            this.title = title;
            this.url = url;
            this.createdAt = createdAt;
        }
    }

    // ── DAO ───────────────────────────────────────────────────────────────────

    @Dao
    public interface BookmarkDao {
        @Insert(onConflict = OnConflictStrategy.REPLACE)
        void insert(Bookmark bookmark);

        @Query("SELECT * FROM bookmarks ORDER BY createdAt DESC")
        List<Bookmark> getAll();

        @Query("DELETE FROM bookmarks WHERE url = :url")
        void deleteByUrl(String url);

        @Query("SELECT COUNT(*) FROM bookmarks WHERE url = :url")
        int existsByUrl(String url);
    }

    // ── Database ──────────────────────────────────────────────────────────────

    @Database(entities = {Bookmark.class}, version = 1, exportSchema = false)
    public abstract static class BookmarkDatabase extends RoomDatabase {
        public abstract BookmarkDao bookmarkDao();
    }
}
