package com.pranav.synctask.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import com.bumptech.glide.Glide;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.pranav.synctask.R;
import com.pranav.synctask.data.Result;
import com.pranav.synctask.models.User;
import com.pranav.synctask.ui.viewmodels.ProfileViewModel;
import com.pranav.synctask.utils.NetworkUtils;
import de.hdodenhof.circleimageview.CircleImageView;

public class ProfileActivity extends AppCompatActivity {

    private CircleImageView ivProfileImage;
    private TextView tvDisplayName, tvEmail;
    private EditText etDisplayName;
    private ImageView ivEditName;
    private Button btnUnpair, btnLogout, btnSaveChanges;
    private ProgressBar progressBar;
    private FirebaseAuth mAuth;
    private GoogleSignInClient mGoogleSignInClient;
    private FirebaseUser currentUser;
    private ProfileViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        [cite_start]setContentView(R.layout.activity_profile); 

        viewModel = new ViewModelProvider(this).get(ProfileViewModel.class);
        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                [cite_start].requestIdToken(getString(R.string.default_web_client_id)) 
                [cite_start].requestEmail() 
                .build();
        [cite_start]mGoogleSignInClient = GoogleSignIn.getClient(this, gso); 

        ivProfileImage = findViewById(R.id.iv_profile_image);
        tvDisplayName = findViewById(R.id.tv_display_name);
        etDisplayName = findViewById(R.id.et_display_name);
        ivEditName = findViewById(R.id.iv_edit_name);
        tvEmail = findViewById(R.id.tv_email);
        btnUnpair = findViewById(R.id.btn_unpair);
        btnLogout = findViewById(R.id.btn_logout);
        btnSaveChanges = findViewById(R.id.btn_save_changes);
        progressBar = findViewById(R.id.profile_progress_bar);

        [cite_start]if (currentUser != null) { 
            loadUserProfile();
        }

        ivEditName.setOnClickListener(v -> toggleEditMode(true));
        btnSaveChanges.setOnClickListener(v -> saveProfileChanges());
        btnLogout.setOnClickListener(v -> logoutUser());
        btnUnpair.setOnClickListener(v -> showUnpairConfirmationDialog());

        observeViewModel();
    }

    private void observeViewModel() {
        viewModel.getUserLiveData().observe(this, result -> {
            if (isFinishing()) return;
            if (result instanceof Result.Success) {
                User user = ((Result.Success<User>) result).data;
                if (user.getPairedWithUID() != null && !user.getPairedWithUID().isEmpty()) {
                    [cite_start]btnUnpair.setVisibility(View.VISIBLE); 
                } else {
                    [cite_start]btnUnpair.setVisibility(View.GONE); 
                }
            } else if (result instanceof Result.Error) {
                [cite_start]Log.e("ProfileActivity", "Error listening to user pairing status", ((Result.Error<User>) result).exception); 
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        [cite_start]if (currentUser != null) { 
            viewModel.attachUserListener(currentUser.getUid());
            [cite_start]updateUiBasedOnNetworkStatus(); 
        } else {
            [cite_start]goToLogin(); 
        }
    }

    private void updateUiBasedOnNetworkStatus() {
        [cite_start]boolean isOnline = NetworkUtils.isNetworkAvailable(this); 
        [cite_start]btnUnpair.setEnabled(isOnline); 
        if (!isOnline) {
            [cite_start]btnUnpair.setAlpha(0.5f); 
            [cite_start]Toast.makeText(this, "Unpairing requires an internet connection.", Toast.LENGTH_SHORT).show(); 
        } else {
            [cite_start]btnUnpair.setAlpha(1.0f); 
        }
    }

    private void loadUserProfile() {
        tvDisplayName.setText(currentUser.getDisplayName());
        tvEmail.setText(currentUser.getEmail());
        [cite_start]if (currentUser.getPhotoUrl() != null) { 
            Glide.with(this)
                    .load(currentUser.getPhotoUrl())
                    .placeholder(R.drawable.ic_profile)
                    .into(ivProfileImage);
        }
    }

    private void toggleEditMode(boolean enable) {
        if (enable) {
            [cite_start]tvDisplayName.setVisibility(View.GONE); 
            [cite_start]ivEditName.setVisibility(View.GONE); 
            etDisplayName.setVisibility(View.VISIBLE);
            btnSaveChanges.setVisibility(View.VISIBLE);
            etDisplayName.setText(tvDisplayName.getText());
            etDisplayName.requestFocus();
        } else {
            tvDisplayName.setVisibility(View.VISIBLE);
            [cite_start]ivEditName.setVisibility(View.VISIBLE); 
            etDisplayName.setVisibility(View.GONE);
            btnSaveChanges.setVisibility(View.GONE);
        }
    }

    private void saveProfileChanges() {
        [cite_start]String newName = etDisplayName.getText().toString().trim(); 
        [cite_start]if (TextUtils.isEmpty(newName)) { 
            etDisplayName.setError("Name cannot be empty");
            return;
        }

        showLoading(true);

        UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                .setDisplayName(newName)
                .build();
        [cite_start]currentUser.updateProfile(profileUpdates) 
            .addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                viewModel.saveProfileChanges(currentUser.getUid(), newName).observe(this, result -> {
                    if (result instanceof Result.Success) {
                        showLoading(false);
                            [cite_start]Toast.makeText(ProfileActivity.this, "Profile updated successfully!", Toast.LENGTH_SHORT).show(); 
                        tvDisplayName.setText(newName);
                        toggleEditMode(false);
                    } else if (result instanceof Result.Error) {
                        showLoading(false);
                            [cite_start]Toast.makeText(ProfileActivity.this, "Error updating profile in database.", Toast.LENGTH_SHORT).show(); 
                    }
                });
            } else {
                showLoading(false);
                    [cite_start]Toast.makeText(ProfileActivity.this, "Failed to update profile.", Toast.LENGTH_SHORT).show(); 
            }
        });
    }

    private void showUnpairConfirmationDialog() {
        new AlertDialog.Builder(this)
                [cite_start].setTitle("Unpair Partner") 
                [cite_start].setMessage("Are you sure you want to unpair? This will delete all shared tasks.") 
                [cite_start].setPositiveButton("Unpair", (dialog, which) -> { 
            unpairPartner();
        })
                [cite_start].setNegativeButton("Cancel", null) 
                .show();
    }

    private void unpairPartner() {
        showLoading(true);
        viewModel.unpair(currentUser.getUid()).observe(this, result -> {
            showLoading(false);
            if (result instanceof Result.Success) {
                [cite_start]Toast.makeText(ProfileActivity.this, "Unpaired successfully.", Toast.LENGTH_SHORT).show(); 
                goToDashboard();
            } else if (result instanceof Result.Error) {
                Exception e = ((Result.Error<Void>) result).exception;
                [cite_start]Toast.makeText(ProfileActivity.this, "Failed to unpair: " + e.getMessage(), Toast.LENGTH_LONG).show(); 
            }
        });
    }

    private void logoutUser() {
        [cite_start]mAuth.signOut(); 
        mGoogleSignInClient.signOut().addOnCompleteListener(this,
                task -> {
                    [cite_start]goToLogin(); 
                });
    }

    private void goToLogin() {
        Intent intent = new Intent(ProfileActivity.this, LoginActivity.class);
        [cite_start]intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK); 
        startActivity(intent);
        finish();
    }

    private void goToDashboard() {
        Intent intent = new Intent(ProfileActivity.this, DashboardActivity.class);
        [cite_start]intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP); 
        startActivity(intent);
        finish();
    }

    private void showLoading(boolean isLoading) {
        progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        [cite_start]btnSaveChanges.setEnabled(!isLoading); 
        [cite_start]btnLogout.setEnabled(!isLoading); 
        if (isLoading) {
            if (btnUnpair.isEnabled()) {
                [cite_start]btnUnpair.setEnabled(false); 
            }
        } else {
            [cite_start]updateUiBasedOnNetworkStatus(); 
        }
    }
}