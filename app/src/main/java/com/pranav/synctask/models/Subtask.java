package com.pranav.synctask.models;

import com.google.firebase.firestore.PropertyName; // Import this
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class Subtask implements Serializable {
    private String id;
    private String title;
    private boolean isCompleted;
    private String lockedByUid;
    private String lockedByName;
    private String completedByUid;

    public Subtask() {
        this.id = UUID.randomUUID().toString();
        this.isCompleted = false;
        this.lockedByUid = null;
        this.lockedByName = null;
    }

    public Subtask(String title) {
        this();
        this.title = title;
    }

    // --- Getters and Setters ---

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    // FIX: Explicitly map the Firestore property "isCompleted" to this getter
    @PropertyName("isCompleted")
    public boolean isCompleted() { return isCompleted; }

    // FIX: Explicitly map the Firestore property "isCompleted" to this setter
    @PropertyName("isCompleted")
    public void setCompleted(boolean completed) { isCompleted = completed; }

    public String getLockedByUid() { return lockedByUid; }
    public void setLockedByUid(String lockedByUid) { this.lockedByUid = lockedByUid; }

    public String getLockedByName() { return lockedByName; }
    public void setLockedByName(String lockedByName) { this.lockedByName = lockedByName; }

    public String getCompletedByUid() { return completedByUid; }
    public void setCompletedByUid(String completedByUid) { this.completedByUid = completedByUid; }

    // --- Helpers ---

    public boolean isLocked() {
        return lockedByUid != null && !lockedByUid.isEmpty();
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("id", id);
        map.put("title", title);
        map.put("isCompleted", isCompleted);
        map.put("lockedByUid", lockedByUid);
        map.put("lockedByName", lockedByName);
        map.put("completedByUid", completedByUid);
        return map;
    }
}