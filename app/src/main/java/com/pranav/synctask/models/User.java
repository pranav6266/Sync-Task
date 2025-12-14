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
    private List<String> spaceIds;

    // NEW FIELDS FOR V2.0
    private String personalSpaceId; // ID for "Me" Tab
    private String partnerSpaceId;  // ID for "Us" Tab

    public User() {
        this.spaceIds = new ArrayList<>();
    }

    public User(String uid, String email, String displayName, String photoURL) {
        this.uid = uid;
        this.email = email;
        this.displayName = displayName;
        this.photoURL = photoURL;
        this.fcmToken = null;
        this.spaceIds = new ArrayList<>();
    }

    // Getters and Setters
    public String getUid() { return uid; }
    public void setUid(String uid) { this.uid = uid; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public String getPhotoURL() { return photoURL; }
    public void setPhotoURL(String photoURL) { this.photoURL = photoURL; }

    public String getFcmToken() { return fcmToken; }
    public void setFcmToken(String fcmToken) { this.fcmToken = fcmToken; }

    public List<String> getSpaceIds() { return spaceIds; }
    public void setSpaceIds(List<String> spaceIds) { this.spaceIds = spaceIds; }

    public String getPersonalSpaceId() { return personalSpaceId; }
    public void setPersonalSpaceId(String personalSpaceId) { this.personalSpaceId = personalSpaceId; }

    public String getPartnerSpaceId() { return partnerSpaceId; }
    public void setPartnerSpaceId(String partnerSpaceId) { this.partnerSpaceId = partnerSpaceId; }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("uid", uid);
        map.put("email", email);
        map.put("displayName", displayName);
        map.put("photoURL", photoURL);
        map.put("fcmToken", fcmToken);
        map.put("spaceIds", spaceIds);
        map.put("personalSpaceId", personalSpaceId);
        map.put("partnerSpaceId", partnerSpaceId);
        return map;
    }
}