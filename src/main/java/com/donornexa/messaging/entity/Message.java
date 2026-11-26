package com.donornexa.messaging.entity;

import com.donornexa.user.entity.User;


import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Represents an individual message exchanged within a verified donor-hospital conversation.
 */
@Getter
@Setter
@Entity
@Table(
    name = "messages",
    indexes = {
        @Index(name = "idx_messages_conversation_id", columnList = "conversation_id"),
        @Index(name = "idx_messages_sender_id",       columnList = "sender_id"),
        @Index(name = "idx_messages_recipient_id",    columnList = "recipient_id"),
        @Index(name = "idx_messages_sent_at",         columnList = "sent_at"),
        @Index(name = "idx_messages_is_read",         columnList = "is_read")
    }
)
public class Message {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "Conversation reference is required")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "conversation_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_messages_conversation"))
    private Conversation conversation;

    @NotNull(message = "Message sender is required")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sender_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_messages_sender"))
    private User sender;

    @NotNull(message = "Message recipient is required")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recipient_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_messages_recipient"))
    private User recipient;

    @NotBlank(message = "Message content cannot be blank")
    @Size(max = 2000, message = "Message content must not exceed 2000 characters")
    @Column(name = "content", nullable = false, length = 2000)
    private String content;

    @NotNull
    @Column(name = "is_read", nullable = false)
    private Boolean isRead = Boolean.FALSE;

    @Column(name = "read_at")
    private LocalDateTime readAt;

    @Column(name = "sent_at", nullable = false, updatable = false)
    private LocalDateTime sentAt;

    @PrePersist
    protected void onCreate() {
        if (this.sentAt == null) {
            this.sentAt = LocalDateTime.now();
        }
        if (this.isRead == null) {
            this.isRead = Boolean.FALSE;
        }
    }

    public void markAsRead() {
        this.isRead = Boolean.TRUE;
        this.readAt = LocalDateTime.now();
    }
}
