package com.gis.logistics.domain.im;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "im_messages")
@Getter
@Setter
public class ImMessage {

    public enum SenderRole { USER, ADMIN, SYSTEM }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "session_id", nullable = false)
    private Long sessionId;

    @Enumerated(EnumType.STRING)
    @Column(name = "sender_role", nullable = false, length = 8)
    private SenderRole senderRole;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "sent_at", nullable = false)
    private LocalDateTime sentAt;

    public ImMessage() {
    }

    public ImMessage(Long sessionId, SenderRole senderRole, String content) {
        this.sessionId = sessionId;
        this.senderRole = senderRole;
        this.content = content;
        this.sentAt = LocalDateTime.now();
    }
}
