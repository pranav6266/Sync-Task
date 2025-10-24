package com.pranav.synctask.activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.messaging.FirebaseMessaging;
import com.pranav.synctask.R;
import com.pranav.synctask.adapters.SpacesAdapter;
import com.pranav.synctask.data.Result;
import com.pranav.synctask.data.UserRepository;
import com.pranav.synctask.models.Space;
import com.pranav.synctask.models.User;
import com.pranav.synctask.ui.DashboardViewModel;

import java.util.ArrayList;
import java.util.List;

public class DashboardActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private DashboardViewModel viewModel;
    private FirebaseUser currentUser;
    private SpacesAdapter spacesAdapter;
    private RecyclerView spacesRecyclerView;
    private FloatingActionButton fabAddSpace;

    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    Toast.makeText(this, "Notifications enabled!", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Notifications are disabled.", Toast.LENGTH_SHORT).show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);
        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();
        viewModel = new ViewModelProvider(this).get(DashboardViewModel.class);

        spacesRecyclerView = findViewById(R.id.spaces_recycler_view);
        fabAddSpace = findViewById(R.id.fab_add_space);
        Button btnViewProfile = findViewById(R.id.btn_view_profile);
        TextView tvWelcomeMessage = findViewById(R.id.tv_welcome_message);

        if (currentUser != null && currentUser.getDisplayName() != null) {
            tvWelcomeMessage.setText("Welcome, " + currentUser.getDisplayName() + "!");
        }

        setupRecyclerView();

        fabAddSpace.setOnClickListener(v -> showAddSpaceDialog());
        btnViewProfile.setOnClickListener(v -> startActivity(new Intent(DashboardActivity.this, ProfileActivity.class)));

        observeViewModel();
        askNotificationPermission();
        updateFcmToken();
    }

    private void setupRecyclerView() {
        spacesRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        spacesAdapter = new SpacesAdapter(this, new ArrayList<>());
        spacesRecyclerView.setAdapter(spacesAdapter);
    }

    private void showAddSpaceDialog() {
        final String[] options = {"Create a new Space", "Join a Space"};
        new AlertDialog.Builder(this)
                .setTitle("Add Space")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        showCreateSpaceDialog();
                    } else {
                        startActivity(new Intent(DashboardActivity.this, PairingActivity.class));
                    }
                })
                .show();
    }

    private void showCreateSpaceDialog() {
        final EditText input = new EditText(this);
        input.setHint("Space Name (e.g. Home Tasks)");
        input.setInputType(InputType.TYPE_CLASS_TEXT);

        new AlertDialog.Builder(this)
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

    private void updateFcmToken() {
        if (currentUser == null) return;
        FirebaseMessaging.getInstance().getToken().addOnCompleteListener(task -> {
            if (!task.isSuccessful()) {
                Log.w("DashboardActivity", "Fetching FCM registration token failed", task.getException());
                return;
            }
            String token = task.getResult();
            Log.d("DashboardActivity", "FCM Token: " + token);
            UserRepository.getInstance().updateFcmToken(currentUser.getUid(), token);
        });
    }

    private void askNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
                    PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (currentUser == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }
        viewModel.attachUserListener(currentUser.getUid());
    }

    private void observeViewModel() {
        viewModel.getUserLiveData().observe(this, result -> {
            if (result instanceof Result.Success) {
                User user = ((Result.Success<User>) result).data;
                if (user.getSpaceIds() != null && !user.getSpaceIds().isEmpty()) {
                    viewModel.loadSpaces(user.getSpaceIds());
                } else {
                    // User has no spaces, clear the list
                    spacesAdapter.updateSpaces(new ArrayList<>());
                }
            } else if (result instanceof Result.Error) {
                Log.e("DashboardActivity", "Error listening to user", ((Result.Error<User>) result).exception);
            }
        });

        viewModel.getSpacesLiveData().observe(this, result -> {
            if (result instanceof Result.Success) {
                spacesAdapter.updateSpaces(((Result.Success<List<Space>>) result).data);
            } else if (result instanceof Result.Error) {
                Toast.makeText(this, "Error loading spaces.", Toast.LENGTH_SHORT).show();
            }
        });

        viewModel.getCreateSpaceResult().observe(this, result -> {
            if (result instanceof Result.Success) {
                Toast.makeText(this, "Space created!", Toast.LENGTH_SHORT).show();
                // The user listener will automatically refresh the spaces list
            } else if (result instanceof Result.Error) {
                Toast.makeText(this, "Error creating space.", Toast.LENGTH_SHORT).show();
            }
        });
    }
}