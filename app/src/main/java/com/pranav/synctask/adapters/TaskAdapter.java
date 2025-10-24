package com.pranav.synctask.adapters;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.pranav.synctask.R;
import com.pranav.synctask.activities.EditTaskActivity;
import com.pranav.synctask.data.TaskRepository;
import com.pranav.synctask.models.Task;
import com.pranav.synctask.utils.DateUtils;
import java.util.List;
import java.util.Objects;

public class TaskAdapter extends RecyclerView.Adapter<TaskAdapter.TaskViewHolder> {

    private List<Task> taskList;
    private final Context context;
    private final String currentUserId;
    private int lastPosition = -1;

    public TaskAdapter(Context context, List<Task> taskList, String currentUserId) {
        this.context = context;
        this.taskList = taskList;
        this.currentUserId = currentUserId;
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

        // --- PERMISSIONS LOGIC (PHASE 2) ---
        String scope = task.getOwnershipScope();
        // Fallback for older tasks that might not have a scope
        if (scope == null) {
            scope = Task.SCOPE_SHARED;
        }

        boolean isCreator = currentUserId != null && currentUserId.equals(task.getCreatorUID());

        boolean canEdit = false;
        boolean canComplete = false;
        boolean canDelete = false;

        switch (scope) {
            case Task.SCOPE_INDIVIDUAL:
                // Only the creator can see, edit, complete, and delete.
                // Since tasks are filtered in the repository (eventually),
                // we assume if we see it, we are the creator.
                canEdit = true;
                canComplete = true;
                canDelete = true;
                break;
            case Task.SCOPE_SHARED:
                // Both users can do everything
                canEdit = true;
                canComplete = true;
                canDelete = true;
                break;
            case Task.SCOPE_ASSIGNED:
                if (isCreator) {
                    // I created it, I can edit/delete but not complete
                    canEdit = true;
                    canDelete = true;
                    canComplete = false; // Only assignee can complete
                } else {
                    // It's assigned to me. I can complete, but not edit/delete
                    canEdit = false;
                    canDelete = false;
                    canComplete = true;
                }
                break;
        }

        // --- END PERMISSIONS LOGIC ---


        // --- APPLY UI AND PERMISSIONS ---

        holder.tvTitle.setText(task.getTitle());
        holder.tvDescription.setText(task.getDescription());
        holder.tvDescription.setVisibility(task.getDescription().isEmpty() ? View.GONE : View.VISIBLE);

        if (task.getDueDate() != null) {
            holder.tvDueDate.setText(DateUtils.formatDate(task.getDueDate()));
            holder.tvDueDate.setVisibility(View.VISIBLE);
        } else {
            holder.tvDueDate.setVisibility(View.GONE);
        }

        holder.tvCreator.setText(isCreator ? R.string.task_creator_label_you : R.string.task_creator_label_partner);
        holder.sideBar.setBackgroundColor(isCreator ?
                context.getResources().getColor(R.color.my_task_bg, null) :
                context.getResources().getColor(R.color.partner_task_bg, null));

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

        holder.ivPriority.setVisibility(View.VISIBLE);
        switch (task.getPriority()) {
            case "High":
                holder.ivPriority.setImageResource(R.drawable.ic_priority_high);
                break;
            case "Low":
                holder.ivPriority.setImageResource(R.drawable.ic_priority_low);
                break;
            default: // Normal priority
                holder.ivPriority.setVisibility(View.GONE);
                break;
        }

        if (task.isSynced()) {
            holder.itemView.setAlpha(1.0f);
            holder.tvCreator.setText(isCreator ? R.string.task_creator_label_you : R.string.task_creator_label_partner);
        } else {
            holder.itemView.setAlpha(0.7f);
            holder.tvCreator.setText(R.string.task_creator_label_local);
        }

        // Apply completion permission
        holder.cbStatus.setEnabled(canComplete || task.isSynced());
        holder.cbStatus.setOnCheckedChangeListener(null);
        holder.cbStatus.setChecked(Task.STATUS_COMPLETED.equals(task.getStatus()));
        holder.cbStatus.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (buttonView.isPressed()) {
                String newStatus = isChecked ? Task.STATUS_COMPLETED : Task.STATUS_PENDING;
                TaskRepository.getInstance().updateTaskStatus(task.getId(), newStatus);
            }
        });

        // Apply deletion permission
        holder.ivDelete.setVisibility(canDelete ? View.VISIBLE : View.GONE);
        if (canDelete) {
            holder.ivDelete.setOnClickListener(v -> TaskRepository.getInstance().deleteTask(task.getId()));
        } else {
            holder.ivDelete.setOnClickListener(null);
        }

        // Apply edit/view permission
        if (canEdit) {
            holder.itemView.setOnClickListener(v -> {
                Intent intent = new Intent(context, EditTaskActivity.class);
                intent.putExtra(EditTaskActivity.EXTRA_TASK, task);
                context.startActivity(intent);
            });
        } else {
            // It's view-only
            holder.itemView.setOnClickListener(null);
        }

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
        TextView tvTitle, tvDescription, tvDueDate, tvCreator;
        CheckBox cbStatus;
        ImageView ivDelete, ivTaskType, ivPriority;
        View sideBar;

        public TaskViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tv_task_title);
            tvDescription = itemView.findViewById(R.id.tv_task_description);
            tvDueDate = itemView.findViewById(R.id.tv_task_due_date);
            tvCreator = itemView.findViewById(R.id.tv_task_creator);
            cbStatus = itemView.findViewById(R.id.cb_task_status);
            ivDelete = itemView.findViewById(R.id.iv_delete_task);
            ivTaskType = itemView.findViewById(R.id.iv_task_type);
            ivPriority = itemView.findViewById(R.id.iv_task_priority);
            sideBar = itemView.findViewById(R.id.side_bar);
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
            return Objects.equals(oldTask.getTitle(), newTask.getTitle()) &&
                    Objects.equals(oldTask.getDescription(), newTask.getDescription()) &&
                    Objects.equals(oldTask.getStatus(), newTask.getStatus()) &&
                    Objects.equals(oldTask.getDueDate(), newTask.getDueDate()) &&
                    Objects.equals(oldTask.getPriority(), newTask.getPriority()) &&
                    Objects.equals(oldTask.getOwnershipScope(), newTask.getOwnershipScope()) && // ADDED
                    oldTask.isSynced() == newTask.isSynced();
        }
    }
}