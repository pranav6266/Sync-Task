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
import java.util.Collections;
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

    public LiveData<Result<List<Task>>> getTasks() {
        return combinedTasksResult;
    }

    // PHASE 2: Method to calculate statistics from cached tasks
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
        tasksListenerRegistration = FirebaseHelper.getTasks(userUID, new FirebaseHelper.TasksCallback() {
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
        Collections.sort(mergedList, (o1, o2) -> o2.getCreatedAt().compareTo(o1.getCreatedAt()));
        combinedTasksResult.postValue(new Result.Success<>(mergedList));
    }


    public void createTask(Task task, Context context) {
        User currentUser = UserRepository.getInstance().getCurrentUserCache();
        boolean isPaired = currentUser != null && currentUser.getPairedWithUID() != null && !currentUser.getPairedWithUID().isEmpty();
        boolean isOnline = NetworkUtils.isNetworkAvailable(context);

        if (isPaired && isOnline) {
            task.setSynced(true);
            task.setSharedWith(Arrays.asList(currentUser.getUid(), currentUser.getPairedWithUID()));
            FirebaseHelper.createTask(task, new FirebaseHelper.TasksCallback() {
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
                FirebaseHelper.createTask(localTask, new FirebaseHelper.TasksCallback() {
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

    public void updateTaskStatus(String taskId, String newStatus) {
        FirebaseHelper.updateTaskStatus(taskId, newStatus);
    }

    public void deleteTask(String taskId) {
        FirebaseHelper.deleteTask(taskId);
    }
}