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
    private ListenerRegistration tasksListListenerRegistration;
    private ListenerRegistration taskListenerRegistration;
    private final List<Task> localTasks = new CopyOnWriteArrayList<>();
    private List<Task> firestoreTasks = new ArrayList<>();
    private final MutableLiveData<Result<List<Task>>> combinedTasksResult = new MutableLiveData<>();
    private final MutableLiveData<Result<Task>> singleTaskResult = new MutableLiveData<>();
    private final FirebaseHelper firebaseHelper;
    private String currentSpaceId;
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

    // --- Task List Methods ---

    public LiveData<Result<List<Task>>> getTasks() {
        return combinedTasksResult;
    }

    public void attachTasksListener(String spaceId) {
        if (!spaceId.equals(currentSpaceId)) {
            firestoreTasks.clear();
            localTasks.clear();
            currentSpaceId = spaceId;
        }

        if (tasksListListenerRegistration != null) {
            tasksListListenerRegistration.remove();
        }

        combinedTasksResult.setValue(new Result.Loading<>());
        tasksListListenerRegistration = firebaseHelper.getTasks(spaceId, new FirebaseHelper.TasksCallback() {
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

    public void removeTasksListListener() {
        if (tasksListListenerRegistration != null) {
            tasksListListenerRegistration.remove();
            tasksListListenerRegistration = null;
        }
    }

    // --- Single Task Methods ---

    public LiveData<Result<Task>> getTaskById() {
        return singleTaskResult;
    }

    public void attachTaskListener(String taskId) {
        if (taskListenerRegistration != null) {
            taskListenerRegistration.remove();
        }
        singleTaskResult.setValue(new Result.Loading<>());
        taskListenerRegistration = firebaseHelper.getTaskById(taskId, new FirebaseHelper.TaskCallback() {
            @Override
            public void onSuccess(Task task) {
                singleTaskResult.setValue(new Result.Success<>(task));
            }

            @Override
            public void onError(Exception e) {
                singleTaskResult.setValue(new Result.Error<>(e));
            }
        });
    }

    public void removeTaskListener() {
        if (taskListenerRegistration != null) {
            taskListenerRegistration.remove();
            taskListenerRegistration = null;
        }
    }

    // --- Common Task Methods ---

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

    public LiveData<Result<Void>> updateTask(Task task) {
        MutableLiveData<Result<Void>> result = new MutableLiveData<>();
        if (task.getId() == null || task.getId().isEmpty()) {
            // Update local task
            for (int i = 0; i < localTasks.size(); i++) {
                if (localTasks.get(i).getLocalId().equals(task.getLocalId())) {
                    localTasks.set(i, task);
                    break;
                }
            }
            mergeAndNotify();
            result.setValue(new Result.Success<>(null)); // Assume local update succeeds
        } else {
            // Update Firestore task
            result.setValue(new Result.Loading<>());
            // Prepare map, which now includes effort
            Map<String, Object> taskMap = task.toMap();
            firebaseHelper.updateTask(task.getId(), taskMap, new FirebaseHelper.TasksCallback() { // Use generic callback for simplicity
                @Override
                public void onSuccess(List<Task> tasks) { // Parameter ignored
                    result.setValue(new Result.Success<>(null));
                }

                @Override
                public void onError(Exception e) {
                    Log.e("TaskRepository", "Error updating task in Firestore", e);
                    result.setValue(new Result.Error<>(e));
                }
            });
        }
        return result;
    }


    private void mergeAndNotify() {
        List<Task> mergedList = new ArrayList<>(firestoreTasks);
        for (Task localTask : localTasks) {
            if (currentSpaceId != null && currentSpaceId.equals(localTask.getSpaceId())) {
                boolean existsInFirestore = false;
                for (Task firestoreTask : firestoreTasks) {
                    // Check if a task with the same localId already exists from Firestore
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
        // Sort tasks: High > Normal > Low, then by creation date descending
        mergedList.sort((o1, o2) -> {
            int priorityCompare = getPriorityValue(o2.getPriority()) - getPriorityValue(o1.getPriority());
            if (priorityCompare == 0) {
                // If priorities are the same, sort by creation date (newest first)
                if (o1.getCreatedAt() == null || o2.getCreatedAt() == null) return 0;
                return o2.getCreatedAt().compareTo(o1.getCreatedAt());
            }
            return priorityCompare; // Otherwise, sort by priority
        });
        combinedTasksResult.postValue(new Result.Success<>(mergedList)); // Use postValue for thread safety
    }

    // Task object must have spaceId set before calling this
    public void createTask(Task task, Context context) {
        boolean isOnline = NetworkUtils.isNetworkAvailable(context);
        if (isOnline) {
            task.setSynced(true);
            // Mark as synced assuming online creation succeeds initially
            firebaseHelper.createTask(task, new FirebaseHelper.TasksCallback() {
                @Override
                public void onSuccess(List<Task> tasks) {
                    // Listener will automatically update the list, nothing needed here
                    Log.d("TaskRepository", "Task created online successfully.");
                }

                @Override
                public void onError(Exception e) {
                    // If online creation fails despite network check, save locally
                    Log.e("TaskRepository", "Failed to create online task, saving locally.", e);
                    createLocalTask(task);
                }
            });
        } else {
            // No network, save locally directly
            createLocalTask(task);
        }
    }

    private void createLocalTask(Task task) {
        task.setSynced(false);
        // Ensure it's marked as not synced
        localTasks.add(task);
        mergeAndNotify();
        // Update the LiveData with the new local task
    }

    // Sync local tasks when network becomes available
    public void syncLocalTasks(Context context) {
        boolean isOnline = NetworkUtils.isNetworkAvailable(context);
        if (!isOnline || localTasks.isEmpty()) {
            return;
            // No network or nothing to sync
        }

        Log.d("TaskRepository", "Starting sync for " + localTasks.size() + " local tasks.");
        // Iterate over a copy to avoid ConcurrentModificationException if removing items
        List<Task> tasksToSync = new ArrayList<>(localTasks);
        for (Task localTask : tasksToSync) {
            if (!localTask.isSynced()) {
                // Attempt to create the task in Firestore
                firebaseHelper.createTask(localTask, new FirebaseHelper.TasksCallback() {
                    @Override
                    public void onSuccess(List<Task> tasks) {
                        // Task successfully synced, remove from local list
                        localTasks.remove(localTask);
                        // No need to call mergeAndNotify here, Firestore listener will update
                        Log.d("TaskRepository", "Successfully synced local task: " + localTask.getTitle());
                    }

                    @Override
                    public void onError(Exception e) {
                        // Sync failed, keep the task in the local list for next attempt
                        Log.e("TaskRepository", "Sync failed for task: " + localTask.getTitle(), e);
                    }
                });
            }
        }
    }

    // Helper to get numerical priority value for sorting
    private int getPriorityValue(String priority) {
        if (priority == null) return 1;
        // Default to Normal
        switch (priority) {
            case "High":
                return 2;
            case "Low":
                return 0;
            default: // Normal
                return 1;
        }
    }

    public void updateTaskStatus(String taskId, String newStatus) {
        firebaseHelper.updateTaskStatus(taskId, newStatus);
        // REMOVED logic to update progress IN PHASE 1
    }

    // REMOVED updateTaskProgress METHOD IN PHASE 1

    public void deleteTask(String taskId) {
        firebaseHelper.deleteTask(taskId);
        // Firestore listener will handle UI update
    }
}