package com.pranav.synctask.activities;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
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
    private AutoCompleteTextView acTaskType, acTaskPriority;
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
        acTaskType = findViewById(R.id.ac_task_type);
        acTaskPriority = findViewById(R.id.ac_task_priority);
        btnSaveChanges = findViewById(R.id.btn_save_task);

        setupDropdowns();
        setupDatePicker();
        populateData();

        btnSaveChanges.setOnClickListener(v -> saveChanges());
    }

    private void setupDropdowns() {
        String[] taskTypes = {Task.TYPE_TASK, Task.TYPE_REMINDER, Task.TYPE_UPDATE};
        ArrayAdapter<String> typeAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, taskTypes);
        acTaskType.setAdapter(typeAdapter);

        String[] priorities = {"Low", "Normal", "High"};
        ArrayAdapter<String> priorityAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, priorities);
        acTaskPriority.setAdapter(priorityAdapter);
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
        acTaskType.setText(currentTask.getTaskType(), false);
        acTaskPriority.setText(currentTask.getPriority(), false);

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
        currentTask.setTaskType(acTaskType.getText().toString());
        currentTask.setPriority(acTaskPriority.getText().toString());

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
}