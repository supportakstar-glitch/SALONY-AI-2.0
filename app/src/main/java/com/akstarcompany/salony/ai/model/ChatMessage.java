package com.akstarcompany.salony.ai.model;

public class ChatMessage {
    private long id;
    private String text;
    private boolean isUser;
    private long timestamp;
    private String actionTag;
    private String actionLabel;

    public ChatMessage(long id, String text, boolean isUser, long timestamp, String actionTag, String actionLabel) {
        this.id = id;
        this.text = text;
        this.isUser = isUser;
        this.timestamp = timestamp;
        this.actionTag = actionTag;
        this.actionLabel = actionLabel;
    }

    public ChatMessage(String text, boolean isUser, String actionTag, String actionLabel) {
        this(0, text, isUser, System.currentTimeMillis(), actionTag, actionLabel);
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getText() { return text; }
    public void setText(String text) { this.text = text; }

    public boolean isUser() { return isUser; }
    public void setUser(boolean user) { isUser = user; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public String getActionTag() { return actionTag; }
    public void setActionTag(String actionTag) { this.actionTag = actionTag; }

    public String getActionLabel() { return actionLabel; }
    public void setActionLabel(String actionLabel) { this.actionLabel = actionLabel; }
}
