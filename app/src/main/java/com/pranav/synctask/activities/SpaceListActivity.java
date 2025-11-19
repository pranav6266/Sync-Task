package com.pranav.synctask.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.airbnb.lottie.LottieAnimationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.pranav.synctask.R;
import com.pranav.synctask.adapters.SpacesAdapter;
import com.pranav.synctask.data.Result;
import com.pranav.synctask.models.Space;
import com.pranav.synctask.models.Task;
import com.pranav.synctask.models.User;
import com.pranav.synctask.ui.DashboardViewModel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SpaceListActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private DashboardViewModel viewModel;
    private FirebaseUser currentUser;
    private SpacesAdapter spacesAdapter;
    private RecyclerView spacesRecyclerView;
    private FloatingActionButton fabAddSpace;
    private LottieAnimationView emptyView;

    // Mediator for combining data sources
    private final MediatorLiveData<CombinedSpacesResult> combinedData = new MediatorLiveData<>();

    // Local Cache
    private List<Space> currentSpaces = new ArrayList<>();
    private Map<String, User> currentMembers = new HashMap<>();
    private List<Task> allTasks = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_space_list);

        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            finish();
            return;
        }

        viewModel = new ViewModelProvider(this).get(DashboardViewModel.class);

        spacesRecyclerView = findViewById(R.id.spaces_recycler_view);
        fabAddSpace = findViewById(R.id.fab_add_space);
        emptyView = findViewById(R.id.empty_view);

        // Add Space is now handled via Dashboard/Bottom Sheet,
        // but we can keep this FAB to open the new bottom sheet if you like,
        // or redirect to PairingActivity. For now, let's hide it or make it a shortcut.
        fabAddSpace.setOnClickListener(v -> startActivity(new Intent(this, PairingActivity.class)));

        setupRecyclerView();
        observeViewModel();
    }

    private void setupRecyclerView() {
        spacesRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        // Initialize adapter with empty data
        spacesAdapter = new SpacesAdapter(this, new ArrayList<>(), new HashMap<>(), new ArrayList<>());
        spacesRecyclerView.setAdapter(spacesAdapter);
    }

    private void observeViewModel() {
        // 1. Observe Shared Spaces
        combinedData.addSource(viewModel.getSharedSpacesLiveData(), result -> {
            if (result instanceof Result.Success) {
                currentSpaces = ((Result.Success<List<Space>>) result).data;
                emitCombinedResult();
            } else if (result instanceof Result.Error) {
                Toast.makeText(this, "Error loading spaces.", Toast.LENGTH_SHORT).show();
                updateEmptyView(true);
            }
        });

        // 2. Observe Members (For names in description)
        combinedData.addSource(viewModel.getMembersMap(), map -> {
            currentMembers = map;
            emitCombinedResult();
        });

        // 3. Observe All Tasks (For Progress)
        combinedData.addSource(viewModel.getAllTasksResult(), result -> {
            if (result instanceof Result.Success) {
                allTasks = ((Result.Success<List<Task>>) result).data;
                emitCombinedResult();
            }
        });

        // 4. Update Adapter
        combinedData.observe(this, result -> {
            if (result != null) {
                spacesAdapter.updateSpaces(result.spaces, result.members, result.tasks);
                updateEmptyView(result.spaces.isEmpty());
            }
        });

        // Observe Action Results (Leave/Delete)
        viewModel.getLeaveSpaceResult().observe(this, result -> {
            if (result instanceof Result.Success) Toast.makeText(this, "Left space.", Toast.LENGTH_SHORT).show();
            else if(result instanceof Result.Error) Toast.makeText(this, "Error leaving space.", Toast.LENGTH_SHORT).show();
        });

        viewModel.getDeleteSpaceResult().observe(this, result -> {
            if (result instanceof Result.Success) Toast.makeText(this, "Space deleted.", Toast.LENGTH_SHORT).show();
            else if(result instanceof Result.Error) Toast.makeText(this, "Error deleting space.", Toast.LENGTH_SHORT).show();
        });
    }

    private void emitCombinedResult() {
        combinedData.setValue(new CombinedSpacesResult(currentSpaces, currentMembers, allTasks));
    }

    private void updateEmptyView(boolean isEmpty) {
        if (isEmpty) {
            emptyView.setVisibility(View.VISIBLE);
            emptyView.playAnimation();
        } else {
            emptyView.setVisibility(View.GONE);
            emptyView.cancelAnimation();
        }
        spacesRecyclerView.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (currentUser != null) {
            viewModel.attachUserListener(currentUser.getUid());
        }
    }

    // Helper Class
    private static class CombinedSpacesResult {
        final List<Space> spaces;
        final Map<String, User> members;
        final List<Task> tasks;

        CombinedSpacesResult(List<Space> spaces, Map<String, User> members, List<Task> tasks) {
            this.spaces = spaces;
            this.members = members;
            this.tasks = tasks;
        }
    }
}