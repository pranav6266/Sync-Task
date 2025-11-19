package com.pranav.synctask.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.PopupMenu;
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

public class CompletedTaskAdapter extends RecyclerView.Adapter<CompletedTaskAdapter.TaskViewHolder> {

    private List<Task> taskList;
    private final Context context;
    private final OnTaskActionClickListener actionListener;
    private int lastPosition = -1;

    public interface OnTaskActionClickListener {
        void onRestoreTask(Task task);
        void onDeleteTask(Task task);
    }

    public CompletedTaskAdapter(Context context, List<Task> taskList, OnTaskActionClickListener listener) {
        this.context = context;
        this.taskList = taskList;
        this.actionListener = listener;
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

        // 1. Title (Dimmed for completed tasks)
        holder.tvTitle.setText(task.getTitle());
        holder.tvTitle.setAlpha(0.6f); // Visual cue for completed

        // 2. Date
        if (task.getDueDate() != null) {
            holder.tvDate.setText(DateUtils.formatDate(task.getDueDate()));
            holder.tvDate.setVisibility(View.VISIBLE);
            holder.ivCalendar.setVisibility(View.VISIBLE);
        } else {
            holder.tvDate.setText("No Due Date");
            holder.tvDate.setVisibility(View.VISIBLE);
            holder.ivCalendar.setVisibility(View.VISIBLE);
        }

        // 3. Priority Strip Color (Dimmed)
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
        holder.viewPriorityStrip.setAlpha(0.5f); // Dimmed strip

        // 4. Scope/Context Logic
        String scope = task.getOwnershipScope();
        if (scope == null) scope = Task.SCOPE_SHARED;

        // Default styling
        int chipBg = R.color.chip_bg_shared;
        int chipText = R.color.chip_text_shared;
        int chipIcon = R.drawable.ic_scope_shared;
        String scopeText = "Shared";

        if (Task.SCOPE_ASSIGNED.equals(scope)) {
            scopeText = "Assigned";
            chipBg = R.color.chip_bg_assigned;
            chipText = R.color.chip_text_assigned;
            chipIcon = R.drawable.ic_scope_assigned;
        } else if (Task.SCOPE_INDIVIDUAL.equals(scope)) {
            scopeText = "Individual";
            chipBg = R.color.chip_bg_personal;
            chipText = R.color.chip_text_personal;
            chipIcon = R.drawable.ic_scope_individual;
        }

        holder.cardScopeChip.setCardBackgroundColor(ContextCompat.getColor(context, chipBg));
        holder.tvScopeText.setTextColor(ContextCompat.getColor(context, chipText));
        holder.tvScopeText.setText(scopeText);
        holder.ivScopeIcon.setImageResource(chipIcon);
        holder.ivScopeIcon.setColorFilter(ContextCompat.getColor(context, chipText));
        holder.cardScopeChip.setAlpha(0.7f); // Dimmed chip

        // 5. Task Type
        String typeText = task.getTaskType();
        if(typeText == null) typeText = "Task";
        typeText = typeText.substring(0, 1).toUpperCase() + typeText.substring(1).toLowerCase();
        holder.tvTypeText.setText(typeText);

        // 6. Creator Name
        holder.tvCreatedBy.setText("Created by " + task.getCreatorDisplayName());


        // 7. Menu Click Listener (The 3 Dots)
        holder.ivOptionsMenu.setOnClickListener(v -> showPopupMenu(v, task));

        // Animation
        setAnimation(holder.itemView, position);
    }

    private void showPopupMenu(View view, Task task) {
        PopupMenu popup = new PopupMenu(context, view);
        popup.getMenuInflater().inflate(R.menu.completed_task_menu, popup.getMenu());
        popup.setOnMenuItemClickListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.action_restore_task) {
                actionListener.onRestoreTask(task);
                return true;
            } else if (itemId == R.id.action_delete_permanently) {
                actionListener.onDeleteTask(task);
                return true;
            }
            return false;
        });
        popup.show();
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

    public void updateTasks(List<Task> newTasks) {
        CompletedTaskDiffCallback diffCallback = new CompletedTaskDiffCallback(this.taskList, newTasks);
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
            // Matches the IDs in the NEW item_task.xml
            viewPriorityStrip = itemView.findViewById(R.id.view_priority_strip);
            tvTitle = itemView.findViewById(R.id.tv_task_title);
            tvDate = itemView.findViewById(R.id.tv_task_date);
            tvCreatedBy = itemView.findViewById(R.id.tv_created_by);

            cardScopeChip = itemView.findViewById(R.id.card_scope_chip);
            tvScopeText = itemView.findViewById(R.id.tv_chip_text);
            ivScopeIcon = itemView.findViewById(R.id.iv_chip_icon);

            tvTypeText = itemView.findViewById(R.id.tv_type_text);

            ivCalendar = itemView.findViewById(R.id.iv_calendar_icon);
            ivOptionsMenu = itemView.findViewById(R.id.iv_options_menu);
        }
    }

    private static class CompletedTaskDiffCallback extends DiffUtil.Callback {
        private final List<Task> oldList;
        private final List<Task> newList;

        public CompletedTaskDiffCallback(List<Task> oldList, List<Task> newList) {
            this.oldList = oldList;
            this.newList = newList;
        }

        @Override
        public int getOldListSize() {
            return oldList.size();
        }

        @Override
        public int getNewListSize() {
            return newList.size();
        }

        @Override
        public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
            Task oldTask = oldList.get(oldItemPosition);
            Task newTask = newList.get(newItemPosition);
            if (oldTask.getId() == null || newTask.getId() == null) {
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
                    Objects.equals(oldTask.getStatus(), newTask.getStatus());
        }
    }
}