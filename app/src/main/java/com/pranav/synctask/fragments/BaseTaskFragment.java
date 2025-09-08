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

public abstract class BaseTaskFragment extends Fragment {

    protected RecyclerView recyclerView;
    protected TaskAdapter adapter;
    protected SwipeRefreshLayout swipeRefreshLayout;
    protected TextView emptyView;
    private String currentUserId;
    private TasksViewModel viewModel;
    private ConnectivityManager.NetworkCallback networkCallback;

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
        } else {
            Log.e(getClass().getSimpleName(), "User is not authenticated, cannot load tasks.");
        }
        registerNetworkCallback();
    }

    @Override
    public void onStop() {
        super.onStop();
        unregisterNetworkCallback();
    }

    private void observeViewModel() {
        viewModel.getTasksResult().observe(getViewLifecycleOwner(), result -> {
            if (!isAdded()) return;

            swipeRefreshLayout.setRefreshing(result instanceof Result.Loading);

            if (result instanceof Result.Success) {
                List<Task> tasks = ((Result.Success<List<Task>>) result).data;
                List<Task> filteredTasks = filterTasks(tasks);
                adapter.updateTasks(filteredTasks);
                updateEmptyView(filteredTasks.isEmpty());
            } else if (result instanceof Result.Error) {
                Exception e = ((Result.Error<List<Task>>) result).exception;
                Log.e(getClass().getSimpleName(), "Error loading tasks", e);
                Toast.makeText(getContext(), "Error loading tasks.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // OFFLINE SUPPORT: Listen for network changes to trigger a sync
    private void registerNetworkCallback() {
        ConnectivityManager cm = (ConnectivityManager) requireContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkRequest request = new NetworkRequest.Builder().build();
        networkCallback = new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(@NonNull Network network) {
                super.onAvailable(network);
                // This is called on a background thread, so post to the main thread
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        Log.d(getClass().getSimpleName(), "Network available. Triggering sync.");
                        viewModel.syncLocalTasks(requireContext());
                    });
                }
            }
        };
        cm.registerNetworkCallback(request, networkCallback);
    }

    private void unregisterNetworkCallback() {
        if (networkCallback != null) {
            ConnectivityManager cm = (ConnectivityManager) requireContext().getSystemService(Context.CONNECTIVITY_SERVICE);
            cm.unregisterNetworkCallback(networkCallback);
            networkCallback = null;
        }
    }


    private void updateEmptyView(boolean isEmpty) {
        emptyView.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        recyclerView.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
    }

    protected abstract List<Task> filterTasks(List<Task> tasks);
}