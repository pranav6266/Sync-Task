package com.pranav.synctask.activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
// MODIFIED: Changed import
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.airbnb.lottie.LottieAnimationView; // ADDED
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.transition.platform.MaterialFadeThrough;
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
import java.util.Locale;

// MODIFIED IN PHASE 3A: Renamed from DashboardActivity
public class SpaceListActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private DashboardViewModel viewModel;
    private FirebaseUser currentUser;
    private SpacesAdapter spacesAdapter;
    private RecyclerView spacesRecyclerView;
    private FloatingActionButton fabAddSpace;
    private LottieAnimationView emptyView; // ADDED

    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    Toast.makeText(this, "Notifications enabled!", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this,
                            "Notifications are disabled.", Toast.LENGTH_SHORT).show();
                }
            });
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // ADDED: Material Motion Transition
        getWindow().requestFeature(Window.FEATURE_ACTIVITY_TRANSITIONS);
        getWindow().setEnterTransition(new MaterialFadeThrough());
        getWindow().setExitTransition(new MaterialFadeThrough());
        // END ADDED

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_space_list);
        // MODIFIED IN PHASE 3A

        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();
        viewModel = new ViewModelProvider(this).get(DashboardViewModel.class);

        spacesRecyclerView = findViewById(R.id.spaces_recycler_view);
        fabAddSpace = findViewById(R.id.fab_add_space);
        emptyView = findViewById(R.id.empty_view); // ADDED
        Button btnViewProfile = findViewById(R.id.btn_view_profile);
        TextView tvWelcomeMessage = findViewById(R.id.tv_welcome_message);
        if (currentUser != null && currentUser.getDisplayName() != null) {
            tvWelcomeMessage.setText(String.format(Locale.getDefault(), "Welcome, %s!", currentUser.getDisplayName()));
        }

        setupRecyclerView();

        fabAddSpace.setOnClickListener(v -> showAddSpaceDialog());
        btnViewProfile.setOnClickListener(v -> startActivity(new Intent(SpaceListActivity.this, SettingsActivity.class))); // CHANGED

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
        // MODIFIED: Use MaterialAlertDialogBuilder
        new MaterialAlertDialogBuilder(this)
                .setTitle("Add Space")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        showCreateSpaceDialog();

                    } else {
                        startActivity(new Intent(SpaceListActivity.this, PairingActivity.class));
                    }
                })
                .show();
    }

    private void showCreateSpaceDialog() {
        final EditText input = new EditText(this);
        input.setHint("Space Name (e.g. Home Tasks)");
        input.setInputType(InputType.TYPE_CLASS_TEXT);

        // MODIFIED: Use MaterialAlertDialogBuilder
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
        // MODIFIED IN PHASE 3A: This now observes the filtered list
        viewModel.getSharedSpacesLiveData().observe(this, result -> {
            if (result instanceof Result.Success) {
                List<Space> spaces = ((Result.Success<List<Space>>) result).data; // ADDED
                spacesAdapter.updateSpaces(spaces); // MODIFIED
                updateEmptyView(spaces.isEmpty()); // ADDED

            } else if (result instanceof Result.Error) {
                Toast.makeText(this, "Error loading spaces.", Toast.LENGTH_SHORT).show();
                updateEmptyView(true); // ADDED
            } else if (result instanceof Result.Loading) {
                // TODO: Show loading
            }

        });

        viewModel.getCreateSpaceResult().observe(this, result -> {
            if (result instanceof Result.Success) {
                // Get the newly created space from the result
                Space newSpace = ((Result.Success<Space>) result).data;
                if (newSpace != null && newSpace.getInviteCode() != null) {

                    // Show a dialog with the invite code
                    showInviteCodeDialog(newSpace.getSpaceName(), newSpace.getInviteCode());
                } else {
                    Toast.makeText(this, "Space created!", Toast.LENGTH_SHORT).show();
                }

                // The user listener will automatically refresh the spaces list
            } else if (result instanceof Result.Error) {
                Toast.makeText(this, "Error creating space.", Toast.LENGTH_SHORT).show();
            }
        });
        // --- NEW ---
        viewModel.getLeaveSpaceResult().observe(this, result -> {
            if (result instanceof Result.Success) {
                Toast.makeText(this, "Successfully left space.", Toast.LENGTH_SHORT).show();
                // List will refresh automatically
            } else if (result instanceof Result.Error) {

                Toast.makeText(this, "Error leaving space.", Toast.LENGTH_SHORT).show();
            }
        });
        viewModel.getDeleteSpaceResult().observe(this, result -> {
            if (result instanceof Result.Success) {
                Toast.makeText(this, "Space deleted.", Toast.LENGTH_SHORT).show();
                // List will refresh automatically
            } else if (result instanceof Result.Error) {
                String message = "Error deleting space.";

                Exception e = ((Result.Error<Void>) result).exception;
                if (e != null && e.getMessage() != null) {
                    message = e.getMessage();
                }
                Toast.makeText(this, message, Toast.LENGTH_LONG).show();

            }
        });
    }

    // --- ADDED ---
    private void updateEmptyView(boolean isEmpty) {
        if (isEmpty) {
            emptyView.setVisibility(View.VISIBLE);
            emptyView.playAnimation();
        } else {
            emptyView.setVisibility(View.GONE);
            emptyView.cancelAnimation();
        }
        spacesRecyclerView.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
    }
    // --- END ADDED ---

    private void showInviteCodeDialog(String spaceName, String inviteCode) {
        String title = "Space '" + spaceName + "' Created!";
        // Create a TextView for the code to make it selectable
        final TextView codeView = new TextView(this);
        codeView.setText(inviteCode);
        codeView.setTextSize(24);
        codeView.setTextIsSelectable(true);
        codeView.setGravity(Gravity.CENTER);
        codeView.setPadding(40, 40, 40, 40);

        // MODIFIED: Use MaterialAlertDialogBuilder
        new MaterialAlertDialogBuilder(this)
                .setTitle(title)
                .setMessage(getString(R.string.share_code_instruction))
                .setView(codeView) // Add the selectable code here
                .setPositiveButton("OK", null)

                .show();
    }
}