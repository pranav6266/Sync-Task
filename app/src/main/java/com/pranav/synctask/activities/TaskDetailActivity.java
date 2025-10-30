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
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
// import com.google.android.material.slider.Slider; // REMOVED IN PHASE 1
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
    private TextView tvTitle, tvDescription, tvDueDate, tvPriority, tvScope, tvCreator, tvEffort; // MODIFIED IN PHASE 1
    private CheckBox cbStatus;
    // private Slider progressSlider; // REMOVED IN PHASE 1
    private ProgressBar progressBar;
    private View contentLayout;
    private Toolbar toolbar;
    private AppBarLayout appBarLayout; // ADDED IN PHASE 1
    private LottieAnimationView lottieAnimationView; // ADDED IN PHASE 1
    private boolean canEdit = false;
    private boolean canDelete = false;
    private boolean canComplete = false;
    // private boolean canUpdateProgress = false; // REMOVED IN PHASE 1

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
        // setupProgressSliderListener(); // REMOVED IN PHASE 1
    }

    private void initializeViews() {
        toolbar = findViewById(R.id.toolbar_task_detail);
        appBarLayout = findViewById(R.id.app_bar_layout); // ADDED IN PHASE 1
        tvTitle = findViewById(R.id.tv_task_title_detail);
        tvDescription = findViewById(R.id.tv_task_description_detail);
        tvDueDate = findViewById(R.id.tv_task_due_date_detail);
        tvPriority = findViewById(R.id.tv_task_priority_detail);
        tvScope = findViewById(R.id.tv_task_scope_detail);
        tvCreator = findViewById(R.id.tv_task_creator_detail);
        tvEffort = findViewById(R.id.tv_task_effort_detail); // ADDED IN PHASE 1
        cbStatus = findViewById(R.id.cb_task_status_detail);
        progressBar = findViewById(R.id.progress_bar_detail);
        contentLayout = findViewById(R.id.content_layout_detail);
        lottieAnimationView = findViewById(R.id.lottie_complete_animation); // ADDED IN PHASE 1
        // REMOVED progressSlider and tvProgressPercentage
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
        // canUpdateProgress = false; // REMOVED IN PHASE 1

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
                    canComplete = false;
                } else {
                    canEdit = false;
                    canDelete = false;
                    canComplete = true;
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
        tvEffort.setText(String.valueOf(currentTask.getEffort())); // ADDED IN PHASE 1
        tvScope.setText(getScopeDisplayString(currentTask.getOwnershipScope()));

        boolean isCreator = currentUser.getUid().equals(currentTask.getCreatorUID());
        tvCreator.setText(isCreator ? getString(R.string.task_creator_label_you) : currentTask.getCreatorDisplayName());

        // MODIFIED Checkbox logic IN PHASE 1
        cbStatus.setOnCheckedChangeListener(null); // Remove listener to set initial state
        cbStatus.setChecked(Task.STATUS_COMPLETED.equals(currentTask.getStatus()));
        cbStatus.setEnabled(canComplete); // Enable/disable based on permission

        // Add new listener
        cbStatus.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (!buttonView.isPressed()) {
                return; // Ignore programmatic changes
            }

            if (isChecked) {
                // User checked the box
                playCompleteAnimation();
            } else {
                // User unchecked the box
                viewModel.updateTaskStatus(taskId, Task.STATUS_PENDING);
            }
        });

        // REMOVED All progress slider logic IN PHASE 1
    }

    // ADDED IN PHASE 1
    private void playCompleteAnimation() {
        // Disable UI
        contentLayout.setVisibility(View.GONE);
        appBarLayout.setVisibility(View.GONE);

        // Play animation
        lottieAnimationView.setVisibility(View.VISIBLE);
        lottieAnimationView.playAnimation();

        // Update status in Firestore
        viewModel.updateTaskStatus(taskId, Task.STATUS_COMPLETED);

        // Add listener to finish activity on animation end
        lottieAnimationView.addAnimatorListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(@NonNull Animator animation) {}

            @Override
            public void onAnimationEnd(@NonNull Animator animation) {
                finish();
            }

            @Override
            public void onAnimationCancel(@NonNull Animator animation) {}

            @Override
            public void onAnimationRepeat(@NonNull Animator animation) {}
        });
    }

    // REMOVED progressChangeListener and setupProgressSliderListener IN PHASE 1

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