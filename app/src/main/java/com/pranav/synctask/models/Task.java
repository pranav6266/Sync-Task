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
import java.util.Date;

public class Task implements Serializable {
    private static final long serialVersionUID = 1L;

    private String id;
    private String creatorUID;
    private String title;
    private String description;
    private String status;
    private Date dueDate; // Changed from Timestamp to Date for serialization
    private Date createdAt; // Changed from Timestamp to Date for serialization
    private String taskType;
    private List<String> sharedWith;
    private String localId;
    private boolean isSynced;
    private String creatorDisplayName;
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
        this.priority = "Normal";
    }

    public Task(String creatorUID, String title, String description,
                Timestamp dueDate, String taskType, String partnerUID) {
        this.localId = UUID.randomUUID().toString();
        this.creatorUID = creatorUID;
        this.title = title;
        this.description = description;
        this.status = STATUS_PENDING;
        this.dueDate = dueDate != null ? dueDate.toDate() : null;
        this.createdAt = Timestamp.now().toDate();
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
        this.dueDate = dueDate != null ? dueDate.toDate() : null;
        this.createdAt = Timestamp.now().toDate();
        this.taskType = taskType;
        this.sharedWith = Collections.singletonList(creatorUID);
        this.isSynced = false;
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        this.creatorDisplayName = (user != null) ? user.getDisplayName() : "A user";
        this.priority = "Normal";
    }

    // Getters and Setters with Timestamp conversion
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

    // Firebase Timestamp compatibility methods
    public Timestamp getDueDate() {
        return dueDate != null ? new Timestamp(dueDate) : null;
    }

    public void setDueDate(Timestamp dueDate) {
        this.dueDate = dueDate != null ? dueDate.toDate() : null;
    }

    public Timestamp getCreatedAt() {
        return createdAt != null ? new Timestamp(createdAt) : null;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt != null ? createdAt.toDate() : null;
    }

    // Date methods for direct access
    public Date getDueDateAsDate() { return dueDate; }
    public void setDueDateFromDate(Date dueDate) { this.dueDate = dueDate; }
    public Date getCreatedAtAsDate() { return createdAt; }
    public void setCreatedAtFromDate(Date createdAt) { this.createdAt = createdAt; }

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
    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("creatorUID", creatorUID);
        map.put("title", title);
        map.put("description", description);
        map.put("status", status);
        map.put("dueDate", getDueDate()); // Convert back to Timestamp for Firebase
        map.put("createdAt", getCreatedAt()); // Convert back to Timestamp for Firebase
        map.put("taskType", taskType);
        map.put("sharedWith", sharedWith);
        map.put("creatorDisplayName", creatorDisplayName);
        map.put("priority", priority);
        return map;
    }
}
