package com.pranav.synctask.ui.viewmodels;

import android.content.Context;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.pranav.synctask.data.Result;
import com.pranav.synctask.data.TaskRepository;
import com.pranav.synctask.data.UserRepository;
import com.pranav.synctask.models.Task;
import com.pranav.synctask.models.User;
import java.util.List;

public class TasksViewModel extends ViewModel {
    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final MutableLiveData<String> searchQuery = new MutableLiveData<>("");

    public TasksViewModel() {
        this.taskRepository = TaskRepository.getInstance();
        this.userRepository = UserRepository.getInstance();
    }

    public LiveData<Result<List<Task>>> getTasksResult() {
        return taskRepository.getTasks();
    }

    public LiveData<String> getSearchQuery() {
        return searchQuery;
    }

    public void setSearchQuery(String query) {
        searchQuery.setValue(query);
    }

    public void loadTasks(String spaceId) {
        taskRepository.attachTasksListener(spaceId);
    }

    // --- NEW METHOD ---
    public void refreshTasks() {
        // This will tell the repository to re-trigger the listener
        // with the spaceId it already has.
        taskRepository.refreshTasks();
    }
    // --- END NEW METHOD ---

    public void syncLocalTasks(Context context) {
        taskRepository.syncLocalTasks(context);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        taskRepository.removeTasksListener();
    }
}