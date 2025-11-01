package com.pranav.synctask.fragments;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkRequest;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupMenu;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.airbnb.lottie.LottieAnimationView; // ADDED
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar; // ADDED
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.pranav.synctask.R;
import com.pranav.synctask.activities.EditTaskActivity;
import com.pranav.synctask.activities.TaskDetailActivity;
import com.pranav.synctask.adapters.TaskAdapter;
import com.pranav.synctask.data.Result;
import com.pranav.synctask.models.Task;
import com.pranav.synctask.ui.viewmodels.TasksViewModel;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
// --- MODIFIED IN PHASE 4A: Implements listener ---
public abstract class BaseTaskFragment extends Fragment implements TaskAdapter.OnTaskActionListener {

    protected RecyclerView recyclerView;
    protected TaskAdapter adapter;
    protected SwipeRefreshLayout swipeRefreshLayout;
    protected LottieAnimationView emptyView; // MODIFIED: Changed from TextView
    protected String currentUserId;
    // MODIFIED: Made protected
    protected TasksViewModel viewModel; // MODIFIED: Made protected
    private ConnectivityManager.NetworkCallback networkCallback;
    private List<Task> currentTaskList = new ArrayList<>();
    private String currentSearchQuery = "";
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_task_list, container, false);
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            currentUserId = currentUser.getUid();
        }

        recyclerView = view.findViewById(R.id.recycler_view);
        swipeRefreshLayout = view.findViewById(R.id.swipe_refresh_layout);
        emptyView = view.findViewById(R.id.empty_view);
        // This now finds the LottieAnimationView

        setupRecyclerView();
        swipeRefreshLayout.setOnRefreshListener(() -> {
            viewModel.refreshTasks();
        });
        swipeRefreshLayout.setColorSchemeResources(R.color.md_theme_light_primary, R.color.md_theme_light_secondary); // MODIFIED: M3 Colors

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(requireActivity()).get(TasksViewModel.class);
        observeViewModel();
    }

    private void setupRecyclerView() {
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        // --- MODIFIED IN PHASE 4A: Pass 'this' as the listener ---
        adapter = new TaskAdapter(getContext(), new ArrayList<>(), currentUserId, this);
        recyclerView.setAdapter(adapter);
    }

    @Override
    public void onStart() {
        super.onStart();
        registerNetworkCallback();
    }

    @Override
    public void onStop() {
        super.onStop();
        unregisterNetworkCallback();
    }

    private void observeViewModel() {
        viewModel.getTasksResult().observe(getViewLifecycleOwner(), result -> {
            if (!isAdded()) return;

            swipeRefreshLayout.setRefreshing(result instanceof Result.Loading);

            if (result instanceof Result.Success) {
                currentTaskList = ((Result.Success<List<Task>>) result).data;
                filterAndDisplayTasks();

            } else if (result instanceof Result.Error) {
                Log.e(getClass().getSimpleName(), "Error loading tasks", ((Result.Error<List<Task>>) result).exception);
                Toast.makeText(getContext(), "Error loading tasks.", Toast.LENGTH_SHORT).show();
            }
        });
        viewModel.getSearchQuery().observe(getViewLifecycleOwner(), query -> {
            currentSearchQuery = query;
            filterAndDisplayTasks();
        });
    }

    private void filterAndDisplayTasks() {
        List<Task> timeFilteredTasks = filterTasks(currentTaskList);
        List<Task> finalFilteredTasks;
        if (currentSearchQuery.isEmpty()) {
            finalFilteredTasks = timeFilteredTasks;
        } else {
            finalFilteredTasks = timeFilteredTasks.stream()
                    .filter(task -> task.getTitle().toLowerCase().contains(currentSearchQuery.toLowerCase()))
                    .collect(Collectors.toList());
        }

        adapter.updateTasks(finalFilteredTasks);
        updateEmptyView(finalFilteredTasks.isEmpty());
    }

    private void registerNetworkCallback() {
        ConnectivityManager cm = (ConnectivityManager) requireContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        networkCallback = new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(@NonNull Network network) {
                super.onAvailable(network);
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        viewModel.syncLocalTasks(requireContext());
                    });
                }
            }
        };
        cm.registerNetworkCallback(new NetworkRequest.Builder().build(), networkCallback);
    }

    private void unregisterNetworkCallback() {
        if (networkCallback != null) {
            ConnectivityManager cm = (ConnectivityManager) requireContext().getSystemService(Context.CONNECTIVITY_SERVICE);
            cm.unregisterNetworkCallback(networkCallback);
        }
    }

    private void updateEmptyView(boolean isEmpty) {
        if (isEmpty) {
            emptyView.setVisibility(View.VISIBLE);
            emptyView.playAnimation(); // ADDED
        } else {
            emptyView.setVisibility(View.GONE);
            emptyView.cancelAnimation(); // ADDED
        }
        recyclerView.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
    }

    protected abstract List<Task> filterTasks(List<Task> tasks);

    // --- ADDED IN PHASE 4A: Listener implementation ---
    @Override
    public void onTaskClick(Task task) {
        Intent intent = new Intent(getContext(), TaskDetailActivity.class);
        intent.putExtra(TaskDetailActivity.EXTRA_TASK_ID, task.getId());
        startActivity(intent);
    }

    @Override
    public void onTaskLongClick(Task task, View view) {
        PopupMenu popup = new PopupMenu(getContext(), view);
        popup.getMenuInflater().inflate(R.menu.task_item_menu, popup.getMenu());

        // Check permissions
        boolean isCreator = currentUserId != null && currentUserId.equals(task.getCreatorUID());
        String scope = task.getOwnershipScope();

        // Determine who can edit/delete based on new logic (matches TaskDetailActivity)
        boolean canEdit = false;
        boolean canDelete = false;

        if (scope == null) scope = Task.SCOPE_SHARED; // Handle null scope

        switch (scope) {
            case Task.SCOPE_INDIVIDUAL:
                if (isCreator) {
                    canEdit = true;
                    canDelete = true;
                }
                break;
            case Task.SCOPE_SHARED:
                canEdit = true;
                canDelete = true;
                break;
            case Task.SCOPE_ASSIGNED:
                if (isCreator) {
                    canEdit = true;
                    canDelete = true;
                }
                // Note: Non-creator (assignee) can't edit/delete from the list,
                // they can only complete it in TaskDetailActivity.
                break;
        }

        popup.getMenu().findItem(R.id.action_edit_task).setVisible(canEdit);
        popup.getMenu().findItem(R.id.action_delete_task).setVisible(canDelete);
        popup.setOnMenuItemClickListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.action_edit_task) {
                Intent intent = new Intent(getContext(), EditTaskActivity.class);
                intent.putExtra(EditTaskActivity.EXTRA_TASK, task);
                startActivity(intent);

                return true;
            } else if (itemId == R.id.action_delete_task) {
                showDeleteConfirmation(task);
                return true;
            }
            return false;
        });
        popup.show();
    }

    // --- MODIFIED ---
    private void showDeleteConfirmation(Task task) {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.delete_task_dialog_title)
                .setMessage(R.string.delete_task_dialog_message)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.delete, (dialog, which) -> {

                    // --- Undo Logic Added ---
                    Task taskToDelete = task; // Save task to a temp variable
                    viewModel.deleteTask(taskToDelete.getId());

                    Snackbar.make(requireView(), "Task deleted", Snackbar.LENGTH_LONG)
                            .setAction("Undo", v -> {
                                // Re-create the task. We clear ID and status to ensure it's a new pending task.
                                taskToDelete.setId(null);
                                taskToDelete.setStatus(Task.STATUS_PENDING);
                                viewModel.createTask(taskToDelete, requireContext());
                            })
                            .show();
                    // --- End Undo Logic ---
                })
                .show();
    }
    // --- END MODIFIED ---
}