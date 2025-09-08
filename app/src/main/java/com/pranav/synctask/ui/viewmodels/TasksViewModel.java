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

    private final MutableLiveData<Result<User>> userResult = new MutableLiveData<>();

    public TasksViewModel() {
        this.taskRepository = TaskRepository.getInstance();
        this.userRepository = UserRepository.getInstance();
    }

    public LiveData<Result<List<Task>>> getTasksResult() {
        return taskRepository.getTasks();
    }

    public LiveData<Result<User>> getUserResult() {
        return userResult;
    }

    public void loadTasks(String userUID) {
        taskRepository.attachTasksListener(userUID);
    }

    // OFFLINE SUPPORT: Method to trigger a sync
    public void syncLocalTasks(Context context) {
        taskRepository.syncLocalTasks(context);
    }

    public void attachUserListener(String userUID) {
        userRepository.addUserListener(userUID, userResult);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        taskRepository.removeTasksListener();
        userRepository.removeUserListener();
    }
}