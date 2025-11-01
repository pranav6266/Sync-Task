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
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.progressindicator.LinearProgressIndicator; // ADDED
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.pranav.synctask.R;
import com.pranav.synctask.activities.TaskViewActivity;
import com.pranav.synctask.models.Space;
import com.pranav.synctask.models.Task; // ADDED
import com.pranav.synctask.models.User;
import com.pranav.synctask.ui.DashboardViewModel;

import java.util.List;
import java.util.Locale;
import java.util.Objects;
public class PersonalLinksAdapter extends RecyclerView.Adapter<PersonalLinksAdapter.LinkViewHolder> {

    private final Context context;
    private final List<Space> linkList;
    private final String currentUserId;
    private final List<User> partnerDetails; // To store partner user objects
    private final List<Task> allTasks; // ADDED

    public PersonalLinksAdapter(Context context, List<Space> linkList, List<User> partnerDetails, List<Task> allTasks) {
        this.context = context;
        this.linkList = linkList;
        this.partnerDetails = partnerDetails;
        this.allTasks = allTasks; // ADDED
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

        // Find the partner's UID
        String partnerUid = null;
        for (String memberId : link.getMembers()) {
            if (!memberId.equals(currentUserId)) {
                partnerUid = memberId;
                break;
            }
        }

        // Find the partner's details
        String partnerName = "Partner";
        // Default
        if (partnerUid != null) {
            for (User partner : partnerDetails) {
                if (partner.getUid().equals(partnerUid)) {
                    partnerName = partner.getDisplayName();
                    break;
                }
            }
        }

        holder.tvPartnerName.setText(String.format(Locale.getDefault(), "Tasks with %s", partnerName));

        // --- ADDED: Progress Calculation ---
        int totalEffort = 0;
        int completedEffort = 0;
        for (Task task : allTasks) {
            if (link.getSpaceId().equals(task.getSpaceId())) {
                totalEffort += task.getEffort();
                if (Task.STATUS_COMPLETED.equals(task.getStatus())) {
                    completedEffort += task.getEffort();
                }
            }
        }
        int progress = (totalEffort == 0) ? 0 : (int) (100.0 * completedEffort / totalEffort);
        holder.progressLink.setProgress(progress, true);
        // --- END ADDED ---

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, TaskViewActivity.class);
            intent.putExtra("SPACE_ID", link.getSpaceId());
            // --- ADDED IN PHASE 4A ---
            intent.putExtra("CONTEXT_TYPE", Space.TYPE_PERSONAL);
            // --- END ADDED ---
            context.startActivity(intent);
        });
        holder.ivLinkOptions.setOnClickListener(v -> {
            showOptionsDialog(v, link);
        });
    }

    private void showOptionsDialog(View anchor, Space link) {
        PopupMenu popup = new PopupMenu(context, anchor);
        popup.getMenu().add(Menu.NONE, 1, 1, R.string.unlink_partner_title);
        popup.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == 1) {
                // Show confirmation dialog for unlinking
                new MaterialAlertDialogBuilder(context)
                        .setTitle(R.string.unlink_partner_title)

                        .setMessage(R.string.unlink_partner_message)
                        .setNegativeButton(R.string.cancel, null)
                        .setPositiveButton(R.string.unlink, (dialog, which) -> {
                            getViewModel().leaveSpace(link.getSpaceId());

                        })
                        .show();
            }
            return true;
        });
        popup.show();
    }

    private DashboardViewModel getViewModel() {
        // This assumes the context is an AppCompatActivity
        return new ViewModelProvider((AppCompatActivity) context).get(DashboardViewModel.class);
    }

    @Override
    public int getItemCount() {
        return linkList.size();
    }

    // MODIFIED: Now uses DiffUtil
    public void updateLinks(List<Space> newLinks, List<User> newPartners, List<Task> newTasks) {
        // We just need to diff the links.
        PersonalLinkDiffCallback diffCallback = new PersonalLinkDiffCallback(this.linkList, newLinks);
        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(diffCallback);

        this.linkList.clear();
        this.linkList.addAll(newLinks);
        this.partnerDetails.clear();
        this.partnerDetails.addAll(newPartners);
        this.allTasks.clear(); // ADDED
        this.allTasks.addAll(newTasks); // ADDED
        diffResult.dispatchUpdatesTo(this);
    }

    static class LinkViewHolder extends RecyclerView.ViewHolder {
        TextView tvPartnerName;
        ImageView ivLinkOptions;
        LinearProgressIndicator progressLink; // ADDED

        public LinkViewHolder(@NonNull View itemView) {
            super(itemView);
            tvPartnerName = itemView.findViewById(R.id.tv_partner_name);
            ivLinkOptions = itemView.findViewById(R.id.iv_link_options);
            progressLink = itemView.findViewById(R.id.progress_link); // ADDED
        }
    }

    // --- ADDED IN PHASE 3C: DiffUtil ---
    private static class PersonalLinkDiffCallback extends DiffUtil.Callback {
        private final List<Space> oldList;
        private final List<Space> newList;

        public PersonalLinkDiffCallback(List<Space> oldList, List<Space> newList) {
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
            return Objects.equals(oldList.get(oldItemPosition).getSpaceId(), newList.get(newItemPosition).getSpaceId());
        }

        @Override
        public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
            Space oldLink = oldList.get(oldItemPosition);
            Space newLink = newList.get(newItemPosition);
            // Only checking name, as members changing will be handled by the partner list
            // Note: We don't check progress here, as that's calculated dynamically
            return Objects.equals(oldLink.getSpaceName(), newLink.getSpaceName());
        }
    }
}