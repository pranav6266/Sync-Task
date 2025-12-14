package com.pranav.synctask.models;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.ServerTimestamp;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class Message {
    private String id;
    private String text;
    private String senderId;
    private String senderName;
    @ServerTimestamp
    private Date timestamp;
    private boolean isSystemMessage; // True if this is an automated "Task Added" alert

    public Message() {
        this.id = UUID.randomUUID().toString();
        this.isSystemMessage = false;
    }

    public Message(String text, String senderId, String senderName, boolean isSystemMessage) {
        this();
        this.text = text;
        this.senderId = senderId;
        this.senderName = senderName;
        this.isSystemMessage = isSystemMessage;
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getText() { return text; }
    public void setText(String text) { this.text = text; }

    public String getSenderId() { return senderId; }
    public void setSenderId(String senderId) { this.senderId = senderId; }

    public String getSenderName() { return senderName; }
    public void setSenderName(String senderName) { this.senderName = senderName; }

    public Date getTimestamp() { return timestamp; }
    public void setTimestamp(Date timestamp) { this.timestamp = timestamp; }

    public boolean isSystemMessage() { return isSystemMessage; }
    public void setSystemMessage(boolean systemMessage) { isSystemMessage = systemMessage; }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("id", id);
        map.put("text", text);
        map.put("senderId", senderId);
        map.put("senderName", senderName);
        map.put("timestamp", timestamp); // Firestore handles ServerTimestamp conversion
        map.put("isSystemMessage", isSystemMessage);
        return map;
    }
}