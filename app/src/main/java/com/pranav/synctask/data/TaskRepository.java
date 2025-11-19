package com.pranav.synctask.data;

import android.content.Context;
import android.util.Log;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.google.firebase.firestore.ListenerRegistration;
import com.pranav.synctask.models.Task;
import com.pranav.synctask.utils.FirebaseHelper;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TaskRepository {
    private static volatile TaskRepository instance;
    private ListenerRegistration tasksListListenerRegistration;
    private ListenerRegistration taskListenerRegistration;
    private ListenerRegistration completedTasksListener;
    private ListenerRegistration allTasksListener;
    private ListenerRegistration completedTasksForSpacesListener;

    private List<Task> firestoreTasks = new ArrayList<>();

    // LiveData Sources
    private final MutableLiveData<Result<List<Task>>> combinedTasksResult = new MutableLiveData<>();
    private final MutableLiveData<Result<Task>> singleTaskResult = new MutableLiveData<>();
    private final MutableLiveData<Result<List<Task>>> completedTasksResult = new MutableLiveData<>();
    private final MutableLiveData<Result<List<Task>>> allTasksResult = new MutableLiveData<>();

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
        if (spaceId == null) return;

        if (!spaceId.equals(currentSpaceId)) {
            firestoreTasks.clear();
            currentSpaceId = spaceId;
        }

        if (tasksListListenerRegistration != null) {
            tasksListListenerRegistration.remove();
        }

        combinedTasksResult.setValue(new Result.Loading<>());

        // Firestore automatically handles offline caching.
        // We don't need manual local lists.
        tasksListListenerRegistration = firebaseHelper.getTasks(spaceId, new FirebaseHelper.TasksCallback() {
            @Override
            public void onSuccess(List<Task> tasks) {
                firestoreTasks = tasks;
                // Sort: High > Normal > Low
// SMART SORTING LOGIC
                firestoreTasks.sort((t1, t2) -> {
                    // 1. Check if tasks are completed (Completed always goes to bottom if mixed)
                    boolean c1 = "completed".equals(t1.getStatus());
                    boolean c2 = "completed".equals(t2.getStatus());
                    if (c1 != c2) return c1 ? 1 : -1;

                    // 2. Sort by Due Date (Null dates go to the bottom)
                    if (t1.getDueDate() != null && t2.getDueDate() != null) {
                        // Both have dates: Compare them (Earliest first)
                        int dateCompare = t1.getDueDate().compareTo(t2.getDueDate());
                        if (dateCompare != 0) return dateCompare;
                    } else if (t1.getDueDate() != null) {
                        return -1; // t1 has date, t2 doesn't -> t1 comes first
                    } else if (t2.getDueDate() != null) {
                        return 1;  // t2 has date, t1 doesn't -> t2 comes first
                    }

                    // 3. If dates are equal (or both null), Sort by Priority (High > Normal > Low)
                    return getPriorityValue(t2.getPriority()) - getPriorityValue(t1.getPriority());
                });

                combinedTasksResult.setValue(new Result.Success<>(firestoreTasks));                combinedTasksResult.setValue(new Result.Success<>(tasks));
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

    // --- Task Actions ---

    // Simplified: No context needed, no connectivity check needed.
    public void createTask(Task task, Context context) {
        // Just fire and forget. Firestore syncs when possible.
        firebaseHelper.createTask(task, new FirebaseHelper.TasksCallback() {
            @Override
            public void onSuccess(List<Task> tasks) {
                Log.d("TaskRepository", "Task created successfully.");
            }

            @Override
            public void onError(Exception e) {
                Log.e("TaskRepository", "Error creating task", e);
            }
        });
    }

    public LiveData<Result<Void>> updateTask(Task task) {
        MutableLiveData<Result<Void>> result = new MutableLiveData<>();
        result.setValue(new Result.Loading<>());

        Map<String, Object> taskMap = task.toMap();
        firebaseHelper.updateTask(task.getId(), taskMap, new FirebaseHelper.TasksCallback() {
            @Override
            public void onSuccess(List<Task> tasks) {
                result.setValue(new Result.Success<>(null));
            }
            @Override
            public void onError(Exception e) {
                result.setValue(new Result.Error<>(e));
            }
        });
        return result;
    }

    public void updateTaskStatus(String taskId, String newStatus) {
        firebaseHelper.updateTaskStatus(taskId, newStatus);
    }

    public void deleteTask(String taskId) {
        firebaseHelper.deleteTask(taskId);
    }

    // --- Helper Methods ---

    private int getPriorityValue(String priority) {
        if (priority == null) return 1;
        switch (priority) {
            case "High": return 2;
            case "Low": return 0;
            default: return 1;
        }
    }

    // --- Completed / All Tasks Listeners (Kept same as before) ---

    public LiveData<Result<List<Task>>> getCompletedTasks() { return completedTasksResult; }

    public void attachCompletedTasksListener(String spaceId) {
        if (completedTasksListener != null) completedTasksListener.remove();
        completedTasksResult.setValue(new Result.Loading<>());
        completedTasksListener = firebaseHelper.getCompletedTasks(spaceId, new FirebaseHelper.TasksCallback() {
            @Override public void onSuccess(List<Task> tasks) { completedTasksResult.setValue(new Result.Success<>(tasks)); }
            @Override public void onError(Exception e) { completedTasksResult.setValue(new Result.Error<>(e)); }
        });
    }

    public void attachCompletedTasksListenerForSpaces(List<String> spaceIds) {
        if (completedTasksForSpacesListener != null) completedTasksForSpacesListener.remove();
        completedTasksResult.setValue(new Result.Loading<>());
        completedTasksForSpacesListener = firebaseHelper.getCompletedTasksForSpaces(spaceIds, new FirebaseHelper.TasksCallback() {
            @Override public void onSuccess(List<Task> tasks) { completedTasksResult.setValue(new Result.Success<>(tasks)); }
            @Override public void onError(Exception e) { completedTasksResult.setValue(new Result.Error<>(e)); }
        });
    }

    public void removeCompletedTasksListener() {
        if (completedTasksListener != null) { completedTasksListener.remove(); completedTasksListener = null; }
    }
    public void removeCompletedTasksForSpacesListener() {
        if (completedTasksForSpacesListener != null) { completedTasksForSpacesListener.remove(); completedTasksForSpacesListener = null; }
    }

    public LiveData<Result<List<Task>>> getAllTasksResult() { return allTasksResult; }

    public void attachAllTasksListener(List<String> spaceIds) {
        if (allTasksListener != null) allTasksListener.remove();
        allTasksResult.setValue(new Result.Loading<>());
        allTasksListener = firebaseHelper.getAllTasksForSpaces(spaceIds, new FirebaseHelper.TasksCallback() {
            @Override public void onSuccess(List<Task> tasks) { allTasksResult.setValue(new Result.Success<>(tasks)); }
            @Override public void onError(Exception e) { allTasksResult.setValue(new Result.Error<>(e)); }
        });
    }

    public static void removeAllTasksListener() {
        if (instance != null && instance.allTasksListener != null) {
            instance.allTasksListener.remove();
            instance.allTasksListener = null;
        }
    }

    public LiveData<Result<Task>> getTaskById() { return singleTaskResult; }

    public void attachTaskListener(String taskId) {
        if (taskListenerRegistration != null) taskListenerRegistration.remove();
        singleTaskResult.setValue(new Result.Loading<>());
        taskListenerRegistration = firebaseHelper.getTaskById(taskId, new FirebaseHelper.TaskCallback() {
            @Override public void onSuccess(Task task) { singleTaskResult.setValue(new Result.Success<>(task)); }
            @Override public void onError(Exception e) { singleTaskResult.setValue(new Result.Error<>(e)); }
        });
    }

    public void removeTaskListener() {
        if (taskListenerRegistration != null) { taskListenerRegistration.remove(); taskListenerRegistration = null; }
    }
}