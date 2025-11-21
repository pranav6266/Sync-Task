package com.pranav.synctask.activities;

import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.slider.Slider;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.transition.platform.MaterialContainerTransform;
import com.google.android.material.transition.platform.MaterialContainerTransformSharedElementCallback;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.pranav.synctask.R;
import com.pranav.synctask.adapters.CreateSubtaskAdapter;
import com.pranav.synctask.data.Result;
import com.pranav.synctask.models.Space;
import com.pranav.synctask.models.Subtask;
import com.pranav.synctask.models.Task;
import com.pranav.synctask.models.User;
import com.pranav.synctask.ui.CreateTaskViewModel;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.stream.Collectors;

public class CreateTaskActivity extends AppCompatActivity {

    private TextInputEditText etTitle, etDescription, etDueDate;
    private ChipGroup chipGroupType, chipGroupPriority;
    private Slider effortSlider;
    private Button btnCreateTask, btnAddSubtask ;
    private MaterialButton btnAssignMember;
    private LinearLayout layoutAdminAssignment;
    private RecyclerView rvSubtasks;

    private Calendar selectedDueDate = Calendar.getInstance();
    private String currentSpaceId;
    private String contextType;
    private CreateTaskViewModel viewModel;
    private CreateSubtaskAdapter subtaskAdapter;
    private List<Subtask> subtasksList = new ArrayList<>();

    private boolean isAdmin = false;
    private List<User> spaceMembers = new ArrayList<>();
    private User selectedAssignee = null;

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
        setupSubtaskList();
        setupDatePicker();
        checkAdminPermissions();

        btnCreateTask.setOnClickListener(v -> createTask());
    }

    private void initializeViews() {
        etTitle = findViewById(R.id.et_task_title);
        etDescription = findViewById(R.id.et_task_description);
        etDueDate = findViewById(R.id.et_task_due_date);

        chipGroupType = findViewById(R.id.chip_group_type);
        chipGroupPriority = findViewById(R.id.chip_group_priority);

        effortSlider = findViewById(R.id.slider_task_effort);
        btnCreateTask = findViewById(R.id.btn_create_task);

        // Subtask Views
        btnAddSubtask = findViewById(R.id.btn_add_subtask);
        rvSubtasks = findViewById(R.id.rv_create_subtasks);

        // Admin Assignment Views
        layoutAdminAssignment = findViewById(R.id.layout_admin_assignment);
        btnAssignMember = findViewById(R.id.btn_assign_member);

        btnAddSubtask.setOnClickListener(v -> subtaskAdapter.addSubtask());
        btnAssignMember.setOnClickListener(v -> showAssignMemberDialog());
    }

    private void setupSubtaskList() {
        subtaskAdapter = new CreateSubtaskAdapter(subtasksList);
        rvSubtasks.setLayoutManager(new LinearLayoutManager(this));
        rvSubtasks.setAdapter(subtaskAdapter);
    }

    private void checkAdminPermissions() {
        // If this is a Personal space, we skip admin checks (it's always individual)
        if (Space.TYPE_PERSONAL.equals(contextType)) {
            return;
        }

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) return;

        viewModel.getSpace(currentSpaceId).observe(this, result -> {
            if (result instanceof Result.Success) {
                Space space = ((Result.Success<Space>) result).data;
                if (space != null) {
                    // Check if current user is the admin/creator
                    String adminUid = space.getAdminUid();
                    // Fallback for legacy spaces without adminUid: check first member or assume no admin features
                    if (adminUid == null && !space.getMembers().isEmpty()) {
                        adminUid = space.getMembers().get(0);
                    }

                    if (currentUser.getUid().equals(adminUid)) {
                        isAdmin = true;
                        layoutAdminAssignment.setVisibility(View.VISIBLE);
                        fetchMembers(space.getMembers());
                    }
                }
            }
        });
    }

    private void fetchMembers(List<String> memberUids) {
        viewModel.getSpaceMembers(memberUids).observe(this, result -> {
            if (result instanceof Result.Success) {
                spaceMembers = ((Result.Success<List<User>>) result).data;
            }
        });
    }

    private void showAssignMemberDialog() {
        if (spaceMembers.isEmpty()) {
            Toast.makeText(this, "No members found to assign.", Toast.LENGTH_SHORT).show();
            return;
        }

        String[] memberNames = new String[spaceMembers.size() + 1];
        memberNames[0] = "None (Shared Task)"; // Option to clear assignment
        for (int i = 0; i < spaceMembers.size(); i++) {
            User u = spaceMembers.get(i);
            memberNames[i+1] = u.getDisplayName() != null ? u.getDisplayName() : "Unknown";
        }

        new MaterialAlertDialogBuilder(this)
                .setTitle("Assign Task To")
                .setItems(memberNames, (dialog, which) -> {
                    if (which == 0) {
                        selectedAssignee = null;
                        btnAssignMember.setText("Assign to Member (Optional)");
                        btnAssignMember.setIconResource(R.drawable.ic_profile);
                    } else {
                        selectedAssignee = spaceMembers.get(which - 1);
                        btnAssignMember.setText("Assigned to: " + selectedAssignee.getDisplayName());
                        // Ideally set icon tint or something to show active state
                    }
                })
                .show();
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

        String taskType = getSelectedType();
        String priority = getSelectedPriority();

        // Determine Scope
        String ownershipScope;
        if (Space.TYPE_PERSONAL.equals(contextType)) {
            ownershipScope = Task.SCOPE_INDIVIDUAL;
        } else {
            if (selectedAssignee != null) {
                ownershipScope = Task.SCOPE_ASSIGNED;
            } else {
                ownershipScope = Task.SCOPE_SHARED;
            }
        }

        Timestamp dueDateTimestamp = null;
        if (!etDueDate.getText().toString().isEmpty()) {
            dueDateTimestamp = new Timestamp(new Date(selectedDueDate.getTimeInMillis()));
        }

        Task newTask = new Task(currentUser.getUid(), title, description, dueDateTimestamp, taskType);
        newTask.setPriority(priority);
        newTask.setSpaceId(currentSpaceId);
        newTask.setOwnershipScope(ownershipScope);
        newTask.setEffort(effort);

        // 1. Add Subtasks (Filter empty ones)
        List<Subtask> validSubtasks = subtasksList.stream()
                .filter(s -> !s.getTitle().trim().isEmpty())
                .collect(Collectors.toList());
        newTask.setSubtasks(validSubtasks);

        // 2. Add Assignment
        if (selectedAssignee != null) {
            newTask.setAssignedToUid(selectedAssignee.getUid());
            newTask.setAssignedToName(selectedAssignee.getDisplayName());
        }

        btnCreateTask.setEnabled(false);
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

    private String getSelectedType() {
        int id = chipGroupType.getCheckedChipId();
        if (id == R.id.chip_type_reminder) return Task.TYPE_REMINDER;
        if (id == R.id.chip_type_update) return Task.TYPE_UPDATE;
        return Task.TYPE_TASK;
    }

    private String getSelectedPriority() {
        int id = chipGroupPriority.getCheckedChipId();
        if (id == R.id.chip_prio_high) return "High";
        if (id == R.id.chip_prio_low) return "Low";
        return "Normal";
    }
}