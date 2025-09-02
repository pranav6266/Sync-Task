package com.pranav.synctask.models;

import com.google.firebase.Timestamp;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Task {
    private String id;
    private String creatorUID;
    private String title;
    private String description;
    private String status;
    private Timestamp dueDate;
    private Timestamp createdAt;
    private String taskType;
    private List<String> sharedWith;

    public static final String STATUS_PENDING = "pending";
    public static final String STATUS_COMPLETED = "completed";
    public static final String TYPE_TASK = "TASK";
    public static final String TYPE_REMINDER = "REMINDER";
    public static final String TYPE_UPDATE = "UPDATE";

    public Task() {} // Required for Firestore

    public Task(String creatorUID, String title, String description,
                Timestamp dueDate, String taskType, String partnerUID) {
        this.creatorUID = creatorUID;
        this.title = title;
        this.description = description;
        this.status = STATUS_PENDING;
        this.dueDate = dueDate;
        this.createdAt = Timestamp.now();
        this.taskType = taskType;
        this.sharedWith = Arrays.asList(creatorUID, partnerUID);
    }

    // Getters and Setters
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
        return map;
    }
}