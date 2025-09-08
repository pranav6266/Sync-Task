package com.pranav.synctask.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.ViewModelProvider;
import androidx.viewpager2.widget.ViewPager2;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.pranav.synctask.R;
import com.pranav.synctask.adapters.ViewPagerAdapter;
import com.pranav.synctask.data.Result;
import com.pranav.synctask.models.User;
import com.pranav.synctask.ui.viewmodels.TasksViewModel;

public class MainActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private TasksViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mAuth = FirebaseAuth.getInstance();
        viewModel = new ViewModelProvider(this).get(TasksViewModel.class);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        ViewPager2 viewPager = findViewById(R.id.view_pager);
        TabLayout tabLayout = findViewById(R.id.tab_layout);
        FloatingActionButton fab = findViewById(R.id.fab_add_task);

        viewPager.setAdapter(new ViewPagerAdapter(this));
        new TabLayoutMediator(tabLayout, viewPager,
                (tab, position) -> {
                    switch (position) {
                        case 0: tab.setText("Today"); break;
                        case 1: tab.setText("This Month"); break;
                        case 2: tab.setText("All"); break;
                        case 3: tab.setText("Updates"); break;
                    }
                }
        ).attach();

        fab.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, CreateTaskActivity.class)));

        observeViewModel();
    }

    private void observeViewModel() {
        viewModel.getUserResult().observe(this, result -> {
            if (result instanceof Result.Success) {
                User user = ((Result.Success<User>) result).data;
                if ((user.getPairedWithUID() == null || user.getPairedWithUID().isEmpty()) && !isFinishing()) {
                    Toast.makeText(MainActivity.this, "You are no longer paired.", Toast.LENGTH_LONG).show();
                    goToDashboard();
                }
            } else if (result instanceof Result.Error) {
                Log.e("MainActivity", "Error listening to user pairing status", ((Result.Error<User>) result).exception);
                goToDashboard();
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);

        // PHASE 4: Setup search view
        MenuItem searchItem = menu.findItem(R.id.action_search);
        SearchView searchView = (SearchView) searchItem.getActionView();
        searchView.setQueryHint("Search by title...");
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                viewModel.setSearchQuery(newText);
                return true;
            }
        });
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
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            goToLogin();
            return;
        }
        viewModel.attachUserListener(currentUser.getUid());
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