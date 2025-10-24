package com.pranav.synctask.models;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class User implements Serializable {
    private String uid;
    private String email;
    private String displayName;
    private String photoURL;
    private String fcmToken;
    private List<String> spaceIds; // CHANGED

    public User() {
    }

    public User(String uid, String email, String displayName, String photoURL) {
        this.uid = uid;
        this.email = email;
        this.displayName = displayName;
        this.photoURL = photoURL;
        this.fcmToken = null;
        this.spaceIds = new ArrayList<>(); // CHANGED
    }

    // Getters and Setters
    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getPhotoURL() {
        return photoURL;
    }

    public void setPhotoURL(String photoURL) {
        this.photoURL = photoURL;
    }

    public String getFcmToken() {
        return fcmToken;
    }

    public void setFcmToken(String fcmToken) {
        this.fcmToken = fcmToken;
    }

    public List<String> getSpaceIds() { // CHANGED
        return spaceIds;
    }

    public void setSpaceIds(List<String> spaceIds) { // CHANGED
        this.spaceIds = spaceIds;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("uid", uid);
        map.put("email", email);
        map.put("displayName", displayName);
        map.put("photoURL", photoURL);
        map.put("fcmToken", fcmToken);
        map.put("spaceIds", spaceIds); // CHANGED
        return map;
    }
}