package com.donornexa.admin.service;

import com.donornexa.audit.entity.AuditLog;
import com.donornexa.common.enums.AuditAction;
import com.donornexa.audit.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service managing audit log inspection and search for compliance review.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminAuditLogService {

    private final AuditLogRepository auditLogRepository;

    @Transactional(readOnly = true)
    public Page<AuditLog> searchAuditLogs(String query, AuditAction action, String entityName, Pageable pageable) {
        String cleanQuery = (query != null && !query.isBlank()) ? query.trim() : null;
        String cleanEntity = (entityName != null && !entityName.isBlank()) ? entityName.trim() : null;
        return auditLogRepository.searchAndFilter(cleanQuery, action, cleanEntity, pageable);
    }
}
