package com.pranav.synctask.ui.viewmodels;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;
import com.pranav.synctask.data.Result;
import com.pranav.synctask.data.TaskRepository;
import com.pranav.synctask.data.UserRepository; // Added
import com.pranav.synctask.models.Space; // Added
import com.pranav.synctask.models.Task;
import com.pranav.synctask.models.User; // Added
import java.util.List; // Added

public class EditTaskViewModel extends ViewModel {
    private final TaskRepository taskRepository;
    private final UserRepository userRepository; // Added

    public EditTaskViewModel() {
        this.taskRepository = TaskRepository.getInstance();
        this.userRepository = UserRepository.getInstance(); // Added
    }

    public LiveData<Result<Void>> updateTask(Task task) {
        return taskRepository.updateTask(task);
    }

    // Added Methods for Admin Logic
    public LiveData<Result<Space>> getSpace(String spaceId) {
        return userRepository.getSpace(spaceId);
    }

    public LiveData<Result<List<User>>> getSpaceMembers(List<String> uids) {
        return userRepository.getUsers(uids);
    }
}