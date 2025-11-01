package com.pranav.synctask.adapters;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast; // Added for view-only feedback

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.pranav.synctask.R;
import com.pranav.synctask.activities.EditTaskActivity;
// import com.pranav.synctask.data.TaskRepository; // No longer needed for status/delete
import com.pranav.synctask.models.Task;
// import com.pranav.synctask.utils.DateUtils; // No longer needed for due date
import java.util.List;
import java.util.Objects;
public class TaskAdapter extends RecyclerView.Adapter<TaskAdapter.TaskViewHolder> {

    private List<Task> taskList;
    private final Context context;
    private final String currentUserId;
    private int lastPosition = -1;

    // --- ADDED IN PHASE 4A ---
    private final OnTaskActionListener listener;

    public interface OnTaskActionListener {
        void onTaskClick(Task task);
        void onTaskLongClick(Task task, View view);
    }
    // --- END ADDED ---

    // --- MODIFIED IN PHASE 4A ---
    public TaskAdapter(Context context, List<Task> taskList, String currentUserId, OnTaskActionListener listener) {
        this.context = context;
        this.taskList = taskList;
        this.currentUserId = currentUserId;
        this.listener = listener;
    }
    // --- END MODIFIED ---

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

        String scope = task.getOwnershipScope();
        if (scope == null) {
            scope = Task.SCOPE_SHARED;
        }

        // --- APPLY UI AND PERMISSIONS ---

        holder.tvTitle.setText(task.getTitle());
        // Set Task Type Icon
        switch (task.getTaskType()) {
            case Task.TYPE_REMINDER:
                holder.ivTaskType.setImageResource(R.drawable.ic_task_type_reminder);
                break;
            case Task.TYPE_UPDATE:
                holder.ivTaskType.setImageResource(R.drawable.ic_task_type_update);
                break;
            case Task.TYPE_TASK:
            default:
                holder.ivTaskType.setImageResource(R.drawable.ic_task_type_task);
                break;
        }

        // Set Task Scope/Variant Icon
        switch (scope) {
            case Task.SCOPE_INDIVIDUAL:
                holder.ivTaskScope.setImageResource(R.drawable.ic_scope_individual);
                break;
            case Task.SCOPE_ASSIGNED:
                holder.ivTaskScope.setImageResource(R.drawable.ic_scope_assigned);
                break;
            case Task.SCOPE_SHARED:
            default:
                holder.ivTaskScope.setImageResource(R.drawable.ic_scope_shared);
                break;
        }

        // --- MODIFIED IN PHASE 4A: New click logic ---
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onTaskClick(task);
            }
        });

        holder.itemView.setOnLongClickListener(v -> {
            if (listener != null) {
                listener.onTaskLongClick(task, v);
            }
            return true;
        });
        // --- END MODIFIED ---

        // Set animation
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

    public void updateTasks(List<Task> newTasks) {
        TaskDiffCallback diffCallback = new TaskDiffCallback(this.taskList, newTasks);
        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(diffCallback);
        this.taskList.clear();
        this.taskList.addAll(newTasks);
        diffResult.dispatchUpdatesTo(this);
    }

    static class TaskViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle;
        ImageView ivTaskType, ivTaskScope; // Updated views

        public TaskViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tv_task_title);
            ivTaskType = itemView.findViewById(R.id.iv_task_type);
            ivTaskScope = itemView.findViewById(R.id.iv_task_scope); // Added
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
            if (!oldTask.isSynced() || !newTask.isSynced()) {
                return Objects.equals(oldTask.getLocalId(), newTask.getLocalId());
            }
            return oldTask.getId().equals(newTask.getId());
        }

        @Override
        public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
            Task oldTask = oldList.get(oldItemPosition);
            Task newTask = newList.get(newItemPosition);
            // Updated to only check fields that are still relevant to the adapter's logic
            return Objects.equals(oldTask.getTitle(), newTask.getTitle()) &&
                    Objects.equals(oldTask.getTaskType(), newTask.getTaskType()) && // Added check
                    Objects.equals(oldTask.getOwnershipScope(), newTask.getOwnershipScope()) &&
                    oldTask.isSynced()
                            == newTask.isSynced();
        }
    }
}