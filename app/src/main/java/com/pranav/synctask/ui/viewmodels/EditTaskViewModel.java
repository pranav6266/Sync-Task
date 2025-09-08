package com.pranav.synctask.ui.viewmodels;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.pranav.synctask.data.Result;
import com.pranav.synctask.data.TaskRepository;
import com.pranav.synctask.models.Task;

public class EditTaskViewModel extends ViewModel {
    private final TaskRepository taskRepository;

    public EditTaskViewModel() {
        this.taskRepository = TaskRepository.getInstance();
    }

    public LiveData<Result<Void>> updateTask(Task task) {
        return taskRepository.updateTask(task);
    }
}