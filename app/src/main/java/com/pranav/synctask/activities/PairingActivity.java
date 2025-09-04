package com.pranav.synctask.activities;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.ListenerRegistration;
import com.pranav.synctask.R;
import com.pranav.synctask.models.User;
import com.pranav.synctask.utils.FirebaseHelper;

public class PairingActivity extends AppCompatActivity {

    private TextView tvPartnerCode;
    private EditText etPartnerCode;
    private Button btnPair;
    private FirebaseUser currentUser;
    private ProgressBar progressBar;
    private View pairingLayout;
    private ListenerRegistration userListenerRegistration;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pairing);

        tvPartnerCode = findViewById(R.id.tv_partner_code);
        etPartnerCode = findViewById(R.id.et_partner_code);
        btnPair = findViewById(R.id.btn_pair);
        progressBar = findViewById(R.id.pairing_progress_bar);
        pairingLayout = findViewById(R.id.pairing_layout);

        currentUser = FirebaseAuth.getInstance().getCurrentUser();

        if (currentUser != null) {
            loadUserPartnerCode();
        }

        tvPartnerCode.setOnClickListener(v -> {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("Partner Code", tvPartnerCode.getText().toString());
            clipboard.setPrimaryClip(clip);
            Toast.makeText(this, "Code copied to clipboard!", Toast.LENGTH_SHORT).show();
        });

        btnPair.setOnClickListener(v -> {
            String partnerCode = etPartnerCode.getText().toString().trim();
            if (partnerCode.length() == 6) {
                pairWithPartner(partnerCode);
            } else {
                Toast.makeText(this, "Please enter a valid 6-character code.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (currentUser != null) {
            attachUserListener();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        detachUserListener();
    }

    private void attachUserListener() {
        detachUserListener(); // Ensure no multiple listeners are attached
        if (currentUser == null) return;

        userListenerRegistration = FirebaseHelper.addUserListener(currentUser.getUid(), new FirebaseHelper.UserCallback() {
            @Override
            public void onSuccess(User user) {
                // Check if the user is now paired and we are still on this activity
                if (user.getPairedWithUID() != null && !user.getPairedWithUID().isEmpty()) {
                    if (!isFinishing() && !isDestroyed()) {
                        Toast.makeText(PairingActivity.this, "Partner connected!", Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(PairingActivity.this, MainActivity.class));
                        finish();
                    }
                }
            }

            @Override
            public void onError(Exception e) {
                // Don't show a toast here as it might be annoying if there are temporary network issues.
                // The error is logged for debugging.
                Log.e("PairingActivity", "Error listening to user updates", e);
            }
        });
    }

    private void detachUserListener() {
        if (userListenerRegistration != null) {
            userListenerRegistration.remove();
            userListenerRegistration = null;
        }
    }

    private void loadUserPartnerCode() {
        showLoading(true);
        FirebaseHelper.getUser(currentUser.getUid(), new FirebaseHelper.UserCallback() {
            @Override
            public void onSuccess(User user) {
                tvPartnerCode.setText(user.getPartnerCode());
                showLoading(false);
            }

            @Override
            public void onError(Exception e) {
                Toast.makeText(PairingActivity.this, "Could not load your code.", Toast.LENGTH_SHORT).show();
                showLoading(false);
            }
        });
    }

    private void pairWithPartner(String partnerCode) {
        showLoading(true);
        FirebaseHelper.pairUsers(currentUser.getUid(), partnerCode, new FirebaseHelper.PairingCallback() {
            @Override
            public void onSuccess() {
                // We no longer navigate from here.
                // The real-time listener will detect the change and handle navigation.
                // This prevents the race condition.
                showLoading(false); // Hide loading indicator on success
                Toast.makeText(PairingActivity.this, "Pairing successful! Connecting...", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(Exception e) {
                Snackbar.make(pairingLayout, "Pairing failed: " + e.getMessage(), Snackbar.LENGTH_LONG).show();
                showLoading(false);
            }
        });
    }

    private void showLoading(boolean isLoading) {
        if (isLoading) {
            progressBar.setVisibility(View.VISIBLE);
            btnPair.setEnabled(false);
            etPartnerCode.setEnabled(false);
        } else {
            progressBar.setVisibility(View.GONE);
            btnPair.setEnabled(true);
            etPartnerCode.setEnabled(true);
        }
    }
}

