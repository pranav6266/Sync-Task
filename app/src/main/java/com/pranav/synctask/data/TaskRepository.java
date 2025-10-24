package com.pranav.synctask.data;

import android.content.Context;
import android.util.Log;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.google.firebase.firestore.ListenerRegistration;
import com.pranav.synctask.models.Task;
import com.pranav.synctask.models.User;
import com.pranav.synctask.utils.FirebaseHelper;
import com.pranav.synctask.utils.NetworkUtils;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

public class TaskRepository {
    private static volatile TaskRepository instance;
    private ListenerRegistration tasksListenerRegistration;
    private final List<Task> localTasks = new CopyOnWriteArrayList<>();
    private List<Task> firestoreTasks = new ArrayList<>();
    private final MutableLiveData<Result<List<Task>>> combinedTasksResult = new MutableLiveData<>();
    private final FirebaseHelper firebaseHelper;
    private String currentSpaceId; // ADDED

    private TaskRepository() {
        firebaseHelper = new FirebaseHelper();
    }

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

    public LiveData<Result<List<Task>>> getTasks() {
        return combinedTasksResult;
    }

    public Map<String, Integer> getTaskStats() {
        Map<String, Integer> stats = new HashMap<>();
        int completedCount = 0;
        List<Task> allTasks = new ArrayList<>(firestoreTasks);
        allTasks.addAll(localTasks);
        for (Task task : allTasks) {
            if (Task.STATUS_COMPLETED.equals(task.getStatus())) {
                completedCount++;
            }
        }
        stats.put("total", allTasks.size());
        stats.put("completed", completedCount);
        return stats;
    }

    public void attachTasksListener(String spaceId) {
        // If the space ID is new, clear old data
        if (!spaceId.equals(currentSpaceId)) {
            firestoreTasks.clear();
            localTasks.clear(); // Or handle local tasks differently
            currentSpaceId = spaceId;
        }

        if (tasksListenerRegistration != null) {
            tasksListenerRegistration.remove();
        }

        combinedTasksResult.setValue(new Result.Loading<>());
        tasksListenerRegistration = firebaseHelper.getTasks(spaceId, new FirebaseHelper.TasksCallback() {
            @Override
            public void onSuccess(List<Task> tasks) {
                firestoreTasks = tasks;
                mergeAndNotify();
            }

            @Override
            public void onError(Exception e) {
                combinedTasksResult.setValue(new Result.Error<>(e));
            }
        });
    }

    public void refreshTasks() {
        if (currentSpaceId != null) {
            attachTasksListener(currentSpaceId);
        }
    }

    public LiveData<Result<Void>> updateTask(Task task) {
        MutableLiveData<Result<Void>> result = new MutableLiveData<>();
        if (task.getId() == null || task.getId().isEmpty()) {
            for (int i = 0; i < localTasks.size(); i++) {
                if (localTasks.get(i).getLocalId().equals(task.getLocalId())) {
                    localTasks.set(i, task);
                    break;
                }
            }
            mergeAndNotify();
            result.setValue(new Result.Success<>(null));
        } else {
            result.setValue(new Result.Loading<>());
            firebaseHelper.updateTask(task.getId(), task.toMap(), new FirebaseHelper.TasksCallback() {
                @Override
                public void onSuccess(List<Task> tasks) {
                    result.setValue(new Result.Success<>(null));
                }

                @Override
                public void onError(Exception e) {
                    result.setValue(new Result.Error<>(e));
                }
            });
        }
        return result;
    }

    private void mergeAndNotify() {
        List<Task> mergedList = new ArrayList<>(firestoreTasks);
        for (Task localTask : localTasks) {
            // Only add local tasks for the current space
            if (currentSpaceId.equals(localTask.getSpaceId())) {
                boolean existsInFirestore = false;
                for (Task firestoreTask : firestoreTasks) {
                    if (localTask.getLocalId().equals(firestoreTask.getLocalId())) {
                        existsInFirestore = true;
                        break;
                    }
                }
                if (!existsInFirestore) {
                    mergedList.add(localTask);
                }
            }
        }
        mergedList.sort((o1, o2) -> {
            int priorityCompare = getPriorityValue(o2.getPriority()) - getPriorityValue(o1.getPriority());
            if (priorityCompare == 0) {
                if (o1.getCreatedAt() == null || o2.getCreatedAt() == null) return 0;
                return o2.getCreatedAt().compareTo(o1.getCreatedAt());
            }
            return priorityCompare;
        });
        combinedTasksResult.postValue(new Result.Success<>(mergedList));
    }

    // Task object must have spaceId set before calling this
    public void createTask(Task task, Context context) {
        boolean isOnline = NetworkUtils.isNetworkAvailable(context);
        if (isOnline) {
            task.setSynced(true);
            firebaseHelper.createTask(task, new FirebaseHelper.TasksCallback() {
                @Override
                public void onSuccess(List<Task> tasks) {
                }

                @Override
                public void onError(Exception e) {
                    Log.e("TaskRepository", "Failed to create online task, saving locally.", e);
                    createLocalTask(task);
                }
            });
        } else {
            createLocalTask(task);
        }
    }

    private void createLocalTask(Task task) {
        task.setSynced(false);
        localTasks.add(task);
        mergeAndNotify();
    }

    public void syncLocalTasks(Context context) {
        boolean isOnline = NetworkUtils.isNetworkAvailable(context);
        if (!isOnline || localTasks.isEmpty()) {
            return;
        }

        Log.d("TaskRepository", "Starting sync for " + localTasks.size() + " local tasks.");
        for (Task localTask : localTasks) {
            if (!localTask.isSynced()) {
                // Only sync if the local task belongs to the currently viewed space
                // Or, sync all, which is better
                firebaseHelper.createTask(localTask, new FirebaseHelper.TasksCallback() {
                    @Override
                    public void onSuccess(List<Task> tasks) {
                        localTasks.remove(localTask);
                        mergeAndNotify();
                    }

                    @Override
                    public void onError(Exception e) {
                        Log.e("TaskRepository", "Sync failed for task: " + localTask.getTitle(), e);
                    }
                });
            }
        }
    }

    public void removeTasksListener() {
        if (tasksListenerRegistration != null) {
            tasksListenerRegistration.remove();
            tasksListenerRegistration = null;
        }
    }

    private int getPriorityValue(String priority) {
        if (priority == null) return 1;
        switch (priority) {
            case "High":
                return 2;
            case "Low":
                return 0;
            default:
                return 1;
        }
    }

    public void updateTaskStatus(String taskId, String newStatus) {
        firebaseHelper.updateTaskStatus(taskId, newStatus);
    }

    public void deleteTask(String taskId) {
        firebaseHelper.deleteTask(taskId);
    }
}