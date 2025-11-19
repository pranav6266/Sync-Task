package com.pranav.synctask.adapters;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.pranav.synctask.R;
import com.pranav.synctask.activities.TaskViewActivity;
import com.pranav.synctask.models.Space;
import com.pranav.synctask.models.Task;
import com.pranav.synctask.models.User;
import com.pranav.synctask.ui.DashboardViewModel;
// IMPORTANT: Use the custom DateUtils
import com.pranav.synctask.utils.DateUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class PersonalLinksAdapter extends RecyclerView.Adapter<PersonalLinksAdapter.LinkViewHolder> {

    private final Context context;
    private final List<Space> linkList;
    private final String currentUserId;
    private Map<String, User> membersMap;
    private final List<Task> allTasks;

    public PersonalLinksAdapter(Context context, List<Space> linkList, Map<String, User> membersMap, List<Task> allTasks) {
        this.context = context;
        this.linkList = linkList;
        this.membersMap = membersMap;
        this.allTasks = allTasks;
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        this.currentUserId = (user != null) ? user.getUid() : null;
    }

    @NonNull
    @Override
    public LinkViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_personal_links, parent, false);
        return new LinkViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull LinkViewHolder holder, int position) {
        Space link = linkList.get(position);
        if (link == null || currentUserId == null) return;

        // 1. Find Partner Name
        String partnerUid = null;
        for (String memberId : link.getMembers()) {
            if (!memberId.equals(currentUserId)) {
                partnerUid = memberId;
                break;
            }
        }

        String partnerName = "Partner";
        if (partnerUid != null && membersMap != null && membersMap.containsKey(partnerUid)) {
            User partner = membersMap.get(partnerUid);
            if (partner != null && partner.getDisplayName() != null) {
                partnerName = partner.getDisplayName();
            }
        }

        // Update Title
        holder.tvPartnerName.setText("Tasks with " + partnerName);

        // 2. Progress Calculation
        int totalEffort = 0;
        int completedEffort = 0;
        int taskCount = 0;

        for (Task task : allTasks) {
            if (link.getSpaceId().equals(task.getSpaceId())) {

                if (Task.STATUS_PENDING.equals(task.getStatus())) {
                    taskCount++;
                }

                boolean isRelevant = false;
                if (task.getDueDate() != null && DateUtils.isToday(task.getDueDate())) isRelevant = true;
                else if (task.getCreatedAt() != null && DateUtils.isToday(task.getCreatedAt())) isRelevant = true;

                if (isRelevant) {
                    totalEffort += task.getEffort();
                    if (Task.STATUS_COMPLETED.equals(task.getStatus())) {
                        completedEffort += task.getEffort();
                    }
                }
            }
        }

        int progress = (totalEffort == 0) ? 0 : (int) (100.0 * completedEffort / totalEffort);

        if (totalEffort == 0) {
            holder.progressLink.setVisibility(View.GONE);
        } else {
            holder.progressLink.setVisibility(View.VISIBLE);
            holder.progressLink.setProgress(progress, true);
        }

        // 3. Status Text
        holder.tvLinkStatus.setText(taskCount + " Active Tasks");

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, TaskViewActivity.class);
            intent.putExtra("SPACE_ID", link.getSpaceId());
            intent.putExtra("CONTEXT_TYPE", Space.TYPE_PERSONAL);
            context.startActivity(intent);
        });

        holder.ivLinkOptions.setOnClickListener(v -> showOptionsDialog(v, link));
    }

    private void showOptionsDialog(View anchor, Space link) {
        PopupMenu popup = new PopupMenu(context, anchor);
        popup.getMenu().add(Menu.NONE, 1, 1, R.string.unlink_partner_title);
        popup.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == 1) {
                new MaterialAlertDialogBuilder(context)
                        .setTitle(R.string.unlink_partner_title)
                        .setMessage(R.string.unlink_partner_message)
                        .setNegativeButton(R.string.cancel, null)
                        .setPositiveButton(R.string.unlink, (dialog, which) -> getViewModel().leaveSpace(link.getSpaceId()))
                        .show();
            }
            return true;
        });
        popup.show();
    }

    private DashboardViewModel getViewModel() {
        return new ViewModelProvider((AppCompatActivity) context).get(DashboardViewModel.class);
    }

    @Override
    public int getItemCount() {
        return linkList.size();
    }

    public void updateLinks(List<Space> newLinks, Map<String, User> newMembersMap, List<Task> newTasks) {
        this.linkList.clear();
        this.linkList.addAll(newLinks);
        this.membersMap = newMembersMap;
        this.allTasks.clear();
        this.allTasks.addAll(newTasks);
        notifyDataSetChanged();
    }

    // CHANGED: Made public static class
    public static class LinkViewHolder extends RecyclerView.ViewHolder {
        TextView tvPartnerName, tvLinkStatus;
        ImageView ivLinkOptions;
        LinearProgressIndicator progressLink;

        public LinkViewHolder(@NonNull View itemView) {
            super(itemView);
            tvPartnerName = itemView.findViewById(R.id.tv_partner_name);
            tvLinkStatus = itemView.findViewById(R.id.tv_link_status);
            ivLinkOptions = itemView.findViewById(R.id.iv_link_options);
            progressLink = itemView.findViewById(R.id.progress_link);
        }
    }
}