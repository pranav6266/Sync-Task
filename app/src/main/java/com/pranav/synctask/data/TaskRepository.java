package com.pranav.synctask.data;

import android.content.Context;
import android.util.Log;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Transaction;
import com.pranav.synctask.models.Subtask;
import com.pranav.synctask.models.Task;
import com.pranav.synctask.utils.FirebaseHelper;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TaskRepository {
    private static volatile TaskRepository instance;
    private final FirebaseHelper firebaseHelper;
    private final FirebaseFirestore db;

    // Listeners Map to manage multiple listeners
    private final Map<String, ListenerRegistration> activeListeners = new HashMap<>();

    // Legacy Single LiveDatas (Kept for backward compatibility with DashboardViewModel)
    private final MutableLiveData<Result<List<Task>>> combinedTasksResult = new MutableLiveData<>();
    private final MutableLiveData<Result<Task>> singleTaskResult = new MutableLiveData<>();
    private final MutableLiveData<Result<List<Task>>> completedTasksResult = new MutableLiveData<>();
    private final MutableLiveData<Result<List<Task>>> allTasksResult = new MutableLiveData<>();

    private String currentSpaceId;

    private TaskRepository() {
        firebaseHelper = new FirebaseHelper();
        db = FirebaseFirestore.getInstance();
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

    // --- NEW: V2.0 SUPPORT (Personal vs Partner) ---

    public void attachTasksListener(String spaceId, MutableLiveData<Result<List<Task>>> targetLiveData) {
        if (spaceId == null) return;

        // Remove existing listener for this specific space/target key if needed
        // For simplicity in this hybrid phase, we just add a new one.
        ListenerRegistration registration = firebaseHelper.getTasks(spaceId, new FirebaseHelper.TasksCallback() {
            @Override
            public void onSuccess(List<Task> tasks) {
                sortTasks(tasks);
                targetLiveData.setValue(new Result.Success<>(tasks));
            }
            @Override
            public void onError(Exception e) {
                targetLiveData.setValue(new Result.Error<>(e));
            }
        });
        activeListeners.put(spaceId, registration);
    }

    // --- LEGACY SUPPORT METHODS (Required to fix your build errors) ---

    // Used by MyFirebaseMessagingService
    public void refreshTasks() {
        if (currentSpaceId != null) {
            attachTasksListener(currentSpaceId);
        }
    }

    // Used by TasksViewModel (Old)
    public LiveData<Result<List<Task>>> getTasks() { return combinedTasksResult; }

    public void attachTasksListener(String spaceId) {
        if (spaceId == null) return;
        currentSpaceId = spaceId;
        attachTasksListener(spaceId, combinedTasksResult);
    }

    public void removeTasksListListener() {
        for (ListenerRegistration reg : activeListeners.values()) {
            reg.remove();
        }
        activeListeners.clear();
    }

    // Used by DashboardViewModel
    public LiveData<Result<List<Task>>> getAllTasksResult() { return allTasksResult; }

    public void attachAllTasksListener(List<String> spaceIds) {
        if (activeListeners.containsKey("ALL_TASKS")) {
            activeListeners.get("ALL_TASKS").remove();
        }
        allTasksResult.setValue(new Result.Loading<>());
        ListenerRegistration reg = firebaseHelper.getAllTasksForSpaces(spaceIds, new FirebaseHelper.TasksCallback() {
            @Override public void onSuccess(List<Task> tasks) { allTasksResult.setValue(new Result.Success<>(tasks)); }
            @Override public void onError(Exception e) { allTasksResult.setValue(new Result.Error<>(e)); }
        });
        activeListeners.put("ALL_TASKS", reg);
    }

    public static void removeAllTasksListener() {
        if (instance != null && instance.activeListeners.containsKey("ALL_TASKS")) {
            instance.activeListeners.get("ALL_TASKS").remove();
        }
    }

    // Used by CompletedTasksViewModel
    public LiveData<Result<List<Task>>> getCompletedTasks() { return completedTasksResult; }

    public void attachCompletedTasksListener(String spaceId) {
        completedTasksResult.setValue(new Result.Loading<>());
        ListenerRegistration reg = firebaseHelper.getCompletedTasks(spaceId, new FirebaseHelper.TasksCallback() {
            @Override public void onSuccess(List<Task> tasks) { completedTasksResult.setValue(new Result.Success<>(tasks)); }
            @Override public void onError(Exception e) { completedTasksResult.setValue(new Result.Error<>(e)); }
        });
        activeListeners.put("COMPLETED_" + spaceId, reg);
    }

    public void attachCompletedTasksListenerForSpaces(List<String> spaceIds) {
        completedTasksResult.setValue(new Result.Loading<>());
        ListenerRegistration reg = firebaseHelper.getCompletedTasksForSpaces(spaceIds, new FirebaseHelper.TasksCallback() {
            @Override public void onSuccess(List<Task> tasks) { completedTasksResult.setValue(new Result.Success<>(tasks)); }
            @Override public void onError(Exception e) { completedTasksResult.setValue(new Result.Error<>(e)); }
        });
        activeListeners.put("COMPLETED_SPACES", reg);
    }

    public void removeCompletedTasksListener() {
        // Cleanup logic if needed
    }
    public void removeCompletedTasksForSpacesListener() {
        if (activeListeners.containsKey("COMPLETED_SPACES")) activeListeners.get("COMPLETED_SPACES").remove();
    }

    // Used by TaskDetailViewModel
    public LiveData<Result<Task>> getTaskById() { return singleTaskResult; }

    public void attachTaskListener(String taskId) {
        singleTaskResult.setValue(new Result.Loading<>());
        ListenerRegistration reg = firebaseHelper.getTaskById(taskId, new FirebaseHelper.TaskCallback() {
            @Override public void onSuccess(Task task) { singleTaskResult.setValue(new Result.Success<>(task)); }
            @Override public void onError(Exception e) { singleTaskResult.setValue(new Result.Error<>(e)); }
        });
        activeListeners.put("TASK_" + taskId, reg);
    }

    public void removeTaskListener() {
        // Cleanup logic
    }

    // --- CRUD OPERATIONS ---

    public void createTask(Task task, Context context) {
        firebaseHelper.createTask(task, new FirebaseHelper.TasksCallback() {
            @Override public void onSuccess(List<Task> tasks) { Log.d("TaskRepository", "Task created"); }
            @Override public void onError(Exception e) { Log.e("TaskRepository", "Error creating task", e); }
        });
    }

    public LiveData<Result<Void>> updateTask(Task task) {
        MutableLiveData<Result<Void>> result = new MutableLiveData<>();
        result.setValue(new Result.Loading<>());
        firebaseHelper.updateTask(task.getId(), task.toMap(), new FirebaseHelper.TasksCallback() {
            @Override public void onSuccess(List<Task> tasks) { result.setValue(new Result.Success<>(null)); }
            @Override public void onError(Exception e) { result.setValue(new Result.Error<>(e)); }
        });
        return result;
    }

    public void updateTaskStatus(String taskId, String newStatus) {
        firebaseHelper.updateTaskStatus(taskId, newStatus);
    }

    public void deleteTask(String taskId) {
        firebaseHelper.deleteTask(taskId);
    }

    // --- SUBTASK TRANSACTIONS (Required by TaskDetailViewModel) ---

    public void toggleSubtaskLock(String taskId, String subtaskId, String userId, String userName, boolean forceUnlock) {
        DocumentReference taskRef = db.collection("tasks").document(taskId);
        db.runTransaction((Transaction.Function<Void>) transaction -> {
            Task snapshot = transaction.get(taskRef).toObject(Task.class);
            if (snapshot == null || snapshot.getSubtasks() == null) return null;
            List<Subtask> subtasks = snapshot.getSubtasks();
            boolean updated = false;
            for (Subtask s : subtasks) {
                if (s.getId().equals(subtaskId)) {
                    if (s.getLockedByUid() == null || s.getLockedByUid().isEmpty()) {
                        s.setLockedByUid(userId);
                        s.setLockedByName(userName);
                        updated = true;
                    } else if (s.getLockedByUid().equals(userId) || forceUnlock) {
                        s.setLockedByUid(null);
                        s.setLockedByName(null);
                        updated = true;
                    }
                    break;
                }
            }
            if (updated) {
                List<Map<String, Object>> subtasksMap = new ArrayList<>();
                for (Subtask s : subtasks) subtasksMap.add(s.toMap());
                transaction.update(taskRef, "subtasks", subtasksMap);
            }
            return null;
        }).addOnFailureListener(e -> Log.e("TaskRepository", "Transaction failure.", e));
    }

    public void toggleSubtaskCompletion(String taskId, String subtaskId, boolean isCompleted, String userId) {
        DocumentReference taskRef = db.collection("tasks").document(taskId);
        db.runTransaction((Transaction.Function<Void>) transaction -> {
            Task snapshot = transaction.get(taskRef).toObject(Task.class);
            if (snapshot == null || snapshot.getSubtasks() == null) return null;
            List<Subtask> subtasks = snapshot.getSubtasks();
            boolean allComplete = true;
            boolean updated = false;
            for (Subtask s : subtasks) {
                if (s.getId().equals(subtaskId)) {
                    s.setCompleted(isCompleted);
                    s.setCompletedByUid(isCompleted ? userId : null);
                    updated = true;
                }
                if (!s.isCompleted()) {
                    allComplete = false;
                }
            }
            if (updated) {
                List<Map<String, Object>> subtasksMap = new ArrayList<>();
                for (Subtask s : subtasks) subtasksMap.add(s.toMap());
                transaction.update(taskRef, "subtasks", subtasksMap);
                if (allComplete) transaction.update(taskRef, "status", Task.STATUS_COMPLETED);
                else transaction.update(taskRef, "status", Task.STATUS_PENDING);
            }
            return null;
        }).addOnFailureListener(e -> Log.e("TaskRepository", "Completion Transaction failure.", e));
    }

    // --- HELPERS ---
    private void sortTasks(List<Task> tasks) {
        tasks.sort((t1, t2) -> {
            boolean c1 = "completed".equals(t1.getStatus());
            boolean c2 = "completed".equals(t2.getStatus());
            if (c1 != c2) return c1 ? 1 : -1;
            if (t1.getDueDate() != null && t2.getDueDate() != null) {
                int dateCompare = t1.getDueDate().compareTo(t2.getDueDate());
                if (dateCompare != 0) return dateCompare;
            } else if (t1.getDueDate() != null) return -1;
            else if (t2.getDueDate() != null) return 1;
            return getPriorityValue(t2.getPriority()) - getPriorityValue(t1.getPriority());
        });
    }

    private int getPriorityValue(String priority) {
        if (priority == null) return 1;
        switch (priority) {
            case "High": return 2;
            case "Low": return 0;
            default: return 1;
        }
    }
}