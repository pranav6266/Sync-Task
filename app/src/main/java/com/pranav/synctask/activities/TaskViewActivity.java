package com.pranav.synctask.activities;

import android.app.ActivityOptions;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Window;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.ViewModelProvider;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.google.android.material.transition.platform.MaterialFadeThrough;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.pranav.synctask.R;
import com.pranav.synctask.adapters.ViewPagerAdapter;
import com.pranav.synctask.models.Space;
import com.pranav.synctask.ui.viewmodels.TasksViewModel;

public class TaskViewActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private TasksViewModel viewModel;
    private String currentSpaceId;
    private String contextType;
    private TextView tvHeaderTitle;
    private TextView tvHeaderSubtitle; // Added for Personal context

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

        // --- LAYOUT SELECTION LOGIC START ---
        contextType = getIntent().getStringExtra("CONTEXT_TYPE");
        if (contextType == null) {
            contextType = Space.TYPE_SHARED;
        }

        if (Space.TYPE_PERSONAL.equals(contextType)) {
            setContentView(R.layout.activity_task_view); // Loads layout with Subtitle
        } else {
            setContentView(R.layout.activity_task_view_shared); // Loads layout with Heading only
        }
        // --- LAYOUT SELECTION LOGIC END ---

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
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }

        // --- HEADER TEXT LOGIC START ---
        tvHeaderTitle = findViewById(R.id.tv_header_title);

        if (Space.TYPE_PERSONAL.equals(contextType)) {
            tvHeaderTitle.setText(R.string.app_name); // "Sync Task"
            tvHeaderSubtitle = findViewById(R.id.tv_header_subtitle);
            if (tvHeaderSubtitle != null) {
                tvHeaderSubtitle.setText("Personal Tasks");
            }
        } else {
            String spaceName = getIntent().getStringExtra("SPACE_NAME");
            if (spaceName != null && !spaceName.isEmpty()) {
                tvHeaderTitle.setText(spaceName);
            } else {
                tvHeaderTitle.setText("Shared Space");
            }
        }
        // --- HEADER TEXT LOGIC END ---

        ViewPager2 viewPager = findViewById(R.id.view_pager);
        TabLayout tabLayout = findViewById(R.id.tab_layout);

        ExtendedFloatingActionButton fab = findViewById(R.id.fab_add_task);
        viewPager.setAdapter(new ViewPagerAdapter(this, contextType));
        new TabLayoutMediator(tabLayout, viewPager,
                (tab, position) -> {
                    if (Space.TYPE_PERSONAL.equals(contextType)) {
                        switch (position) {
                            case 0: tab.setText("All"); break;
                            case 1: tab.setText("My Tasks"); break;
                            case 2: tab.setText("Partner"); break;
                        }
                    } else {
                        switch (position) {
                            case 0: tab.setText("All"); break;
                            case 1: tab.setText("Shared"); break;
                            case 2: tab.setText("Assigned"); break;
                        }
                    }
                }
        ).attach();

        fab.setOnClickListener(v -> {
            Intent intent = new Intent(TaskViewActivity.this, CreateTaskActivity.class);
            intent.putExtra("SPACE_ID", currentSpaceId);
            intent.putExtra("CONTEXT_TYPE", contextType);

            ActivityOptions options = ActivityOptions.makeSceneTransitionAnimation(
                    this,
                    fab,
                    "fab_to_create_task"
            );
            startActivity(intent, options.toBundle());
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Use space_menu for both, or maintain separate ones if needed.
        // Assuming you created space_menu.xml in the previous step.
        getMenuInflater().inflate(R.menu.space_view, menu);

        MenuItem searchItem = menu.findItem(R.id.action_search);
        if (searchItem != null) {
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
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
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

    public String getContextType() {
        return contextType;
    }
}