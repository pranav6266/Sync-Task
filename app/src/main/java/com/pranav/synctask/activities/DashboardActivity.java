package com.pranav.synctask.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.content.ContextCompat;
import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.pranav.synctask.R;
import com.pranav.synctask.adapters.SpaceSelectionAdapter;
import com.pranav.synctask.data.Result;
import com.pranav.synctask.data.TaskRepository;
import com.pranav.synctask.models.DialogItem;
import com.pranav.synctask.models.Space;
import com.pranav.synctask.ui.DashboardViewModel;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class DashboardActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;
    private DashboardViewModel viewModel;
    private TextView tvWelcomeMessage, tvPersonalSummary, tvSharedSummary;
    private MaterialCardView cardPersonalSpaces, cardSharedSpaces;
    private MaterialCardView btnViewProfile;
    private ExtendedFloatingActionButton fabAdd;
    private List<DialogItem> dialogItemsCache = new ArrayList<>();

    // 1. Permission Launcher
    private final ActivityResultLauncher<String> requestNotificationPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    Toast.makeText(this, "Notifications enabled!", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Notifications are required to see task updates.", Toast.LENGTH_LONG).show();
                }
            });

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();

        if (currentUser == null) {
            goToLogin();
            return;
        }

        setContentView(R.layout.activity_dashboard);
        viewModel = new ViewModelProvider(this).get(DashboardViewModel.class);

        initializeViews();
        setupClickListeners();
        observeViewModel();

        if (currentUser.getDisplayName() != null) {
            tvWelcomeMessage.setText(currentUser.getDisplayName());
        }
    }

    private void initializeViews() {
        tvWelcomeMessage = findViewById(R.id.tv_welcome_message);
        tvPersonalSummary = findViewById(R.id.tv_personal_summary);
        tvSharedSummary = findViewById(R.id.tv_shared_summary);
        cardPersonalSpaces = findViewById(R.id.card_personal_spaces);
        cardSharedSpaces = findViewById(R.id.card_shared_spaces);
        btnViewProfile = findViewById(R.id.btn_view_profile);
        fabAdd = findViewById(R.id.fab_add);
    }

    private void setupClickListeners() {
        btnViewProfile.setOnClickListener(v ->
                startActivity(new Intent(DashboardActivity.this, SettingsActivity.class)));

        cardPersonalSpaces.setOnClickListener(v ->
                startActivity(new Intent(DashboardActivity.this, PersonalLinksActivity.class)));

        cardSharedSpaces.setOnClickListener(v ->
                startActivity(new Intent(DashboardActivity.this, SpaceListActivity.class)));

        // CHANGED: ONLY set OnClickListener. The LongClick is removed to prevent conflicts.
        fabAdd.setOnClickListener(v -> showAddTaskDialog());
    }

    private void observeViewModel() {
        viewModel.getPersonalLinksLiveData().observe(this, result -> {
            if (result instanceof Result.Success) {
                List<Space> links = ((Result.Success<List<Space>>) result).data;
                String summary = String.format(Locale.getDefault(), "Linked with %d partner(s)", links.size());
                tvPersonalSummary.setText(summary);
            } else if (result instanceof Result.Error) {
                tvPersonalSummary.setText("Error loading links");
            }
        });

        viewModel.getSharedSpacesLiveData().observe(this, result -> {
            if (result instanceof Result.Success) {
                List<Space> spaces = ((Result.Success<List<Space>>) result).data;
                String summary = String.format(Locale.getDefault(), "You are in %d shared spaces", spaces.size());
                tvSharedSummary.setText(summary);
            } else if (result instanceof Result.Error) {
                tvSharedSummary.setText("Error loading spaces");
            }
        });

        viewModel.getAllDialogItems().observe(this, result -> {
            if (result instanceof Result.Success) {
                dialogItemsCache = ((Result.Success<List<DialogItem>>) result).data;
            }
        });

        viewModel.getCreateSpaceResult().observe(this, result -> {
            if (result instanceof Result.Success) {
                Toast.makeText(this, "Space created!", Toast.LENGTH_SHORT).show();
            } else if (result instanceof Result.Error) {
                Toast.makeText(this, "Error creating space.", Toast.LENGTH_SHORT).show();
            }
        });

        viewModel.getCreateLinkResult().observe(this, result -> {
            if (result instanceof Result.Success) {
                Toast.makeText(this, "Partner linked successfully!", Toast.LENGTH_SHORT).show();
            } else if (result instanceof Result.Error) {
                String error = ((Result.Error<Space>) result).exception.getMessage();
                Toast.makeText(this, "Error: " + error, Toast.LENGTH_LONG).show();
            }
        });
    }

    // CHANGED: This now handles the entire "Create" flow (Spaces, Links, and Tasks)
    private void showAddTaskDialog() {
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this);
        View sheetView = LayoutInflater.from(this).inflate(R.layout.bottom_sheet_select_space, null);
        bottomSheetDialog.setContentView(sheetView);

        // 1. Create Space Button
        sheetView.findViewById(R.id.btn_action_create_space).setOnClickListener(v -> {
            bottomSheetDialog.dismiss();
            showCreateSpaceDialog();
        });

        // 2. Join / Connect Button (Opens PairingActivity)
        sheetView.findViewById(R.id.btn_action_join_connect).setOnClickListener(v -> {
            bottomSheetDialog.dismiss();
            startActivity(new Intent(DashboardActivity.this, PairingActivity.class));
        });

        // 3. Setup RecyclerView for existing spaces
        RecyclerView rvSpaces = sheetView.findViewById(R.id.rv_space_selection);
        rvSpaces.setLayoutManager(new LinearLayoutManager(this));

        SpaceSelectionAdapter adapter = new SpaceSelectionAdapter(dialogItemsCache, selectedItem -> {
            bottomSheetDialog.dismiss();
            Intent intent = new Intent(DashboardActivity.this, CreateTaskActivity.class);
            intent.putExtra("SPACE_ID", selectedItem.getSpaceId());
            intent.putExtra("CONTEXT_TYPE", selectedItem.getSpaceType());
            startActivity(intent);
        });

        rvSpaces.setAdapter(adapter);
        bottomSheetDialog.show();
    }

    private void showCreateSpaceDialog() {
        final EditText input = new EditText(this);
        input.setHint("Space Name (e.g. Home Tasks)");
        input.setInputType(InputType.TYPE_CLASS_TEXT);

        new MaterialAlertDialogBuilder(this)
                .setTitle("Create New Space")
                .setView(input)
                .setPositiveButton("Create", (dialog, which) -> {
                    String spaceName = input.getText().toString().trim();
                    if (!spaceName.isEmpty()) {
                        viewModel.createSpace(spaceName);
                    } else {
                        Toast.makeText(this, "Space name cannot be empty.", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void askNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
                    PackageManager.PERMISSION_GRANTED) {
                requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (currentUser == null) {
            goToLogin();
            return;
        }
        askNotificationPermission();
        viewModel.attachUserListener(currentUser.getUid());
    }

    @Override
    protected void onStop() {
        super.onStop();
        TaskRepository.removeAllTasksListener();
    }

    private void goToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}