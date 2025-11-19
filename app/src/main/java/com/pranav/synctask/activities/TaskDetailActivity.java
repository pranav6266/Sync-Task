package com.pranav.synctask.activities;

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
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.chip.Chip;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.progressindicator.LinearProgressIndicator; // ADDED
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.pranav.synctask.R;
import com.pranav.synctask.data.Result;
import com.pranav.synctask.models.Task;
import com.pranav.synctask.ui.viewmodels.TaskDetailViewModel;
import com.pranav.synctask.utils.DateUtils;

public class TaskDetailActivity extends AppCompatActivity {

    public static final String EXTRA_TASK_ID = "EXTRA_TASK_ID";
    private static final String TAG = "TaskDetailActivity";

    private TaskDetailViewModel viewModel;
    private String taskId;
    private Task currentTask;
    private FirebaseUser currentUser;

    // Views
    private TextView tvTitle, tvDescription, tvDueDate, tvCreator, tvEffortValue;
    private CheckBox cbStatus;
    private Chip chipStatus;
    private Chip chipPriority, chipType, chipScope; // ADDED
    private LinearProgressIndicator indicatorEffort; // ADDED
    private ProgressBar progressBar;
    private View contentLayout;
    private Toolbar toolbar;

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
        tvTitle = findViewById(R.id.tv_task_title_detail);
        tvDescription = findViewById(R.id.tv_task_description_detail);
        tvDueDate = findViewById(R.id.tv_task_due_date_detail);
        tvCreator = findViewById(R.id.tv_task_creator_detail);

        // New Chips
        chipPriority = findViewById(R.id.chip_priority_display);
        chipType = findViewById(R.id.chip_type_display);
        chipScope = findViewById(R.id.chip_scope_display);

        // Effort
        tvEffortValue = findViewById(R.id.tv_task_effort_detail);
        indicatorEffort = findViewById(R.id.indicator_effort);

        cbStatus = findViewById(R.id.cb_task_status_detail);
        chipStatus = findViewById(R.id.chip_read_only_status);

        progressBar = findViewById(R.id.progress_bar_detail);
        contentLayout = findViewById(R.id.content_layout_detail);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            getSupportActionBar().setTitle(""); // Clean header
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
                }
            } else if (result instanceof Result.Error) {
                progressBar.setVisibility(View.GONE);
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
        canEdit = false; canDelete = false; canComplete = false;

        switch (scope) {
            case Task.SCOPE_INDIVIDUAL:
                if (isCreator) { canEdit = true; canDelete = true; canComplete = true; }
                break;
            case Task.SCOPE_SHARED:
                canEdit = true; canDelete = true; canComplete = true;
                break;
            case Task.SCOPE_ASSIGNED:
                if (isCreator) { canEdit = true; canDelete = true; canComplete = false; }
                else { canEdit = false; canDelete = false; canComplete = true; }
                break;
        }
        invalidateOptionsMenu();
    }

    private void populateUi() {
        tvTitle.setText(currentTask.getTitle());

        if (currentTask.getDescription() != null && !currentTask.getDescription().isEmpty()) {
            tvDescription.setText(currentTask.getDescription());
            tvDescription.setAlpha(1.0f);
        } else {
            tvDescription.setText("No description provided.");
            tvDescription.setAlpha(0.5f);
        }

        if (currentTask.getDueDate() != null) {
            tvDueDate.setText(DateUtils.formatDate(currentTask.getDueDate()));
        } else {
            tvDueDate.setText(R.string.not_set);
        }

        // Effort
        int effort = currentTask.getEffort();
        indicatorEffort.setProgress(effort);
        tvEffortValue.setText(effort + "/5");

        // Created By
        boolean isCreator = currentUser.getUid().equals(currentTask.getCreatorUID());
        tvCreator.setText(isCreator ? "Created by You" : "Created by " + currentTask.getCreatorDisplayName());

        // -- SET CHIPS --

        // Priority Chip
        String priority = currentTask.getPriority();
        chipPriority.setText(priority);
        if ("High".equalsIgnoreCase(priority)) {
            chipPriority.setChipIconTintResource(R.color.priority_high);
        } else if ("Low".equalsIgnoreCase(priority)) {
            chipPriority.setChipIconTintResource(R.color.priority_low);
        } else {
            chipPriority.setChipIconTintResource(R.color.priority_normal);
        }

        // Type Chip
        String type = currentTask.getTaskType();
        if (Task.TYPE_REMINDER.equals(type)) {
            chipType.setText("Reminder");
            chipType.setChipIconResource(R.drawable.ic_task_type_reminder);
        } else if (Task.TYPE_UPDATE.equals(type)) {
            chipType.setText("Update");
            chipType.setChipIconResource(R.drawable.ic_task_type_update);
        } else {
            chipType.setText("Task");
            chipType.setChipIconResource(R.drawable.ic_task_type_task);
        }

        // Scope Chip
        chipScope.setText(getScopeDisplayString(currentTask.getOwnershipScope()));

        // -- STATUS LOGIC --
        if (canComplete) {
            cbStatus.setVisibility(View.VISIBLE);
            chipStatus.setVisibility(View.GONE);
            cbStatus.setOnCheckedChangeListener(null);
            cbStatus.setChecked(Task.STATUS_COMPLETED.equals(currentTask.getStatus()));
            cbStatus.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (!buttonView.isPressed()) return;
                if (isChecked) playCompleteAnimation();
                else viewModel.updateTaskStatus(taskId, Task.STATUS_PENDING);
            });
        } else {
            cbStatus.setVisibility(View.GONE);
            chipStatus.setVisibility(View.VISIBLE);
            boolean isCompleted = Task.STATUS_COMPLETED.equals(currentTask.getStatus());
            String statusText = isCompleted ? "Completed" : "Pending";
            if (Task.SCOPE_ASSIGNED.equals(currentTask.getOwnershipScope())) {
                statusText += " (Partner)";
            }
            chipStatus.setText(statusText);
        }
    }

    private void playCompleteAnimation() {
        viewModel.updateTaskStatus(taskId, Task.STATUS_COMPLETED);
        Intent intent = new Intent(this, CompletionAnimationActivity.class);
        startActivity(intent);
        finish();
    }

    private String getScopeDisplayString(String scope) {
        if (scope == null) return "Shared";
        switch (scope) {
            case Task.SCOPE_INDIVIDUAL: return "Private";
            case Task.SCOPE_ASSIGNED: return "Assigned";
            default: return "Shared";
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
            if (canDelete) showDeleteConfirmation();
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