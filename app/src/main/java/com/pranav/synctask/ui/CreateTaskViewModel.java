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

    // The task object passed in already has the spaceId
    public LiveData<Result<Void>> createTask(Task task, Context context) {
        taskRepository.createTask(task, context);
        return new MutableLiveData<>(new Result.Success<>(null));
    }
}