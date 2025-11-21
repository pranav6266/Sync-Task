package com.pranav.synctask.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.airbnb.lottie.LottieAnimationView;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.pranav.synctask.R;
import com.pranav.synctask.activities.CreateTaskActivity;
import com.pranav.synctask.activities.TaskDetailActivity;
import com.pranav.synctask.adapters.TaskAdapter;
import com.pranav.synctask.data.Result;
import com.pranav.synctask.models.Space;
import com.pranav.synctask.models.Task;
import com.pranav.synctask.ui.viewmodels.TasksViewModel;

import java.util.ArrayList;
import java.util.List;

public class PersonalTabFragment extends Fragment implements TaskAdapter.OnTaskActionListener {

    private TasksViewModel viewModel;
    private RecyclerView recyclerView;
    private TaskAdapter adapter;
    private SwipeRefreshLayout swipeRefreshLayout;
    private LottieAnimationView emptyView;
    private String currentUserId;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_personal_tab, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        currentUserId = FirebaseAuth.getInstance().getUid();
        viewModel = new ViewModelProvider(this).get(TasksViewModel.class);

        recyclerView = view.findViewById(R.id.rv_personal_tasks);
        swipeRefreshLayout = view.findViewById(R.id.swipe_refresh_personal);
        emptyView = view.findViewById(R.id.empty_view_personal);
        ExtendedFloatingActionButton fab = view.findViewById(R.id.fab_add_personal_task);

        setupRecyclerView();

        fab.setOnClickListener(v -> {
            Intent intent = new Intent(getContext(), CreateTaskActivity.class);
            // IMPORTANT: For personal tasks, we pass the User ID as the SPACE_ID
            intent.putExtra("SPACE_ID", currentUserId);
            intent.putExtra("CONTEXT_TYPE", Space.TYPE_PERSONAL);
            startActivity(intent);
        });

        swipeRefreshLayout.setOnRefreshListener(() -> viewModel.refreshTasks());

        // Load tasks using User UID as the Space ID
        if (currentUserId != null) {
            viewModel.loadTasks(currentUserId);
        }

        observeViewModel();
    }

    private void setupRecyclerView() {
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        // Context is PERSONAL
        adapter = new TaskAdapter(Space.TYPE_PERSONAL, getContext(), new ArrayList<>(), currentUserId, this);
        recyclerView.setAdapter(adapter);
    }

    private void observeViewModel() {
        viewModel.getTasksResult().observe(getViewLifecycleOwner(), result -> {
            if (result instanceof Result.Loading) {
                swipeRefreshLayout.setRefreshing(true);
            } else if (result instanceof Result.Success) {
                swipeRefreshLayout.setRefreshing(false);
                List<Task> tasks = ((Result.Success<List<Task>>) result).data;
                adapter.updateTasks(tasks);
                updateEmptyView(tasks.isEmpty());
            } else if (result instanceof Result.Error) {
                swipeRefreshLayout.setRefreshing(false);
                Toast.makeText(getContext(), "Error loading tasks", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateEmptyView(boolean isEmpty) {
        if (isEmpty) {
            emptyView.setVisibility(View.VISIBLE);
            emptyView.playAnimation();
        } else {
            emptyView.setVisibility(View.GONE);
            emptyView.cancelAnimation();
        }
    }

    @Override
    public void onTaskClick(Task task) {
        Intent intent = new Intent(getContext(), TaskDetailActivity.class);
        intent.putExtra(TaskDetailActivity.EXTRA_TASK_ID, task.getId());
        startActivity(intent);
    }

    @Override
    public void onTaskLongClick(Task task, View view) {
        // Optional: Implement delete/edit menu here
    }
}