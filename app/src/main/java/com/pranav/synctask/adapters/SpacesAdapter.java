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
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.pranav.synctask.R;
import com.pranav.synctask.activities.MainActivity;
import com.pranav.synctask.models.Space;
import com.pranav.synctask.ui.DashboardViewModel;

import java.util.ArrayList;
import java.util.List;

public class SpacesAdapter extends RecyclerView.Adapter<SpacesAdapter.SpaceViewHolder> {

    private final Context context;
    private final List<Space> spaceList;
    private final String currentUserId;

    public SpacesAdapter(Context context, List<Space> spaceList) {
        this.context = context;
        this.spaceList = spaceList;
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

        // FIX: Use the camelCase variable name 'tvSpaceName'
        holder.tvSpaceName.setText(space.getSpaceName());

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, MainActivity.class);
            intent.putExtra("SPACE_ID", space.getSpaceId());
            context.startActivity(intent);
        });

        boolean isCreator = currentUserId != null && !space.getMembers().isEmpty() && space.getMembers().get(0).equals(currentUserId);

        // FIX: Use the camelCase variable name 'ivSpaceOptions'
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

        // MODIFIED: Use MaterialAlertDialogBuilder
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
        // Create a TextView for the code
        final TextView codeView = new TextView(context);
        codeView.setText(space.getInviteCode());
        codeView.setTextSize(24);
        codeView.setTextIsSelectable(true);
        codeView.setGravity(Gravity.CENTER);
        codeView.setPadding(40, 40, 40, 40);

        // MODIFIED: Use MaterialAlertDialogBuilder
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
        // MODIFIED: Use MaterialAlertDialogBuilder
        new MaterialAlertDialogBuilder(context)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(title, (dialog, which) -> onConfirm.run())
                .setNegativeButton("Cancel", null)

                .show();
    }

    private DashboardViewModel getViewModel() {
        // This assumes the context is the DashboardActivity
        return new ViewModelProvider((AppCompatActivity) context).get(DashboardViewModel.class);
    }


    @Override
    public int getItemCount() {
        return spaceList.size();
    }

    public void updateSpaces(List<Space> newSpaces) {
        this.spaceList.clear();
        this.spaceList.addAll(newSpaces);
        notifyDataSetChanged();
    }

    static class SpaceViewHolder extends RecyclerView.ViewHolder {
        TextView tvSpaceName;
        ImageView ivSpaceOptions;

        public SpaceViewHolder(@NonNull View itemView) {
            super(itemView);
            tvSpaceName = itemView.findViewById(R.id.tv_space_name);
            ivSpaceOptions = itemView.findViewById(R.id.iv_space_options);
        }
    }
}