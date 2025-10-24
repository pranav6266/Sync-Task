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

    // CHANGED: Added an instance of FirebaseHelper
    private final FirebaseHelper firebaseHelper;

    // CHANGED: Constructor is private and initializes FirebaseHelper
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

    public void attachTasksListener(String userUID) {
        if (tasksListenerRegistration != null) {
            tasksListenerRegistration.remove();
        }
        combinedTasksResult.setValue(new Result.Loading<>());
        // CHANGED: Call instance method
        tasksListenerRegistration = firebaseHelper.getTasks(userUID, new FirebaseHelper.TasksCallback() {
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

    // ADDED: This method was missing but called by MyFirebaseMessagingService
    public void refreshTasks() {
        User user = UserRepository.getInstance().getCurrentUserCache();
        if (user != null && user.getUid() != null) {
            attachTasksListener(user.getUid());
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
            // CHANGED: Call instance method
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

    public void createTask(Task task, Context context) {
        User currentUser = UserRepository.getInstance().getCurrentUserCache();
        boolean isPaired = currentUser != null && currentUser.getPairedWithUID() != null && !currentUser.getPairedWithUID().isEmpty();
        boolean isOnline = NetworkUtils.isNetworkAvailable(context);

        if (isPaired && isOnline) {
            task.setSynced(true);
            task.setSharedWith(Arrays.asList(currentUser.getUid(), currentUser.getPairedWithUID()));
            // CHANGED: Call instance method
            firebaseHelper.createTask(task, new FirebaseHelper.TasksCallback() {
                @Override public void onSuccess(List<Task> tasks) {}
                @Override public void onError(Exception e) {
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
        User currentUser = UserRepository.getInstance().getCurrentUserCache();
        boolean isPaired = currentUser != null && currentUser.getPairedWithUID() != null && !currentUser.getPairedWithUID().isEmpty();
        boolean isOnline = NetworkUtils.isNetworkAvailable(context);

        if (!isPaired || !isOnline || localTasks.isEmpty()) {
            return;
        }

        Log.d("TaskRepository", "Starting sync for " + localTasks.size() + " local tasks.");
        for (Task localTask : localTasks) {
            if (!localTask.isSynced()) {
                localTask.setSharedWith(Arrays.asList(currentUser.getUid(), currentUser.getPairedWithUID()));
                // CHANGED: Call instance method
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
            case "High": return 2;
            case "Low": return 0;
            default: return 1;
        }
    }

    public void updateTaskStatus(String taskId, String newStatus) {
        // CHANGED: Call instance method
        firebaseHelper.updateTaskStatus(taskId, newStatus);
    }

    public void deleteTask(String taskId) {
        // CHANGED: Call instance method
        firebaseHelper.deleteTask(taskId);
    }
}
