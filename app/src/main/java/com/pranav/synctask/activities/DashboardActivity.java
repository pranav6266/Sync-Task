package com.pranav.synctask.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.annotation.NonNull;
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

        // Load Default Fragment (Personal)
        if (savedInstanceState == null) {
            bottomNav.setSelectedItemId(R.id.nav_personal);
        }

        updateFcmToken();
    }

    // Inflate the menu (Only Profile icon now)
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        // Search Logic Removed
        return true;
    }

    // Handle clicks on the top bar icons
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        // Archive Logic Removed

        if (id == R.id.action_profile) {
            // Open Profile/Settings
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void updateFcmToken() {
        FirebaseMessaging.getInstance().getToken().addOnCompleteListener(task -> {
            if (!task.isSuccessful()) {
                Log.w("DashboardActivity", "Fetching FCM registration token failed", task.getException());
                return;
            }
            String token = task.getResult();
            if (currentUser != null) {
                UserRepository.getInstance().updateFcmToken(currentUser.getUid(), token);
            }
        });
    }

    private void goToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}