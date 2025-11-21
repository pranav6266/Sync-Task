package com.pranav.synctask.ui.viewmodels;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;
import com.pranav.synctask.data.Result;
import com.pranav.synctask.data.TaskRepository;
import com.pranav.synctask.data.UserRepository;
import com.pranav.synctask.models.Space;
import com.pranav.synctask.models.Task;

public class TaskDetailViewModel extends ViewModel {

    private final TaskRepository taskRepository;
    private final UserRepository userRepository;

    public TaskDetailViewModel() {
        this.taskRepository = TaskRepository.getInstance();
        this.userRepository = UserRepository.getInstance();
    }

    public LiveData<Result<Task>> getTask() {
        return taskRepository.getTaskById();
    }

    public void attachTaskListener(String taskId) {
        taskRepository.attachTaskListener(taskId);
    }

    public void removeTaskListener() {
        taskRepository.removeTaskListener();
    }

    public void updateTaskStatus(String taskId, String newStatus) {
        taskRepository.updateTaskStatus(taskId, newStatus);
    }

    public void deleteTask(String taskId) {
        taskRepository.deleteTask(taskId);
    }

    // --- New Subtask Methods ---

    public void toggleSubtaskLock(String taskId, String subtaskId, String userId, String userName, boolean forceUnlock) {
        taskRepository.toggleSubtaskLock(taskId, subtaskId, userId, userName, forceUnlock);
    }

    public void toggleSubtaskCompletion(String taskId, String subtaskId, boolean isCompleted, String userId) {
        taskRepository.toggleSubtaskCompletion(taskId, subtaskId, isCompleted, userId);
    }

    public LiveData<Result<Space>> getSpace(String spaceId) {
        return userRepository.getSpace(spaceId);
    }
}