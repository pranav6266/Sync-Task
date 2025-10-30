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

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import com.bumptech.glide.Glide;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.pranav.synctask.R;
import com.pranav.synctask.data.Result;
import com.pranav.synctask.models.User;
import com.pranav.synctask.ui.viewmodels.ProfileViewModel;

import de.hdodenhof.circleimageview.CircleImageView;
public class ProfileActivity extends AppCompatActivity {

    private CircleImageView ivProfileImage;
    private TextView tvDisplayName, tvEmail;
    private EditText etDisplayName;
    private ImageView ivEditName;
    private MaterialCardView ivEditPhoto;
    private Button btnLogout, btnSaveChanges;
    private ProgressBar progressBar;
    private FirebaseAuth mAuth;
    private GoogleSignInClient mGoogleSignInClient;
    private FirebaseUser currentUser;
    private ProfileViewModel viewModel;
    private ActivityResultLauncher<String> mGetContent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);
        viewModel = new ViewModelProvider(this).get(ProfileViewModel.class);
        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        ivProfileImage = findViewById(R.id.iv_profile_image);
        tvDisplayName = findViewById(R.id.tv_display_name);
        etDisplayName = findViewById(R.id.et_display_name);
        ivEditName = findViewById(R.id.iv_edit_name);
        ivEditPhoto = findViewById(R.id.iv_edit_photo);
        tvEmail = findViewById(R.id.tv_email);
        btnLogout = findViewById(R.id.btn_logout);
        btnSaveChanges = findViewById(R.id.btn_save_changes);
        progressBar = findViewById(R.id.profile_progress_bar);
        if (currentUser != null) {
            loadUserProfile();
        }

        mGetContent = registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
            if (uri != null) {
                viewModel.uploadProfilePicture(currentUser, uri);
            }
        });
        ivEditName.setOnClickListener(v -> toggleEditMode(true));
        ivEditPhoto.setOnClickListener(v -> mGetContent.launch("image/*"));
        btnSaveChanges.setOnClickListener(v -> saveProfileChanges());
        btnLogout.setOnClickListener(v -> logoutUser());

        observeViewModel();
    }

    private void observeViewModel() {
        viewModel.getUserLiveData().observe(this, result -> {
            if (isFinishing()) return;
            if (result instanceof Result.Success) {
                // User user = ((Result.Success<User>) result).data; // No longer needed
            } else if (result instanceof Result.Error) {
                Log.e("ProfileActivity", "Error listening to user pairing status", ((Result.Error<User>) result).exception);
            }
        });
        viewModel.getPhotoUpdateResult().observe(this, result -> {
            showLoading(result instanceof Result.Loading);
            if (result instanceof Result.Success) {
                String newUrl = ((Result.Success<String>) result).data;
                Glide.with(this).load(newUrl).placeholder(R.drawable.ic_profile).into(ivProfileImage);
                Toast.makeText(this, "Profile picture updated!", Toast.LENGTH_SHORT).show();
            } else if (result instanceof Result.Error) {
                Toast.makeText(this, "Failed to update photo.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (currentUser != null) {
            viewModel.attachUserListener(currentUser.getUid());
            updateUiBasedOnNetworkStatus();
        } else {
            goToLogin();
        }
    }

    private void updateUiBasedOnNetworkStatus() {
        // No longer needed
    }

    private void loadUserProfile() {
        tvDisplayName.setText(currentUser.getDisplayName());
        tvEmail.setText(currentUser.getEmail());
        if (currentUser.getPhotoUrl() != null) {
            Glide.with(this)
                    .load(currentUser.getPhotoUrl())
                    .placeholder(R.drawable.ic_profile)
                    .into(ivProfileImage);
        }
    }

    private void toggleEditMode(boolean enable) {
        tvDisplayName.setVisibility(enable ? View.GONE : View.VISIBLE);
        ivEditName.setVisibility(enable ? View.GONE : View.VISIBLE);
        etDisplayName.setVisibility(enable ? View.VISIBLE : View.GONE);
        btnSaveChanges.setVisibility(enable ? View.VISIBLE : View.GONE);
        if (enable) {
            etDisplayName.setText(tvDisplayName.getText());
            etDisplayName.requestFocus();
        }
    }

    private void saveProfileChanges() {
        String newName = etDisplayName.getText().toString().trim();
        if (TextUtils.isEmpty(newName)) {
            etDisplayName.setError("Name cannot be empty");
            return;
        }

        showLoading(true);

        UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                .setDisplayName(newName)
                .build();
        currentUser.updateProfile(profileUpdates)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        viewModel.saveProfileChanges(currentUser.getUid(), newName).observe(this, result -> {
                            showLoading(false);
                            if (result instanceof Result.Success) {
                                Toast.makeText(ProfileActivity.this, "Profile updated successfully!", Toast.LENGTH_SHORT).show();
                                tvDisplayName.setText(newName);
                                toggleEditMode(false);
                            } else if (result instanceof Result.Error) {
                                Toast.makeText(ProfileActivity.this, "Error updating profile in database.",
                                        Toast.LENGTH_SHORT).show();
                            }
                        });
                    } else {
                        showLoading(false);
                        Toast.makeText(ProfileActivity.this, "Failed to update profile.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void logoutUser() {
        mAuth.signOut();
        mGoogleSignInClient.signOut().addOnCompleteListener(this, task -> goToLogin());
    }

    private void goToLogin() {
        Intent intent = new Intent(ProfileActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void goToDashboard() {
        // MODIFIED IN PHASE 3B
        Intent intent = new Intent(ProfileActivity.this, DashboardActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        finish();
    }

    private void showLoading(boolean isLoading) {
        progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        btnSaveChanges.setEnabled(!isLoading);
        btnLogout.setEnabled(!isLoading);
        ivEditPhoto.setEnabled(!isLoading);
    }
}