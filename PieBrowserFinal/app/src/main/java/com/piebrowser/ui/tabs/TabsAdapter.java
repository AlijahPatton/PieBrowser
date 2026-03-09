package com.piebrowser.ui.tabs;

import android.content.Context;
import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.piebrowser.R;
import com.piebrowser.browser.TabManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * TabsAdapter — horizontal swipeable tab previews.
 *
 * Each tab card shows:
 * - Page title
 * - URL
 * - Thumbnail screenshot of the page
 * - Incognito indicator
 * - Close button
 *
 * Displayed in a horizontal ViewPager2 or RecyclerView.
 */
public class TabsAdapter extends RecyclerView.Adapter<TabsAdapter.TabViewHolder> {

    public interface OnTabSelectedListener { void onTabSelected(int index); }
    public interface OnTabClosedListener { void onTabClosed(int index); }

    private final List<TabManager.Tab> tabs;
    private final Map<String, Bitmap> thumbnails = new HashMap<>();
    private final OnTabSelectedListener selectListener;
    private final OnTabClosedListener closeListener;
    private int activeIndex;

    public TabsAdapter(List<TabManager.Tab> tabs, int activeIndex,
                       OnTabSelectedListener selectListener,
                       OnTabClosedListener closeListener) {
        this.tabs = new ArrayList<>(tabs);
        this.activeIndex = activeIndex;
        this.selectListener = selectListener;
        this.closeListener = closeListener;
    }

    @NonNull
    @Override
    public TabViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_tab_preview, parent, false);
        return new TabViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull TabViewHolder holder, int position) {
        TabManager.Tab tab = tabs.get(position);

        holder.title.setText(tab.getTitle() != null ? tab.getTitle() : "New Tab");
        holder.url.setText(tab.getUrl() != null ? tab.getUrl() : "");

        // Active tab highlight
        boolean isActive = position == activeIndex;
        holder.itemView.setAlpha(isActive ? 1f : 0.7f);
        holder.itemView.setScaleX(isActive ? 1f : 0.95f);
        holder.itemView.setScaleY(isActive ? 1f : 0.95f);

        // Incognito badge
        holder.incognitoBadge.setVisibility(tab.isIncognito() ? View.VISIBLE : View.GONE);

        // Thumbnail
        Bitmap thumb = thumbnails.get(tab.getUrl());
        if (thumb != null) {
            holder.thumbnail.setImageBitmap(thumb);
        } else {
            holder.thumbnail.setImageResource(R.drawable.ic_tab_placeholder);
        }

        holder.itemView.setOnClickListener(v -> selectListener.onTabSelected(position));
        holder.closeBtn.setOnClickListener(v -> {
            int pos = holder.getAdapterPosition();
            if (pos != RecyclerView.NO_ID) {
                tabs.remove(pos);
                notifyItemRemoved(pos);
                closeListener.onTabClosed(pos);
            }
        });
    }

    @Override
    public int getItemCount() { return tabs.size(); }

    public void setThumbnail(String url, Bitmap bitmap) {
        thumbnails.put(url, bitmap);
    }

    public void updateActiveIndex(int index) {
        int prev = activeIndex;
        activeIndex = index;
        notifyItemChanged(prev);
        notifyItemChanged(index);
    }

    static class TabViewHolder extends RecyclerView.ViewHolder {
        TextView title, url;
        ImageView thumbnail, incognitoBadge;
        ImageButton closeBtn;

        TabViewHolder(View v) {
            super(v);
            title         = v.findViewById(R.id.tab_title);
            url           = v.findViewById(R.id.tab_url);
            thumbnail     = v.findViewById(R.id.tab_thumbnail);
            incognitoBadge = v.findViewById(R.id.tab_incognito);
            closeBtn      = v.findViewById(R.id.tab_close);
        }
    }
}
