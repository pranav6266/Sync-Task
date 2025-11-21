package com.pranav.synctask.ui;

import android.content.Context;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.pranav.synctask.data.Result;
import com.pranav.synctask.data.TaskRepository;
import com.pranav.synctask.data.UserRepository;
import com.pranav.synctask.models.Space;
import com.pranav.synctask.models.Task;
import com.pranav.synctask.models.User;
import java.util.List;

public class CreateTaskViewModel extends ViewModel {
    private final TaskRepository taskRepository;
    private final UserRepository userRepository;

    public CreateTaskViewModel() {
        this.taskRepository = TaskRepository.getInstance();
        this.userRepository = UserRepository.getInstance();
    }

    public LiveData<Result<Void>> createTask(Task task, Context context) {
        taskRepository.createTask(task, context);
        return new MutableLiveData<>(new Result.Success<>(null));
    }

    public LiveData<Result<Space>> getSpace(String spaceId) {
        return userRepository.getSpace(spaceId);
    }

    public LiveData<Result<List<User>>> getSpaceMembers(List<String> uids) {
        return userRepository.getUsers(uids);
    }
}