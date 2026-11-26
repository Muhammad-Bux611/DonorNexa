package com.donornexa.scheduler;

import com.donornexa.bloodrequest.service.BloodRequestAutoExpiryScheduler;
import com.donornexa.bloodrequest.entity.BloodRequest;
import com.donornexa.hospital.entity.Hospital;
import com.donornexa.matching.entity.RequestMatch;
import com.donornexa.user.entity.User;
import com.donornexa.common.enums.BloodGroup;
import com.donornexa.common.enums.MatchStatus;
import com.donornexa.common.enums.RequestStatus;
import com.donornexa.common.enums.RequestUrgency;
import com.donornexa.notification.service.NotificationService;
import com.donornexa.audit.repository.AuditLogRepository;
import com.donornexa.bloodrequest.repository.BloodRequestRepository;
import com.donornexa.matching.repository.RequestMatchRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CEP Audit â€” Auto Expiry Scheduler Tests")
class AutoExpirySchedulerTest {

    @Mock
    private BloodRequestRepository bloodRequestRepository;

    @Mock
    private RequestMatchRepository requestMatchRepository;

    @Mock
    private AuditLogRepository auditLogRepository;

    @Mock
    private NotificationService notificationService;

    private BloodRequestAutoExpiryScheduler scheduler;

    @BeforeEach
    void setUp() {
        scheduler = new BloodRequestAutoExpiryScheduler(
                bloodRequestRepository,
                requestMatchRepository,
                auditLogRepository,
                notificationService
        );
    }

    @Test
    @DisplayName("Should sweep and expire overdue requests past their requiredBy deadline")
    void shouldExpireOverdueBloodRequests() {
        User hospitalUser = new User();
        hospitalUser.setEmail("hospital@donornexa.com");

        Hospital hospital = new Hospital();
        hospital.setUser(hospitalUser);
        hospital.setHospitalName("Indus Hospital");

        BloodRequest overdueRequest = new BloodRequest();
        overdueRequest.setId(101L);
        overdueRequest.setHospital(hospital);
        overdueRequest.setBloodGroup(BloodGroup.O_NEGATIVE);
        overdueRequest.setUnitsRequired(2);
        overdueRequest.setUnitsFulfilled(0);
        overdueRequest.setUrgency(RequestUrgency.CRITICAL);
        overdueRequest.setStatus(RequestStatus.MATCHING);
        overdueRequest.setRequiredBy(LocalDateTime.now().minusHours(1));

        RequestMatch pendingMatch = new RequestMatch();
        pendingMatch.setId(501L);
        pendingMatch.setBloodRequest(overdueRequest);
        pendingMatch.setMatchStatus(MatchStatus.INVITED);

        when(bloodRequestRepository.findByStatusInAndRequiredByBefore(any(), any()))
                .thenReturn(List.of(overdueRequest));
        when(requestMatchRepository.findByBloodRequestId(101L))
                .thenReturn(List.of(pendingMatch));

        scheduler.sweepExpiredBloodRequests();

        // Verify request transitioned to EXPIRED
        ArgumentCaptor<BloodRequest> reqCaptor = ArgumentCaptor.forClass(BloodRequest.class);
        verify(bloodRequestRepository).save(reqCaptor.capture());
        assertThat(reqCaptor.getValue().getStatus()).isEqualTo(RequestStatus.EXPIRED);

        // Verify match transitioned to EXPIRED
        ArgumentCaptor<RequestMatch> matchCaptor = ArgumentCaptor.forClass(RequestMatch.class);
        verify(requestMatchRepository).save(matchCaptor.capture());
        assertThat(matchCaptor.getValue().getMatchStatus()).isEqualTo(MatchStatus.EXPIRED);

        // Verify audit log was recorded
        verify(auditLogRepository, atLeastOnce()).save(any());

        // Verify hospital was notified
        verify(notificationService).createNotification(
                eq(hospitalUser),
                contains("Has Expired"),
                contains("O NEGATIVE"),
                any()
        );
    }
}
