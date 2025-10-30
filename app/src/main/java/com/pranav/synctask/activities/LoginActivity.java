package com.pranav.synctask.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreSettings;
import com.pranav.synctask.R;
import com.pranav.synctask.data.Result; // CHANGED: Added import
import com.pranav.synctask.data.UserRepository;
// CHANGED: Added import
import com.pranav.synctask.models.User;
// CHANGED: Removed FirebaseHelper import, it's no longer used here

public class LoginActivity extends AppCompatActivity {
    private static final String TAG = "LoginActivity";
    private FirebaseAuth mAuth;
    private GoogleSignInClient mGoogleSignInClient;
    private ProgressBar progressBar;
    private Button signInButton;
    private final ActivityResultLauncher<Intent> signInLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK) {
                    showLoading(true);
                    Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(result.getData());
                    try {
                        GoogleSignInAccount account = task.getResult(ApiException.class);
                        firebaseAuthWithGoogle(account.getIdToken());
                    } catch (ApiException e) {
                        Log.w(TAG, "Google sign in failed", e);
                        Toast.makeText(this, "Authentication failed.", Toast.LENGTH_SHORT).show();
                        showLoading(false);
                    }
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        FirebaseFirestoreSettings settings = new FirebaseFirestoreSettings.Builder()
                .setPersistenceEnabled(true)
                .build();
        db.setFirestoreSettings(settings);

        mAuth = FirebaseAuth.getInstance();

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        signInButton = findViewById(R.id.btn_google_sign_in);
        progressBar = findViewById(R.id.login_progress_bar);

        signInButton.setOnClickListener(v -> signIn());
    }

    @Override
    public void onStart() {
        super.onStart();
        // This activity is no longer the launcher, so we don't check for currentUser here.
        // The new DashboardActivity will redirect here if needed.
    }

    private void signIn() {
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        signInLauncher.launch(signInIntent);
    }

    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        createOrUpdateUser(user);
                    } else {
                        Log.w(TAG, "signInWithCredential:failure", task.getException());
                        Toast.makeText(LoginActivity.this, "Authentication failed.",
                                Toast.LENGTH_SHORT).show();
                        showLoading(false);
                    }
                });
    }

    private void createOrUpdateUser(FirebaseUser firebaseUser) {
        UserRepository.getInstance().createOrUpdateUser(firebaseUser).observe(this, result -> {
            if (result instanceof Result.Success) {
                // MODIFIED IN PHASE 3B: Go to the new hub
                startActivity(new Intent(LoginActivity.this, DashboardActivity.class));
                finish();
            } else if (result instanceof Result.Error) {
                Log.e(TAG, "Error creating/updating user", ((Result.Error<User>) result).exception);
                Toast.makeText(LoginActivity.this, "Error setting up user profile.",
                        Toast.LENGTH_SHORT).show();
                showLoading(false);
            } else if (result instanceof Result.Loading) {
                showLoading(true);
            }
        });
    }

    private void showLoading(boolean isLoading) {
        if (isLoading) {
            progressBar.setVisibility(View.VISIBLE);
            signInButton.setVisibility(View.GONE);
        } else {
            progressBar.setVisibility(View.GONE);
            signInButton.setVisibility(View.VISIBLE);
        }
    }
}