package com.sunrise.repository;

import com.sunrise.core.dataservice.type.ChatStatsDBResult;
import com.sunrise.core.dataservice.type.UserFullChatResult;
import com.sunrise.entity.db.Chat;

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
public interface ChatRepository extends JpaRepository<Chat, Long> {


    // ========== ОПЕРАЦИИ С ЧАТОМ ==========

    @Transactional
    @Query(value = "SELECT create_personal_chat(:chatId, :user1Id, :user2Id, :createdAt)", nativeQuery = true)
    void savePersonalChatAndMembers(@Param("chatId") long chatId, @Param("user1Id") long user1Id,
                                    @Param("user2Id") long user2Id, @Param("createdAt") LocalDateTime createdAt);


    @Transactional
    @Query(value = "SELECT create_group_chat_with_members(:chatId, :name, :memberIds, :isAdminFlags, :creatorId, :createdAt)", nativeQuery = true)
    void saveGroupChatAndMembers(@Param("chatId") long chatId, @Param("name") String name,
                                 @Param("memberIds") Long[] memberIds, @Param("isAdminFlags") Boolean[] isAdminFlags,
                                 @Param("creatorId") long creatorId, @Param("createdAt") LocalDateTime createdAt);
    @Modifying
    @Transactional
    @Query("UPDATE Chat c SET c.isDeleted = false, c.deletedAt = null WHERE c.id = :chatId")
    int restoreChat(@Param("chatId") long chatId);

    @Modifying
    @Transactional
    @Query("UPDATE Chat c SET c.isDeleted = true, c.deletedAt = CURRENT_TIMESTAMP WHERE c.id = :chatId")
    int deleteChat(@Param("chatId") long chatId);


    // ========== ПОИСК ==========



    @Query("""
           SELECT c
           FROM Chat c
           INNER JOIN ChatMember cm1 ON cm1.id.chatId = c.id AND cm1.id.userId = :userId1 AND cm1.isDeleted = false
           INNER JOIN ChatMember cm2 ON cm2.id.chatId = c.id AND cm2.id.userId = :userId2 AND cm2.isDeleted = false
           WHERE c.isGroup = false
           """)
    Optional<Chat> getPersonalChat(@Param("userId1") long userId1, @Param("userId2") long userId2);

    @Query(value = "SELECT * FROM get_user_chats_page(:user_id, :cursor, :limit)", nativeQuery = true)
    List<UserFullChatResult> getUserChatsPage(@Param("user_id") long userId, @Param("cursor") Long cursor, @Param("limit") int limit);


    // ========== ДЕЙСТВИЯ С ИСТОРИЕЙ ЧАТОВ ==========


    // Статистика
    @Query(value = "SELECT * FROM get_chat_clear_stats(:chatId, :userId)", nativeQuery = true)
    ChatStatsDBResult getChatClearStats(@Param("chatId") long chatId, @Param("userId") long userId);
}