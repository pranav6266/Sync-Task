package com.pranav.synctask.activities;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.pranav.synctask.R;
import com.pranav.synctask.models.Task;
import com.pranav.synctask.models.User;
import com.pranav.synctask.utils.FirebaseHelper;
import java.util.Calendar;
import java.util.Date;

public class CreateTaskActivity extends AppCompatActivity {

    private TextInputEditText etTitle, etDescription, etDueDate;
    private AutoCompleteTextView acTaskType;
    private Button btnCreateTask;
    private Calendar selectedDueDate = Calendar.getInstance();
    private String partnerUID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_task);

        etTitle = findViewById(R.id.et_task_title);
        etDescription = findViewById(R.id.et_task_description);
        etDueDate = findViewById(R.id.et_task_due_date);
        acTaskType = findViewById(R.id.ac_task_type);
        btnCreateTask = findViewById(R.id.btn_create_task);

        setupTaskTypeDropdown();
        setupDatePicker();

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            FirebaseHelper.getUser(currentUser.getUid(), new FirebaseHelper.UserCallback() {
                @Override
                public void onSuccess(User user) {
                    partnerUID = user.getPairedWithUID();
                }

                @Override
                public void onError(Exception e) {
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
        String title = etTitle.getText().toString().trim();
        String description = etDescription.getText().toString().trim();
        String taskType = acTaskType.getText().toString().trim();
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();

        if (TextUtils.isEmpty(title) || TextUtils.isEmpty(taskType) || currentUser == null || partnerUID == null) {
            Toast.makeText(this, "Title and Task Type are required.", Toast.LENGTH_SHORT).show();
            return;
        }

        Timestamp dueDateTimestamp = null;
        if (!etDueDate.getText().toString().isEmpty()) {
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

        FirebaseHelper.createTask(newTask, new FirebaseHelper.TasksCallback() {
            @Override
            public void onSuccess(java.util.List<Task> tasks) {
                // Not used here, but required by interface
            }

            @Override
            public void onError(Exception e) {
                Toast.makeText(CreateTaskActivity.this, "Error creating task.", Toast.LENGTH_SHORT).show();
            }
        });

        Toast.makeText(this, "Task created!", Toast.LENGTH_SHORT).show();
        finish();
    }
}
