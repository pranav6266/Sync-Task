package com.pranav.synctask.fragments;

import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
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

public class SpacesTabFragment extends Fragment {

    private DashboardViewModel viewModel;
    private RecyclerView recyclerView;
    private SpacesAdapter adapter;
    private SwipeRefreshLayout swipeRefreshLayout;
    private TextView emptyView;

    // Cache
    private List<Space> currentSpaces = new ArrayList<>();
    private Map<String, User> currentMembers = new HashMap<>();
    private List<Task> allTasks = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_spaces_tab, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(requireActivity()).get(DashboardViewModel.class);

        recyclerView = view.findViewById(R.id.rv_spaces);
        swipeRefreshLayout = view.findViewById(R.id.swipe_refresh_spaces);
        emptyView = view.findViewById(R.id.tv_empty_spaces);
        ExtendedFloatingActionButton fab = view.findViewById(R.id.fab_manage_spaces);

        setupRecyclerView();

        fab.setOnClickListener(v -> showActionSelectionDialog());

        swipeRefreshLayout.setOnRefreshListener(() -> viewModel.refreshSpaces());

        observeData();
        observeActions();
    }

    private void setupRecyclerView() {
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new SpacesAdapter(getContext(), new ArrayList<>(), new HashMap<>(), new ArrayList<>());
        recyclerView.setAdapter(adapter);
    }

    private void observeData() {
        // 1. Shared Spaces
        viewModel.getSharedSpacesLiveData().observe(getViewLifecycleOwner(), result -> {
            swipeRefreshLayout.setRefreshing(result instanceof Result.Loading);
            if (result instanceof Result.Success) {
                currentSpaces = ((Result.Success<List<Space>>) result).data;
                updateAdapter();
            } else if (result instanceof Result.Error) {
                Toast.makeText(getContext(), "Error loading spaces", Toast.LENGTH_SHORT).show();
            }
        });

        // 2. Members Map
        viewModel.getMembersMap().observe(getViewLifecycleOwner(), map -> {
            currentMembers = map;
            updateAdapter();
        });

        // 3. Tasks (for progress)
        viewModel.getAllTasksResult().observe(getViewLifecycleOwner(), result -> {
            if (result instanceof Result.Success) {
                allTasks = ((Result.Success<List<Task>>) result).data;
                updateAdapter();
            }
        });
    }

    private void observeActions() {
        viewModel.getCreateSpaceResult().observe(getViewLifecycleOwner(), result -> {
            if (result instanceof Result.Success) {
                Toast.makeText(getContext(), "Space Created!", Toast.LENGTH_SHORT).show();
                // Auto refresh handled by Firestore listener
            } else if (result instanceof Result.Error) {
                Toast.makeText(getContext(), "Failed: " + ((Result.Error<Space>) result).exception.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });

        viewModel.getJoinSpaceResult().observe(getViewLifecycleOwner(), result -> {
            if (result instanceof Result.Success) {
                Toast.makeText(getContext(), "Joined successfully!", Toast.LENGTH_SHORT).show();
            } else if (result instanceof Result.Error) {
                Toast.makeText(getContext(), "Failed: " + ((Result.Error<Space>) result).exception.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateAdapter() {
        adapter.updateSpaces(currentSpaces, currentMembers, allTasks);
        if (currentSpaces.isEmpty()) {
            emptyView.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            emptyView.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }
    }

    // --- Dialogs ---

    private void showActionSelectionDialog() {
        CharSequence[] options = {"Create New Space", "Join a Space"};
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Manage Spaces")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) showCreateSpaceDialog();
                    else showJoinSpaceDialog();
                })
                .show();
    }

    private void showCreateSpaceDialog() {
        final EditText input = new EditText(getContext());
        input.setHint("e.g. Home, Office, Project X");
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);

        FrameLayout container = new FrameLayout(getContext());
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMargins(50, 20, 50, 20);
        container.addView(input, params);

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Create New Space")
                .setView(container)
                .setPositiveButton("Create", (dialog, which) -> {
                    String name = input.getText().toString().trim();
                    if (!name.isEmpty()) viewModel.createSpace(name);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showJoinSpaceDialog() {
        final EditText input = new EditText(getContext());
        input.setHint("Enter 6-character Invite Code");
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS);

        FrameLayout container = new FrameLayout(getContext());
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMargins(50, 20, 50, 20);
        container.addView(input, params);

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Join Space")
                .setView(container)
                .setPositiveButton("Join", (dialog, which) -> {
                    String code = input.getText().toString().trim();
                    if (code.length() >= 6) viewModel.joinSpace(code);
                    else Toast.makeText(getContext(), "Invalid Code", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}