package com.pranav.synctask.models;

import java.io.Serializable;
import java.util.List;

public class Space implements Serializable {
    private String spaceId;
    private String spaceName;
    private List<String> members;
    private String inviteCode;
    private String spaceType;
    private String adminUid; // Added to identify the creator/admin

    public static final String TYPE_PERSONAL = "PERSONAL";
    public static final String TYPE_SHARED = "SHARED";

    public Space() {
        this.spaceType = TYPE_SHARED;
    }

    public Space(String spaceId, String spaceName, List<String> members, String inviteCode, String adminUid) {
        this.spaceId = spaceId;
        this.spaceName = spaceName;
        this.members = members;
        this.inviteCode = inviteCode;
        this.adminUid = adminUid;
        this.spaceType = TYPE_SHARED;
    }

    // Getters and Setters
    public String getSpaceId() { return spaceId; }
    public void setSpaceId(String spaceId) { this.spaceId = spaceId; }

    public String getSpaceName() { return spaceName; }
    public void setSpaceName(String spaceName) { this.spaceName = spaceName; }

    public List<String> getMembers() { return members; }
    public void setMembers(List<String> members) { this.members = members; }

    public String getInviteCode() { return inviteCode; }
    public void setInviteCode(String inviteCode) { this.inviteCode = inviteCode; }

    public String getSpaceType() { return spaceType; }
    public void setSpaceType(String spaceType) { this.spaceType = spaceType; }

    public String getAdminUid() { return adminUid; }
    public void setAdminUid(String adminUid) { this.adminUid = adminUid; }
}