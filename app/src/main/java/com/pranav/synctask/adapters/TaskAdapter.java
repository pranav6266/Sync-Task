package com.pranav.synctask.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.card.MaterialCardView;
import com.pranav.synctask.R;
import com.pranav.synctask.models.Space;
import com.pranav.synctask.models.Task;
import com.pranav.synctask.utils.DateUtils;
import java.util.List;
import java.util.Objects;

public class TaskAdapter extends RecyclerView.Adapter<TaskAdapter.TaskViewHolder> {

    private final String contextType;
    private List<Task> taskList;
    private final Context context;
    private final String currentUserId;
    private final OnTaskActionListener listener;
    private int lastPosition = -1;

    public interface OnTaskActionListener {
        void onTaskClick(Task task);
        void onTaskLongClick(Task task, View view);
    }

    public TaskAdapter(String contextType, Context context, List<Task> taskList, String currentUserId, OnTaskActionListener listener) {
        this.contextType = contextType;
        this.context = context;
        this.taskList = taskList;
        this.currentUserId = currentUserId;
        this.listener = listener;
    }

    @NonNull
    @Override
    public TaskViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_task, parent, false);
        return new TaskViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TaskViewHolder holder, int position) {
        Task task = taskList.get(position);
        if (task == null) return;

        // 1. Title
        holder.tvTitle.setText(task.getTitle());

        // 2. Date Logic (UPDATED)
        if (task.getDueDate() != null) {
            // Use the new Relative Date ("Tomorrow", "Yesterday")
            holder.tvDate.setText(com.pranav.synctask.utils.DateUtils.getRelativeDate(task.getDueDate()));
            holder.tvDate.setVisibility(View.VISIBLE);
            holder.ivCalendar.setVisibility(View.VISIBLE);

            // Check for Overdue
            if (com.pranav.synctask.utils.DateUtils.isOverdue(task.getDueDate())
                    && !Task.STATUS_COMPLETED.equals(task.getStatus())) {

                // Task is late! Make it RED.
                int errorColor = ContextCompat.getColor(context, R.color.md_theme_light_error);
                holder.tvDate.setTextColor(errorColor);
                holder.ivCalendar.setColorFilter(errorColor);
                holder.tvDate.setText(holder.tvDate.getText() + " (Overdue)");
                holder.tvDate.setTypeface(null, android.graphics.Typeface.BOLD);
            } else {
                // Normal Date
                int normalColor = ContextCompat.getColor(context, R.color.md_theme_light_onSurfaceVariant); // Or your variant color
                holder.tvDate.setTextColor(normalColor);
                holder.ivCalendar.setColorFilter(normalColor);
                holder.tvDate.setTypeface(null, android.graphics.Typeface.NORMAL);
            }

        } else {
            holder.tvDate.setText("No Due Date");
            holder.tvDate.setVisibility(View.VISIBLE);
            holder.ivCalendar.setVisibility(View.VISIBLE);

            // Reset colors for recycled views
            int normalColor = ContextCompat.getColor(context, R.color.md_theme_light_onSurfaceVariant);
            holder.tvDate.setTextColor(normalColor);
            holder.ivCalendar.setColorFilter(normalColor);
        }

        // 3. Priority Strip Color
        String priority = task.getPriority();
        int priorityColor;
        if ("High".equalsIgnoreCase(priority)) {
            priorityColor = ContextCompat.getColor(context, R.color.priority_high);
        } else if ("Low".equalsIgnoreCase(priority)) {
            priorityColor = ContextCompat.getColor(context, R.color.priority_low);
        } else {
            priorityColor = ContextCompat.getColor(context, R.color.priority_normal);
        }
        holder.viewPriorityStrip.setBackgroundColor(priorityColor);

        // 4. Scope/Context Logic (The "Chip")
        String scope = task.getOwnershipScope();
        if (scope == null) scope = Task.SCOPE_SHARED;

        // Default styling
        int chipBg = R.color.chip_bg_shared;
        int chipText = R.color.chip_text_shared;
        int chipIcon = R.drawable.ic_scope_shared;
        String scopeText = "Shared";

        if (Space.TYPE_PERSONAL.equals(contextType)) {
            // Personal Space Logic
            chipBg = R.color.chip_bg_personal;
            chipText = R.color.chip_text_personal;
            chipIcon = R.drawable.ic_scope_individual;

            if (currentUserId != null && currentUserId.equals(task.getCreatorUID())) {
                scopeText = "My Task";
            } else {
                scopeText = "Partner's Task";
                // Make partner tasks stand out with Assigned styling
                chipBg = R.color.chip_bg_assigned;
                chipText = R.color.chip_text_assigned;
                chipIcon = R.drawable.ic_scope_assigned;
            }
        } else {
            // Shared Space Logic
            if (Task.SCOPE_ASSIGNED.equals(scope)) {
                scopeText = "Assigned";
                chipBg = R.color.chip_bg_assigned;
                chipText = R.color.chip_text_assigned;
                chipIcon = R.drawable.ic_scope_assigned;
            } else {
                scopeText = "Shared";
                chipBg = R.color.chip_bg_shared;
                chipText = R.color.chip_text_shared;
                chipIcon = R.drawable.ic_scope_shared;
            }
        }

        holder.cardScopeChip.setCardBackgroundColor(ContextCompat.getColor(context, chipBg));
        holder.tvScopeText.setTextColor(ContextCompat.getColor(context, chipText));
        holder.tvScopeText.setText(scopeText);
        holder.ivScopeIcon.setImageResource(chipIcon);
        holder.ivScopeIcon.setColorFilter(ContextCompat.getColor(context, chipText));

        // 5. Task Type
        String typeText = task.getTaskType(); // TASK, REMINDER, UPDATE
        if(typeText == null) typeText = "Task";
        // Capitalize first letter only
        typeText = typeText.substring(0, 1).toUpperCase() + typeText.substring(1).toLowerCase();
        holder.tvTypeText.setText(typeText);


        // 6. Creator Name
        boolean isMe = currentUserId != null && currentUserId.equals(task.getCreatorUID());
        String creatorText = isMe ? "Created by You" : "Created by " + task.getCreatorDisplayName();
        holder.tvCreatedBy.setText(creatorText);


        // 7. Listeners
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onTaskClick(task);
        });

        // Use the specific 3-dot menu for the long press action usually,
        // but here we attach it to the specific view
        holder.ivOptionsMenu.setOnClickListener(v -> {
            if (listener != null) listener.onTaskLongClick(task, holder.ivOptionsMenu);
        });

        // Animation
        setAnimation(holder.itemView, position);
    }

    private void setAnimation(View viewToAnimate, int position) {
        if (position > lastPosition) {
            Animation animation = AnimationUtils.loadAnimation(context, R.anim.fade_in);
            viewToAnimate.startAnimation(animation);
            lastPosition = position;
        }
    }

    @Override
    public int getItemCount() {
        return taskList.size();
    }
    public Task getTaskAt(int position) {
        if (position >= 0 && position < taskList.size()) {
            return taskList.get(position);
        }
        return null;
    }

    public void updateTasks(List<Task> newTasks) {
        TaskDiffCallback diffCallback = new TaskDiffCallback(this.taskList, newTasks);
        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(diffCallback);
        this.taskList.clear();
        this.taskList.addAll(newTasks);
        diffResult.dispatchUpdatesTo(this);
    }

    static class TaskViewHolder extends RecyclerView.ViewHolder {
        View viewPriorityStrip;
        TextView tvTitle, tvDate, tvCreatedBy, tvScopeText, tvTypeText;
        ImageView ivCalendar, ivScopeIcon, ivOptionsMenu;
        MaterialCardView cardScopeChip;

        public TaskViewHolder(@NonNull View itemView) {
            super(itemView);
            viewPriorityStrip = itemView.findViewById(R.id.view_priority_strip);
            tvTitle = itemView.findViewById(R.id.tv_task_title);
            tvDate = itemView.findViewById(R.id.tv_task_date);
            tvCreatedBy = itemView.findViewById(R.id.tv_created_by);

            // Scope Chip internals
            cardScopeChip = itemView.findViewById(R.id.card_scope_chip);
            tvScopeText = itemView.findViewById(R.id.tv_chip_text);
            ivScopeIcon = itemView.findViewById(R.id.iv_chip_icon);

            // Type Chip internals
            tvTypeText = itemView.findViewById(R.id.tv_type_text);

            ivCalendar = itemView.findViewById(R.id.iv_calendar_icon);
            ivOptionsMenu = itemView.findViewById(R.id.iv_options_menu);
        }
    }

    private static class TaskDiffCallback extends DiffUtil.Callback {
        private final List<Task> oldList;
        private final List<Task> newList;

        public TaskDiffCallback(List<Task> oldList, List<Task> newList) {
            this.oldList = oldList;
            this.newList = newList;
        }

        @Override
        public int getOldListSize() { return oldList.size(); }

        @Override
        public int getNewListSize() { return newList.size(); }

        @Override
        public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
            Task oldTask = oldList.get(oldItemPosition);
            Task newTask = newList.get(newItemPosition);
            if (!oldTask.isSynced() || !newTask.isSynced()) {
                return Objects.equals(oldTask.getLocalId(), newTask.getLocalId());
            }
            return oldTask.getId().equals(newTask.getId());
        }

        @Override
        public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
            Task oldTask = oldList.get(oldItemPosition);
            Task newTask = newList.get(newItemPosition);
            return Objects.equals(oldTask.getTitle(), newTask.getTitle()) &&
                    Objects.equals(oldTask.getTaskType(), newTask.getTaskType()) &&
                    Objects.equals(oldTask.getOwnershipScope(), newTask.getOwnershipScope()) &&
                    Objects.equals(oldTask.getPriority(), newTask.getPriority()) &&
                    Objects.equals(oldTask.getStatus(), newTask.getStatus());
        }

    }
}