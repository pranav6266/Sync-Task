package com.pranav.synctask.fragments;

import android.content.Intent;
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
import androidx.recyclerview.widget.ItemTouchHelper; // ADDED
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.airbnb.lottie.LottieAnimationView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.pranav.synctask.R;
import com.pranav.synctask.activities.CompletionAnimationActivity; // ADDED
import com.pranav.synctask.activities.EditTaskActivity;
import com.pranav.synctask.activities.TaskDetailActivity;
import com.pranav.synctask.activities.TaskViewActivity;
import com.pranav.synctask.adapters.TaskAdapter;
import com.pranav.synctask.data.Result;
import com.pranav.synctask.models.Task;
import com.pranav.synctask.ui.viewmodels.TasksViewModel;
import com.pranav.synctask.utils.SwipeTaskCallback; // ADDED

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public abstract class BaseTaskFragment extends Fragment implements TaskAdapter.OnTaskActionListener {

    protected RecyclerView recyclerView;
    protected TaskAdapter adapter;
    protected SwipeRefreshLayout swipeRefreshLayout;
    protected LottieAnimationView emptyView;
    protected String currentUserId;
    protected TasksViewModel viewModel;
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

        setupRecyclerView();
        setupSwipeGestures(); // ADDED

        swipeRefreshLayout.setOnRefreshListener(() -> {
            viewModel.refreshTasks();
        });
        swipeRefreshLayout.setColorSchemeResources(R.color.md_theme_light_primary, R.color.md_theme_light_secondary);

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
        String contextType = ((TaskViewActivity) requireActivity()).getContextType();
        adapter = new TaskAdapter(contextType, getContext(), new ArrayList<>(), currentUserId, this);
        recyclerView.setAdapter(adapter);
    }

    // --- SWIPE LOGIC ---
    private void setupSwipeGestures() {
        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(new SwipeTaskCallback(getContext()) {
            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getAdapterPosition();
                Task task = adapter.getTaskAt(position);

                if (task == null) return;

                boolean isCreator = currentUserId != null && currentUserId.equals(task.getCreatorUID());
                String scope = task.getOwnershipScope();
                if (scope == null) scope = Task.SCOPE_SHARED;

                if (direction == ItemTouchHelper.LEFT) {
                    // DELETE ACTION
                    boolean canDelete = false;
                    if (isCreator) canDelete = true;
                    if (Task.SCOPE_SHARED.equals(scope)) canDelete = true; // Anyone deletes shared
                    // Assignee CANNOT delete assigned tasks from list (only creator)

                    if (canDelete) {
                        showDeleteConfirmation(task); // We show dialog. If cancelled, we need to notifyAdapter to bring item back.
                    } else {
                        notifyPermissionDenied(position, "You cannot delete this task.");
                    }

                } else if (direction == ItemTouchHelper.RIGHT) {
                    // COMPLETE ACTION
                    boolean canComplete = false;
                    if (Task.SCOPE_INDIVIDUAL.equals(scope) && isCreator) canComplete = true;
                    else if (Task.SCOPE_SHARED.equals(scope)) canComplete = true;
                    else if (Task.SCOPE_ASSIGNED.equals(scope) && !isCreator) canComplete = true; // Only assignee completes

                    if (canComplete) {
                        completeTask(task);
                    } else {
                        notifyPermissionDenied(position, "You cannot complete this task.");
                    }
                }
            }
        });
        itemTouchHelper.attachToRecyclerView(recyclerView);
    }

    private void notifyPermissionDenied(int position, String message) {
        adapter.notifyItemChanged(position); // Snap back
        Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
    }

    private void completeTask(Task task) {
        viewModel.updateTaskStatus(task.getId(), Task.STATUS_COMPLETED);
        // Launch animation
        Intent intent = new Intent(getContext(), CompletionAnimationActivity.class);
        startActivity(intent);
    }
    // -------------------

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
            // DEEP SEARCH LOGIC
            String query = currentSearchQuery.toLowerCase();
            finalFilteredTasks = timeFilteredTasks.stream()
                    .filter(task ->
                            (task.getTitle() != null && task.getTitle().toLowerCase().contains(query)) ||
                                    (task.getDescription() != null && task.getDescription().toLowerCase().contains(query)) ||
                                    (task.getCreatorDisplayName() != null && task.getCreatorDisplayName().toLowerCase().contains(query))
                    )
                    .collect(Collectors.toList());
        }

        adapter.updateTasks(finalFilteredTasks);
        updateEmptyView(finalFilteredTasks.isEmpty());
    }

    private void updateEmptyView(boolean isEmpty) {
        if (isEmpty) {
            emptyView.setVisibility(View.VISIBLE);
            emptyView.playAnimation();
        } else {
            emptyView.setVisibility(View.GONE);
            emptyView.cancelAnimation();
        }
        recyclerView.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
    }

    protected abstract List<Task> filterTasks(List<Task> tasks);

    @Override
    public void onTaskClick(Task task) {
        Intent intent = new Intent(getContext(), TaskDetailActivity.class);
        intent.putExtra(TaskDetailActivity.EXTRA_TASK_ID, task.getId());
        startActivity(intent);
    }

    @Override
    public void onTaskLongClick(Task task, View view) {
        // Long press menu logic (Keep existing or remove if you prefer swipe only)
        // For now, keeping it as a backup method
        PopupMenu popup = new PopupMenu(getContext(), view);
        popup.getMenuInflater().inflate(R.menu.task_item_menu, popup.getMenu());

        boolean isCreator = currentUserId != null && currentUserId.equals(task.getCreatorUID());
        String scope = task.getOwnershipScope();
        if(scope == null) scope = Task.SCOPE_SHARED;

        boolean canEdit = false;
        boolean canDelete = false;

        switch (scope) {
            case Task.SCOPE_INDIVIDUAL:
                if (isCreator) { canEdit = true; canDelete = true; }
                break;
            case Task.SCOPE_SHARED:
                canEdit = true; canDelete = true;
                break;
            case Task.SCOPE_ASSIGNED:
                if (isCreator) { canEdit = true; canDelete = true; }
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

    private void showDeleteConfirmation(Task task) {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.delete_task_dialog_title)
                .setMessage(R.string.delete_task_dialog_message)
                .setNegativeButton(R.string.cancel, (dialog, which) -> {
                    adapter.notifyDataSetChanged(); // Restore item if swipe cancelled via dialog
                })
                .setPositiveButton(R.string.delete, (dialog, which) -> {
                    // Undo Logic
                    Task taskToDelete = task;
                    viewModel.deleteTask(taskToDelete.getId());

                    // We don't need to remove from adapter manually because
                    // Firestore listener will update the list automatically.

                    Snackbar.make(requireView(), "Task deleted", Snackbar.LENGTH_LONG)
                            .setAction("Undo", v -> {
                                taskToDelete.setId(null);
                                taskToDelete.setStatus(Task.STATUS_PENDING);
                                viewModel.createTask(taskToDelete, requireContext());
                            })
                            .show();
                })
                .setOnCancelListener(dialog -> adapter.notifyDataSetChanged()) // Restore on outside click
                .show();
    }
}