package com.pranav.synctask.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.pranav.synctask.R;
import com.pranav.synctask.models.Task;
import com.pranav.synctask.utils.DateUtils;
import com.pranav.synctask.utils.FirebaseHelper;
import java.util.List;
import java.util.Objects;

public class TaskAdapter extends RecyclerView.Adapter<TaskAdapter.TaskViewHolder> {

    private List<Task> taskList;
    private final Context context;
    private final String currentUserId;

    public TaskAdapter(Context context, List<Task> taskList, String currentUserId) {
        this.context = context;
        this.taskList = taskList;
        this.currentUserId = currentUserId;
    }

    @NonNull
    @Override
    public TaskViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_task, parent, false);
        return new TaskViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TaskViewHolder holder, int position) {
        Task task = taskList.get(position);

        holder.tvTitle.setText(task.getTitle());
        holder.tvDescription.setText(task.getDescription());

        if (task.getDueDate() != null) {
            holder.tvDueDate.setText(DateUtils.formatDate(task.getDueDate()));
            holder.tvDueDate.setVisibility(View.VISIBLE);
        } else {
            holder.tvDueDate.setVisibility(View.GONE);
        }

        boolean isMyTask = currentUserId != null && currentUserId.equals(task.getCreatorUID());

        // Set who created the task
        holder.tvCreator.setText(isMyTask ? R.string.task_creator_label_you : R.string.task_creator_label_partner);

        // Set side bar color
        holder.sideBar.setBackgroundColor(isMyTask ?
                context.getResources().getColor(R.color.my_task_bg, null) :
                context.getResources().getColor(R.color.partner_task_bg, null));

        // Set task type icon
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


        // Set checkbox status without triggering the listener
        holder.cbStatus.setOnCheckedChangeListener(null);
        holder.cbStatus.setChecked(Task.STATUS_COMPLETED.equals(task.getStatus()));
        holder.cbStatus.setEnabled(isMyTask);

        // Re-attach the listener
        holder.cbStatus.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (buttonView.isPressed()) {
                String newStatus = isChecked ? Task.STATUS_COMPLETED : Task.STATUS_PENDING;
                FirebaseHelper.updateTaskStatus(task.getId(), newStatus);
            }
        });

        // Set delete button visibility and action
        holder.ivDelete.setVisibility(isMyTask ? View.VISIBLE : View.GONE);
        if (isMyTask) {
            holder.ivDelete.setOnClickListener(v -> FirebaseHelper.deleteTask(task.getId()));
        } else {
            holder.ivDelete.setOnClickListener(null);
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
        ImageView ivDelete, ivTaskType;
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
            sideBar = itemView.findViewById(R.id.side_bar);
        }
    }

    // DiffUtil Callback to improve RecyclerView performance
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
            return oldList.get(oldItemPosition).getId().equals(newList.get(newItemPosition).getId());
        }

        @Override
        public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
            Task oldTask = oldList.get(oldItemPosition);
            Task newTask = newList.get(newItemPosition);
            return Objects.equals(oldTask.getTitle(), newTask.getTitle()) &&
                    Objects.equals(oldTask.getDescription(), newTask.getDescription()) &&
                    Objects.equals(oldTask.getStatus(), newTask.getStatus()) &&
                    Objects.equals(oldTask.getDueDate(), newTask.getDueDate());
        }
    }
}