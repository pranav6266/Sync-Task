package com.pranav.synctask.activities;

import android.animation.Animator;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.lifecycle.ViewModelProvider;

import com.airbnb.lottie.LottieAnimationView;
import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.chip.Chip; // ADDED
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.pranav.synctask.R;
import com.pranav.synctask.data.Result;
import com.pranav.synctask.models.Task;
import com.pranav.synctask.ui.viewmodels.TaskDetailViewModel;
import com.pranav.synctask.utils.DateUtils;

import java.util.Locale;

public class TaskDetailActivity extends AppCompatActivity {

    public static final String EXTRA_TASK_ID = "EXTRA_TASK_ID";
    private static final String TAG = "TaskDetailActivity";

    private TaskDetailViewModel viewModel;
    private String taskId;
    private Task currentTask;
    private FirebaseUser currentUser;
    private TextView tvTitle, tvDescription, tvDueDate, tvPriority, tvScope, tvCreator, tvEffort;
    private CheckBox cbStatus;
    private Chip chipStatus; // ADDED for Requirement 1
    private ProgressBar progressBar;
    private View contentLayout;
    private Toolbar toolbar;
    private AppBarLayout appBarLayout;
    private boolean canEdit = false;
    private boolean canDelete = false;
    private boolean canComplete = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_task_detail);

        taskId = getIntent().getStringExtra(EXTRA_TASK_ID);
        currentUser = FirebaseAuth.getInstance().getCurrentUser();

        if (taskId == null || currentUser == null) {
            Toast.makeText(this, "Error: Task ID or User missing.", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "TaskId or CurrentUser is null. Finishing activity.");
            finish();
            return;
        }

        viewModel = new ViewModelProvider(this).get(TaskDetailViewModel.class);

        initializeViews();
        setupToolbar();
        observeViewModel();
    }

    private void initializeViews() {
        toolbar = findViewById(R.id.toolbar_task_detail);
        appBarLayout = findViewById(R.id.app_bar_layout);
        tvTitle = findViewById(R.id.tv_task_title_detail);
        tvDescription = findViewById(R.id.tv_task_description_detail);
        tvDueDate = findViewById(R.id.tv_task_due_date_detail);
        tvPriority = findViewById(R.id.tv_task_priority_detail);
        tvScope = findViewById(R.id.tv_task_scope_detail);
        tvCreator = findViewById(R.id.tv_task_creator_detail);
        tvEffort = findViewById(R.id.tv_task_effort_detail);
        cbStatus = findViewById(R.id.cb_task_status_detail);
        chipStatus = findViewById(R.id.chip_read_only_status); // ADDED
        progressBar = findViewById(R.id.progress_bar_detail);
        contentLayout = findViewById(R.id.content_layout_detail);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            getSupportActionBar().setTitle(R.string.task_details_title);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        viewModel.attachTaskListener(taskId);
    }

    @Override
    protected void onStop() {
        super.onStop();
        viewModel.removeTaskListener();
    }

    private void observeViewModel() {
        viewModel.getTask().observe(this, result -> {
            if (result instanceof Result.Loading) {
                progressBar.setVisibility(View.VISIBLE);
                contentLayout.setVisibility(View.GONE);
            } else if (result instanceof Result.Success) {
                progressBar.setVisibility(View.GONE);
                contentLayout.setVisibility(View.VISIBLE);
                currentTask = ((Result.Success<Task>) result).data;
                if (currentTask != null) {
                    calculatePermissions();
                    populateUi();
                } else {
                    Toast.makeText(this, "Error loading task data.", Toast.LENGTH_SHORT).show();
                    finish();
                }
            } else if (result instanceof Result.Error) {
                progressBar.setVisibility(View.GONE);
                Log.e(TAG, "Error loading task", ((Result.Error<Task>) result).exception);
                Toast.makeText(this, "Error loading task.", Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }

    private void calculatePermissions() {
        if (currentTask == null || currentUser == null) return;
        String scope = currentTask.getOwnershipScope();
        if (scope == null) scope = Task.SCOPE_SHARED;

        boolean isCreator = currentUser.getUid().equals(currentTask.getCreatorUID());
        // Reset flags
        canEdit = false;
        canDelete = false;
        canComplete = false;

        switch (scope) {
            case Task.SCOPE_INDIVIDUAL:
                if (isCreator) {
                    canEdit = true;
                    canDelete = true;
                    canComplete = true;
                }
                break;
            case Task.SCOPE_SHARED:
                canEdit = true;
                canDelete = true;
                canComplete = true;
                break;
            case Task.SCOPE_ASSIGNED:
                if (isCreator) {
                    canEdit = true;
                    canDelete = true;
                    canComplete = false; // Creator assigns, but cannot complete (Requirement 1 logic)
                } else {
                    canEdit = false;
                    canDelete = false;
                    canComplete = true; // Assignee completes
                }
                break;
        }
        invalidateOptionsMenu();
    }

    private void populateUi() {
        tvTitle.setText(currentTask.getTitle());
        if (currentTask.getDescription() != null && !currentTask.getDescription().isEmpty()) {
            tvDescription.setText(currentTask.getDescription());
            tvDescription.setVisibility(View.VISIBLE);
        } else {
            tvDescription.setText(R.string.not_set);
            tvDescription.setVisibility(View.GONE);
        }

        if (currentTask.getDueDate() != null) {
            tvDueDate.setText(DateUtils.formatDate(currentTask.getDueDate()));
        } else {
            tvDueDate.setText(R.string.not_set);
        }

        tvPriority.setText(currentTask.getPriority());
        tvEffort.setText(String.valueOf(currentTask.getEffort()));
        tvScope.setText(getScopeDisplayString(currentTask.getOwnershipScope()));

        boolean isCreator = currentUser.getUid().equals(currentTask.getCreatorUID());
        tvCreator.setText(isCreator ? getString(R.string.task_creator_label_you) : currentTask.getCreatorDisplayName());

        // --- MODIFIED: Requirement 1 Logic ---
        if (canComplete) {
            // User has permission: Show Checkbox, Hide Status Chip
            cbStatus.setVisibility(View.VISIBLE);
            chipStatus.setVisibility(View.GONE);

            cbStatus.setOnCheckedChangeListener(null); // Remove listener to set initial state
            cbStatus.setChecked(Task.STATUS_COMPLETED.equals(currentTask.getStatus()));

            // Add new listener
            cbStatus.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (!buttonView.isPressed()) {
                    return; // Ignore programmatic changes
                }
                if (isChecked) {
                    playCompleteAnimation();
                } else {
                    viewModel.updateTaskStatus(taskId, Task.STATUS_PENDING);
                }
            });
        } else {
            // User CANNOT complete: Hide Checkbox, Show Status Chip
            cbStatus.setVisibility(View.GONE);
            chipStatus.setVisibility(View.VISIBLE);

            boolean isCompleted = Task.STATUS_COMPLETED.equals(currentTask.getStatus());
            String statusText = isCompleted ? "Completed" : "Pending";

            // Add context if it's an assigned task
            if (Task.SCOPE_ASSIGNED.equals(currentTask.getOwnershipScope())) {
                statusText += " (Assigned to Partner)";
            }

            chipStatus.setText("Status: " + statusText);

            // Optional: Change chip color based on status
            if (isCompleted) {
                chipStatus.setChipBackgroundColorResource(R.color.md_theme_light_secondaryContainer);
                chipStatus.setTextColor(getColor(R.color.md_theme_light_onSecondaryContainer));
            } else {
                chipStatus.setChipBackgroundColorResource(R.color.md_theme_light_surfaceVariant);
                chipStatus.setTextColor(getColor(R.color.md_theme_light_onSurfaceVariant));
            }
        }
        // --- END MODIFIED ---
    }

    private void playCompleteAnimation() {
        viewModel.updateTaskStatus(taskId, Task.STATUS_COMPLETED);
        Intent intent = new Intent(this, CompletionAnimationActivity.class);
        startActivity(intent);
        finish();
    }

    private String getScopeDisplayString(String scope) {
        if (scope == null) return getString(R.string.scope_shared_short);
        switch (scope) {
            case Task.SCOPE_INDIVIDUAL:
                return getString(R.string.scope_individual_short);
            case Task.SCOPE_ASSIGNED:
                return getString(R.string.scope_assigned_short);
            case Task.SCOPE_SHARED:
            default:
                return getString(R.string.scope_shared_short);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.task_detail_menu, menu);
        MenuItem editItem = menu.findItem(R.id.action_edit_task);
        MenuItem deleteItem = menu.findItem(R.id.action_delete_task);

        editItem.setVisible(canEdit);
        deleteItem.setVisible(canDelete);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == android.R.id.home) {
            finish();
            return true;
        } else if (itemId == R.id.action_edit_task) {
            if (canEdit) {
                Intent intent = new Intent(this, EditTaskActivity.class);
                intent.putExtra(EditTaskActivity.EXTRA_TASK, currentTask);
                startActivity(intent);
            }
            return true;
        } else if (itemId == R.id.action_delete_task) {
            if (canDelete) {
                showDeleteConfirmation();
            }
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showDeleteConfirmation() {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.delete_task_dialog_title)
                .setMessage(R.string.delete_task_dialog_message)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.delete, (dialog, which) -> {
                    viewModel.deleteTask(taskId);
                    Toast.makeText(this, "Task deleted", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .show();
    }
}