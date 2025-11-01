package com.pranav.synctask.activities;
import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View; // ADDED
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.slider.Slider; // ADDED IN PHASE 1
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout; // ADDED
import com.google.android.material.transition.platform.MaterialContainerTransform;
// ADDED
import com.google.android.material.transition.platform.MaterialContainerTransformSharedElementCallback;
// ADDED
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.pranav.synctask.R;
import com.pranav.synctask.data.Result;
import com.pranav.synctask.models.Space; // ADDED
import com.pranav.synctask.models.Task;
import com.pranav.synctask.models.User;
import com.pranav.synctask.ui.CreateTaskViewModel;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
public class CreateTaskActivity extends AppCompatActivity {


    private TextInputEditText etTitle, etDescription, etDueDate;
    private AutoCompleteTextView acTaskType, acTaskPriority, acTaskScope;
    private TextInputLayout layoutTaskScope; // ADDED
    private Slider effortSlider; // ADDED IN PHASE 1
    private Button btnCreateTask;
    private Calendar selectedDueDate = Calendar.getInstance();
    private String currentSpaceId;
    private String contextType; // ADDED
    private CreateTaskViewModel viewModel;
    private final Map<String, String> scopeMap = new HashMap<>();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // --- ADDED: Container Transform Setup ---
        View view = findViewById(android.R.id.content);
        setExitSharedElementCallback(new MaterialContainerTransformSharedElementCallback());
        MaterialContainerTransform transition = new MaterialContainerTransform();
        transition.setScrimColor(Color.TRANSPARENT);
        transition.setDuration(400);
        transition.addTarget(view);
        getWindow().setSharedElementEnterTransition(transition);
        // --- END ADDED ---

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_task);

        currentSpaceId = getIntent().getStringExtra("SPACE_ID");
        // --- ADDED ---
        contextType = getIntent().getStringExtra("CONTEXT_TYPE");
        if (contextType == null) {
            contextType = Space.TYPE_SHARED; // Default to shared
        }
        // --- END ADDED ---

        if (currentSpaceId == null || currentSpaceId.isEmpty()) {
            Toast.makeText(this, "Error: No Space ID provided.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        viewModel = new ViewModelProvider(this).get(CreateTaskViewModel.class);

        etTitle = findViewById(R.id.et_task_title);
        etDescription = findViewById(R.id.et_task_description);
        etDueDate = findViewById(R.id.et_task_due_date);
        acTaskType = findViewById(R.id.ac_task_type);
        acTaskPriority = findViewById(R.id.ac_task_priority);
        acTaskScope = findViewById(R.id.ac_task_scope);
        layoutTaskScope = findViewById(R.id.layout_task_scope); // ADDED
        effortSlider = findViewById(R.id.slider_task_effort);
        // ADDED IN PHASE 1
        btnCreateTask = findViewById(R.id.btn_create_task);

        setupDropdowns();
        setupDatePicker();

        btnCreateTask.setOnClickListener(v -> createTask());
    }

    private void setupDropdowns() {
        // --- Task Type ---
        String[] taskTypes = {Task.TYPE_TASK, Task.TYPE_REMINDER, Task.TYPE_UPDATE};
        ArrayAdapter<String> typeAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, taskTypes);
        acTaskType.setAdapter(typeAdapter);
        acTaskType.setText(Task.TYPE_TASK, false);

        // --- Priority ---
        String[] priorities = {"Low", "Normal", "High"};
        ArrayAdapter<String> priorityAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, priorities);
        acTaskPriority.setAdapter(priorityAdapter);
        acTaskPriority.setText("Normal", false);

        // --- MODIFIED: Context-Aware Scope ---
        scopeMap.clear();
        String[] scopeDisplayNames;
        String defaultScope;

        if (Space.TYPE_PERSONAL.equals(contextType)) {
            // Personal Context: Owner
            layoutTaskScope.setHint(getString(R.string.owner_hint)); 
            scopeMap.put(getString(R.string.owner_me), Task.SCOPE_INDIVIDUAL); 
            scopeMap.put(getString(R.string.owner_partner), Task.SCOPE_ASSIGNED); 
            scopeDisplayNames = scopeMap.keySet().toArray(new String[0]); 
            defaultScope = getString(R.string.owner_me);
        } else {
            // Shared Context: Scope (REMOVING "INDIVIDUAL")
            layoutTaskScope.setHint(getString(R.string.scope_hint)); 
            scopeMap.put(getString(R.string.scope_shared), Task.SCOPE_SHARED);
            scopeMap.put(getString(R.string.scope_assigned), Task.SCOPE_ASSIGNED);
            scopeDisplayNames = scopeMap.keySet().toArray(new String[0]);
            defaultScope = getString(R.string.scope_shared);
        }

        ArrayAdapter<String> scopeAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, scopeDisplayNames);
        acTaskScope.setAdapter(scopeAdapter);
        acTaskScope.setText(defaultScope, false);
        // --- END MODIFIED ---
    }

    private void setupDatePicker() {
        // ... (no changes in this method)
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
        // --- MODIFIED: Read from scopeMap ---
        String title = etTitle.getText().toString().trim();
        String description = etDescription.getText().toString().trim();
        String taskType = acTaskType.getText().toString().trim();
        String priority = acTaskPriority.getText().toString().trim();
        String scopeDisplayName = acTaskScope.getText().toString().trim();
        String ownershipScope = scopeMap.get(scopeDisplayName); // Get mapped value
        int effort = (int) effortSlider.getValue(); // ADDED IN PHASE 1
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (TextUtils.isEmpty(title) || TextUtils.isEmpty(taskType) || TextUtils.isEmpty(ownershipScope) || currentUser == null) {
            Toast.makeText(this, "Title, Type, and Scope/Owner are required.", Toast.LENGTH_SHORT).show();
            return;
        }
        // --- END MODIFIED ---

        Timestamp dueDateTimestamp = null;
        if (!etDueDate.getText().toString().isEmpty()) {
            dueDateTimestamp = new Timestamp(new Date(selectedDueDate.getTimeInMillis()));
        }

        Task newTask = new Task(currentUser.getUid(), title, description, dueDateTimestamp, taskType);
        newTask.setPriority(priority);
        newTask.setSpaceId(currentSpaceId);
        newTask.setOwnershipScope(ownershipScope);
        newTask.setEffort(effort); // ADDED IN PHASE 1
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