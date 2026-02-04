package com.Sunrise.Repositories;

import com.Sunrise.DTO.DBResults.MessageResult;
import com.Sunrise.Entities.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {

    // ========== ОПЕРАЦИИ С СООБЩЕНИЯМИ ==========
    @Query(value = "SELECT message_id, sender_id, sender_username, text, sent_at, read_count, is_read_by_user, is_hidden_by_user, is_hidden_by_admin " +
            "FROM get_first_messages(:chatId, :userId, :limit)", nativeQuery = true)
    List<MessageResult> getChatMessagesFirst(@Param("chatId") Long chatId, @Param("userId") Long userId, @Param("limit") Integer limit);

    @Query(value = "SELECT message_id, sender_id, sender_username, text, sent_at, read_count, is_read_by_user, is_hidden_by_user, is_hidden_by_admin " +
            "FROM get_messages_before(:chatId, :userId, :messageId, :limit)", nativeQuery = true)
    List<MessageResult> getChatMessagesBefore(@Param("chatId") Long chatId, @Param("userId") Long userId, @Param("messageId") Long messageId, @Param("limit") Integer limit);

    @Query(value = "SELECT message_id, sender_id, sender_username, text, sent_at, read_count, is_read_by_user, is_hidden_by_user, is_hidden_by_admin " +
            "FROM get_messages_after(:chatId, :userId, :messageId, :limit)", nativeQuery = true)
    List<MessageResult> getChatMessagesAfter(@Param("chatId") Long chatId, @Param("userId") Long userId, @Param("messageId") Long messageId, @Param("limit") Integer limit);

    @Modifying
    @Transactional
    @Query(value = "INSERT INTO message_read_status (message_id, user_id, read_at) " +
            "VALUES (:messageId, :userId, NOW()) ON CONFLICT (message_id, user_id) DO NOTHING", nativeQuery = true)
    void markMessageAsRead(@Param("messageId") Long messageId, @Param("userId") Long userId);

    @Query("SELECT COUNT(m) FROM Message m WHERE m.chatId = :chatId AND m.hiddenByAdmin = false " +
            "AND m.id NOT IN (SELECT uhm.messageId FROM UserHiddenMessage uhm WHERE uhm.userId = :userId)")
    Integer getVisibleMessagesCount(@Param("chatId") Long chatId, @Param("userId") Long userId);
}
