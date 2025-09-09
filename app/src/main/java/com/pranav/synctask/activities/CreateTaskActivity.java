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
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.pranav.synctask.R;
import com.pranav.synctask.data.Result;
import com.pranav.synctask.models.Task;
import com.pranav.synctask.models.User;
import com.pranav.synctask.ui.CreateTaskViewModel;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class CreateTaskActivity extends AppCompatActivity {

    private TextInputEditText etTitle, etDescription, etDueDate;
    private AutoCompleteTextView acTaskType, acTaskPriority; // PHASE 4: Added priority
    private Button btnCreateTask;
    private Calendar selectedDueDate = Calendar.getInstance();
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
        acTaskPriority = findViewById(R.id.ac_task_priority); // PHASE 4
        btnCreateTask = findViewById(R.id.btn_create_task);

        setupDropdowns();
        setupDatePicker();

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            viewModel.getPartnerInfo(currentUser.getUid()).observe(this, result -> {
                if (result instanceof Result.Success) {
                    partnerUID = ((Result.Success<User>) result).data.getPairedWithUID();
                } else if (result instanceof Result.Error) {
                    Toast.makeText(CreateTaskActivity.this, "Could not get user info.", Toast.LENGTH_SHORT).show();
                }
            });
        }

        btnCreateTask.setOnClickListener(v -> createTask());
    }

    private void setupDropdowns() {
        String[] taskTypes = {Task.TYPE_TASK, Task.TYPE_REMINDER, Task.TYPE_UPDATE};
        ArrayAdapter<String> typeAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, taskTypes);
        acTaskType.setAdapter(typeAdapter);
        acTaskType.setText(Task.TYPE_TASK, false); // Set default value

        // PHASE 4: Setup priority dropdown
        String[] priorities = {"Low", "Normal", "High"};
        ArrayAdapter<String> priorityAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, priorities);
        acTaskPriority.setAdapter(priorityAdapter);
        acTaskPriority.setText("Normal", false); // Set default value
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
        String taskType = acTaskType.getText().toString().trim();
        String priority = acTaskPriority.getText().toString().trim(); // PHASE 4
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();

        if (TextUtils.isEmpty(title) || TextUtils.isEmpty(taskType) || currentUser == null) {
            Toast.makeText(this, "Title and Task Type are required.", Toast.LENGTH_SHORT).show();
            return;
        }

        Timestamp dueDateTimestamp = null;
        if (!etDueDate.getText().toString().isEmpty()) {
            dueDateTimestamp = new Timestamp(new Date(selectedDueDate.getTimeInMillis()));
        }

        Task newTask = new Task(currentUser.getUid(), title, description, dueDateTimestamp, taskType);
        newTask.setPriority(priority); // PHASE 4: Set priority on the new task

        viewModel.createTask(newTask, this).observe(this, result -> {
            if (result instanceof Result.Success) {
                Toast.makeText(this, "Task created!", Toast.LENGTH_SHORT).show();
                finish();
            } else if (result instanceof Result.Error) {
                Toast.makeText(CreateTaskActivity.this, "Error creating task.", Toast.LENGTH_SHORT).show();
            }
        });
    }
}