package com.pranav.synctask.activities;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.pranav.synctask.R;
import com.pranav.synctask.data.Result;
import com.pranav.synctask.models.Task;
import com.pranav.synctask.models.User;
import com.pranav.synctask.ui.viewmodels.CreateTaskViewModel;
import java.util.Calendar;
import java.util.Date;
import java.util.Objects;

public class CreateTaskActivity extends AppCompatActivity {

    private TextInputEditText etTitle, etDescription, etDueDate;
    private AutoCompleteTextView acTaskType;
    private final Calendar selectedDueDate = Calendar.getInstance();
    private String partnerUID;
    private CreateTaskViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_task);

        viewModel = new ViewModelProvider(this).get(CreateTaskViewModel.class);

        etTitle = findViewById(R.id.et_task_title);
        etDescription = findViewById(R.id.et_task_description);
        etDueDate = findViewById(R.id.et_task_due_date);
        acTaskType = findViewById(R.id.ac_task_type);
        Button btnCreateTask = findViewById(R.id.btn_create_task);

        setupTaskTypeDropdown();
        setupDatePicker();

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            viewModel.getPartnerInfo(currentUser.getUid()).observe(this, result -> {
                if (result instanceof Result.Success) {
                    User user = ((Result.Success<User>) result).data;
                    partnerUID = user.getPairedWithUID();
                } else if (result instanceof Result.Error) {
                    Toast.makeText(CreateTaskActivity.this, "Could not get partner info.", Toast.LENGTH_SHORT).show();
                    finish();
                }
            });
        }

        btnCreateTask.setOnClickListener(v -> createTask());
    }

    private void setupTaskTypeDropdown() {
        String[] taskTypes = {Task.TYPE_TASK, Task.TYPE_REMINDER, Task.TYPE_UPDATE};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, taskTypes);
        acTaskType.setAdapter(adapter);
    }

    private void setupDatePicker() {
        etDueDate.setOnClickListener(v -> {
            DatePickerDialog datePickerDialog = new DatePickerDialog(this,
                    (view, year, month, dayOfMonth) -> {
                        selectedDueDate.set(year, month, dayOfMonth);
                        etDueDate.setText(String.format("%d-%02d-%02d", year, month + 1, dayOfMonth));
                    },
                    selectedDueDate.get(Calendar.YEAR),
                    selectedDueDate.get(Calendar.MONTH),
                    selectedDueDate.get(Calendar.DAY_OF_MONTH));
            datePickerDialog.show();
        });
    }

    private void createTask() {
        String title = Objects.requireNonNull(etTitle.getText()).toString().trim();
        String description = Objects.requireNonNull(etDescription.getText()).toString().trim();
        String taskType = acTaskType.getText().toString().trim();
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();

        if (TextUtils.isEmpty(title) || TextUtils.isEmpty(taskType) || currentUser == null || partnerUID == null) {
            Toast.makeText(this, "Title and Task Type are required.", Toast.LENGTH_SHORT).show();
            return;
        }

        Timestamp dueDateTimestamp = null;
        if (!Objects.requireNonNull(etDueDate.getText()).toString().isEmpty()) {
            dueDateTimestamp = new Timestamp(new Date(selectedDueDate.getTimeInMillis()));
        }

        Task newTask = new Task(
                currentUser.getUid(),
                title,
                description,
                dueDateTimestamp,
                taskType,
                partnerUID
                        );

        viewModel.createTask(newTask).observe(this, result -> {
            if (result instanceof Result.Success) {
                Toast.makeText(this, "Task created!", Toast.LENGTH_SHORT).show();
                finish();
            } else if (result instanceof Result.Error) {
                Toast.makeText(CreateTaskActivity.this, "Error creating task.", Toast.LENGTH_SHORT).show();
            }
        });
    }
}