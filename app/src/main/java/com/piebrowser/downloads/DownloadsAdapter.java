package com.piebrowser.downloads;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.piebrowser.R;

import java.util.List;

/**
 * RecyclerView adapter for the Downloads screen.
 * Shows filename, size/progress, status, and open/delete actions.
 */
public class DownloadsAdapter extends RecyclerView.Adapter<DownloadsAdapter.ViewHolder> {

    public interface OnOpenListener  { void onOpen(DownloadItem item); }
    public interface OnDeleteListener { void onDelete(DownloadItem item); }

    private final List<DownloadItem> items;
    private final OnOpenListener openListener;
    private final OnDeleteListener deleteListener;

    public DownloadsAdapter(List<DownloadItem> items,
                            OnOpenListener openListener,
                            OnDeleteListener deleteListener) {
        this.items = items;
        this.openListener = openListener;
        this.deleteListener = deleteListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_download, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        DownloadItem item = items.get(position);

        holder.filename.setText(item.getFilename());
        holder.progress.setText(item.getProgressString());
        holder.status.setText(statusText(item.getStatus()));

        int percent = item.getProgressPercent();
        if (percent >= 0) {
            holder.progressBar.setVisibility(View.VISIBLE);
            holder.progressBar.setProgress(percent);
        } else {
            holder.progressBar.setVisibility(View.GONE);
        }

        // Only allow opening completed downloads
        boolean completed = item.getStatus() == DownloadItem.Status.COMPLETED;
        holder.openBtn.setEnabled(completed);
        holder.openBtn.setAlpha(completed ? 1f : 0.4f);

        holder.openBtn.setOnClickListener(v -> openListener.onOpen(item));
        holder.deleteBtn.setOnClickListener(v -> deleteListener.onDelete(item));
    }

    @Override
    public int getItemCount() { return items.size(); }

    private String statusText(DownloadItem.Status status) {
        switch (status) {
            case QUEUED:    return "Queued";
            case RUNNING:   return "Downloading…";
            case PAUSED:    return "Paused";
            case COMPLETED: return "Complete";
            case FAILED:    return "Failed";
            case CANCELLED: return "Cancelled";
            default:        return "";
        }
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView filename, progress, status;
        ProgressBar progressBar;
        ImageButton openBtn, deleteBtn;

        ViewHolder(View itemView) {
            super(itemView);
            filename    = itemView.findViewById(R.id.download_filename);
            progress    = itemView.findViewById(R.id.download_progress_text);
            status      = itemView.findViewById(R.id.download_status);
            progressBar = itemView.findViewById(R.id.download_progress_bar);
            openBtn     = itemView.findViewById(R.id.download_open);
            deleteBtn   = itemView.findViewById(R.id.download_delete);
        }
    }
}
