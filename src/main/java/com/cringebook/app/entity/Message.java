package com.cringebook.app.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity(name="messages")
public class Message {
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Id
    @Column(name="message_id")
    private Integer messageId;

    @Column(name="sender_id")
    private Integer senderId;

    @Column(name="receiver_id")
    private Integer receiverId;

    @Column(name="message_text", length = 1000)
    private String messageText;

    @Column(name="sent_at")
    private LocalDateTime sentAt;

    @Column(name="is_read")
    private Boolean isRead;

    public Message() {
        this.sentAt = LocalDateTime.now();
        this.isRead = false;
    }

    public Message(Integer senderId, Integer receiverId, String messageText) {
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.messageText = messageText;
        this.sentAt = LocalDateTime.now();
        this.isRead = false;
    }

    public Integer getMessageId() {
        return messageId;
    }

    public Integer getSenderId() {
        return senderId;
    }

    public Integer getReceiverId() {
        return receiverId;
    }

    public String getMessageText() {
        return messageText;
    }

    public LocalDateTime getSentAt() {
        return sentAt;
    }

    public Boolean getIsRead() {
        return isRead;
    }

    public void setIsRead(Boolean isRead) {
        this.isRead = isRead;
    }
}