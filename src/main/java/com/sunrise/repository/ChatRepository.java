package com.sunrise.repository;

import com.sunrise.core.dataservice.type.ChatStatsDBResult;
import com.sunrise.core.dataservice.type.ChatType;
import com.sunrise.core.dataservice.type.UserChatResult;
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

    @Modifying
    @Transactional
    @Query(value = "SELECT create_personal_chat_with_members(:chatId, :chatType, :user1Id, :user2Id, :createdAt)", nativeQuery = true)
    void savePersonalChatAndMembers(@Param("chatId") long chatId, @Param("chatType") String chatType,
                                    @Param("user1Id") long user1Id, @Param("user2Id") long user2Id,
                                    @Param("createdAt") LocalDateTime createdAt);


    @Modifying
    @Transactional
    @Query(value = "SELECT create_group_chat_with_members(:chatId, :name, :description, :chatType, :memberIds, :isAdminFlags, :creatorId, :createdAt)", nativeQuery = true)
    void saveGroupChatAndMembers(@Param("chatId") long chatId, @Param("name") String name, @Param("description") String description,
                                 @Param("chatType") String chatType,
                                 @Param("memberIds") Long[] memberIds, @Param("isAdminFlags") Boolean[] isAdminFlags,
                                 @Param("creatorId") long creatorId, @Param("createdAt") LocalDateTime createdAt);

    @Modifying
    @Transactional
    @Query("UPDATE Chat c SET c.name = :name, c.description = :description, c.updatedAt = :updatedAt WHERE c.id = :chatId")
    int updateChatInfo(@Param("chatId") long chatId, @Param("name") String chatName, @Param("description") String chatDescription, @Param("updatedAt") LocalDateTime updatedAt);

    @Modifying
    @Transactional
    @Query("UPDATE Chat c SET c.chatType = :chatType, c.updatedAt = :updatedAt WHERE c.id = :chatId")
    int updateChatType(@Param("chatId") long chatId, @Param("chatType") String chatType, @Param("updatedAt") LocalDateTime updatedAt);

    @Modifying
    @Transactional
    @Query("UPDATE Chat c SET c.isDeleted = false, c.deletedAt = null, c.updatedAt = :updatedAt WHERE c.id = :chatId")
    int restoreChat(@Param("chatId") long chatId, @Param("updatedAt") LocalDateTime updatedAt);

    @Modifying
    @Transactional
    @Query("UPDATE Chat c SET c.isDeleted = true, c.deletedAt = :updatedAt, c.updatedAt = :updatedAt WHERE c.id = :chatId")
    int deleteChat(@Param("chatId") long chatId, @Param("updatedAt") LocalDateTime updatedAt);


    // ========== ПОИСК ==========



    @Query("""
           SELECT c
           FROM Chat c
           INNER JOIN ChatMember cm1 ON cm1.id.chatId = c.id AND cm1.id.userId = :userId1 AND cm1.isDeleted = false
           INNER JOIN ChatMember cm2 ON cm2.id.chatId = c.id AND cm2.id.userId = :userId2 AND cm2.isDeleted = false
           WHERE c.chatType = :chatType
           """)
    Optional<Chat> getPersonalChat(@Param("userId1") long userId1, @Param("userId2") long userId2, @Param("chatType") ChatType chatType);

    @Query(value = "SELECT * FROM get_user_chats_page(:user_id, :isPinnedCursor, :lastMsgIdCursor, :chatIdCursor, , :limit)", nativeQuery = true)
    List<UserChatResult> getUserChatsPage(@Param("user_id") long userId, @Param("isPinnedCursor") Boolean isPinnedCursor, @Param("lastMsgIdCursor") Long lastMsgIdCursor, @Param("chatIdCursor") Long chatIdCursor, @Param("limit") int limit);

    @Query(value = "SELECT * FROM get_chat_by_id(:chatId, :userId)", nativeQuery = true)
    Optional<UserChatResult> getUserChat(@Param("chatId") long chatId, @Param("userId") long userId);


    // ========== ДЕЙСТВИЯ С ИСТОРИЕЙ ЧАТОВ ==========


    // Статистика
    @Query(value = "SELECT * FROM get_chat_clear_stats(:chatId, :userId)", nativeQuery = true)
    ChatStatsDBResult getChatClearStats(@Param("chatId") long chatId, @Param("userId") long userId);
}