package com.pranav.synctask.ui;

import android.content.Context;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
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

    // OFFLINE SUPPORT: Pass context to check network status
    public LiveData<Result<Void>> createTask(Task task, Context context) {
        // The LiveData returned here is mainly for showing errors,
        // since success is handled by the real-time listener.
        // For offline, we will handle it optimistically.
        taskRepository.createTask(task, context);
        // Return an empty LiveData or a simple success for navigation purposes
        return new MutableLiveData<>(new Result.Success<>(null));
    }
}