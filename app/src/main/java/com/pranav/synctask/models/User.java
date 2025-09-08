package com.pranav.synctask.models;

import java.util.HashMap;
import java.util.Map;

public class User {
    private String uid;
    private String email;
    private String displayName;
    private String photoURL;
    private String partnerCode;
    private String pairedWithUID;
    private String fcmToken; // PHASE 3: ADDED

    public User() {}

    public User(String uid, String email, String displayName, String photoURL) {
        this.uid = uid;
        this.email = email;
        this.displayName = displayName;
        this.photoURL = photoURL;
        this.partnerCode = generatePartnerCode();
        this.pairedWithUID = null;
        this.fcmToken = null;
    }

    private String generatePartnerCode() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder code = new StringBuilder();
        for (int i = 0; i < 6; i++) {
            code.append(chars.charAt((int) (Math.random() * chars.length())));
        }
        return code.toString();
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

    public String getPartnerCode() { return partnerCode; }
    public void setPartnerCode(String partnerCode) { this.partnerCode = partnerCode; }

    public String getPairedWithUID() { return pairedWithUID; }
    public void setPairedWithUID(String pairedWithUID) { this.pairedWithUID = pairedWithUID; }

    // PHASE 3: Getter and Setter for FCM token
    public String getFcmToken() { return fcmToken; }
    public void setFcmToken(String fcmToken) { this.fcmToken = fcmToken; }


    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("uid", uid);
        map.put("email", email);
        map.put("displayName", displayName);
        map.put("photoURL", photoURL);
        map.put("partnerCode", partnerCode);
        map.put("pairedWithUID", pairedWithUID);
        map.put("fcmToken", fcmToken); // PHASE 3: ADDED
        return map;
    }
}