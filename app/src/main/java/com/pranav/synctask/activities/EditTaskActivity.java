package com.pranav.synctask.activities;

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
import com.google.android.material.textfield.TextInputEditText;
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
import com.pranav.synctask.ui.viewmodels.EditTaskViewModel;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.stream.Collectors;

public class EditTaskActivity extends AppCompatActivity {

    public static final String EXTRA_TASK = "EXTRA_TASK";
    private TextInputEditText etTitle, etDescription, etDueDate;
    private ChipGroup chipGroupType, chipGroupPriority;
    private MaterialButton btnSaveChanges, btnAddSubtask, btnAssignMember;
    private LinearLayout layoutAdminAssignment;
    private RecyclerView rvSubtasks;

    private Calendar selectedDueDate = Calendar.getInstance();
    private EditTaskViewModel viewModel;
    private Task currentTask;
    private CreateSubtaskAdapter subtaskAdapter;
    private List<Subtask> subtasksList;

    private boolean isAdmin = false;
    private List<User> spaceMembers = new ArrayList<>();
    private User selectedAssignee = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_task);

        currentTask = (Task) getIntent().getSerializableExtra(EXTRA_TASK);
        if (currentTask == null) {
            Toast.makeText(this, "Error: Task data missing.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        viewModel = new ViewModelProvider(this).get(EditTaskViewModel.class);

        // Clone subtasks to avoid modifying reference directly before save
        subtasksList = new ArrayList<>();
        if (currentTask.getSubtasks() != null) {
            subtasksList.addAll(currentTask.getSubtasks());
        }

        initializeViews();
        setupSubtaskList();
        setupDatePicker();
        populateData();
        checkAdminPermissions();

        btnSaveChanges.setOnClickListener(v -> saveChanges());
    }

    private void initializeViews() {
        etTitle = findViewById(R.id.et_task_title);
        etDescription = findViewById(R.id.et_task_description);
        etDueDate = findViewById(R.id.et_task_due_date);

        chipGroupType = findViewById(R.id.chip_group_type);
        chipGroupPriority = findViewById(R.id.chip_group_priority);

        btnSaveChanges = findViewById(R.id.btn_save_task);
        btnAddSubtask = findViewById(R.id.btn_add_subtask_edit);
        rvSubtasks = findViewById(R.id.rv_edit_subtasks);

        layoutAdminAssignment = findViewById(R.id.layout_admin_assignment_edit);
        btnAssignMember = findViewById(R.id.btn_assign_member_edit);

        btnAddSubtask.setOnClickListener(v -> subtaskAdapter.addSubtask());
        btnAssignMember.setOnClickListener(v -> showAssignMemberDialog());
    }

    private void setupSubtaskList() {
        subtaskAdapter = new CreateSubtaskAdapter(subtasksList);
        rvSubtasks.setLayoutManager(new LinearLayoutManager(this));
        rvSubtasks.setAdapter(subtaskAdapter);
    }

    private void setupDatePicker() {
        etDueDate.setOnClickListener(v -> {
            MaterialDatePicker<Long> datePicker = MaterialDatePicker.Builder.datePicker()
                    .setTitleText("Select Due Date")
                    .setSelection(currentTask.getDueDate() != null ? currentTask.getDueDate().toDate().getTime() : MaterialDatePicker.todayInUtcMilliseconds())
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

    private void populateData() {
        etTitle.setText(currentTask.getTitle());
        etDescription.setText(currentTask.getDescription());

        // Set Type Chip
        String type = currentTask.getTaskType();
        if (Task.TYPE_REMINDER.equals(type)) chipGroupType.check(R.id.chip_type_reminder);
        else if (Task.TYPE_UPDATE.equals(type)) chipGroupType.check(R.id.chip_type_update);
        else chipGroupType.check(R.id.chip_type_task);

        // Set Priority Chip
        String priority = currentTask.getPriority();
        if ("High".equalsIgnoreCase(priority)) chipGroupPriority.check(R.id.chip_prio_high);
        else if ("Low".equalsIgnoreCase(priority)) chipGroupPriority.check(R.id.chip_prio_low);
        else chipGroupPriority.check(R.id.chip_prio_normal);

        if (currentTask.getDueDate() != null) {
            selectedDueDate.setTime(currentTask.getDueDate().toDate());
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            etDueDate.setText(sdf.format(selectedDueDate.getTime()));
        }

        // Pre-set Assignee display
        if (currentTask.getAssignedToUid() != null) {
            btnAssignMember.setText("Assigned to: " + currentTask.getAssignedToName());
            // We need to fetch the User object for logic consistency, done in checkAdminPermissions
        }
    }

    private void checkAdminPermissions() {
        if (currentTask.getSpaceId() == null) return;

        // Don't show assignment for personal
        if (Space.TYPE_PERSONAL.equals(currentTask.getSpaceId())) return; // Rough check, ideally check context type

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) return;

        viewModel.getSpace(currentTask.getSpaceId()).observe(this, result -> {
            if (result instanceof Result.Success) {
                Space space = ((Result.Success<Space>) result).data;
                if (space != null) {
                    String adminUid = space.getAdminUid();
                    if (adminUid == null && !space.getMembers().isEmpty()) adminUid = space.getMembers().get(0);

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

                // Restore selectedAssignee object if it exists
                if (currentTask.getAssignedToUid() != null) {
                    for (User u : spaceMembers) {
                        if (u.getUid().equals(currentTask.getAssignedToUid())) {
                            selectedAssignee = u;
                            break;
                        }
                    }
                }
            }
        });
    }

    private void showAssignMemberDialog() {
        if (spaceMembers.isEmpty()) {
            Toast.makeText(this, "No members found.", Toast.LENGTH_SHORT).show();
            return;
        }

        String[] memberNames = new String[spaceMembers.size() + 1];
        memberNames[0] = "None (Shared Task)";
        for (int i = 0; i < spaceMembers.size(); i++) {
            User u = spaceMembers.get(i);
            memberNames[i+1] = u.getDisplayName() != null ? u.getDisplayName() : "Unknown";
        }

        new MaterialAlertDialogBuilder(this)
                .setTitle("Change Assignment")
                .setItems(memberNames, (dialog, which) -> {
                    if (which == 0) {
                        selectedAssignee = null;
                        btnAssignMember.setText("Assign to Member (Optional)");
                        btnAssignMember.setIconResource(R.drawable.ic_profile);
                    } else {
                        selectedAssignee = spaceMembers.get(which - 1);
                        btnAssignMember.setText("Assigned to: " + selectedAssignee.getDisplayName());
                        btnAssignMember.setIconResource(0);
                    }
                })
                .show();
    }

    private void saveChanges() {
        String title = etTitle.getText().toString().trim();
        if (TextUtils.isEmpty(title)) {
            Toast.makeText(this, "Title cannot be empty.", Toast.LENGTH_SHORT).show();
            return;
        }

        currentTask.setTitle(title);
        currentTask.setDescription(etDescription.getText().toString().trim());
        currentTask.setTaskType(getSelectedType());
        currentTask.setPriority(getSelectedPriority());

        if (!etDueDate.getText().toString().isEmpty()) {
            currentTask.setDueDate(new Timestamp(new Date(selectedDueDate.getTimeInMillis())));
        } else {
            currentTask.setDueDate(null);
        }

        // 1. Update Subtasks
        List<Subtask> validSubtasks = subtasksList.stream()
                .filter(s -> !s.getTitle().trim().isEmpty())
                .collect(Collectors.toList());
        currentTask.setSubtasks(validSubtasks);

        // 2. Update Assignment (Only if Admin changed it)
        if (isAdmin) {
            if (selectedAssignee != null) {
                currentTask.setOwnershipScope(Task.SCOPE_ASSIGNED);
                currentTask.setAssignedToUid(selectedAssignee.getUid());
                currentTask.setAssignedToName(selectedAssignee.getDisplayName());
            } else {
                // If clearing assignment, revert to Shared
                if (Task.SCOPE_ASSIGNED.equals(currentTask.getOwnershipScope())) {
                    currentTask.setOwnershipScope(Task.SCOPE_SHARED);
                }
                currentTask.setAssignedToUid(null);
                currentTask.setAssignedToName(null);
            }
        }

        viewModel.updateTask(currentTask).observe(this, result -> {
            if (result instanceof Result.Success) {
                Toast.makeText(this, "Task updated!", Toast.LENGTH_SHORT).show();
                finish();
            } else if (result instanceof Result.Error) {
                Toast.makeText(this, "Error updating task.", Toast.LENGTH_SHORT).show();
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