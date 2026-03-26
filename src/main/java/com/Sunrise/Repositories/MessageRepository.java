package com.Sunrise.Repositories;

import com.Sunrise.DTOs.DBResults.LastUserReadStatusResult;
import com.Sunrise.DTOs.Paginations.UserMessageDBResult;
import com.Sunrise.Entities.DBs.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {

    // ========== ОПЕРАЦИИ С СООБЩЕНИЯМИ ==========

    @Query(value = "SELECT * FROM get_first_messages(:chatId, :userId, :limit)", nativeQuery = true)
    List<UserMessageDBResult> getChatMessagesFirst(@Param("chatId") long chatId, @Param("userId") long userId, @Param("limit") long limit);

    @Query(value = "SELECT * FROM get_messages_before(:chatId, :userId, :messageId, :limit)", nativeQuery = true)
    List<UserMessageDBResult> getChatMessagesBefore(@Param("chatId") long chatId, @Param("userId") long userId, @Param("messageId") long messageId, @Param("limit") long limit);

    @Query(value = "SELECT * FROM get_messages_after(:chatId, :userId, :messageId, :limit)", nativeQuery = true)
    List<UserMessageDBResult> getChatMessagesAfter(@Param("chatId") long chatId, @Param("userId") long userId, @Param("messageId") long messageId, @Param("limit") long limit);

    @Query(value = "SELECT * FROM get_messages_after_with_gap(:chatId, :userId, :afterId, :lastKnownId, :limit, :maxGap)", nativeQuery = true)
    List<UserMessageDBResult> getMessagesWithGapCheckAfter(@Param("chatId") long chatId, @Param("userId") long userId, @Param("afterId") long afterMessageId, @Param("lastKnownId") Long lastKnownId, @Param("limit") long limit, @Param("maxGap") long maxGap);

    @Query(value = "SELECT * FROM get_messages_before_with_gap(:chatId, :userId, :beforeId, :lastKnownId, :limit, :maxGap)", nativeQuery = true)
    List<UserMessageDBResult> getMessagesWithGapCheckBefore(@Param("chatId") long chatId, @Param("userId") long userId, @Param("beforeId") long beforeMessageId, @Param("lastKnownId") Long lastKnownId, @Param("limit") long limit, @Param("maxGap") long maxGap);

    @Query(value = "SELECT * FROM get_messages_after_with_gap(:chatId, :userId, :messageId, :readAt)", nativeQuery = true)
    void markMessageAsRead(@Param("chatId") long chatId, @Param("userId") long userId, @Param("messageId") long messageId, @Param("readAt") LocalDateTime readAt);

    @Query(value = "SELECT * FROM user_chat_read_state state WHERE state.user_id = :userId AND state.chat_id IN (:chatIds)", nativeQuery = true)
    List<LastUserReadStatusResult> getUserReadStatusByChatIds(@Param("chatIds") Set<Long> chatIds, @Param("userId") Long userId);

    @Query(value = "SELECT last_read_message_id FROM user_chat_read_state state WHERE state.user_id = :userId AND state.chat_id = :chatId", nativeQuery = true)
    Optional<Long> getUserReadStatusByChatId(@Param("chatId") long chatId, @Param("userId") long userId);
}
