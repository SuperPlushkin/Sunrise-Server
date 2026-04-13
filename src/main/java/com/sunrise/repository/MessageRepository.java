package com.sunrise.repository;

import com.sunrise.core.dataservice.type.MessageReadStatusResult;
import com.sunrise.core.dataservice.type.UserMessageDBResult;
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

    @Query("""
           SELECT
               m.id AS id,
               m.chatId AS chatId,
               m.senderId AS senderId,
               m.text AS text,
               m.readCount AS readCount,
               (ucrs.lastReadMessageId IS NOT NULL AND m.id <= ucrs.lastReadMessageId) AS isReadByUser,
               m.sentAt AS sentAt,
               m.updatedAt as updatedAt,
               m.deletedAt as deletedAt,
               m.isDeleted AS isDeleted
           FROM Message m
           LEFT JOIN UserChatReadStatus ucrs
               ON ucrs.id.userId = :userId
               AND ucrs.id.chatId = m.chatId
           WHERE m.id = :messageId
           """)
    Optional<UserMessageDBResult> getMessageById(@Param("userId") long userId, @Param("messageId") long messageId);

    @Query("""
           SELECT
               m.id AS id,
               m.chatId AS chatId,
               m.senderId AS senderId,
               m.text AS text,
               m.readCount AS readCount,
               (ucrs.lastReadMessageId IS NOT NULL AND m.id <= ucrs.lastReadMessageId) AS isReadByUser,
               m.sentAt AS sentAt,
               m.updatedAt as updatedAt,
               m.deletedAt as deletedAt,
               m.isDeleted AS isDeleted
           FROM Message m
           LEFT JOIN UserChatReadStatus ucrs
               ON ucrs.id.userId = :userId
               AND ucrs.id.chatId = m.chatId
           WHERE m.chatId = :chatId
           ORDER BY m.id DESC
           """)
    List<UserMessageDBResult> getFirstMessagePage(@Param("chatId") long chatId, @Param("userId") long userId, Pageable pageable);

    @Query("""
           SELECT
               m.id AS id,
               m.chatId AS chatId,
               m.senderId AS senderId,
               m.text AS text,
               m.readCount AS readCount,
               (ucrs.lastReadMessageId IS NOT NULL AND m.id <= ucrs.lastReadMessageId) AS isReadByUser,
               m.sentAt AS sentAt,
               m.updatedAt as updatedAt,
               m.deletedAt as deletedAt,
               m.isDeleted AS isDeleted
           FROM Message m
           LEFT JOIN UserChatReadStatus ucrs
               ON ucrs.id.userId = :userId AND ucrs.id.chatId = m.chatId
           WHERE m.chatId = :chatId AND m.id < :cursor
           ORDER BY m.id DESC
           """)
    List<UserMessageDBResult> getMessagePageBefore(@Param("chatId") long chatId, @Param("userId") long userId, @Param("cursor") long cursor, Pageable pageable);

    @Query("""
           SELECT
               m.id AS id,
               m.chatId AS chatId,
               m.senderId AS senderId,
               m.text AS text,
               m.readCount AS readCount,
               (ucrs.lastReadMessageId IS NOT NULL AND m.id <= ucrs.lastReadMessageId) AS isReadByUser,
               m.sentAt AS sentAt,
               m.updatedAt as updatedAt,
               m.deletedAt as deletedAt,
               m.isDeleted AS isDeleted
           FROM Message m
           LEFT JOIN UserChatReadStatus ucrs
               ON ucrs.id.userId = :userId AND ucrs.id.chatId = m.chatId
           WHERE m.chatId = :chatId AND m.id > :cursor
           ORDER BY m.id ASC
           """)
    List<UserMessageDBResult> getMessagePageAfter(@Param("chatId") long chatId, @Param("userId") long userId, @Param("cursor") long cursor, Pageable pageable);


    @Modifying
    @Transactional
    @Query(value = "SELECT mark_messages_up_to_read(:chatId, :userId, :messageId, :readAt, CAST(:interval AS INTERVAL))", nativeQuery = true)
    void markMessagesUpToRead(@Param("chatId") long chatId, @Param("userId") long userId, @Param("messageId") long messageId,
                              @Param("readAt") LocalDateTime readAt, @Param("interval") String interval);

    @Modifying
    @Transactional
    @Query("UPDATE Message m SET m.text = :newText, m.updatedAt = :updatedAt WHERE m.id = :messageId")
    int updateMessage(@Param("messageId") long messageId, @Param("newText") String text, @Param("updatedAt") LocalDateTime updatedAt);

    @Modifying
    @Transactional
    @Query("UPDATE Message m SET m.isDeleted = false, m.deletedAt = null, m.updatedAt = :updatedAt WHERE m.id = :messageId")
    int restoreMessage(@Param("messageId") long messageId, @Param("updatedAt") LocalDateTime updatedAt);

    @Modifying
    @Transactional
    @Query("UPDATE Message m SET m.isDeleted = true, m.deletedAt = :updatedAt, m.updatedAt = :updatedAt WHERE m.id = :messageId")
    int deleteMessage(@Param("messageId") long messageId, @Param("updatedAt") LocalDateTime updatedAt);

    @Query("SELECT mrs.id.userId as userId, mrs.readAt as readAt FROM MessageReadStatus mrs WHERE mrs.id.messageId = :messageId ORDER BY mrs.readAt")
    List<MessageReadStatusResult> getMessageReaders(@Param("messageId") long messageId);
}