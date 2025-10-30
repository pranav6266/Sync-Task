package com.pranav.synctask.activities;

import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.pranav.synctask.R;
import com.pranav.synctask.adapters.PersonalLinksAdapter;
import com.pranav.synctask.data.Result;
import com.pranav.synctask.data.UserRepository;
import com.pranav.synctask.models.Space;
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

    // --- MODIFIED IN PHASE 3C: Use MediatorLiveData ---
    private MediatorLiveData<CombinedResult> combinedData = new MediatorLiveData<>();
    private List<Space> currentLinks = new ArrayList<>();
    private List<User> currentPartners = new ArrayList<>();
    // --- END MODIFIED ---

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_personal_links);

        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            finish(); // Should not happen if coming from dashboard
            return;
        }

        viewModel = new ViewModelProvider(this).get(DashboardViewModel.class);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        recyclerView = findViewById(R.id.personal_links_recycler_view);
        setupRecyclerView();
        observeViewModel();
    }

    private void setupRecyclerView() {
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new PersonalLinksAdapter(this, new ArrayList<>(), new ArrayList<>());
        recyclerView.setAdapter(adapter);
    }

    // --- MODIFIED IN PHASE 3C: Observe VM LiveData ---
    private void observeViewModel() {

        combinedData.addSource(viewModel.getPersonalLinksLiveData(), result -> {
            if (result instanceof Result.Success) {
                currentLinks = ((Result.Success<List<Space>>) result).data;
                combinedData.setValue(new CombinedResult(currentLinks, currentPartners));
            } else if (result instanceof Result.Error) {
                Toast.makeText(this, "Error loading personal links.", Toast.LENGTH_SHORT).show();
            }
        });

        combinedData.addSource(viewModel.getPartnerDetails(), result -> {
            if (result instanceof Result.Success) {
                currentPartners = ((Result.Success<List<User>>) result).data;
                combinedData.setValue(new CombinedResult(currentLinks, currentPartners));
            } else if (result instanceof Result.Error) {
                Toast.makeText(this, "Error loading partner details.", Toast.LENGTH_SHORT).show();
            }
        });

        // This observer fires when *both* data sources have reported success
        combinedData.observe(this, combinedResult -> {
            if (combinedResult != null && !combinedResult.links.isEmpty() && !combinedResult.partners.isEmpty()) {
                adapter.updateLinks(combinedResult.links, combinedResult.partners);
            } else if (combinedResult != null && combinedResult.links.isEmpty()) {
                adapter.updateLinks(new ArrayList<>(), new ArrayList<>());
                // TODO: Show empty state
            }
        });
    }
    // --- END MODIFIED ---

    @Override
    protected void onStart() {
        super.onStart();
        // Attaching the listener will trigger the LiveData flow in the ViewModel
        viewModel.attachUserListener(currentUser.getUid());
    }

    // --- ADDED IN PHASE 3C: Helper class ---
    private static class CombinedResult {
        final List<Space> links;
        final List<User> partners;

        CombinedResult(List<Space> links, List<User> partners) {
            this.links = links;
            this.partners = partners;
        }
    }
}