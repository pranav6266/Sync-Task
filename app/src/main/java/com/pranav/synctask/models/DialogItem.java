package com.pranav.synctask.models;

// Helper class to store items for the "Add Task" dialog
public class DialogItem {
    private String displayName;
    private String spaceId;
    private String spaceType;

    public DialogItem(String displayName, String spaceId, String spaceType) {
        this.displayName = displayName;
        this.spaceId = spaceId;
        this.spaceType = spaceType;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getSpaceId() {
        return spaceId;
    }

    public String getSpaceType() {
        return spaceType;
    }
}