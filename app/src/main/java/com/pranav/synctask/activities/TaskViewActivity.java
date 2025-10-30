package com.pranav.synctask.activities;

import android.app.ActivityOptions; // ADDED
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Window;
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
import com.google.android.material.transition.platform.MaterialFadeThrough;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.pranav.synctask.R;
import com.pranav.synctask.adapters.ViewPagerAdapter;
import com.pranav.synctask.ui.viewmodels.TasksViewModel;

// MODIFIED IN PHASE 2: Renamed from MainActivity
public class TaskViewActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private TasksViewModel viewModel;
    private String currentSpaceId;
    @Override
    protected void onNewIntent(@NonNull Intent intent) {
        super.onNewIntent(intent);
        handleNotificationIntent(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        getWindow().requestFeature(Window.FEATURE_ACTIVITY_TRANSITIONS);
        getWindow().setEnterTransition(new MaterialFadeThrough());
        getWindow().setExitTransition(new MaterialFadeThrough());

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_task_view); // MODIFIED IN PHASE 2
        handleNotificationIntent(getIntent());

        currentSpaceId = getIntent().getStringExtra("SPACE_ID");
        if (currentSpaceId == null || currentSpaceId.isEmpty()) {
            Toast.makeText(this, "Error: No Space ID provided.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        mAuth = FirebaseAuth.getInstance();
        viewModel = new ViewModelProvider(this).get(TasksViewModel.class);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        // TODO: Set toolbar title to space name

        ViewPager2 viewPager = findViewById(R.id.view_pager);
        TabLayout tabLayout = findViewById(R.id.tab_layout);
        FloatingActionButton fab = findViewById(R.id.fab_add_task);

        viewPager.setAdapter(new ViewPagerAdapter(this));
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
        // --- MODIFIED: Launch with Animation ---
        fab.setOnClickListener(v -> {
            Intent intent = new Intent(TaskViewActivity.this, CreateTaskActivity.class);
            intent.putExtra("SPACE_ID", currentSpaceId);

            ActivityOptions options = ActivityOptions.makeSceneTransitionAnimation(
                    this,
                    fab,
                    "fab_to_create_task" // The transitionName
            );
            startActivity(intent, options.toBundle());
        });
        // --- END MODIFIED ---
    }

    // ... (rest of the file is unchanged) ...
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
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
        // MODIFIED IN PHASE 2
        int itemId = item.getItemId();
        if (itemId == R.id.action_archive) {
            Intent intent = new Intent(this, CompletedTasksActivity.class);
            intent.putExtra("SPACE_ID", currentSpaceId);
            startActivity(intent);
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
        // This viewmodel now only loads PENDING tasks
        viewModel.loadTasks(currentSpaceId);
    }

    private void goToLogin() {
        Intent intent = new Intent(TaskViewActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void handleNotificationIntent(Intent intent) {
        if (intent != null) {
            String taskId = intent.getStringExtra("taskId");
            String action = intent.getStringExtra("action");

            if (taskId != null && action != null) {
                switch (action) {
                    case "view":
                    case "new_task":
                    case "task_updated":
                        Toast.makeText(this, "Opening task: " + taskId, Toast.LENGTH_SHORT).show();
                        break;
                    case "status_changed":
                        Toast.makeText(this, "Task status updated", Toast.LENGTH_SHORT).show();
                        break;
                }
            }
        }
    }
}