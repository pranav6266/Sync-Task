package com.pranav.synctask.models;

import java.io.Serializable;
import java.util.List;

public class Space implements Serializable {
    private String spaceId;
    private String spaceName;
    private List<String> members;
    private String inviteCode;
    private String spaceType; // --- ADDED IN PHASE 1 ---

    // --- ADDED IN PHASE 1 ---
    public static final String TYPE_PERSONAL = "PERSONAL";
    public static final String TYPE_SHARED = "SHARED";

    public Space() {
        // Default constructor
        this.spaceType = TYPE_SHARED; // Default for existing data
    }

    public Space(String spaceId, String spaceName, List<String> members, String inviteCode) {
        this.spaceId = spaceId;
        this.spaceName = spaceName;
        this.members = members;
        this.inviteCode = inviteCode;
        this.spaceType = TYPE_SHARED; // Default for existing data
    }

    // Getters and Setters
    public String getSpaceId() {
        return spaceId;
    }

    public void setSpaceId(String spaceId) {
        this.spaceId = spaceId;
    }

    public String getSpaceName() {
        return spaceName;
    }

    public void setSpaceName(String spaceName) {
        this.spaceName = spaceName;
    }

    public List<String> getMembers() {
        return members;
    }

    public void setMembers(List<String> members) {
        this.members = members;
    }

    public String getInviteCode() {
        return inviteCode;
    }

    public void setInviteCode(String inviteCode) {
        this.inviteCode = inviteCode;
    }

    // --- ADDED IN PHASE 1 ---
    public String getSpaceType() {
        return spaceType;
    }

    public void setSpaceType(String spaceType) {
        this.spaceType = spaceType;
    }
}