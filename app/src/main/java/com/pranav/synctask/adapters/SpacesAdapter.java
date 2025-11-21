package com.pranav.synctask.adapters;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

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
import com.pranav.synctask.utils.DateUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SpacesAdapter extends RecyclerView.Adapter<SpacesAdapter.SpaceViewHolder> {

    private final Context context;
    private final List<Space> spaceList;
    private final List<Task> allTasks;
    private Map<String, User> membersMap;
    private final String currentUserId;

    public SpacesAdapter(Context context, List<Space> spaceList, Map<String, User> membersMap, List<Task> allTasks) {
        this.context = context;
        this.spaceList = spaceList;
        this.membersMap = membersMap;
        this.allTasks = allTasks;
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        this.currentUserId = (user != null) ? user.getUid() : null;
    }

    @NonNull
    @Override
    public SpaceViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // This MUST match the fixed item_space.xml file
        View view = LayoutInflater.from(context).inflate(R.layout.item_space, parent, false);
        return new SpaceViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SpaceViewHolder holder, int position) {
        Space space = spaceList.get(position);
        if (space == null) return;

        holder.tvSpaceName.setText(space.getSpaceName());

        // Calculate Progress
        int totalEffort = 0;
        int completedEffort = 0;
        int activeTaskCount = 0;

        for (Task task : allTasks) {
            if (space.getSpaceId().equals(task.getSpaceId())) {
                if (Task.STATUS_PENDING.equals(task.getStatus())) {
                    activeTaskCount++;
                }
                // Progress based on recent activity
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
            holder.progressSpace.setVisibility(View.GONE);
        } else {
            holder.progressSpace.setVisibility(View.VISIBLE);
            holder.progressSpace.setProgress(progress, true);
        }

        holder.tvSpaceDesc.setText(activeTaskCount + " Active Tasks");

        // Click on Card -> Open Task View
        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, TaskViewActivity.class);
            intent.putExtra("SPACE_ID", space.getSpaceId());
            intent.putExtra("CONTEXT_TYPE", Space.TYPE_SHARED);
            context.startActivity(intent);
        });

        // Determine if current user is Admin/Creator
        boolean isCreator = false;
        if (space.getAdminUid() != null) {
            isCreator = space.getAdminUid().equals(currentUserId);
        } else if (space.getMembers() != null && !space.getMembers().isEmpty()) {
            // Fallback for legacy data
            isCreator = space.getMembers().get(0).equals(currentUserId);
        }

        final boolean finalIsCreator = isCreator;
        holder.ivSpaceOptions.setOnClickListener(v -> showOptionsDialog(space, finalIsCreator));
    }

    private void showOptionsDialog(Space space, boolean isCreator) {
        final String viewMembersOption = "View Members";
        final String shareOption = "Copy Invite Code";
        final String leaveOption = "Leave Space";
        final String deleteOption = "Delete Space";

        ArrayList<String> options = new ArrayList<>();
        options.add(viewMembersOption);
        options.add(shareOption);

        if (isCreator) options.add(deleteOption);
        else options.add(leaveOption);

        new MaterialAlertDialogBuilder(context)
                .setTitle(space.getSpaceName())
                .setItems(options.toArray(new String[0]), (dialog, which) -> {
                    String selectedOption = options.get(which);
                    switch (selectedOption) {
                        case viewMembersOption:
                            showMembersDialog(space);
                            break;
                        case shareOption:
                            copyInviteCode(space.getInviteCode());
                            break;
                        case leaveOption:
                            showConfirmationDialog("Leave Space", "Are you sure you want to leave? You will lose access to these tasks.", () -> getViewModel().leaveSpace(space.getSpaceId()));
                            break;
                        case deleteOption:
                            showConfirmationDialog("Delete Space", "Delete this space for EVERYONE? This action cannot be undone.", () -> getViewModel().deleteSpace(space.getSpaceId()));
                            break;
                    }
                }).show();
    }

    private void showMembersDialog(Space space) {
        List<String> memberNames = new ArrayList<>();
        if (space.getMembers() != null && membersMap != null) {
            String adminId = space.getAdminUid() != null ? space.getAdminUid() : (space.getMembers().isEmpty() ? "" : space.getMembers().get(0));

            for (String uid : space.getMembers()) {
                String role = uid.equals(adminId) ? " (Admin)" : "";
                if (uid.equals(currentUserId)) {
                    memberNames.add("You" + role);
                } else if (membersMap.containsKey(uid)) {
                    User member = membersMap.get(uid);
                    memberNames.add((member != null ? member.getDisplayName() : "Unknown") + role);
                } else {
                    memberNames.add("Loading..." + role);
                }
            }
        }

        new MaterialAlertDialogBuilder(context)
                .setTitle("Members")
                .setAdapter(new ArrayAdapter<>(context, android.R.layout.simple_list_item_1, memberNames), null)
                .setPositiveButton("Close", null)
                .show();
    }

    private void copyInviteCode(String code) {
        ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("Invite Code", code);
        if (clipboard != null) {
            clipboard.setPrimaryClip(clip);
            Toast.makeText(context, "Invite Code copied: " + code, Toast.LENGTH_SHORT).show();
        }
    }

    private void showConfirmationDialog(String title, String message, Runnable onConfirm) {
        new MaterialAlertDialogBuilder(context)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(title.split(" ")[0], (dialog, which) -> onConfirm.run()) // "Delete" or "Leave"
                .setNegativeButton("Cancel", null)
                .show();
    }

    private DashboardViewModel getViewModel() {
        // This requires context to be an Activity (ensured by Fragment using requireActivity())
        return new ViewModelProvider((AppCompatActivity) context).get(DashboardViewModel.class);
    }

    @Override
    public int getItemCount() {
        return spaceList.size();
    }

    public void updateSpaces(List<Space> newSpaces, Map<String, User> newMembersMap, List<Task> newTasks) {
        this.spaceList.clear();
        this.spaceList.addAll(newSpaces);
        this.membersMap = newMembersMap;
        this.allTasks.clear();
        this.allTasks.addAll(newTasks);
        notifyDataSetChanged();
    }

    public static class SpaceViewHolder extends RecyclerView.ViewHolder {
        TextView tvSpaceName, tvSpaceDesc;
        ImageView ivSpaceOptions;
        LinearProgressIndicator progressSpace;

        public SpaceViewHolder(@NonNull View itemView) {
            super(itemView);
            tvSpaceName = itemView.findViewById(R.id.tv_space_name);
            tvSpaceDesc = itemView.findViewById(R.id.tv_space_desc);
            ivSpaceOptions = itemView.findViewById(R.id.iv_space_options);
            progressSpace = itemView.findViewById(R.id.progress_space);
        }
    }
}