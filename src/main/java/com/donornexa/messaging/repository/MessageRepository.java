package com.donornexa.messaging.repository;


import com.donornexa.messaging.entity.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {

    @Query("SELECT m FROM Message m " +
           "JOIN FETCH m.sender s " +
           "JOIN FETCH m.recipient r " +
           "WHERE m.conversation.id = :conversationId " +
           "ORDER BY m.sentAt ASC")
    List<Message> findByConversationIdWithSender(@Param("conversationId") Long conversationId);

    List<Message> findByConversationIdAndRecipientIdAndIsReadFalse(Long conversationId, Long recipientId);

    long countByRecipientIdAndIsReadFalse(Long recipientId);

    long countByIsReadFalse();

    long countByConversationIdAndRecipientIdAndIsReadFalse(Long conversationId, Long recipientId);

    Optional<Message> findTopByConversationIdOrderBySentAtDesc(Long conversationId);
}
