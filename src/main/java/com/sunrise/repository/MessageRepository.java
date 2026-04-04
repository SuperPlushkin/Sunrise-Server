package com.sunrise.repository;

import com.sunrise.core.dataservice.type.MessageDBResult;
import com.sunrise.entity.db.Message;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {

    // ========== ОПЕРАЦИИ С СООБЩЕНИЯМИ ==========

    @Query(value = "SELECT * FROM get_messages_by_ids(:chatId, :messageIds)", nativeQuery = true)
    List<MessageDBResult> findMessagesById(@Param("chatId") long chatId, @Param("messageIds") Long[] messageIds);

    @Query("SELECT m.id FROM Message m WHERE m.chatId = :chatId ORDER BY m.id DESC")
    List<Long> findFirstMessageIds(@Param("chatId") long chatId, Pageable pageable);

    @Query("SELECT m.id FROM Message m WHERE m.chatId = :chatId AND m.id < :cursor ORDER BY m.id DESC")
    List<Long> findMessageIdsBefore(@Param("chatId") long chatId, @Param("cursor") long cursor, Pageable pageable);

    @Query("SELECT m.id FROM Message m WHERE m.chatId = :chatId AND m.id > :cursor ORDER BY m.id ASC")
    List<Long> findMessageIdsAfter(@Param("chatId") long chatId, @Param("cursor") long cursor, Pageable pageable);


    @Query(value = "SELECT * FROM mark_message_read(:chatId, :userId, :messageId, :readAt)", nativeQuery = true)
    void markMessageAsRead(@Param("chatId") long chatId, @Param("userId") long userId, @Param("messageId") long messageId, @Param("readAt") LocalDateTime readAt);

    @Modifying
    @Transactional
    @Query("UPDATE Message m SET m.hiddenByAdmin = true WHERE m.id = :messageId")
    void restoreMessage(@Param("messageId") long messageId);

    @Modifying
    @Transactional
    @Query("UPDATE Message m SET m.hiddenByAdmin = true WHERE m.id = :messageId")
    void deleteMessage(@Param("messageId") long messageId);

    @Query(value = "SELECT last_read_message_id FROM user_chat_read_state state WHERE state.user_id = :userId AND state.chat_id = :chatId", nativeQuery = true)
    Optional<Long> getUserReadStatusByChatId(@Param("chatId") long chatId, @Param("userId") long userId);
}
