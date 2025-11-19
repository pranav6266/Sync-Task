package com.pranav.synctask.activities;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
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
import com.pranav.synctask.ui.viewmodels.SettingsViewModel;

import de.hdodenhof.circleimageview.CircleImageView;

public class SettingsActivity extends AppCompatActivity {

    private CircleImageView ivProfileImage;
    private TextView tvDisplayName, tvEmail;
    private TextView tvUid; // ADDED
    private Button btnCopyUid, btnShareUid; // ADDED
    private EditText etDisplayName;
    private ImageView ivEditName;
    private MaterialCardView ivEditPhoto;
    private Button btnLogout, btnSaveChanges, btnViewAllCompleted;
    private ProgressBar progressBar;
    private FirebaseAuth mAuth;
    private GoogleSignInClient mGoogleSignInClient;
    private FirebaseUser currentUser;
    private SettingsViewModel viewModel;
    private ActivityResultLauncher<String> mGetContent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        viewModel = new ViewModelProvider(this).get(SettingsViewModel.class);
        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        // --- View Initialization ---
        ivProfileImage = findViewById(R.id.iv_profile_image);
        tvDisplayName = findViewById(R.id.tv_display_name);
        etDisplayName = findViewById(R.id.et_display_name);
        ivEditName = findViewById(R.id.iv_edit_name);
        ivEditPhoto = findViewById(R.id.iv_edit_photo);
        tvEmail = findViewById(R.id.tv_email);
        btnLogout = findViewById(R.id.btn_logout);
        btnSaveChanges = findViewById(R.id.btn_save_changes);
        progressBar = findViewById(R.id.profile_progress_bar);
        btnViewAllCompleted = findViewById(R.id.btn_view_all_completed);

        // --- ADDED: ID Card Views ---
        tvUid = findViewById(R.id.tv_user_uid);
        btnCopyUid = findViewById(R.id.btn_copy_uid);
        btnShareUid = findViewById(R.id.btn_share_uid);
        // --- End View Initialization ---

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

        btnViewAllCompleted.setOnClickListener(v -> {
            Intent intent = new Intent(SettingsActivity.this, CompletedTasksActivity.class);
            startActivity(intent);
        });

        // --- ADDED: Copy and Share Listeners ---
        btnCopyUid.setOnClickListener(v -> {
            if (currentUser != null) {
                ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("User ID", currentUser.getUid());
                clipboard.setPrimaryClip(clip);
                Toast.makeText(this, "ID Copied to clipboard", Toast.LENGTH_SHORT).show();
            }
        });

        btnShareUid.setOnClickListener(v -> {
            if (currentUser != null) {
                Intent sendIntent = new Intent();
                sendIntent.setAction(Intent.ACTION_SEND);
                sendIntent.putExtra(Intent.EXTRA_TEXT, "Let's link tasks on SyncTask! My User ID is:\n" + currentUser.getUid());
                sendIntent.setType("text/plain");
                startActivity(Intent.createChooser(sendIntent, "Share ID via"));
            }
        });
        // --- End ADDED ---

        observeViewModel();
    }

    private void observeViewModel() {
        viewModel.getUserLiveData().observe(this, result -> {
            if (isFinishing()) return;
            if (result instanceof Result.Success) {
                // Data handles itself mostly, but we could refresh UI here if needed
            } else if (result instanceof Result.Error) {
                Log.e("SettingsActivity", "Error listening to user updates", ((Result.Error<User>) result).exception);
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
        } else {
            goToLogin();
        }
    }

    private void loadUserProfile() {
        tvDisplayName.setText(currentUser.getDisplayName());
        tvEmail.setText(currentUser.getEmail());

        // --- ADDED: Set UID text ---
        tvUid.setText(currentUser.getUid());

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
                                Toast.makeText(SettingsActivity.this, "Profile updated successfully!", Toast.LENGTH_SHORT).show();
                                tvDisplayName.setText(newName);
                                toggleEditMode(false);
                            } else if (result instanceof Result.Error) {
                                Toast.makeText(SettingsActivity.this, "Error updating profile in database.", Toast.LENGTH_SHORT).show();
                            }
                        });
                    } else {
                        showLoading(false);
                        Toast.makeText(SettingsActivity.this, "Failed to update profile.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void logoutUser() {
        mAuth.signOut();
        mGoogleSignInClient.signOut().addOnCompleteListener(this, task -> goToLogin());
    }

    private void goToLogin() {
        Intent intent = new Intent(SettingsActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
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