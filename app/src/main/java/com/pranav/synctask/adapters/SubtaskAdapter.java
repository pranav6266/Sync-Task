package com.pranav.synctask.adapters;

import android.content.Context;
import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.checkbox.MaterialCheckBox;
import com.pranav.synctask.R;
import com.pranav.synctask.models.Subtask;
import java.util.List;

public class SubtaskAdapter extends RecyclerView.Adapter<SubtaskAdapter.ViewHolder> {

    private List<Subtask> subtasks;
    private final String currentUserId;
    private final boolean isAdmin;
    private final OnSubtaskActionListener listener;
    private final Context context;

    public interface OnSubtaskActionListener {
        void onLockToggle(Subtask subtask);
        void onCompletionToggle(Subtask subtask, boolean isChecked);
    }

    public SubtaskAdapter(Context context, List<Subtask> subtasks, String currentUserId, boolean isAdmin, OnSubtaskActionListener listener) {
        this.context = context;
        this.subtasks = subtasks;
        this.currentUserId = currentUserId;
        this.isAdmin = isAdmin;
        this.listener = listener;
    }

    public void updateSubtasks(List<Subtask> newSubtasks) {
        this.subtasks = newSubtasks;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_subtask, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Subtask subtask = subtasks.get(position);

        holder.tvTitle.setText(subtask.getTitle());

        // Reset listener to prevent triggers during binding
        holder.checkbox.setOnCheckedChangeListener(null);
        holder.checkbox.setChecked(subtask.isCompleted());

        // Strikethrough if completed
        if (subtask.isCompleted()) {
            holder.tvTitle.setPaintFlags(holder.tvTitle.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            holder.tvTitle.setAlpha(0.6f);
        } else {
            holder.tvTitle.setPaintFlags(holder.tvTitle.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
            holder.tvTitle.setAlpha(1.0f);
        }

        // --- LOCKING LOGIC ---
        boolean isLockedByMe = subtask.getLockedByUid() != null && subtask.getLockedByUid().equals(currentUserId);
        boolean isLockedByOther = subtask.getLockedByUid() != null && !subtask.getLockedByUid().equals(currentUserId);
        boolean isUnlocked = subtask.getLockedByUid() == null;

        if (isUnlocked) {
            // State: FREE FOR ALL
            holder.ivLock.setImageResource(R.drawable.ic_lock_open); // You might need an open lock icon, using delete/edit as placeholder if missing
            // If ic_lock_open doesn't exist, use a dimmed lock or specialized icon
            holder.ivLock.setImageResource(android.R.drawable.ic_menu_more); // Fallback if icon missing, ideally use R.drawable.ic_lock_open
            // Actually, let's use the standard lock but tinted grey to signify "Open"
            holder.ivLock.setImageResource(android.R.drawable.ic_lock_idle_lock);
            holder.ivLock.setAlpha(0.3f);

            holder.tvLockedBy.setVisibility(View.GONE);
            holder.checkbox.setEnabled(false); // Must lock first

            holder.ivLock.setOnClickListener(v -> listener.onLockToggle(subtask));

        } else if (isLockedByMe) {
            // State: LOCKED BY ME (Working on it)
            holder.ivLock.setImageResource(android.R.drawable.ic_lock_lock);
            holder.ivLock.setColorFilter(ContextCompat.getColor(context, R.color.priority_low)); // Green lock
            holder.ivLock.setAlpha(1.0f);

            holder.tvLockedBy.setVisibility(View.VISIBLE);
            holder.tvLockedBy.setText("Locked by You");
            holder.tvLockedBy.setTextColor(ContextCompat.getColor(context, R.color.priority_low));

            holder.checkbox.setEnabled(true); // Can complete
            holder.ivLock.setOnClickListener(v -> listener.onLockToggle(subtask)); // Can unlock

        } else if (isLockedByOther) {
            // State: LOCKED BY SOMEONE ELSE
            holder.ivLock.setImageResource(android.R.drawable.ic_lock_lock);
            holder.ivLock.setColorFilter(ContextCompat.getColor(context, R.color.priority_high)); // Red lock
            holder.ivLock.setAlpha(1.0f);

            holder.tvLockedBy.setVisibility(View.VISIBLE);
            holder.tvLockedBy.setText("Locked by " + (subtask.getLockedByName() != null ? subtask.getLockedByName() : "Partner"));
            holder.tvLockedBy.setTextColor(ContextCompat.getColor(context, R.color.priority_high));

            holder.checkbox.setEnabled(false); // Cannot complete

            if (isAdmin) {
                // Admin Override
                holder.ivLock.setOnClickListener(v -> listener.onLockToggle(subtask)); // Force unlock
            } else {
                holder.ivLock.setOnClickListener(null); // Cannot touch
            }
        }

        // Re-attach listener
        holder.checkbox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (buttonView.isPressed()) {
                listener.onCompletionToggle(subtask, isChecked);
            }
        });
    }

    @Override
    public int getItemCount() {
        return subtasks != null ? subtasks.size() : 0;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvLockedBy;
        ImageView ivLock;
        MaterialCheckBox checkbox;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tv_subtask_title);
            tvLockedBy = itemView.findViewById(R.id.tv_locked_by);
            ivLock = itemView.findViewById(R.id.iv_lock_status);
            checkbox = itemView.findViewById(R.id.cb_subtask_complete);
        }
    }
}