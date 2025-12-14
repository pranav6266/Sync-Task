package com.pranav.synctask.ui.viewmodels;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.google.firebase.auth.FirebaseAuth;
import com.pranav.synctask.data.Result;
import com.pranav.synctask.data.TaskRepository;
import com.pranav.synctask.data.UserRepository;
import com.pranav.synctask.models.Space;
import com.pranav.synctask.models.Task;
import com.pranav.synctask.models.User;
import java.util.List;

public class GroupDetailViewModel extends ViewModel {
    private final TaskRepository taskRepository;
    private final UserRepository userRepository;

    // Data Streams
    private final MutableLiveData<Result<Space>> spaceResult = new MutableLiveData<>();
    private final MutableLiveData<Result<List<User>>> membersResult = new MutableLiveData<>();
    private final MutableLiveData<Result<List<Task>>> tasksResult = new MutableLiveData<>();

    // Action Results
    private final MutableLiveData<Result<Void>> leaveSpaceResult = new MutableLiveData<>();

    public GroupDetailViewModel() {
        this.taskRepository = TaskRepository.getInstance();
        this.userRepository = UserRepository.getInstance();
    }

    public void loadSpaceData(String spaceId) {
        // 1. Load Space Details
        spaceResult.setValue(new Result.Loading<>());
        userRepository.getSpace(spaceId).observeForever(result -> {
            spaceResult.setValue(result);

            if (result instanceof Result.Success) {
                Space space = ((Result.Success<Space>) result).data;

                // 2. Load Members
                if (space.getMembers() != null && !space.getMembers().isEmpty()) {
                    loadMembers(space.getMembers());
                }

                // 3. Load Tasks (Real-time)
                taskRepository.attachTasksListener(spaceId, tasksResult);
            }
        });
    }

    private void loadMembers(List<String> uids) {
        userRepository.getUsers(uids).observeForever(membersResult::setValue);
    }

    public void createTask(Task task) {
        taskRepository.createTask(task, null);
    }

    public void updateTaskStatus(String taskId, boolean isCompleted) {
        String status = isCompleted ? Task.STATUS_COMPLETED : Task.STATUS_PENDING;
        taskRepository.updateTaskStatus(taskId, status);
    }

    public void leaveGroup(String spaceId) {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid != null) {
            leaveSpaceResult.setValue(new Result.Loading<>());
            userRepository.leaveSpace(spaceId, uid).observeForever(leaveSpaceResult::setValue);
        }
    }

    // Getters
    public LiveData<Result<Space>> getSpace() { return spaceResult; }
    public LiveData<Result<List<User>>> getMembers() { return membersResult; }
    public LiveData<Result<List<Task>>> getTasks() { return tasksResult; }
    public LiveData<Result<Void>> getLeaveSpaceResult() { return leaveSpaceResult; }

    @Override
    protected void onCleared() {
        super.onCleared();
        taskRepository.removeTasksListListener();
    }
}