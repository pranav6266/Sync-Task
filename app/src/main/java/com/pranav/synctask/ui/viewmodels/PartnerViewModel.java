package com.pranav.synctask.ui.viewmodels;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.pranav.synctask.data.Result;
import com.pranav.synctask.data.TaskRepository;
import com.pranav.synctask.data.UserRepository;
import com.pranav.synctask.models.Message;
import com.pranav.synctask.models.Task;
import com.pranav.synctask.models.User;
import java.util.Collections;
import java.util.List;

public class PartnerViewModel extends ViewModel {
    private final TaskRepository taskRepository;
    private final UserRepository userRepository;

    // Streams
    private final MutableLiveData<Result<List<Task>>> partnerTasksResult = new MutableLiveData<>();
    private final MutableLiveData<Result<List<Message>>> messagesResult = new MutableLiveData<>();
    private final MutableLiveData<String> partnerSpaceId = new MutableLiveData<>(null);

    public PartnerViewModel() {
        this.taskRepository = TaskRepository.getInstance();
        this.userRepository = UserRepository.getInstance();
        loadPartnerSpace();
    }

    // 1. Initialize: Find the Shared Space ID
    private void loadPartnerSpace() {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) return;

        userRepository.getUser(uid).observeForever(result -> {
            if (result instanceof Result.Success) {
                User user = ((Result.Success<User>) result).data;
                if (user.getPartnerSpaceId() != null) {
                    partnerSpaceId.setValue(user.getPartnerSpaceId());
                    // Start listening to data
                    taskRepository.attachTasksListener(user.getPartnerSpaceId(), partnerTasksResult);
                    taskRepository.attachMessagesListener(user.getPartnerSpaceId(), messagesResult);
                } else {
                    partnerSpaceId.setValue(null); // Not linked
                }
            }
        });
    }

    // 2. Chat Logic
    public void sendMessage(String text) {
        String spaceId = partnerSpaceId.getValue();
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (spaceId != null && user != null && !text.trim().isEmpty()) {
            Message msg = new Message(text, user.getUid(), user.getDisplayName(), false);
            taskRepository.sendMessage(msg, spaceId);
        }
    }

    // 3. Task Logic with System Message Injection
    public void createSharedTask(String title) {
        String spaceId = partnerSpaceId.getValue();
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (spaceId != null && user != null) {
            // A. Create the Task
            Task task = new Task();
            task.setTitle(title);
            task.setCreatorUID(user.getUid());
            task.setSpaceId(spaceId); // CRITICAL: Set to Partner Space
            task.setOwnershipScope(Task.SCOPE_SHARED);
            task.setStatus(Task.STATUS_PENDING);

            taskRepository.createTask(task, null);

            // B. Auto-send System Message to Chat
            Message sysMsg = new Message("Added task: " + title, user.getUid(), "System", true);
            taskRepository.sendMessage(sysMsg, spaceId);
        }
    }

    public void updateTaskStatus(Task task, boolean isCompleted) {
        String newStatus = isCompleted ? Task.STATUS_COMPLETED : Task.STATUS_PENDING;
        taskRepository.updateTaskStatus(task.getId(), newStatus);

        // Optional: Send system message on completion too
        if (isCompleted) {
            String spaceId = partnerSpaceId.getValue();
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if (spaceId != null && user != null) {
                Message sysMsg = new Message("Completed: " + task.getTitle(), user.getUid(), "System", true);
                taskRepository.sendMessage(sysMsg, spaceId);
            }
        }
    }

    // Getters
    public LiveData<Result<List<Task>>> getPartnerTasks() { return partnerTasksResult; }
    public LiveData<Result<List<Message>>> getMessages() { return messagesResult; }
    public LiveData<String> getPartnerSpaceId() { return partnerSpaceId; }
}