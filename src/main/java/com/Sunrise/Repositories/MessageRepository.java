package com.Sunrise.Repositories;

import com.Sunrise.DTO.DBResults.MessageDBResult;
import com.Sunrise.Entities.DB.Message;
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

    @Query(value = "SELECT * FROM get_messages_in_range(:chatId, :userId, :fromId, :toId)", nativeQuery = true)
    List<MessageDBResult> getChatMessagesInRange(@Param("chatId") Long chatId, @Param("userId") Long userId, @Param("fromId") Long fromId, @Param("toId") Long toId);

    @Query(value = "SELECT * FROM get_first_messages(:chatId, :userId, :limit)", nativeQuery = true)
    List<MessageDBResult> getChatMessagesFirst(@Param("chatId") Long chatId, @Param("userId") Long userId, @Param("limit") Integer limit);

    @Query(value = "SELECT * FROM get_messages_before(:chatId, :userId, :messageId, :limit)", nativeQuery = true)
    List<MessageDBResult> getChatMessagesBefore(@Param("chatId") Long chatId, @Param("userId") Long userId, @Param("messageId") Long messageId, @Param("limit") Integer limit);

    @Query(value = "SELECT * FROM get_messages_after(:chatId, :userId, :messageId, :limit)", nativeQuery = true)
    List<MessageDBResult> getChatMessagesAfter(@Param("chatId") Long chatId, @Param("userId") Long userId, @Param("messageId") Long messageId, @Param("limit") Integer limit);

    @Query(value = "SELECT * FROM get_messages_after_smart(:chatId, :userId, :afterId, :lastKnownId, :limit, :maxGap)", nativeQuery = true)
    List<MessageDBResult> getChatMessagesAfterAndSomeBefore(@Param("chatId") Long chatId, @Param("userId") Long userId, @Param("afterId") Long afterMessageId, @Param("lastKnownId") Long lastKnownId, @Param("limit") Integer limit, @Param("maxGap") Integer maxGap);

    @Modifying
    @Transactional
    @Query(value = "INSERT INTO message_read_status (message_id, user_id, read_at) " +
            "VALUES (:messageId, :userId, NOW()) ON CONFLICT (message_id, user_id) DO NOTHING", nativeQuery = true)
    void markMessageAsRead(@Param("messageId") Long messageId, @Param("userId") Long userId);

    @Query("SELECT COUNT(m) FROM Message m WHERE m.chatId = :chatId AND m.hiddenByAdmin = false")
    int getVisibleMessagesCount(@Param("chatId") Long chatId, @Param("userId") Long userId);
}
