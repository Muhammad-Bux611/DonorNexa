package com.donornexa.audit.repository;


import com.donornexa.audit.entity.AuditLog;
import com.donornexa.common.enums.AuditAction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for {@link AuditLog} entities.
 * Audit logs are append-only; delete operations on this repository
 * should be restricted by application policy, not by the data layer.
 */
@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    /**
     * Find all audit log entries for a specific actor, ordered newest first.
     */
    List<AuditLog> findByActorIdOrderByCreatedAtDesc(Long actorId);

    /**
     * Find all audit log entries for a specific entity type and entity ID.
     * Useful for viewing the full history of a single record.
     */
    List<AuditLog> findByEntityNameAndEntityId(String entityName, String entityId);

    /** Find all audit logs for a specific action type. */
    List<AuditLog> findByAction(AuditAction action);

    /** Find audit logs for a specific actor, with pagination. */
    Page<AuditLog> findByActorIdOrderByCreatedAtDesc(Long actorId, Pageable pageable);

    /** Find audit logs for a specific entity type, with pagination. */
    Page<AuditLog> findByEntityNameOrderByCreatedAtDesc(String entityName, Pageable pageable);

    /** Find top 5 most recent audit logs with eager actor. */
    @org.springframework.data.jpa.repository.Query("SELECT a FROM AuditLog a LEFT JOIN FETCH a.actor ORDER BY a.createdAt DESC")
    List<AuditLog> findTop5WithActor(Pageable pageable);

    /**
     * Search and filter audit log entries with pagination.
     */
    @org.springframework.data.jpa.repository.Query(
            value = "SELECT a FROM AuditLog a LEFT JOIN FETCH a.actor u " +
                    "WHERE (:query IS NULL OR :query = '' " +
                    "       OR LOWER(a.description) LIKE LOWER(CONCAT('%', :query, '%')) " +
                    "       OR LOWER(a.entityId) LIKE LOWER(CONCAT('%', :query, '%')) " +
                    "       OR (u IS NOT NULL AND (LOWER(u.email) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(u.fullName) LIKE LOWER(CONCAT('%', :query, '%'))))) " +
                    "  AND (:action IS NULL OR a.action = :action) " +
                    "  AND (:entityName IS NULL OR :entityName = '' OR LOWER(a.entityName) = LOWER(:entityName)) " +
                    "ORDER BY a.createdAt DESC",
            countQuery = "SELECT COUNT(a) FROM AuditLog a LEFT JOIN a.actor u " +
                         "WHERE (:query IS NULL OR :query = '' " +
                         "       OR LOWER(a.description) LIKE LOWER(CONCAT('%', :query, '%')) " +
                         "       OR LOWER(a.entityId) LIKE LOWER(CONCAT('%', :query, '%')) " +
                         "       OR (u IS NOT NULL AND (LOWER(u.email) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(u.fullName) LIKE LOWER(CONCAT('%', :query, '%'))))) " +
                         "  AND (:action IS NULL OR a.action = :action) " +
                         "  AND (:entityName IS NULL OR :entityName = '' OR LOWER(a.entityName) = LOWER(:entityName))")
    Page<AuditLog> searchAndFilter(
            @org.springframework.data.repository.query.Param("query") String query,
            @org.springframework.data.repository.query.Param("action") AuditAction action,
            @org.springframework.data.repository.query.Param("entityName") String entityName,
            Pageable pageable);
}
