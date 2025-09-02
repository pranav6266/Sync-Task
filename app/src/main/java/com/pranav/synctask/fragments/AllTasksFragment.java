package com.pranav.synctask.fragments;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.ListenerRegistration;
import com.pranav.synctask.R;
import com.pranav.synctask.adapters.TaskAdapter;
import com.pranav.synctask.models.Task;
import com.pranav.synctask.utils.FirebaseHelper;
import java.util.ArrayList;
import java.util.List;

public class AllTasksFragment extends Fragment {
    private RecyclerView recyclerView;
    private TaskAdapter adapter;
    private List<Task> taskList = new ArrayList<>();
    private ListenerRegistration listenerRegistration;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_task_list, container, false);
        recyclerView = view.findViewById(R.id.recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new TaskAdapter(getContext(), taskList);
        recyclerView.setAdapter(adapter);
        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            String currentUserId = currentUser.getUid();
            listenerRegistration = FirebaseHelper.getTasks(currentUserId, new FirebaseHelper.TasksCallback() {
                @Override
                public void onSuccess(List<Task> tasks) {
                    adapter.updateTasks(tasks);
                }

                @Override
                public void onError(Exception e) {
                    Log.e("AllTasksFragment", "Error loading tasks", e);
                }
            });
        } else {
            Log.e("AllTasksFragment", "User is not authenticated, cannot load tasks.");
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        if (listenerRegistration != null) {
            listenerRegistration.remove();
        }
    }
}
