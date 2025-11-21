package com.pranav.synctask.activities;

import android.content.Intent;
import android.os.Bundle;
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
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.chip.Chip;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.pranav.synctask.R;
import com.pranav.synctask.adapters.SubtaskAdapter;
import com.pranav.synctask.data.Result;
import com.pranav.synctask.models.Space;
import com.pranav.synctask.models.Subtask;
import com.pranav.synctask.models.Task;
import com.pranav.synctask.ui.viewmodels.TaskDetailViewModel;
import com.pranav.synctask.utils.DateUtils;
import java.util.ArrayList;
import java.util.List;

public class TaskDetailActivity extends AppCompatActivity implements SubtaskAdapter.OnSubtaskActionListener {

    public static final String EXTRA_TASK_ID = "EXTRA_TASK_ID";
    private TaskDetailViewModel viewModel;
    private String taskId;
    private Task currentTask;
    private FirebaseUser currentUser;
    private boolean isAdmin = false;

    // Views
    private TextView tvTitle, tvDescription, tvDueDate, tvCreator, tvEffortValue;
    private CheckBox cbStatus;
    private Chip chipStatus, chipPriority, chipType, chipScope;
    private LinearProgressIndicator indicatorEffort;
    private ProgressBar progressBar;
    private View contentLayout;
    private Toolbar toolbar;

    // Subtask Views
    private View layoutSubtasks;
    private RecyclerView rvSubtasks;
    private LinearProgressIndicator indicatorProgress;
    private TextView tvProgressText;
    private SubtaskAdapter subtaskAdapter;

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

        chipPriority = findViewById(R.id.chip_priority_display);
        chipType = findViewById(R.id.chip_type_display);
        chipScope = findViewById(R.id.chip_scope_display);

        tvEffortValue = findViewById(R.id.tv_task_effort_detail);
        indicatorEffort = findViewById(R.id.indicator_effort);

        cbStatus = findViewById(R.id.cb_task_status_detail);
        chipStatus = findViewById(R.id.chip_read_only_status);

        progressBar = findViewById(R.id.progress_bar_detail);
        contentLayout = findViewById(R.id.content_layout_detail);

        // Subtasks
        layoutSubtasks = findViewById(R.id.layout_subtasks_container);
        rvSubtasks = findViewById(R.id.rv_subtasks_detail);
        indicatorProgress = findViewById(R.id.indicator_subtask_progress);
        tvProgressText = findViewById(R.id.tv_progress_text);

        rvSubtasks.setLayoutManager(new LinearLayoutManager(this));
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            getSupportActionBar().setTitle("");
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
                    checkAdminStatus();
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

    private void checkAdminStatus() {
        if (currentTask.getSpaceId() != null) {
            viewModel.getSpace(currentTask.getSpaceId()).observe(this, result -> {
                if (result instanceof Result.Success) {
                    Space space = ((Result.Success<Space>) result).data;
                    if (space == null) return;
                    String adminUid = space.getAdminUid();
                    if (adminUid == null && !space.getMembers().isEmpty()) adminUid = space.getMembers().get(0);

                    if (currentUser.getUid().equals(adminUid)) {
                        isAdmin = true;
                        setupSubtaskAdapter(); // Refresh adapter with admin status
                    }
                }
            });
        }
    }

    private void calculatePermissions() {
        if (currentTask == null || currentUser == null) return;
        String scope = currentTask.getOwnershipScope();
        if (scope == null) scope = Task.SCOPE_SHARED;

        boolean isCreator = currentUser.getUid().equals(currentTask.getCreatorUID());
        canEdit = false;
        canDelete = false;
        canComplete = false;

        switch (scope) {
            case Task.SCOPE_INDIVIDUAL:
                if (isCreator) { canEdit = true; canDelete = true; canComplete = true; }
                break;
            case Task.SCOPE_SHARED:
                canEdit = true; canDelete = true; canComplete = true;
                break;
            case Task.SCOPE_ASSIGNED:
                if (isCreator) { canEdit = true; canDelete = true; }
                String assignedTo = currentTask.getAssignedToUid();
                if (assignedTo != null && assignedTo.equals(currentUser.getUid())) {
                    canComplete = true;
                }
                if (isCreator) canComplete = true;
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

        // Creator
        boolean isCreator = currentUser.getUid().equals(currentTask.getCreatorUID());
        tvCreator.setText(isCreator ? "Created by You" : "Created by " + currentTask.getCreatorDisplayName());

        chipPriority.setText(currentTask.getPriority());
        chipType.setText(currentTask.getTaskType());

        String scopeText = getScopeDisplayString(currentTask.getOwnershipScope());
        if (Task.SCOPE_ASSIGNED.equals(currentTask.getOwnershipScope()) && currentTask.getAssignedToName() != null) {
            scopeText = "Assigned to " + currentTask.getAssignedToName();
        }
        chipScope.setText(scopeText);

        // --- LOGIC FOR SUBTASKS & COMPLETION ---
        boolean hasSubtasks = currentTask.getSubtasks() != null && !currentTask.getSubtasks().isEmpty();
        boolean isCompleted = Task.STATUS_COMPLETED.equals(currentTask.getStatus());

        if (hasSubtasks) {
            // 1. Has Subtasks: Hide main checkbox, show calculated progress
            layoutSubtasks.setVisibility(View.VISIBLE);
            setupSubtaskAdapter();

            int total = currentTask.getSubtasks().size();
            int done = currentTask.getProgressPercentage() * total / 100;

            indicatorProgress.setMax(100);
            indicatorProgress.setProgress(currentTask.getProgressPercentage(), true);
            tvProgressText.setText(done + "/" + total);

            // HIDE MANUAL CHECKBOX
            cbStatus.setVisibility(View.GONE);
            chipStatus.setVisibility(View.VISIBLE);
            chipStatus.setText(isCompleted ? "Completed (Auto)" : "In Progress (" + done + "/" + total + ")");

            // Check if we need to trigger animation if it just became 100% while we were watching?
            // (Handled by ViewModel or Repo usually, but pure UI update here is fine)

        } else {
            // 2. No Subtasks: Show main checkbox based on permissions
            layoutSubtasks.setVisibility(View.GONE);

            if (canComplete) {
                cbStatus.setVisibility(View.VISIBLE);
                chipStatus.setVisibility(View.GONE);
                cbStatus.setOnCheckedChangeListener(null);
                cbStatus.setChecked(isCompleted);
                cbStatus.setOnCheckedChangeListener((buttonView, isChecked) -> {
                    if (!buttonView.isPressed()) return;
                    if (isChecked) playCompleteAnimation();
                    else viewModel.updateTaskStatus(taskId, Task.STATUS_PENDING);
                });
            } else {
                cbStatus.setVisibility(View.GONE);
                chipStatus.setVisibility(View.VISIBLE);
                chipStatus.setText(isCompleted ? "Completed" : "Pending");
            }
        }
    }

    private void setupSubtaskAdapter() {
        if (currentTask.getSubtasks() == null) return;
        if (subtaskAdapter == null) {
            subtaskAdapter = new SubtaskAdapter(this, currentTask.getSubtasks(), currentUser.getUid(), isAdmin, this);
            rvSubtasks.setAdapter(subtaskAdapter);
        } else {
            subtaskAdapter.updateSubtasks(currentTask.getSubtasks());
        }
    }

    @Override
    public void onLockToggle(Subtask subtask) {
        boolean force = isAdmin && subtask.getLockedByUid() != null && !subtask.getLockedByUid().equals(currentUser.getUid());
        viewModel.toggleSubtaskLock(taskId, subtask.getId(), currentUser.getUid(), currentUser.getDisplayName(), force);
    }

    @Override
    public void onCompletionToggle(Subtask subtask, boolean isChecked) {
        viewModel.toggleSubtaskCompletion(taskId, subtask.getId(), isChecked, currentUser.getUid());
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