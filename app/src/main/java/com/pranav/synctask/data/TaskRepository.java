package com.pranav.synctask.data;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.google.firebase.firestore.ListenerRegistration;
import com.pranav.synctask.models.Task;
import com.pranav.synctask.utils.FirebaseHelper;
import java.util.List;

public class TaskRepository {
    private static volatile TaskRepository instance;
    private ListenerRegistration tasksListenerRegistration;

    private TaskRepository() {}

    public static TaskRepository getInstance() {
        if (instance == null) {
            synchronized (TaskRepository.class) {
                if (instance == null) {
                    instance = new TaskRepository();
                }
            }
        }
        return instance;
    }

    public void attachTasksListener(String userUID, MutableLiveData<Result<List<Task>>> tasksLiveData) {
        if (tasksListenerRegistration != null) {
            tasksListenerRegistration.remove();
        }
        tasksLiveData.setValue(new Result.Loading<>());
        tasksListenerRegistration = FirebaseHelper.getTasks(userUID, new FirebaseHelper.TasksCallback() {
            @Override
            public void onSuccess(List<Task> tasks) {
                tasksLiveData.setValue(new Result.Success<>(tasks));
            }

            @Override
            public void onError(Exception e) {
                tasksLiveData.setValue(new Result.Error<>(e));
            }
        });
    }

    public void removeTasksListener() {
        if (tasksListenerRegistration != null) {
            tasksListenerRegistration.remove();
            tasksListenerRegistration = null;
        }
    }

    public LiveData<Result<Void>> createTask(Task task) {
        MutableLiveData<Result<Void>> result = new MutableLiveData<>();
        result.setValue(new Result.Loading<>());
        FirebaseHelper.createTask(task, new FirebaseHelper.TasksCallback() {
            @Override
            public void onSuccess(List<Task> tasks) {
                // Success is handled by the listener picking up the new task
            }

            @Override
            public void onError(Exception e) {
                result.setValue(new Result.Error<>(e));
            }
        });
        // We can consider this a "success" immediately, as the listener will handle the update.
        result.setValue(new Result.Success<>(null));
        return result;
    }

    public void updateTaskStatus(String taskId, String newStatus) {
        FirebaseHelper.updateTaskStatus(taskId, newStatus);
    }

    public void deleteTask(String taskId) {
        FirebaseHelper.deleteTask(taskId);
    }
}