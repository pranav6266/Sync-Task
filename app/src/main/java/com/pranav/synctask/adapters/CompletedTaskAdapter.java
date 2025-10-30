package com.pranav.synctask.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.pranav.synctask.R;
import com.pranav.synctask.models.Task;

import java.util.List;
import java.util.Objects;

public class CompletedTaskAdapter extends RecyclerView.Adapter<CompletedTaskAdapter.TaskViewHolder> {

    private List<Task> taskList;
    private final Context context;
    private final OnTaskActionClickListener actionListener;

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

        holder.tvTitle.setText(task.getTitle());
        // Make text appear "completed"
        holder.tvTitle.setAlpha(0.7f);

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
        String scope = task.getOwnershipScope();
        if (scope == null) scope = Task.SCOPE_SHARED;

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

        // --- Context Menu on Long Press ---
        holder.itemView.setOnLongClickListener(v -> {
            showPopupMenu(v, task);
            return true;
        });

        holder.itemView.setOnClickListener(v -> {
            Toast.makeText(context, "Long-press for options", Toast.LENGTH_SHORT).show();
        });
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
        TextView tvTitle;
        ImageView ivTaskType, ivTaskScope;

        public TaskViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tv_task_title);
            ivTaskType = itemView.findViewById(R.id.iv_task_type);
            ivTaskScope = itemView.findViewById(R.id.iv_task_scope);
        }
    }

    // DiffUtil to animate changes
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
            // Only check fields relevant to this adapter
            return Objects.equals(oldTask.getTitle(), newTask.getTitle()) &&
                    Objects.equals(oldTask.getTaskType(), newTask.getTaskType()) &&
                    Objects.equals(oldTask.getOwnershipScope(), newTask.getOwnershipScope()) &&
                    Objects.equals(oldTask.getStatus(), newTask.getStatus());
        }
    }
}