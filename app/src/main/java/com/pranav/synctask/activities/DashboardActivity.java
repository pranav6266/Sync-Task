package com.pranav.synctask.activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.messaging.FirebaseMessaging;
import com.pranav.synctask.R;
import com.pranav.synctask.data.Result;
import com.pranav.synctask.data.UserRepository;
import com.pranav.synctask.models.User;
import com.pranav.synctask.ui.DashboardViewModel;

public class DashboardActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private DashboardViewModel viewModel;
    private Button btnGoToTasks;
    private Button btnPairWithPartner;
    private LinearLayout layoutPairedStatus;

    // PHASE 3: Launcher for notification permission request
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
        viewModel = new ViewModelProvider(this).get(DashboardViewModel.class);

        btnGoToTasks = findViewById(R.id.btn_go_to_tasks);
        btnPairWithPartner = findViewById(R.id.btn_pair_with_partner);
        Button btnViewProfile = findViewById(R.id.btn_view_profile);
        TextView tvWelcomeMessage = findViewById(R.id.tv_welcome_message);
        layoutPairedStatus = findViewById(R.id.layout_paired_status);

        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null && currentUser.getDisplayName() != null) {
            tvWelcomeMessage.setText("Welcome, " + currentUser.getDisplayName() + "!");
        }

        btnGoToTasks.setOnClickListener(v -> startActivity(new Intent(DashboardActivity.this, MainActivity.class)));
        btnPairWithPartner.setOnClickListener(v -> startActivity(new Intent(DashboardActivity.this, PairingActivity.class)));
        btnViewProfile.setOnClickListener(v -> startActivity(new Intent(DashboardActivity.this, ProfileActivity.class)));

        observeViewModel();
        askNotificationPermission(); // PHASE 3
        updateFcmToken(); // PHASE 3
    }

    // PHASE 3: Get and update the FCM token
    private void updateFcmToken() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
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


    // PHASE 3: Request permission for notifications on Android 13+
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
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }
        viewModel.attachUserListener(currentUser.getUid());
    }

    private void observeViewModel() {
        viewModel.getUserPairingStatus().observe(this, result -> {
            if (result instanceof Result.Success) {
                updateUI(((Result.Success<User>) result).data);
            } else if (result instanceof Result.Error) {
                Exception e = ((Result.Error<User>) result).exception;
                Log.e("DashboardActivity", "Error listening to user pairing status", e);
                Toast.makeText(DashboardActivity.this, "Could not check pairing status.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateUI(User user) {
        if (user.getPairedWithUID() != null && !user.getPairedWithUID().isEmpty()) {
            btnGoToTasks.setVisibility(View.VISIBLE);
            layoutPairedStatus.setVisibility(View.VISIBLE);
            btnPairWithPartner.setVisibility(View.GONE);
        } else {
            btnGoToTasks.setVisibility(View.GONE);
            layoutPairedStatus.setVisibility(View.GONE);
            btnPairWithPartner.setVisibility(View.VISIBLE);
        }
    }
}