package com.pranav.synctask.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.messaging.FirebaseMessaging;
import com.pranav.synctask.R;
import com.pranav.synctask.data.UserRepository;
import com.pranav.synctask.fragments.PersonalTabFragment;
import com.pranav.synctask.fragments.SpacesTabFragment;

public class DashboardActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;
    private MaterialToolbar topAppBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();

        if (currentUser == null) {
            goToLogin();
            return;
        }

        topAppBar = findViewById(R.id.topAppBar);
        setSupportActionBar(topAppBar);

        // Setup Bottom Navigation
        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        bottomNav.setOnItemSelectedListener(item -> {
            Fragment selectedFragment = null;
            String title = "";

            if (item.getItemId() == R.id.nav_personal) {
                selectedFragment = new PersonalTabFragment();
                title = "My Tasks";
            } else if (item.getItemId() == R.id.nav_spaces) {
                selectedFragment = new SpacesTabFragment();
                title = "Shared Spaces";
            }

            if (selectedFragment != null) {
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, selectedFragment)
                        .commit();
                if (getSupportActionBar() != null) {
                    getSupportActionBar().setTitle(title);
                }
            }
            return true;
        });

        // Menu Item Clicks (Settings)
        topAppBar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.action_archive) {
                // Archive is now handled globally or per space,
                // for now let's point to the Settings activity or global archive
                startActivity(new Intent(this, CompletedTasksActivity.class));
                return true;
            }
            // We can add a profile icon to the menu later to go to SettingsActivity
            return false;
        });

        // Profile Access via Toolbar Navigation Icon (Optional, can set icon)
        // topAppBar.setNavigationIcon(R.drawable.ic_profile);
        // topAppBar.setNavigationOnClickListener(v -> startActivity(new Intent(this, SettingsActivity.class)));

        // Load Default Fragment (Personal)
        if (savedInstanceState == null) {
            bottomNav.setSelectedItemId(R.id.nav_personal);
        }

        updateFcmToken();
    }

    private void updateFcmToken() {
        FirebaseMessaging.getInstance().getToken().addOnCompleteListener(task -> {
            if (!task.isSuccessful()) {
                Log.w("DashboardActivity", "Fetching FCM registration token failed", task.getException());
                return;
            }
            String token = task.getResult();
            UserRepository.getInstance().updateFcmToken(currentUser.getUid(), token);
        });
    }

    private void goToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}