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

    public void refreshTasks() {
        taskRepository.refreshTasks();
    }

    public void syncLocalTasks(Context context) {
        taskRepository.syncLocalTasks(context);
    }

    // --- ADDED ---
    // Used for the "Undo" delete feature
    public void createTask(Task task, Context context) {
        taskRepository.createTask(task, context);
    }
    // --- END ADDED ---

    // --- ADDED IN PHASE 4A ---
    public void deleteTask(String taskId) {
        taskRepository.deleteTask(taskId);
    }
    // --- END ADDED ---

    @Override
    protected void onCleared() {
        super.onCleared();
        taskRepository.removeTasksListListener(); // MODIFIED to call renamed method
    }
}