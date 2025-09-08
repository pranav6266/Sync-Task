package com.pranav.synctask.models;

import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

// PHASE 4: Implement Serializable to pass Task objects between activities
public class Task implements Serializable {
    private String id;
    private String creatorUID;
    private String title;
    private String description;
    private String status;
    private Timestamp dueDate;
    private Timestamp createdAt;
    private String taskType;
    private List<String> sharedWith;
    private String localId;
    private boolean isSynced;
    private String creatorDisplayName;

    // PHASE 4: Add priority field
    private String priority;

    public static final String STATUS_PENDING = "pending";
    public static final String STATUS_COMPLETED = "completed";
    public static final String TYPE_TASK = "TASK";
    public static final String TYPE_REMINDER = "REMINDER";
    public static final String TYPE_UPDATE = "UPDATE";

    public Task() {
        this.isSynced = true;
        this.localId = UUID.randomUUID().toString();
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        this.creatorDisplayName = (user != null) ? user.getDisplayName() : "A user";
        this.priority = "Normal"; // Default priority
    }

    public Task(String creatorUID, String title, String description,
                Timestamp dueDate, String taskType, String partnerUID) {
        this.localId = UUID.randomUUID().toString();
        this.creatorUID = creatorUID;
        this.title = title;
        this.description = description;
        this.status = STATUS_PENDING;
        this.dueDate = dueDate;
        this.createdAt = Timestamp.now();
        this.taskType = taskType;
        this.sharedWith = Arrays.asList(creatorUID, partnerUID);
        this.isSynced = true;
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        this.creatorDisplayName = (user != null) ? user.getDisplayName() : "A user";
        this.priority = "Normal";
    }

    public Task(String creatorUID, String title, String description,
                Timestamp dueDate, String taskType) {
        this.localId = UUID.randomUUID().toString();
        this.creatorUID = creatorUID;
        this.title = title;
        this.description = description;
        this.status = STATUS_PENDING;
        this.dueDate = dueDate;
        this.createdAt = Timestamp.now();
        this.taskType = taskType;
        this.sharedWith = Collections.singletonList(creatorUID);
        this.isSynced = false;
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        this.creatorDisplayName = (user != null) ? user.getDisplayName() : "A user";
        this.priority = "Normal";
    }

    // Getters and Setters...
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getCreatorUID() { return creatorUID; }
    public void setCreatorUID(String creatorUID) { this.creatorUID = creatorUID; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Timestamp getDueDate() { return dueDate; }
    public void setDueDate(Timestamp dueDate) { this.dueDate = dueDate; }
    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }
    public String getTaskType() { return taskType; }
    public void setTaskType(String taskType) { this.taskType = taskType; }
    public List<String> getSharedWith() { return sharedWith; }
    public void setSharedWith(List<String> sharedWith) { this.sharedWith = sharedWith; }
    public String getLocalId() { return localId; }
    public void setLocalId(String localId) { this.localId = localId; }
    public boolean isSynced() { return isSynced; }
    public void setSynced(boolean synced) { isSynced = synced; }
    public String getCreatorDisplayName() { return creatorDisplayName; }
    public void setCreatorDisplayName(String creatorDisplayName) { this.creatorDisplayName = creatorDisplayName; }

    // PHASE 4: Getter and setter for priority
    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("creatorUID", creatorUID);
        map.put("title", title);
        map.put("description", description);
        map.put("status", status);
        map.put("dueDate", dueDate);
        map.put("createdAt", createdAt);
        map.put("taskType", taskType);
        map.put("sharedWith", sharedWith);
        map.put("creatorDisplayName", creatorDisplayName);
        map.put("priority", priority); // PHASE 4: Add to map
        return map;
    }
}