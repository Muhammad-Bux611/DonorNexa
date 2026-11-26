package com.donornexa.messaging.dto;

import com.donornexa.common.enums.BloodGroup;
import com.donornexa.common.enums.ConversationStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class ConversationSummaryDTO {
    private Long id;
    private Long donorId;
    private String donorName;
    private Long hospitalId;
    private String hospitalName;
    private Long bloodRequestId;
    private BloodGroup bloodGroup;
    private String lastMessagePreview;
    private LocalDateTime lastMessageAt;
    private long unreadCount;
    private ConversationStatus status;
}
