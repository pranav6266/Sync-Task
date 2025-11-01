package com.pranav.synctask.adapters;

import android.content.Context;
import android.content.Intent;
import android.text.InputType;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
// MODIFIED: Changed import
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.progressindicator.LinearProgressIndicator; // ADDED
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.pranav.synctask.R;
import com.pranav.synctask.activities.TaskViewActivity; // MODIFIED IN PHASE 2
import com.pranav.synctask.models.Space;
import com.pranav.synctask.models.Task; // ADDED
import com.pranav.synctask.ui.DashboardViewModel;

import java.util.ArrayList;
import java.util.List;
public class SpacesAdapter extends RecyclerView.Adapter<SpacesAdapter.SpaceViewHolder> {

    private final Context context;
    private final List<Space> spaceList;
    private final List<Task> allTasks; // ADDED
    private final String currentUserId;

    public SpacesAdapter(Context context, List<Space> spaceList, List<Task> allTasks) {
        this.context = context;
        this.spaceList = spaceList;
        this.allTasks = allTasks; // ADDED
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        this.currentUserId = (user != null) ? user.getUid() : null;
    }

    @NonNull
    @Override
    public SpaceViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_space, parent, false);
        return new SpaceViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SpaceViewHolder holder, int position) {
        Space space = spaceList.get(position);
        if (space == null) return;

        holder.tvSpaceName.setText(space.getSpaceName());

        // --- ADDED: Progress Calculation ---
        int totalEffort = 0;
        int completedEffort = 0;
        for (Task task : allTasks) {
            if (space.getSpaceId().equals(task.getSpaceId())) {
                totalEffort += task.getEffort();
                if (Task.STATUS_COMPLETED.equals(task.getStatus())) {
                    completedEffort += task.getEffort();
                }
            }
        }
        int progress = (totalEffort == 0) ? 0 : (int) (100.0 * completedEffort / totalEffort);
        holder.progressSpace.setProgress(progress, true);
        // --- END ADDED ---

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, TaskViewActivity.class); // MODIFIED IN PHASE 2
            intent.putExtra("SPACE_ID", space.getSpaceId());
            // --- ADDED IN PHASE 4A ---
            intent.putExtra("CONTEXT_TYPE", Space.TYPE_SHARED);
            // --- END ADDED ---

            context.startActivity(intent);
        });
        boolean isCreator = currentUserId != null && !space.getMembers().isEmpty() && space.getMembers().get(0).equals(currentUserId);
        holder.ivSpaceOptions.setOnClickListener(v -> {
            showOptionsDialog(space, isCreator);
        });
    }

    private void showOptionsDialog(Space space, boolean isCreator) {
        final String shareOption = "Share Invite Code";
        final String leaveOption = "Leave Space";
        final String deleteOption = "Delete Space";

        ArrayList<String> options = new ArrayList<>();
        options.add(shareOption);
        if (isCreator) {
            options.add(deleteOption);
        } else {
            options.add(leaveOption);
        }

        new MaterialAlertDialogBuilder(context)
                .setItems(options.toArray(new String[0]), (dialog, which) -> {
                    String selectedOption = options.get(which);
                    switch (selectedOption) {
                        case

                                shareOption:
                            showInviteCodeDialog(space);
                            break;

                        case leaveOption:

                            showConfirmationDialog("Leave", "Are you sure you want to leave this space?",
                                    () -> getViewModel().leaveSpace(space.getSpaceId()));

                            break;

                        case deleteOption:
                            showConfirmationDialog("Delete", "Are you sure? This will delete the space and all its tasks for EVERYONE.",

                                    () -> getViewModel().deleteSpace(space.getSpaceId()));
                            break;
                    }
                })

                .show();
    }

    private void showInviteCodeDialog(Space space) {
        String title = "Share Invite Code";
        String message = "Share this code with a partner to let them join your space.";
        final TextView codeView = new TextView(context);
        codeView.setText(space.getInviteCode());
        codeView.setTextSize(24);
        codeView.setTextIsSelectable(true);
        codeView.setGravity(Gravity.CENTER);
        codeView.setPadding(40, 40, 40, 40);
        new MaterialAlertDialogBuilder(context)
                .setTitle(title)
                .setMessage(message)
                .setView(codeView)
                .setPositiveButton("Share", (dialog, which) -> {
                    Intent sendIntent =

                            new Intent();
                    sendIntent.setAction(Intent.ACTION_SEND);
                    sendIntent.putExtra(Intent.EXTRA_TEXT, "Join my '" + space.getSpaceName() + "' space on SyncTask! \n\nInvite Code: " + space.getInviteCode());
                    sendIntent.setType("text/plain");

                    context.startActivity(Intent.createChooser(sendIntent, "Share code via"));

                })
                .setNegativeButton("Close", null)
                .show();
    }

    private void showConfirmationDialog(String title, String message, Runnable onConfirm) {
        new MaterialAlertDialogBuilder(context)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(title, (dialog, which) -> onConfirm.run())
                .setNegativeButton("Cancel", null)


                .show();
    }

    private DashboardViewModel getViewModel() {
        return new ViewModelProvider((AppCompatActivity) context).get(DashboardViewModel.class);
    }


    @Override
    public int getItemCount() {
        return spaceList.size();
    }

    public void updateSpaces(List<Space> newSpaces, List<Task> newTasks) {
        this.spaceList.clear();
        this.spaceList.addAll(newSpaces);
        this.allTasks.clear(); // ADDED
        this.allTasks.addAll(newTasks); // ADDED
        notifyDataSetChanged(); // DiffUtil would be better, but this matches original
    }

    static class SpaceViewHolder extends RecyclerView.ViewHolder {
        TextView tvSpaceName;
        ImageView ivSpaceOptions;
        LinearProgressIndicator progressSpace; // ADDED

        public SpaceViewHolder(@NonNull View itemView) {
            super(itemView);
            tvSpaceName = itemView.findViewById(R.id.tv_space_name);
            ivSpaceOptions = itemView.findViewById(R.id.iv_space_options);
            progressSpace = itemView.findViewById(R.id.progress_space); // ADDED
        }
    }
}