package com.pranav.synctask.activities;

import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.slider.Slider;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.transition.platform.MaterialContainerTransform;
import com.google.android.material.transition.platform.MaterialContainerTransformSharedElementCallback;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.pranav.synctask.R;
import com.pranav.synctask.data.Result;
import com.pranav.synctask.models.Space;
import com.pranav.synctask.models.Task;
import com.pranav.synctask.ui.CreateTaskViewModel;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class CreateTaskActivity extends AppCompatActivity {

    private TextInputEditText etTitle, etDescription, etDueDate;
    private ChipGroup chipGroupType, chipGroupPriority, chipGroupScope;
    private TextView tvScopeLabel;
    private Slider effortSlider;
    private Button btnCreateTask;
    private Calendar selectedDueDate = Calendar.getInstance();
    private String currentSpaceId;
    private String contextType;
    private CreateTaskViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Container Transform Setup
        View view = findViewById(android.R.id.content);
        setExitSharedElementCallback(new MaterialContainerTransformSharedElementCallback());
        MaterialContainerTransform transition = new MaterialContainerTransform();
        transition.setScrimColor(Color.TRANSPARENT);
        transition.setDuration(400);
        transition.addTarget(view);
        getWindow().setSharedElementEnterTransition(transition);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_task);

        currentSpaceId = getIntent().getStringExtra("SPACE_ID");
        contextType = getIntent().getStringExtra("CONTEXT_TYPE");
        if (contextType == null) contextType = Space.TYPE_SHARED;

        if (currentSpaceId == null || currentSpaceId.isEmpty()) {
            Toast.makeText(this, "Error: No Space ID provided.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        viewModel = new ViewModelProvider(this).get(CreateTaskViewModel.class);
        initializeViews();
        setupChips();
        setupDatePicker();

        btnCreateTask.setOnClickListener(v -> createTask());
    }

    private void initializeViews() {
        etTitle = findViewById(R.id.et_task_title);
        etDescription = findViewById(R.id.et_task_description);
        etDueDate = findViewById(R.id.et_task_due_date);

        chipGroupType = findViewById(R.id.chip_group_type);
        chipGroupPriority = findViewById(R.id.chip_group_priority);
        chipGroupScope = findViewById(R.id.chip_group_scope);

        tvScopeLabel = findViewById(R.id.tv_scope_label);
        effortSlider = findViewById(R.id.slider_task_effort);
        btnCreateTask = findViewById(R.id.btn_create_task);
    }

    private void setupChips() {
        // Handle Dynamic Scope Visibility based on Context
        if (Space.TYPE_PERSONAL.equals(contextType)) {
            // Personal Mode: Show "Me" and "Partner", Hide "Shared"/"Assigned"
            tvScopeLabel.setText(getString(R.string.owner_hint));
            findViewById(R.id.chip_scope_shared).setVisibility(View.GONE);
            findViewById(R.id.chip_scope_assigned).setVisibility(View.GONE);

            findViewById(R.id.chip_scope_me).setVisibility(View.VISIBLE);
            findViewById(R.id.chip_scope_partner).setVisibility(View.VISIBLE);

            // Default Select "Me"
            chipGroupScope.check(R.id.chip_scope_me);
        } else {
            // Shared Mode: Show "Shared" and "Assigned"
            tvScopeLabel.setText(getString(R.string.scope_hint));
            findViewById(R.id.chip_scope_shared).setVisibility(View.VISIBLE);
            findViewById(R.id.chip_scope_assigned).setVisibility(View.VISIBLE);

            findViewById(R.id.chip_scope_me).setVisibility(View.GONE);
            findViewById(R.id.chip_scope_partner).setVisibility(View.GONE);

            // Default Select "Shared"
            chipGroupScope.check(R.id.chip_scope_shared);
        }
    }

    private void setupDatePicker() {
        etDueDate.setOnClickListener(v -> {
            MaterialDatePicker<Long> datePicker = MaterialDatePicker.Builder.datePicker()
                    .setTitleText("Select Due Date")
                    .setSelection(MaterialDatePicker.todayInUtcMilliseconds())
                    .build();

            datePicker.addOnPositiveButtonClickListener(selection -> {
                selectedDueDate.setTimeInMillis(selection);
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
                etDueDate.setText(sdf.format(new Date(selection)));
            });

            datePicker.show(getSupportFragmentManager(), "MATERIAL_DATE_PICKER");
        });
    }

    private void createTask() {
        String title = etTitle.getText().toString().trim();
        String description = etDescription.getText().toString().trim();
        int effort = (int) effortSlider.getValue();
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();

        if (TextUtils.isEmpty(title) || currentUser == null) {
            etTitle.setError("Title is required");
            return;
        }

        // 1. Get Values from Chips
        String taskType = getSelectedType();
        String priority = getSelectedPriority();
        String ownershipScope = getSelectedScope();

        Timestamp dueDateTimestamp = null;
        if (!etDueDate.getText().toString().isEmpty()) {
            dueDateTimestamp = new Timestamp(new Date(selectedDueDate.getTimeInMillis()));
        }

        Task newTask = new Task(currentUser.getUid(), title, description, dueDateTimestamp, taskType);
        newTask.setPriority(priority);
        newTask.setSpaceId(currentSpaceId);
        newTask.setOwnershipScope(ownershipScope);
        newTask.setEffort(effort);

        // 2. Create
        btnCreateTask.setEnabled(false); // Prevent double click
        btnCreateTask.setText("Creating...");

        viewModel.createTask(newTask, this).observe(this, result -> {
            if (result instanceof Result.Success) {
                Toast.makeText(this, "Task created!", Toast.LENGTH_SHORT).show();
                finish();
            } else if (result instanceof Result.Error) {
                btnCreateTask.setEnabled(true);
                btnCreateTask.setText(R.string.create_task_button);
                Toast.makeText(CreateTaskActivity.this, "Error creating task.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Helper to map Chip IDs to Constants
    private String getSelectedType() {
        int id = chipGroupType.getCheckedChipId();
        if (id == R.id.chip_type_reminder) return Task.TYPE_REMINDER;
        if (id == R.id.chip_type_update) return Task.TYPE_UPDATE;
        return Task.TYPE_TASK; // Default
    }

    private String getSelectedPriority() {
        int id = chipGroupPriority.getCheckedChipId();
        if (id == R.id.chip_prio_high) return "High";
        if (id == R.id.chip_prio_low) return "Low";
        return "Normal"; // Default
    }

    private String getSelectedScope() {
        int id = chipGroupScope.getCheckedChipId();
        if (id == R.id.chip_scope_assigned) return Task.SCOPE_ASSIGNED;
        if (id == R.id.chip_scope_me) return Task.SCOPE_INDIVIDUAL;
        if (id == R.id.chip_scope_partner) return Task.SCOPE_ASSIGNED; // "Partner's Task" is an assignment
        return Task.SCOPE_SHARED; // Default
    }
}