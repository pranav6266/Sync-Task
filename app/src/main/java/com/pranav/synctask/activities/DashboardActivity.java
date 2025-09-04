package com.pranav.synctask.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.ListenerRegistration;
import com.pranav.synctask.R;
import com.pranav.synctask.models.User;
import com.pranav.synctask.utils.FirebaseHelper;

public class DashboardActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private ListenerRegistration userListenerRegistration;

    private Button btnGoToTasks, btnPairWithPartner, btnViewProfile;
    private TextView tvWelcomeMessage;
    private LinearLayout layoutPairedStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        mAuth = FirebaseAuth.getInstance();

        btnGoToTasks = findViewById(R.id.btn_go_to_tasks);
        btnPairWithPartner = findViewById(R.id.btn_pair_with_partner);
        btnViewProfile = findViewById(R.id.btn_view_profile);
        tvWelcomeMessage = findViewById(R.id.tv_welcome_message);
        layoutPairedStatus = findViewById(R.id.layout_paired_status);

        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null && currentUser.getDisplayName() != null) {
            tvWelcomeMessage.setText("Welcome, " + currentUser.getDisplayName() + "!");
        }

        btnGoToTasks.setOnClickListener(v -> {
            startActivity(new Intent(DashboardActivity.this, MainActivity.class));
        });

        btnPairWithPartner.setOnClickListener(v -> {
            startActivity(new Intent(DashboardActivity.this, PairingActivity.class));
        });

        btnViewProfile.setOnClickListener(v -> {
            startActivity(new Intent(DashboardActivity.this, ProfileActivity.class));
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            // This should not happen, but as a safeguard
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }
        attachUserListener(currentUser.getUid());
    }

    @Override
    protected void onStop() {
        super.onStop();
        detachUserListener();
    }

    private void attachUserListener(String uid) {
        detachUserListener();
        userListenerRegistration = FirebaseHelper.addUserListener(uid, new FirebaseHelper.UserCallback() {
            @Override
            public void onSuccess(User user) {
                if (user.getPairedWithUID() != null && !user.getPairedWithUID().isEmpty()) {
                    // User is paired
                    btnGoToTasks.setVisibility(View.VISIBLE);
                    layoutPairedStatus.setVisibility(View.VISIBLE);
                    btnPairWithPartner.setVisibility(View.GONE);
                } else {
                    // User is not paired
                    btnGoToTasks.setVisibility(View.GONE);
                    layoutPairedStatus.setVisibility(View.GONE);
                    btnPairWithPartner.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onError(Exception e) {
                Log.e("DashboardActivity", "Error listening to user pairing status", e);
                Toast.makeText(DashboardActivity.this, "Could not check pairing status.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void detachUserListener() {
        if (userListenerRegistration != null) {
            userListenerRegistration.remove();
        }
    }
}

