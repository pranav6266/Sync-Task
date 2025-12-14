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
import java.util.Collections;
import java.util.List;

public class TasksViewModel extends ViewModel {
    private final TaskRepository taskRepository;
    private final UserRepository userRepository;

    // Two distinct live data streams for the two main tabs
    private final MutableLiveData<Result<List<Task>>> personalTasksResult = new MutableLiveData<>();
    private final MutableLiveData<Result<List<Task>>> partnerTasksResult = new MutableLiveData<>();

    public TasksViewModel() {
        this.taskRepository = TaskRepository.getInstance();
        this.userRepository = UserRepository.getInstance();
    }

    // --- PERSONAL TASKS (ME TAB) ---
    public LiveData<Result<List<Task>>> getPersonalTasks() {
        return personalTasksResult;
    }

    public void loadPersonalTasks(String uid) {
        personalTasksResult.setValue(new Result.Loading<>());
        userRepository.getUser(uid).observeForever(result -> {
            if (result instanceof Result.Success) {
                User user = ((Result.Success<User>) result).data;
                if (user.getPersonalSpaceId() != null) {
                    // Re-use the existing repository logic, but point it to our specific LiveData
                    taskRepository.attachTasksListener(user.getPersonalSpaceId(), personalTasksResult);
                } else {
                    // If no personal space exists (edge case), return empty
                    personalTasksResult.setValue(new Result.Success<>(Collections.emptyList()));
                }
            } else if (result instanceof Result.Error) {
                personalTasksResult.setValue(new Result.Error<>(((Result.Error<User>) result).exception));
            }
        });
    }

    // --- PARTNER TASKS (US TAB) ---
    public LiveData<Result<List<Task>>> getPartnerTasks() {
        return partnerTasksResult;
    }

    public void loadPartnerTasks(String uid) {
        partnerTasksResult.setValue(new Result.Loading<>());
        userRepository.getUser(uid).observeForever(result -> {
            if (result instanceof Result.Success) {
                User user = ((Result.Success<User>) result).data;
                if (user.getPartnerSpaceId() != null) {
                    taskRepository.attachTasksListener(user.getPartnerSpaceId(), partnerTasksResult);
                } else {
                    // No partner linked yet
                    partnerTasksResult.setValue(new Result.Success<>(Collections.emptyList()));
                }
            } else if (result instanceof Result.Error) {
                partnerTasksResult.setValue(new Result.Error<>(((Result.Error<User>) result).exception));
            }
        });
    }

    // --- ACTIONS ---
    public void createTask(Task task, Context context) {
        taskRepository.createTask(task, context);
    }

    public void updateTaskStatus(String taskId, String newStatus) {
        taskRepository.updateTaskStatus(taskId, newStatus);
    }

    public void deleteTask(String taskId) {
        taskRepository.deleteTask(taskId);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        taskRepository.removeTasksListListener();
    }
}