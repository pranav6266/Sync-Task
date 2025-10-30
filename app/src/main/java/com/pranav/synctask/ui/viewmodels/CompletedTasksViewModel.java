package com.pranav.synctask.ui.viewmodels;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.pranav.synctask.data.Result;
import com.pranav.synctask.data.TaskRepository;
import com.pranav.synctask.models.Task;

import java.util.List;

public class CompletedTasksViewModel extends ViewModel {

    private final TaskRepository taskRepository;

    public CompletedTasksViewModel() {
        this.taskRepository = TaskRepository.getInstance();
    }

    public LiveData<Result<List<Task>>> getCompletedTasksResult() {
        return taskRepository.getCompletedTasks();
    }

    public void loadCompletedTasks(String spaceId) {
        taskRepository.attachCompletedTasksListener(spaceId);
    }

    public void restoreTask(Task task) {
        task.setStatus(Task.STATUS_PENDING);
        // We don't observe the result, the listener will just update the list
        taskRepository.updateTask(task);
    }

    public void deleteTask(String taskId) {
        taskRepository.deleteTask(taskId);
    }

    public void removeListeners() {
        taskRepository.removeCompletedTasksListener();
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        removeListeners();
    }
}