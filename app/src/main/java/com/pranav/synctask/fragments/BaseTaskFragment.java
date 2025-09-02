package com.pranav.synctask.fragments;

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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.ListenerRegistration;
import com.pranav.synctask.R;
import com.pranav.synctask.adapters.TaskAdapter;
import com.pranav.synctask.models.Task;
import com.pranav.synctask.utils.FirebaseHelper;

import java.util.ArrayList;
import java.util.List;

public abstract class BaseTaskFragment extends Fragment {

    protected RecyclerView recyclerView;
    protected TaskAdapter adapter;
    protected List<Task> taskList = new ArrayList<>();
    protected ListenerRegistration listenerRegistration;
    protected SwipeRefreshLayout swipeRefreshLayout;
    protected TextView emptyView;
    private String currentUserId;

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

        swipeRefreshLayout.setOnRefreshListener(this::attachListener);
        swipeRefreshLayout.setColorSchemeResources(R.color.primary_color, R.color.accent_color);

        return view;
    }

    private void setupRecyclerView() {
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new TaskAdapter(getContext(), taskList, currentUserId);
        recyclerView.setAdapter(adapter);
    }

    @Override
    public void onStart() {
        super.onStart();
        attachListener();
    }

    @Override
    public void onStop() {
        super.onStop();
        detachListener();
    }

    protected void attachListener() {
        if (currentUserId == null) {
            Log.e(getClass().getSimpleName(), "User is not authenticated, cannot load tasks.");
            return;
        }
        swipeRefreshLayout.setRefreshing(true);
        detachListener(); // Ensure only one listener is active

        listenerRegistration = FirebaseHelper.getTasks(currentUserId, new FirebaseHelper.TasksCallback() {
            @Override
            public void onSuccess(List<Task> tasks) {
                if (isAdded()) { // Check if fragment is still attached
                    List<Task> filteredTasks = filterTasks(tasks);
                    adapter.updateTasks(filteredTasks);
                    swipeRefreshLayout.setRefreshing(false);
                    updateEmptyView(filteredTasks.isEmpty());
                }
            }

            @Override
            public void onError(Exception e) {
                if(isAdded()) {
                    Log.e(getClass().getSimpleName(), "Error loading tasks", e);
                    Toast.makeText(getContext(), "Error loading tasks.", Toast.LENGTH_SHORT).show();
                    swipeRefreshLayout.setRefreshing(false);
                }
            }
        });
    }

    protected void detachListener() {
        if (listenerRegistration != null) {
            listenerRegistration.remove();
            listenerRegistration = null;
        }
    }

    private void updateEmptyView(boolean isEmpty) {
        if (isEmpty) {
            recyclerView.setVisibility(View.GONE);
            emptyView.setVisibility(View.VISIBLE);
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            emptyView.setVisibility(View.GONE);
        }
    }

    protected abstract List<Task> filterTasks(List<Task> tasks);
}