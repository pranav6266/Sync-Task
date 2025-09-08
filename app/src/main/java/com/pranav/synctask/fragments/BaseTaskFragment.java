package com.pranav.synctask.fragments;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkRequest;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.pranav.synctask.R;
import com.pranav.synctask.adapters.TaskAdapter;
import com.pranav.synctask.data.Result;
import com.pranav.synctask.models.Task;
import com.pranav.synctask.ui.viewmodels.TasksViewModel;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public abstract class BaseTaskFragment extends Fragment {

    protected RecyclerView recyclerView;
    protected TaskAdapter adapter;
    protected SwipeRefreshLayout swipeRefreshLayout;
    protected TextView emptyView;
    private String currentUserId;
    private TasksViewModel viewModel;
    private ConnectivityManager.NetworkCallback networkCallback;

    // PHASE 4: Local copies of data for filtering
    private List<Task> currentTaskList = new ArrayList<>();
    private String currentSearchQuery = "";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_task_list, container, false);
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            currentUserId = currentUser.getUid();
        }

        recyclerView = view.findViewById(R.id.recycler_view);
        swipeRefreshLayout = view.findViewById(R.id.swipe_refresh_layout);
        emptyView = view.findViewById(R.id.empty_view);

        setupRecyclerView();
        swipeRefreshLayout.setOnRefreshListener(() -> viewModel.loadTasks(currentUserId));
        swipeRefreshLayout.setColorSchemeResources(R.color.primary_color, R.color.accent_color);

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(requireActivity()).get(TasksViewModel.class);
        observeViewModel();
    }

    private void setupRecyclerView() {
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new TaskAdapter(getContext(), new ArrayList<>(), currentUserId);
        recyclerView.setAdapter(adapter);
    }

    @Override
    public void onStart() {
        super.onStart();
        if (currentUserId != null) {
            viewModel.loadTasks(currentUserId);
        }
        registerNetworkCallback();
    }

    @Override
    public void onStop() {
        super.onStop();
        unregisterNetworkCallback();
    }

    private void observeViewModel() {
        // Observe task list changes
        viewModel.getTasksResult().observe(getViewLifecycleOwner(), result -> {
            if (!isAdded()) return;

            swipeRefreshLayout.setRefreshing(result instanceof Result.Loading);

            if (result instanceof Result.Success) {
                currentTaskList = ((Result.Success<List<Task>>) result).data;
                filterAndDisplayTasks(); // PHASE 4
            } else if (result instanceof Result.Error) {
                Log.e(getClass().getSimpleName(), "Error loading tasks", ((Result.Error<List<Task>>) result).exception);
                Toast.makeText(getContext(), "Error loading tasks.", Toast.LENGTH_SHORT).show();
            }
        });

        // PHASE 4: Observe search query changes
        viewModel.getSearchQuery().observe(getViewLifecycleOwner(), query -> {
            currentSearchQuery = query;
            filterAndDisplayTasks();
        });
    }

    // PHASE 4: Centralized filtering logic
    private void filterAndDisplayTasks() {
        // 1. Apply time-based filter (Today, All, etc.)
        List<Task> timeFilteredTasks = filterTasks(currentTaskList);

        // 2. Apply search filter
        List<Task> finalFilteredTasks;
        if (currentSearchQuery.isEmpty()) {
            finalFilteredTasks = timeFilteredTasks;
        } else {
            finalFilteredTasks = timeFilteredTasks.stream()
                    .filter(task -> task.getTitle().toLowerCase().contains(currentSearchQuery.toLowerCase()))
                    .collect(Collectors.toList());
        }

        adapter.updateTasks(finalFilteredTasks);
        updateEmptyView(finalFilteredTasks.isEmpty());
    }

    private void registerNetworkCallback() {
        ConnectivityManager cm = (ConnectivityManager) requireContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        networkCallback = new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(@NonNull Network network) {
                super.onAvailable(network);
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        viewModel.syncLocalTasks(requireContext());
                    });
                }
            }
        };
        cm.registerNetworkCallback(new NetworkRequest.Builder().build(), networkCallback);
    }

    private void unregisterNetworkCallback() {
        if (networkCallback != null) {
            ConnectivityManager cm = (ConnectivityManager) requireContext().getSystemService(Context.CONNECTIVITY_SERVICE);
            cm.unregisterNetworkCallback(networkCallback);
        }
    }

    private void updateEmptyView(boolean isEmpty) {
        emptyView.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        recyclerView.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
    }

    protected abstract List<Task> filterTasks(List<Task> tasks);
}