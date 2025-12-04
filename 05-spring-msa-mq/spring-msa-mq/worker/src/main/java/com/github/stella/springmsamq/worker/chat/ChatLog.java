package com.github.stella.springmsamq.worker.chat;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "chatLog")
public class ChatLog {
    @Id
    private String id;
    private String roomId;
    private String sender;
    private String content;
    private long timestamp;

    public ChatLog() {}

    public ChatLog(String roomId, String sender, String content, long timestamp) {
        this.roomId = roomId;
        this.sender = sender;
        this.content = content;
        this.timestamp = timestamp;
    }

    public String getId() { return id; }
    public String getRoomId() { return roomId; }
    public String getSender() { return sender; }
    public String getContent() { return content; }
    public long getTimestamp() { return timestamp; }
}
