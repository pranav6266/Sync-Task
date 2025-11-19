package com.pranav.synctask.activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.messaging.FirebaseMessaging; // ADDED
import com.pranav.synctask.R;
import com.pranav.synctask.adapters.SpaceSelectionAdapter;
import com.pranav.synctask.data.Result;
import com.pranav.synctask.data.TaskRepository;
import com.pranav.synctask.data.UserRepository; // ADDED
import com.pranav.synctask.models.DialogItem;
import com.pranav.synctask.models.Space;
import com.pranav.synctask.models.User;
import com.pranav.synctask.ui.DashboardViewModel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class DashboardActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;
    private DashboardViewModel viewModel;

    // Views
    private TextView tvWelcomeMessage, tvPersonalSummary, tvSharedSummary;
    private MaterialCardView cardPersonalSpaces, cardSharedSpaces;
    private MaterialCardView btnViewProfile;
    private ExtendedFloatingActionButton fabAdd;
    private SwipeRefreshLayout swipeRefreshLayout;

    // Data Cache
    private List<DialogItem> dialogItemsCache = new ArrayList<>();
    private Map<String, User> memberMapCache = new HashMap<>();
    private int previousLinkCount = -1;

    private final ActivityResultLauncher<String> requestNotificationPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) Toast.makeText(this, "Notifications enabled!", Toast.LENGTH_SHORT).show();
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

        // --- ADDED: Essential for Notifications ---
        updateFcmToken();
        // ------------------------------------------

        if (currentUser.getDisplayName() != null) {
            tvWelcomeMessage.setText(currentUser.getDisplayName());
        }
    }

    // --- ADDED METHOD ---
    private void updateFcmToken() {
        FirebaseMessaging.getInstance().getToken().addOnCompleteListener(task -> {
            if (!task.isSuccessful()) {
                Log.w("DashboardActivity", "Fetching FCM registration token failed", task.getException());
                return;
            }
            String token = task.getResult();
            UserRepository.getInstance().updateFcmToken(currentUser.getUid(), token);
        });
    }
    // --------------------

    private void initializeViews() {
        tvWelcomeMessage = findViewById(R.id.tv_welcome_message);
        tvPersonalSummary = findViewById(R.id.tv_personal_summary);
        tvSharedSummary = findViewById(R.id.tv_shared_summary);
        cardPersonalSpaces = findViewById(R.id.card_personal_spaces);
        cardSharedSpaces = findViewById(R.id.card_shared_spaces);
        btnViewProfile = findViewById(R.id.btn_view_profile);
        fabAdd = findViewById(R.id.fab_add);
        swipeRefreshLayout = findViewById(R.id.swipe_refresh_dashboard);

        swipeRefreshLayout.setColorSchemeResources(R.color.md_theme_light_primary);
        swipeRefreshLayout.setOnRefreshListener(() -> {
            viewModel.refreshSpaces();
            new android.os.Handler().postDelayed(() -> swipeRefreshLayout.setRefreshing(false), 1000);
        });
    }

    private void setupClickListeners() {
        btnViewProfile.setOnClickListener(v -> startActivity(new Intent(DashboardActivity.this, SettingsActivity.class)));
        cardPersonalSpaces.setOnClickListener(v -> startActivity(new Intent(DashboardActivity.this, PersonalLinksActivity.class)));
        cardSharedSpaces.setOnClickListener(v -> startActivity(new Intent(DashboardActivity.this, SpaceListActivity.class)));
        fabAdd.setOnClickListener(v -> showAddTaskDialog());
    }

    private void observeViewModel() {
        viewModel.getMembersMap().observe(this, map -> memberMapCache = map);

        viewModel.getPersonalLinksLiveData().observe(this, result -> {
            if (result instanceof Result.Success) {
                List<Space> links = ((Result.Success<List<Space>>) result).data;
                String summary = String.format(Locale.getDefault(), "Linked with %d partner(s)", links.size());
                tvPersonalSummary.setText(summary);
                if (previousLinkCount != -1 && links.size() > previousLinkCount) {
                    Snackbar.make(findViewById(android.R.id.content), "New partner connected! 🎉", Snackbar.LENGTH_LONG).show();
                }
                previousLinkCount = links.size();
            } else if (result instanceof Result.Error) {
                tvPersonalSummary.setText("Error loading links");
            }
        });

        viewModel.getSharedSpacesLiveData().observe(this, result -> {
            if (result instanceof Result.Success) {
                List<Space> spaces = ((Result.Success<List<Space>>) result).data;
                tvSharedSummary.setText(String.format(Locale.getDefault(), "You are in %d shared spaces", spaces.size()));
            } else if (result instanceof Result.Error) {
                tvSharedSummary.setText("Error loading spaces");
            }
        });

        viewModel.getAllDialogItems().observe(this, result -> {
            if (result instanceof Result.Success) dialogItemsCache = ((Result.Success<List<DialogItem>>) result).data;
        });

        viewModel.getCreateSpaceResult().observe(this, result -> {
            if (result instanceof Result.Success) Toast.makeText(this, "Space created!", Toast.LENGTH_SHORT).show();
            else if (result instanceof Result.Error) Toast.makeText(this, "Error creating space.", Toast.LENGTH_SHORT).show();
        });

        viewModel.getCreateLinkResult().observe(this, result -> {
            if (result instanceof Result.Success) Toast.makeText(this, "Partner linked successfully!", Toast.LENGTH_SHORT).show();
            else if (result instanceof Result.Error) {
                String error = ((Result.Error<Space>) result).exception.getMessage();
                Toast.makeText(this, "Error: " + error, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void showAddTaskDialog() {
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this);
        View sheetView = LayoutInflater.from(this).inflate(R.layout.bottom_sheet_select_space, null);
        bottomSheetDialog.setContentView(sheetView);

        sheetView.findViewById(R.id.btn_action_create_space).setOnClickListener(v -> {
            bottomSheetDialog.dismiss();
            showCreateSpaceDialog();
        });
        sheetView.findViewById(R.id.btn_action_join_connect).setOnClickListener(v -> {
            bottomSheetDialog.dismiss();
            startActivity(new Intent(DashboardActivity.this, PairingActivity.class));
        });

        RecyclerView rvSpaces = sheetView.findViewById(R.id.rv_space_selection);
        rvSpaces.setLayoutManager(new LinearLayoutManager(this));

        if (dialogItemsCache.isEmpty()) {
            Toast.makeText(this, "No spaces found.", Toast.LENGTH_SHORT).show();
        }

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
                    if (!spaceName.isEmpty()) viewModel.createSpace(spaceName);
                    else Toast.makeText(this, "Space name cannot be empty.", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null).show();
    }

    private void askNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
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