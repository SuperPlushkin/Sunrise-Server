package com.Sunrise.Repositories;

import com.Sunrise.DTOs.DBResults.ChatStatsDBResult;
import com.Sunrise.DTOs.DBResults.FullChatResult;
import com.Sunrise.DTOs.Paginations.UserFullChatResult;
import com.Sunrise.Entities.DBs.Chat;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface ChatRepository extends JpaRepository<Chat, Long> {


    // ========== ОПЕРАЦИИ С ЧАТОМ ==========


    @Query(value = "SELECT create_personal_chat(:chatId, :user1Id, :user2Id, :createdAt)", nativeQuery = true)
    void createPersonalChat(@Param("chatId") long chatId, @Param("user1Id") long user1Id,
                            @Param("user2Id") long user2Id, @Param("createdAt") LocalDateTime createdAt);

    @Query(value = "SELECT create_group_chat(:chatId, :name, :creatorId, :memberIds, :createdAt)", nativeQuery = true)
    void createGroupChat(@Param("chatId") long chatId, @Param("name") String name,
                         @Param("creatorId") long creatorId, @Param("memberIds") Long[] memberIds,
                         @Param("createdAt") LocalDateTime createdAt);

    @Modifying
    @Transactional
    @Query("UPDATE Chat c SET c.isDeleted = false, c.deletedAt = null WHERE c.id = :chatId")
    void restoreChat(@Param("chatId") long chatId);

    @Modifying
    @Transactional
    @Query("UPDATE Chat c SET c.isDeleted = true, c.deletedAt = CURRENT_TIMESTAMP WHERE c.id = :chatId")
    void deleteChat(@Param("chatId") long chatId);


    // ========== ПОИСК ==========


    @Query(value = "SELECT * FROM get_full_chat(:chatId)", nativeQuery = true)
    Optional<FullChatResult> findFullChat(@Param("chatId") long chatId);

    @Query(value = "SELECT * FROM get_full_personal_chat(:userId1, :userId2)", nativeQuery = true)
    Optional<FullChatResult> findPersonalChat(@Param("userId1") long userId1, @Param("userId2") long userId2);

    @Query(value = "SELECT * FROM get_full_chats_batch(:chatId, :userId)", nativeQuery = true)
    List<UserFullChatResult> findFullChats(@Param("chatIds") Set<Long> chatIds, @Param("userId") long userId);

    @Query(value = "SELECT * FROM get_user_chats_page(:user_id, :cursor, :limit)", nativeQuery = true)
    List<UserFullChatResult> getUserChatsPage(@Param("user_id") long userId, @Param("cursor") Long cursor, @Param("limit") int limit);


    // ========== ДЕЙСТВИЯ С ИСТОРИЕЙ ЧАТОВ ==========


    // Статистика
    @Query(value = "SELECT * FROM get_chat_clear_stats(:chatId, :userId)", nativeQuery = true)
    ChatStatsDBResult getChatClearStats(@Param("chatId") long chatId, @Param("userId") long userId);
}