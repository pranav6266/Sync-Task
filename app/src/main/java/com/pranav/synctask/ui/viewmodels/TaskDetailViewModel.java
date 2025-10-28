package com.pranav.synctask.ui.viewmodels;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;
import com.pranav.synctask.data.Result;
import com.pranav.synctask.data.TaskRepository;
import com.pranav.synctask.models.Task;

public class TaskDetailViewModel extends ViewModel {

    private final TaskRepository taskRepository;

    public TaskDetailViewModel() {
        this.taskRepository = TaskRepository.getInstance();
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

    // NEW METHOD for Phase 3
    public void updateTaskProgress(String taskId, int newProgress) { //
        taskRepository.updateTaskProgress(taskId, newProgress); //
    } //

    public void deleteTask(String taskId) {
        taskRepository.deleteTask(taskId);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        removeTaskListener();
    }
}