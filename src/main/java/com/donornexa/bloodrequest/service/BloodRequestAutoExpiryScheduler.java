package com.donornexa.bloodrequest.service;

import com.donornexa.audit.entity.AuditLog;
import com.donornexa.bloodrequest.entity.BloodRequest;
import com.donornexa.matching.entity.RequestMatch;
import com.donornexa.common.enums.AuditAction;
import com.donornexa.common.enums.MatchStatus;
import com.donornexa.common.enums.NotificationType;
import com.donornexa.common.enums.RequestStatus;
import com.donornexa.notification.service.NotificationService;
import com.donornexa.audit.repository.AuditLogRepository;
import com.donornexa.bloodrequest.repository.BloodRequestRepository;
import com.donornexa.matching.repository.RequestMatchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Background scheduler that periodically monitors and expires overdue blood requests.
 * Runs in the Asia/Karachi timezone, marks unfulfilled requests past their requiredBy
 * deadline as EXPIRED, updates pending matches, records audit logs, and alerts hospitals.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BloodRequestAutoExpiryScheduler {

    private final BloodRequestRepository bloodRequestRepository;
    private final RequestMatchRepository requestMatchRepository;
    private final AuditLogRepository auditLogRepository;
    private final NotificationService notificationService;

    /**
     * Executes every 60 seconds.
     */
    @Scheduled(fixedRate = 60000)
    @Transactional
    public void sweepExpiredBloodRequests() {
        LocalDateTime now = LocalDateTime.now();
        List<RequestStatus> candidateStatuses = List.of(
                RequestStatus.PENDING,
                RequestStatus.PENDING_ADMIN_APPROVAL,
                RequestStatus.MATCHING,
                RequestStatus.PARTIALLY_FULFILLED
        );

        List<BloodRequest> expiredRequests = bloodRequestRepository.findByStatusInAndRequiredByBefore(candidateStatuses, now);
        if (expiredRequests.isEmpty()) {
            return;
        }

        log.info("Auto-expiry sweep: found {} overdue blood request(s)", expiredRequests.size());

        for (BloodRequest req : expiredRequests) {
            RequestStatus previousStatus = req.getStatus();
            req.setStatus(RequestStatus.EXPIRED);
            bloodRequestRepository.save(req);

            // Expire pending/invited donor matches as EXPIRED
            List<RequestMatch> matches = requestMatchRepository.findByBloodRequestId(req.getId());
            for (RequestMatch match : matches) {
                if (match.getMatchStatus() == MatchStatus.PENDING || match.getMatchStatus() == MatchStatus.INVITED) {
                    match.setMatchStatus(MatchStatus.EXPIRED);
                    requestMatchRepository.save(match);
                }
            }

            // Security & Compliance Audit Log
            try {
                AuditLog audit = new AuditLog();
                audit.setAction(AuditAction.EXPIRE_REQUEST);
                audit.setEntityName("BloodRequest");
                audit.setEntityId(String.valueOf(req.getId()));
                audit.setOldValue("status=" + previousStatus);
                audit.setNewValue("status=EXPIRED");
                audit.setDescription(String.format("Blood request #%d auto-expired by system scheduler. Deadline was %s.",
                        req.getId(), req.getRequiredBy()));
                audit.setIpAddress("127.0.0.1 (System)");
                audit.setCreatedAt(now);
                auditLogRepository.save(audit);
            } catch (Exception ex) {
                log.error("Failed to record audit log for expired request #{}: {}", req.getId(), ex.getMessage());
            }

            // Notify Hospital
            try {
                if (req.getHospital() != null && req.getHospital().getUser() != null) {
                    notificationService.createNotification(
                            req.getHospital().getUser(),
                            "Blood Request #" + req.getId() + " Has Expired",
                            String.format("Your blood request for %d unit(s) of %s expired on %s.",
                                    req.getUnitsRequired(),
                                    req.getBloodGroup().name().replace('_', ' '),
                                    req.getRequiredBy().toString().replace('T', ' ')),
                            NotificationType.BLOOD_REQUEST
                    );
                }
            } catch (Exception ex) {
                log.warn("Failed to notify hospital for expired request #{}: {}", req.getId(), ex.getMessage());
            }
        }
    }
}
