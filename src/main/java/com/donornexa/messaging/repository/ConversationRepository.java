package com.donornexa.messaging.repository;

import com.donornexa.common.enums.ConversationStatus;


import com.donornexa.messaging.entity.Conversation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ConversationRepository extends JpaRepository<Conversation, Long> {

    @Query("SELECT c FROM Conversation c " +
           "JOIN FETCH c.donor d " +
           "JOIN FETCH d.user du " +
           "JOIN FETCH c.hospital h " +
           "JOIN FETCH h.user hu " +
           "JOIN FETCH c.bloodRequest br " +
           "WHERE c.donor.id = :donorId " +
           "ORDER BY c.lastMessageAt DESC")
    List<Conversation> findByDonorIdWithDetails(@Param("donorId") Long donorId);

    @Query("SELECT c FROM Conversation c " +
           "JOIN FETCH c.donor d " +
           "JOIN FETCH d.user du " +
           "JOIN FETCH c.hospital h " +
           "JOIN FETCH h.user hu " +
           "JOIN FETCH c.bloodRequest br " +
           "WHERE c.hospital.id = :hospitalId " +
           "ORDER BY c.lastMessageAt DESC")
    List<Conversation> findByHospitalIdWithDetails(@Param("hospitalId") Long hospitalId);

    Optional<Conversation> findByDonorIdAndHospitalIdAndBloodRequestId(Long donorId, Long hospitalId, Long bloodRequestId);

    boolean existsByDonorIdAndHospitalIdAndBloodRequestId(Long donorId, Long hospitalId, Long bloodRequestId);

    @Query("SELECT c FROM Conversation c " +
           "JOIN FETCH c.donor d " +
           "JOIN FETCH d.user du " +
           "JOIN FETCH c.hospital h " +
           "JOIN FETCH h.user hu " +
           "JOIN FETCH c.bloodRequest br " +
           "ORDER BY c.lastMessageAt DESC")
    List<Conversation> findAllWithDetails();

    @Query("SELECT c FROM Conversation c " +
           "JOIN FETCH c.donor d " +
           "JOIN FETCH d.user du " +
           "JOIN FETCH c.hospital h " +
           "JOIN FETCH h.user hu " +
           "JOIN FETCH c.bloodRequest br " +
           "WHERE c.id = :id")
    Optional<Conversation> findByIdWithDetails(@Param("id") Long id);

    long countByDonorId(Long donorId);

    long countByHospitalId(Long hospitalId);

    long countByStatus(com.donornexa.common.enums.ConversationStatus status);
}
