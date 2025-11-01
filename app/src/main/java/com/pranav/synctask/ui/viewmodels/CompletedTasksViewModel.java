package com.pranav.synctask.ui.viewmodels;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.pranav.synctask.data.Result;
import com.pranav.synctask.data.TaskRepository;
import com.pranav.synctask.data.UserRepository; // ADDED
import com.pranav.synctask.models.Task;
import com.pranav.synctask.models.User; // ADDED

import java.util.List;
public class CompletedTasksViewModel extends ViewModel {

    private final TaskRepository taskRepository;
    private final UserRepository userRepository; // ADDED
    private final String currentUserId; // ADDED
    private static final String TAG = "CompletedTasksVM";

    public CompletedTasksViewModel() {
        this.taskRepository = TaskRepository.getInstance();
        this.userRepository = UserRepository.getInstance(); // ADDED
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        this.currentUserId = (user != null) ? user.getUid() : null; // ADDED
    }

    public LiveData<Result<List<Task>>> getCompletedTasksResult() {
        return taskRepository.getCompletedTasks();
    }

    public void loadCompletedTasks(String spaceId) {
        // MODIFIED
        if (spaceId != null) {
            // Case 1: Filtered archive for a specific space
            taskRepository.attachCompletedTasksListener(spaceId);
        } else if (currentUserId != null) {
            // Case 2: Unfiltered archive (from Settings). Get all user's space IDs first.
            userRepository.getUser(currentUserId).observeForever(userResult -> {
                if (userResult instanceof Result.Success) {
                    List<String> spaceIds = ((Result.Success<User>) userResult).data.getSpaceIds();
                    if (spaceIds != null && !spaceIds.isEmpty()) {
                        taskRepository.attachCompletedTasksListenerForSpaces(spaceIds);
                    } else {
                        Log.w(TAG, "User has no spaceIds, cannot load all completed tasks.");
                        // Repository will return an empty list
                    }
                } else if (userResult instanceof Result.Error) {
                    Log.e(TAG, "Could not get user to load all completed tasks", ((Result.Error<User>) userResult).exception);
                }
            });
        } else {
            Log.e(TAG, "Cannot load completed tasks: user is null.");
        }
    }

    public void restoreTask(Task task) {
        task.setStatus(Task.STATUS_PENDING);
        // We don't observe the result, the listener will just update the list
        taskRepository.updateTask(task);
    }

    public void deleteTask(String taskId) {
        taskRepository.deleteTask(taskId);
    }

    public void removeListeners() {
        taskRepository.removeCompletedTasksListener();
        taskRepository.removeCompletedTasksForSpacesListener(); // ADDED
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        removeListeners();
    }
}