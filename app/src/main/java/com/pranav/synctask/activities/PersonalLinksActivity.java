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
import com.pranav.synctask.data.UserRepository;
import com.pranav.synctask.models.Space;
import com.pranav.synctask.models.Task; // ADDED
import com.pranav.synctask.models.User;
import com.pranav.synctask.ui.DashboardViewModel;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
public class PersonalLinksActivity extends AppCompatActivity {

    private DashboardViewModel viewModel;
    private PersonalLinksAdapter adapter;
    private RecyclerView recyclerView;
    private FirebaseUser currentUser;
    private LottieAnimationView emptyView;

    // --- MODIFIED: Use MediatorLiveData ---
    private MediatorLiveData<CombinedResult> combinedData = new MediatorLiveData<>();
    private List<Space> currentLinks = new ArrayList<>();
    private List<User> currentPartners = new ArrayList<>();
    private List<Task> allTasks = new ArrayList<>(); // ADDED

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_personal_links);

        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            finish();
            // Should not happen if coming from dashboard
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
        // MODIFIED: Added allTasks list
        adapter = new PersonalLinksAdapter(this, new ArrayList<>(), new ArrayList<>(), new ArrayList<>());
        recyclerView.setAdapter(adapter);
    }

    // --- MODIFIED: Observe VM LiveData ---
    private void observeViewModel() {

        combinedData.addSource(viewModel.getPersonalLinksLiveData(), result -> {
            if (result instanceof Result.Success) {
                currentLinks = ((Result.Success<List<Space>>) result).data;
                combinedData.setValue(new CombinedResult(currentLinks, currentPartners, allTasks));

            } else if (result instanceof Result.Error) {
                Toast.makeText(this, "Error loading personal links.", Toast.LENGTH_SHORT).show();
                updateEmptyView(true);
            }
        });
        combinedData.addSource(viewModel.getPartnerDetails(), result -> {
            if (result instanceof Result.Success) {
                currentPartners = ((Result.Success<List<User>>) result).data;
                combinedData.setValue(new CombinedResult(currentLinks, currentPartners, allTasks));
            } else if (result instanceof Result.Error) {
                Toast.makeText(this, "Error loading partner details.", Toast.LENGTH_SHORT).show();

            }
        });
        // ADDED: Observe all tasks
        combinedData.addSource(viewModel.getAllTasksResult(), result -> {
            if (result instanceof Result.Success) {
                allTasks = ((Result.Success<List<Task>>) result).data;
                combinedData.setValue(new CombinedResult(currentLinks, currentPartners, allTasks));
            } else if (result instanceof Result.Error) {
                Toast.makeText(this, "Error loading tasks for progress.", Toast.LENGTH_SHORT).show();
            }
        });

        // This observer fires when *any* data source has reported success
        combinedData.observe(this, combinedResult -> {
            if (combinedResult != null) {
                // We update adapter as long as we have results, progress will calc based on what it has
                adapter.updateLinks(combinedResult.links, combinedResult.partners, combinedResult.tasks);
                updateEmptyView(combinedResult.links.isEmpty());
            }
        });
    }
    // --- END MODIFIED ---

    // --- ADDED ---
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
    // --- END ADDED ---

    @Override
    protected void onStart() {
        super.onStart();
        // Attaching the listener will trigger the LiveData flow in the ViewModel
        viewModel.attachUserListener(currentUser.getUid());
    }

    // --- MODIFIED: Helper class ---
    private static class CombinedResult {
        final List<Space> links;
        final List<User> partners;
        final List<Task> tasks; // ADDED

        CombinedResult(List<Space> links, List<User> partners, List<Task> tasks) {
            this.links = links;
            this.partners = partners;
            this.tasks = tasks; // ADDED
        }
    }
}