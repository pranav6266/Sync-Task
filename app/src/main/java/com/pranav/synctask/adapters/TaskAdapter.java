package com.pranav.synctask.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.pranav.synctask.R;
import com.pranav.synctask.models.Task;
import com.pranav.synctask.utils.DateUtils;
import com.pranav.synctask.utils.FirebaseHelper;
import java.util.List;

public class TaskAdapter extends RecyclerView.Adapter<TaskAdapter.TaskViewHolder> {

    private List<Task> taskList;
    private Context context;
    private String currentUserId;

    public TaskAdapter(Context context, List<Task> taskList) {
        this.context = context;
        this.taskList = taskList;
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        this.currentUserId = currentUser != null ? currentUser.getUid() : "";
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

        holder.tvTitle.setText(task.getTitle() != null ? task.getTitle() : "");
        holder.tvDescription.setText(task.getDescription() != null ? task.getDescription() : "");
        holder.tvDueDate.setText(DateUtils.formatDate(task.getDueDate()));

        boolean isMyTask = currentUserId != null && !currentUserId.isEmpty()
                && task.getCreatorUID() != null
                && task.getCreatorUID().equals(currentUserId);

        holder.cbStatus.setChecked(Task.STATUS_COMPLETED.equals(task.getStatus()));
        holder.cbStatus.setEnabled(isMyTask);
        holder.ivDelete.setVisibility(isMyTask ? View.VISIBLE : View.GONE);

        if(isMyTask) {
            holder.itemView.setBackgroundColor(context.getResources().getColor(R.color.my_task_bg, null));
        } else {
            holder.itemView.setBackgroundColor(context.getResources().getColor(R.color.partner_task_bg, null));
        }

        holder.cbStatus.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (buttonView.isPressed()) {
                String newStatus = isChecked ? Task.STATUS_COMPLETED : Task.STATUS_PENDING;
                FirebaseHelper.updateTaskStatus(task.getId(), newStatus);
            }
        });

        holder.ivDelete.setOnClickListener(v -> {
            FirebaseHelper.deleteTask(task.getId());
        });
    }

    @Override
    public int getItemCount() {
        return taskList.size();
    }

    public void updateTasks(List<Task> newTasks) {
        this.taskList.clear();
        this.taskList.addAll(newTasks);
        notifyDataSetChanged();
    }

    static class TaskViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvDescription, tvDueDate;
        CheckBox cbStatus;
        ImageView ivDelete;

        public TaskViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tv_task_title);
            tvDescription = itemView.findViewById(R.id.tv_task_description);
            tvDueDate = itemView.findViewById(R.id.tv_task_due_date);
            cbStatus = itemView.findViewById(R.id.cb_task_status);
            ivDelete = itemView.findViewById(R.id.iv_delete_task);
        }
    }
}
