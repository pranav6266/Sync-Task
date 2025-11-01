package com.pranav.synctask.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.pranav.synctask.R;
import com.pranav.synctask.data.Result;
import com.pranav.synctask.data.TaskRepository;
import com.pranav.synctask.models.DialogItem;
import com.pranav.synctask.models.Space;
import com.pranav.synctask.ui.DashboardViewModel;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;
public class DashboardActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;
    private DashboardViewModel viewModel;
    private TextView tvWelcomeMessage, tvPersonalSummary, tvSharedSummary;
    private MaterialCardView cardPersonalSpaces, cardSharedSpaces;
    private Button btnViewProfile;
    // private ProgressBar loadingView;
    // Optional
    private LinearProgressIndicator progressPersonal, progressShared;
    private FloatingActionButton fabAdd;
    private List<DialogItem> dialogItemsCache = new ArrayList<>();
    // Cache for dialog
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();

        // This is the "AuthCheck" logic
        if (currentUser == null) {
            goToLogin();
            return;
        }

        // User is logged in, proceed to set up the hub
        setContentView(R.layout.activity_dashboard);
        viewModel = new ViewModelProvider(this).get(DashboardViewModel.class);

        initializeViews();
        setupClickListeners();
        observeViewModel();

        if (currentUser.getDisplayName() != null) {
            tvWelcomeMessage.setText(String.format(Locale.getDefault(), "Welcome, %s!", currentUser.getDisplayName()));
        }
    }

    private void initializeViews() {
        tvWelcomeMessage = findViewById(R.id.tv_welcome_message);
        tvPersonalSummary = findViewById(R.id.tv_personal_summary);
        tvSharedSummary = findViewById(R.id.tv_shared_summary);
        cardPersonalSpaces = findViewById(R.id.card_personal_spaces);
        cardSharedSpaces = findViewById(R.id.card_shared_spaces);
        btnViewProfile = findViewById(R.id.btn_view_profile);
        progressPersonal = findViewById(R.id.progress_personal);
        progressShared = findViewById(R.id.progress_shared);
        fabAdd = findViewById(R.id.fab_add);
        // loadingView = findViewById(R.id.loading_view);
        // Optional
    }

    private void setupClickListeners() {
        btnViewProfile.setOnClickListener(v ->
                startActivity(new Intent(DashboardActivity.this, SettingsActivity.class)));
        cardPersonalSpaces.setOnClickListener(v ->
                startActivity(new Intent(DashboardActivity.this, PersonalLinksActivity.class)));
        cardSharedSpaces.setOnClickListener(v ->
                startActivity(new Intent(DashboardActivity.this, SpaceListActivity.class)));
        fabAdd.setOnClickListener(v -> showAddTaskDialog());
        fabAdd.setOnLongClickListener(v -> {
            showCreateNewDialog();
            return true;
        });
    }

    private void observeViewModel() {
        // Observe personal links to update summary
        viewModel.getPersonalLinksLiveData().observe(this, result -> {
            if (result instanceof Result.Success) {
                List<Space> links = ((Result.Success<List<Space>>) result).data;
                String summary = String.format(Locale.getDefault(), "Linked with %d partner(s)", links.size());

                tvPersonalSummary.setText(summary);
            } else if (result instanceof Result.Error) {
                tvPersonalSummary.setText("Error loading links");
            }
        });
        // Observe shared spaces to update summary
        viewModel.getSharedSpacesLiveData().observe(this, result -> {
            if (result instanceof Result.Success) {
                List<Space> spaces = ((Result.Success<List<Space>>) result).data;
                String summary = String.format(Locale.getDefault(), "You are in %d shared spaces", spaces.size());
                tvSharedSummary.setText(summary);

            } else if (result instanceof Result.Error) {
                tvSharedSummary.setText("Error loading spaces");
            }
        });
        // Observe combined dialog items and cache them
        viewModel.getAllDialogItems().observe(this, result -> {
            if (result instanceof Result.Success) {
                dialogItemsCache = ((Result.Success<List<DialogItem>>) result).data;
            } else if (result instanceof Result.Error) {
                Toast.makeText(this, "Error loading spaces for dialog", Toast.LENGTH_SHORT).show();

            }
        });
        // Observe create space result (for the dialog)
        viewModel.getCreateSpaceResult().observe(this, result -> {
            if (result instanceof Result.Success) {
                Toast.makeText(this, "Space created!", Toast.LENGTH_SHORT).show();
            } else if (result instanceof Result.Error) {
                Toast.makeText(this, "Error creating space.", Toast.LENGTH_SHORT).show();

            }
        });
        // --- ADDED IN PHASE 3E ---
        viewModel.getCreateLinkResult().observe(this, result -> {
            if (result instanceof Result.Success) {
                Toast.makeText(this, "Partner linked successfully!", Toast.LENGTH_SHORT).show();
            } else if (result instanceof Result.Error) {
                String error = ((Result.Error<Space>) result).exception.getMessage();

                Toast.makeText(this, "Error: " + error, Toast.LENGTH_LONG).show();
            }
        });
        // --- END ADDED ---

        // Observe progress
        viewModel.getPersonalProgress().observe(this, progress -> {
            progressPersonal.setProgress(progress, true);
        });
        viewModel.getSharedProgress().observe(this, progress -> {
            progressShared.setProgress(progress, true);
        });
    }

    private void showAddTaskDialog() {
        if (dialogItemsCache.isEmpty()) {
            Toast.makeText(this, "No spaces or links found.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Create a simple array of display names
        CharSequence[] items = dialogItemsCache.stream()
                .map(DialogItem::getDisplayName).toArray(CharSequence[]::new);
        new MaterialAlertDialogBuilder(this)
                .setTitle("Add Task To...")
                .setItems(items, (dialog, which) -> {
                    // Get the selected item's spaceId
                    DialogItem selected = dialogItemsCache.get(which);

                    // --- MODIFIED ---
                    Intent intent = new Intent(DashboardActivity.this, CreateTaskActivity.class);
                    intent.putExtra("SPACE_ID", selected.getSpaceId());
                    intent.putExtra("CONTEXT_TYPE", selected.getSpaceType()); // Pass the context type
                    startActivity(intent);
                    // --- END MODIFIED ---
                })
                .show();
    }

    // --- MODIFIED IN PHASE 3E ---
    private void showCreateNewDialog() {
        final CharSequence[] options = {
                getString(R.string.link_new_partner),
                getString(R.string.create_shared_space),
                getString(R.string.join_shared_space)
        };
        new MaterialAlertDialogBuilder(this)
                .setTitle("Create New")
                .setItems(options, (dialog, which) -> {
                    String selected = options[which].toString();
                    if (selected.equals(getString(R.string.link_new_partner))) {

                        showLinkPartnerDialog();
                    } else if (selected.equals(getString(R.string.create_shared_space))) {
                        showCreateSpaceDialog();
                    } else if (selected.equals(getString(R.string.join_shared_space))) {

                        startActivity(new Intent(DashboardActivity.this, PairingActivity.class));
                    }
                })
                .show();
    }

    // --- ADDED IN PHASE 3E ---
    private void showLinkPartnerDialog() {
        // Inflate the custom layout
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_link_partner, null);
        TextView tvYourUid = dialogView.findViewById(R.id.tv_your_uid);
        TextInputEditText etPartnerUid = dialogView.findViewById(R.id.et_partner_uid);

        // Set your UID
        tvYourUid.setText(currentUser.getUid());
        // Create and show the dialog
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.link_new_partner)
                .setView(dialogView)
                .setPositiveButton(R.string.link_button, (dialog, which) -> {
                    String partnerUid = Objects.requireNonNull(etPartnerUid.getText()).toString().trim();

                    if (TextUtils.isEmpty(partnerUid)) {
                        Toast.makeText(this, "Partner UID cannot be empty.", Toast.LENGTH_SHORT).show();
                    } else {
                        viewModel.createPersonalLink(partnerUid);

                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
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
                        viewModel.createSpace(spaceName); // ViewModel handles this now
                    } else {
                        Toast.makeText(this, "Space name cannot be empty.", Toast.LENGTH_SHORT).show();

                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }


    @Override
    protected void onStart() {
        super.onStart();
        if (currentUser == null) {
            goToLogin();
            return;
        }
        // Attach listener to fetch user's spaceIds, which triggers space loading
        viewModel.attachUserListener(currentUser.getUid());
    }

    @Override
    protected void onStop() {
        super.onStop();
        // Detach the all-tasks listener when the hub is not visible
        TaskRepository.removeAllTasksListener();
    }

    private void goToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}