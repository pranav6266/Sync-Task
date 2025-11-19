package com.pranav.synctask.activities;

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
import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.pranav.synctask.R;
import com.pranav.synctask.adapters.PersonalLinksAdapter;
import com.pranav.synctask.data.Result;
import com.pranav.synctask.models.Space;
import com.pranav.synctask.models.Task;
import com.pranav.synctask.models.User;
import com.pranav.synctask.ui.DashboardViewModel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PersonalLinksActivity extends AppCompatActivity {

    private DashboardViewModel viewModel;
    private PersonalLinksAdapter adapter;
    private RecyclerView recyclerView;
    private FirebaseUser currentUser;
    private LottieAnimationView emptyView;

    // Mediator to combine Links, Members, and Tasks before updating UI
    private final MediatorLiveData<CombinedResult> combinedData = new MediatorLiveData<>();

    // Local State Cache
    private List<Space> currentLinks = new ArrayList<>();
    private Map<String, User> currentMembers = new HashMap<>();
    private List<Task> allTasks = new ArrayList<>();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_personal_links);

        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            finish();
            return;
        }

        viewModel = new ViewModelProvider(this).get(DashboardViewModel.class);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        recyclerView = findViewById(R.id.personal_links_recycler_view);
        emptyView = findViewById(R.id.empty_view);

        setupRecyclerView();
        observeViewModel();
    }

    private void setupRecyclerView() {
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        // Pass empty initial data
        adapter = new PersonalLinksAdapter(this, new ArrayList<>(), new HashMap<>(), new ArrayList<>());
        recyclerView.setAdapter(adapter);
    }

    private void observeViewModel() {
        // 1. Observe Links List
        combinedData.addSource(viewModel.getPersonalLinksLiveData(), result -> {
            if (result instanceof Result.Success) {
                currentLinks = ((Result.Success<List<Space>>) result).data;
                emitCombinedResult();
            } else if (result instanceof Result.Error) {
                Toast.makeText(this, "Error loading links.", Toast.LENGTH_SHORT).show();
                updateEmptyView(true);
            }
        });

        // 2. Observe Members Map (To resolve names)
        combinedData.addSource(viewModel.getMembersMap(), map -> {
            currentMembers = map;
            emitCombinedResult();
        });

        // 3. Observe All Tasks (For Progress Bars)
        combinedData.addSource(viewModel.getAllTasksResult(), result -> {
            if (result instanceof Result.Success) {
                allTasks = ((Result.Success<List<Task>>) result).data;
                emitCombinedResult();
            }
        });

        // 4. Update Adapter when any source changes
        combinedData.observe(this, result -> {
            if (result != null) {
                adapter.updateLinks(result.links, result.members, result.tasks);
                updateEmptyView(result.links.isEmpty());
            }
        });
    }

    private void emitCombinedResult() {
        combinedData.setValue(new CombinedResult(currentLinks, currentMembers, allTasks));
    }

    private void updateEmptyView(boolean isEmpty) {
        if (isEmpty) {
            emptyView.setVisibility(View.VISIBLE);
            emptyView.playAnimation();
        } else {
            emptyView.setVisibility(View.GONE);
            emptyView.cancelAnimation();
        }
        recyclerView.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (currentUser != null) {
            viewModel.attachUserListener(currentUser.getUid());
        }
    }

    // Helper Class
    private static class CombinedResult {
        final List<Space> links;
        final Map<String, User> members;
        final List<Task> tasks;

        CombinedResult(List<Space> links, Map<String, User> members, List<Task> tasks) {
            this.links = links;
            this.members = members;
            this.tasks = tasks;
        }
    }
}