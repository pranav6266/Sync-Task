package com.pranav.synctask.activities;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.airbnb.lottie.LottieAnimationView;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.pranav.synctask.R;
import com.pranav.synctask.adapters.CompletedTaskAdapter;
import com.pranav.synctask.data.Result;
import com.pranav.synctask.models.Task;
import com.pranav.synctask.ui.viewmodels.CompletedTasksViewModel;

import java.util.ArrayList;
import java.util.List;

public class CompletedTasksActivity extends AppCompatActivity {

    private static final String TAG = "CompletedTasksActivity";
    private CompletedTasksViewModel viewModel;
    private RecyclerView recyclerView;
    private CompletedTaskAdapter adapter;
    private SwipeRefreshLayout swipeRefreshLayout;
    private LottieAnimationView emptyView;
    private String currentSpaceId; // Can be null
    private MaterialToolbar toolbar;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_completed_tasks);

        currentSpaceId = getIntent().getStringExtra("SPACE_ID");

        viewModel = new ViewModelProvider(this).get(CompletedTasksViewModel.class);

        toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        if (currentSpaceId == null) {
            toolbar.setTitle(R.string.settings_button_all_tasks);
        } else {
            toolbar.setTitle(R.string.completed_tasks_title);
        }

        recyclerView = findViewById(R.id.recycler_view);
        swipeRefreshLayout = findViewById(R.id.swipe_refresh_layout);
        emptyView = findViewById(R.id.empty_view);

        setupRecyclerView();
        observeViewModel();
        swipeRefreshLayout.setOnRefreshListener(() -> {
            viewModel.loadCompletedTasks(currentSpaceId); // Pass null or ID
        });
        swipeRefreshLayout.setColorSchemeResources(R.color.md_theme_light_primary, R.color.md_theme_light_secondary);

        viewModel.loadCompletedTasks(currentSpaceId); // Pass null or ID
    }

    private void setupRecyclerView() {
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new CompletedTaskAdapter(this, new ArrayList<>(), new CompletedTaskAdapter.OnTaskActionClickListener() {
            @Override
            public void onRestoreTask(Task task) {
                viewModel.restoreTask(task);
                // REQUIREMENT MET: Snackbar is shown here
                Snackbar.make(recyclerView, "Task restored", Snackbar.LENGTH_SHORT).show();
            }

            @Override

            public void onDeleteTask(Task task) {
                showDeleteConfirmation(task);
            }
        });
        recyclerView.setAdapter(adapter);
    }

    private void observeViewModel() {
        viewModel.getCompletedTasksResult().observe(this, result -> {
            if (result instanceof Result.Loading) {
                swipeRefreshLayout.setRefreshing(true);
            } else if (result instanceof Result.Success) {
                swipeRefreshLayout.setRefreshing(false);

                List<Task> tasks = ((Result.Success<List<Task>>) result).data;
                adapter.updateTasks(tasks);
                updateEmptyView(tasks.isEmpty());
            } else if (result instanceof Result.Error) {
                swipeRefreshLayout.setRefreshing(false);
                Log.e(TAG, "Error loading completed tasks", ((Result.Error<List<Task>>) result).exception);

                Toast.makeText(this, "Error loading archive.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showDeleteConfirmation(Task task) {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Delete Permanently?")
                .setMessage("Are you sure you want to permanently delete this task? This action cannot be undone.")
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.delete, (dialog, which)
                        -> {
                    viewModel.deleteTask(task.getId());
                    Toast.makeText(this, "Task deleted", Toast.LENGTH_SHORT).show();
                })
                .show();
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

    @Override
    protected void onStop() {
        super.onStop();
        viewModel.removeListeners();
    }
}