package com.pranav.synctask.activities;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.Timestamp;
import com.pranav.synctask.R;
import com.pranav.synctask.data.Result;
import com.pranav.synctask.models.Task;
import com.pranav.synctask.ui.viewmodels.EditTaskViewModel;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class EditTaskActivity extends AppCompatActivity {

    public static final String EXTRA_TASK = "EXTRA_TASK";
    private TextInputEditText etTitle, etDescription, etDueDate;
    private ChipGroup chipGroupType, chipGroupPriority;
    private Button btnSaveChanges;
    private Calendar selectedDueDate = Calendar.getInstance();
    private EditTaskViewModel viewModel;
    private Task currentTask;

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

        etTitle = findViewById(R.id.et_task_title);
        etDescription = findViewById(R.id.et_task_description);
        etDueDate = findViewById(R.id.et_task_due_date);

        chipGroupType = findViewById(R.id.chip_group_type);
        chipGroupPriority = findViewById(R.id.chip_group_priority);

        btnSaveChanges = findViewById(R.id.btn_save_task);

        setupDatePicker();
        populateData();

        btnSaveChanges.setOnClickListener(v -> saveChanges());
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