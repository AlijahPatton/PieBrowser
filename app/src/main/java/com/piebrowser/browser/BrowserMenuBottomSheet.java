package com.piebrowser.browser;

import android.content.Context;

import com.google.android.material.bottomsheet.BottomSheetDialog;

/**
 * BrowserMenuBottomSheet — the "hamburger" menu at the bottom.
 *
 * Actions available in the menu:
 * - Bookmarks
 * - Downloads
 * - Find in page
 * - Desktop/Mobile site
 * - New incognito tab
 * - Share page
 * - Settings
 */
public class BrowserMenuBottomSheet extends BottomSheetDialog {

    public enum Action {
        BOOKMARKS, DOWNLOADS, FIND_IN_PAGE, DESKTOP_SITE,
        NEW_INCOGNITO_TAB, SHARE_PAGE, BOOKMARK_PAGE, SETTINGS
    }

    public interface OnItemClickListener {
        void onClick(Action action);
    }

    private OnItemClickListener listener;

    public BrowserMenuBottomSheet(Context context) {
        super(context);
        // In a complete implementation: inflate R.layout.bottom_sheet_browser_menu
        // and wire up each action button to call listener.onClick(Action.XXX)
        //
        // Example:
        //   setContentView(R.layout.bottom_sheet_browser_menu);
        //   findViewById(R.id.menu_settings).setOnClickListener(v -> {
        //       dismiss();
        //       if (listener != null) listener.onClick(Action.SETTINGS);
        //   });
    }

    public void setOnItemClickListener(OnItemClickListener l) {
        this.listener = l;
    }
}
