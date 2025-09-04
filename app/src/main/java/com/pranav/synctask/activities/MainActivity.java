package com.pranav.synctask.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.ListenerRegistration;
import com.pranav.synctask.R;
import com.pranav.synctask.adapters.ViewPagerAdapter;
import com.pranav.synctask.models.User;
import com.pranav.synctask.utils.FirebaseHelper;

public class MainActivity extends AppCompatActivity {

    private ListenerRegistration userListenerRegistration;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mAuth = FirebaseAuth.getInstance();

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        ViewPager2 viewPager = findViewById(R.id.view_pager);
        TabLayout tabLayout = findViewById(R.id.tab_layout);
        FloatingActionButton fab = findViewById(R.id.fab_add_task);

        ViewPagerAdapter adapter = new ViewPagerAdapter(this);
        viewPager.setAdapter(adapter);

        new TabLayoutMediator(tabLayout, viewPager,
                (tab, position) -> {
                    switch (position) {
                        case 0:
                            tab.setText("Today");
                            break;
                        case 1:
                            tab.setText("This Month");
                            break;
                        case 2:
                            tab.setText("All");
                            break;
                        case 3:
                            tab.setText("Updates");
                            break;
                    }
                }
        ).attach();

        fab.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, CreateTaskActivity.class));
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_profile) {
            startActivity(new Intent(this, ProfileActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onStart() {
        super.onStart();
        attachUserListener();
    }

    @Override
    protected void onStop() {
        super.onStop();
        detachUserListener();
    }

    private void attachUserListener() {
        detachUserListener();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            goToLogin();
            return;
        }

        userListenerRegistration = FirebaseHelper.addUserListener(currentUser.getUid(), new FirebaseHelper.UserCallback() {
            @Override
            public void onSuccess(User user) {
                // If user is no longer paired, send them back to the dashboard screen
                if ((user.getPairedWithUID() == null || user.getPairedWithUID().isEmpty()) && !isFinishing()) {
                    Toast.makeText(MainActivity.this, "You are no longer paired.", Toast.LENGTH_LONG).show();
                    // THIS IS THE LINE THAT WAS CHANGED
                    goToDashboard();
                }
            }

            @Override
            public void onError(Exception e) {
                Log.e("MainActivity", "Error listening to user pairing status", e);
                // If there's an error, it might be safer to go back to the dashboard
                goToDashboard();
            }
        });
    }

    private void detachUserListener() {
        if (userListenerRegistration != null) {
            userListenerRegistration.remove();
            userListenerRegistration = null;
        }
    }

    private void goToLogin() {
        Intent intent = new Intent(MainActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void goToDashboard() {
        Intent intent = new Intent(MainActivity.this, DashboardActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}

