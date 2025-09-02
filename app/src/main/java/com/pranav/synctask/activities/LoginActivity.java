package com.pranav.synctask.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Toast;

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
import com.pranav.synctask.R;
import com.pranav.synctask.models.User;
import com.pranav.synctask.utils.FirebaseHelper;

public class LoginActivity extends AppCompatActivity {
    private static final String TAG = "LoginActivity";
    private static final int RC_SIGN_IN = 9001;

    private FirebaseAuth mAuth;
    private GoogleSignInClient mGoogleSignInClient;
    private ProgressBar progressBar;
    private Button signInButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

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
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            showLoading(true);
            checkUserPairingStatus(currentUser);
        }
    }

    private void signIn() {
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SIGN_IN) {
            showLoading(true);
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                firebaseAuthWithGoogle(account.getIdToken());
            } catch (ApiException e) {
                Log.w(TAG, "Google sign in failed", e);
                Toast.makeText(this, "Authentication failed.", Toast.LENGTH_SHORT).show();
                showLoading(false);
            }
        }
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
        FirebaseHelper.createOrUpdateUser(firebaseUser, new FirebaseHelper.UserCallback() {
            @Override
            public void onSuccess(User user) {
                checkUserPairingStatus(firebaseUser);
            }

            @Override
            public void onError(Exception e) {
                Log.e(TAG, "Error creating/updating user", e);
                Toast.makeText(LoginActivity.this, "Error setting up user profile.",
                        Toast.LENGTH_SHORT).show();
                showLoading(false);
            }
        });
    }

    private void checkUserPairingStatus(FirebaseUser user) {
        FirebaseHelper.getUser(user.getUid(), new FirebaseHelper.UserCallback() {
            @Override
            public void onSuccess(User userData) {
                if (userData.getPairedWithUID() != null && !userData.getPairedWithUID().isEmpty()) {
                    startActivity(new Intent(LoginActivity.this, MainActivity.class));
                } else {
                    startActivity(new Intent(LoginActivity.this, PairingActivity.class));
                }
                finish();
            }

            @Override
            public void onError(Exception e) {
                Log.e(TAG, "Error checking pairing status", e);
                Toast.makeText(LoginActivity.this, "Error loading user data.",
                        Toast.LENGTH_SHORT).show();
                showLoading(false);
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