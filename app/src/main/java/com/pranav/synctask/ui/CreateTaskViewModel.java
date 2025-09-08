package com.pranav.synctask.ui.viewmodels;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;
import com.pranav.synctask.data.Result;
import com.pranav.synctask.data.TaskRepository;
import com.pranav.synctask.data.UserRepository;
import com.pranav.synctask.models.Task;
import com.pranav.synctask.models.User;

public class CreateTaskViewModel extends ViewModel {
    private final TaskRepository taskRepository;
    private final UserRepository userRepository;

    public CreateTaskViewModel() {
        this.taskRepository = TaskRepository.getInstance();
        this.userRepository = UserRepository.getInstance();
    }

    public LiveData<Result<User>> getPartnerInfo(String uid) {
        return userRepository.getUser(uid);
    }

    public LiveData<Result<Void>> createTask(Task task) {
        return taskRepository.createTask(task);
    }
}