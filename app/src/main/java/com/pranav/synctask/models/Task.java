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
    private Date dueDate;
    private Date createdAt;
    private String taskType;
    private String spaceId;
    private String localId;
    private boolean isSynced;
    private String creatorDisplayName;
    private String priority;

    // --- ADDED IN PHASE 2 ---
    private String ownershipScope;

    // --- CONSTANTS ---
    public static final String STATUS_PENDING = "pending";
    public static final String STATUS_COMPLETED = "completed";
    public static final String TYPE_TASK = "TASK";
    public static final String TYPE_REMINDER = "REMINDER";
    public static final String TYPE_UPDATE = "UPDATE";

    // --- ADDED IN PHASE 2 ---
    public static final String SCOPE_INDIVIDUAL = "INDIVIDUAL"; // For my eyes only
    public static final String SCOPE_SHARED = "SHARED";         // We can both edit/complete
    public static final String SCOPE_ASSIGNED = "ASSIGNED";     // I created it, you complete it


    public Task() {
        this.isSynced = true;
        this.localId = UUID.randomUUID().toString();
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        this.creatorDisplayName = (user != null) ? user.getDisplayName() : "A user";
        this.priority = "Normal";
        this.ownershipScope = SCOPE_SHARED; // Default to shared
    }

    // This constructor is for local/offline tasks
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
        this.isSynced = false; // By default, a new task is not synced
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        this.creatorDisplayName = (user != null) ? user.getDisplayName() : "A user";
        this.priority = "Normal";
        this.ownershipScope = SCOPE_SHARED; // Default to shared
    }

    // --- Getters and Setters ---

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getCreatorUID() {
        return creatorUID;
    }

    public void setCreatorUID(String creatorUID) {
        this.creatorUID = creatorUID;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

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

    public Date getDueDateAsDate() {
        return dueDate;
    }

    public void setDueDateFromDate(Date dueDate) {
        this.dueDate = dueDate;
    }

    public Date getCreatedAtAsDate() {
        return createdAt;
    }

    public void setCreatedAtFromDate(Date createdAt) {
        this.createdAt = createdAt;
    }

    public String getTaskType() {
        return taskType;
    }

    public void setTaskType(String taskType) {
        this.taskType = taskType;
    }

    public String getSpaceId() {
        return spaceId;
    }

    public void setSpaceId(String spaceId) {
        this.spaceId = spaceId;
    }

    public String getLocalId() {
        return localId;
    }

    public void setLocalId(String localId) {
        this.localId = localId;
    }

    public boolean isSynced() {
        return isSynced;
    }

    public void setSynced(boolean synced) {
        isSynced = synced;
    }

    public String getCreatorDisplayName() {
        return creatorDisplayName;
    }

    public void setCreatorDisplayName(String creatorDisplayName) {
        this.creatorDisplayName = creatorDisplayName;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    // --- ADDED IN PHASE 2 ---
    public String getOwnershipScope() {
        return ownershipScope;
    }

    public void setOwnershipScope(String ownershipScope) {
        this.ownershipScope = ownershipScope;
    }


    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("creatorUID", creatorUID);
        map.put("title", title);
        map.put("description", description);
        map.put("status", status);
        map.put("dueDate", getDueDate());
        map.put("createdAt", getCreatedAt());
        map.put("taskType", taskType);
        map.put("spaceId", spaceId);
        map.put("creatorDisplayName", creatorDisplayName);
        map.put("priority", priority);
        map.put("ownershipScope", ownershipScope); // --- ADDED IN PHASE 2 ---
        return map;
    }
}