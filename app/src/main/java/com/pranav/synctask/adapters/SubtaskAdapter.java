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

        // 1. Reset listener to prevent triggers during scrolling/binding
        holder.checkbox.setOnCheckedChangeListener(null);

        // 2. Set visual state for completion (Strikethrough)
        holder.checkbox.setChecked(subtask.isCompleted());
        if (subtask.isCompleted()) {
            holder.tvTitle.setPaintFlags(holder.tvTitle.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            holder.tvTitle.setAlpha(0.6f);
        } else {
            holder.tvTitle.setPaintFlags(holder.tvTitle.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
            holder.tvTitle.setAlpha(1.0f);
        }

        // 3. LOCKING LOGIC
        // Check if lockedByUid is null or empty string
        boolean isLockedBySomeone = subtask.getLockedByUid() != null && !subtask.getLockedByUid().isEmpty();
        boolean isLockedByMe = isLockedBySomeone && subtask.getLockedByUid().equals(currentUserId);

        if (!isLockedBySomeone) {
            // STATE: UNLOCKED / FREE
            // Behavior: Show open lock. Checkbox DISABLED (User must lock it first to work on it).
            holder.ivLock.setImageResource(android.R.drawable.ic_lock_idle_lock); // Or use a specific open lock icon
            holder.ivLock.setColorFilter(ContextCompat.getColor(context, R.color.md_theme_light_outline));
            holder.ivLock.setAlpha(0.4f);

            holder.tvLockedBy.setVisibility(View.GONE);

            // Logic change: User must click lock to claim, cannot just check box
            holder.checkbox.setEnabled(false);
            holder.ivLock.setOnClickListener(v -> listener.onLockToggle(subtask));

        } else if (isLockedByMe) {
            // STATE: LOCKED BY ME
            // Behavior: Green Lock. Checkbox ENABLED.
            holder.ivLock.setImageResource(android.R.drawable.ic_lock_lock);
            holder.ivLock.setColorFilter(ContextCompat.getColor(context, R.color.priority_low)); // Green
            holder.ivLock.setAlpha(1.0f);

            holder.tvLockedBy.setVisibility(View.VISIBLE);
            holder.tvLockedBy.setText("Locked by You");
            holder.tvLockedBy.setTextColor(ContextCompat.getColor(context, R.color.priority_low));

            holder.checkbox.setEnabled(true);
            holder.ivLock.setOnClickListener(v -> listener.onLockToggle(subtask)); // Click to unlock/release

        } else {
            // STATE: LOCKED BY OTHER
            // Behavior: Red Lock. Checkbox DISABLED.
            holder.ivLock.setImageResource(android.R.drawable.ic_lock_lock);
            holder.ivLock.setColorFilter(ContextCompat.getColor(context, R.color.priority_high)); // Red
            holder.ivLock.setAlpha(1.0f);

            String lockerName = subtask.getLockedByName() != null ? subtask.getLockedByName() : "Partner";
            holder.tvLockedBy.setVisibility(View.VISIBLE);
            holder.tvLockedBy.setText("Locked by " + lockerName);
            holder.tvLockedBy.setTextColor(ContextCompat.getColor(context, R.color.priority_high));

            holder.checkbox.setEnabled(false); // Cannot touch

            if (isAdmin) {
                // Admin can force unlock
                holder.ivLock.setOnClickListener(v -> listener.onLockToggle(subtask));
            } else {
                holder.ivLock.setOnClickListener(v -> {
                    // Optional: Toast "Task is being worked on by X"
                });
            }
        }

        // 4. Re-attach Checkbox Listener
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