package com.Sunrise.Repositories;

import com.Sunrise.DTO.DBResults.ChatStatsResult;
import com.Sunrise.DTO.DBResults.GetChatMemberResult;
import com.Sunrise.DTO.DBResults.GetPersonalChatResult;
import com.Sunrise.DTO.DBResults.MessageResult;
import com.Sunrise.Entities.Chat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface ChatRepository extends JpaRepository<Chat, Long> {

    @Query("SELECT cm.chatId as chatId, cm.userId as userId, cm.isAdmin as isAdmin FROM ChatMember cm")
    List<GetChatMemberResult> getAllChatMembers();

    @Query("SELECT c.id as chatId, cm1.userId as userId1, cm2.userId as userId2 " +
            "FROM Chat c " +
            "JOIN ChatMember cm1 ON c.id = cm1.chatId AND cm1.isDeleted = false " +
            "JOIN ChatMember cm2 ON c.id = cm2.chatId AND cm2.isDeleted = false " +
            "WHERE c.isGroup = false " +
            "AND c.isDeleted = false AND cm1.userId < cm2.userId")
    List<GetPersonalChatResult> getAllPersonalChats();


    // ========== ДЕЙСТВИЯ С ИСТОРИЕЙ ЧАТОВ ==========


    // Удаление и восстановление определенного кол-во сообщений (для всех в чате) | АДМИН
    @Query(value = "SELECT * FROM hide_messages_for_all(:chatId, :userId, :limit)", nativeQuery = true)
    Integer hideMessagesForAll(@Param("chatId") Long chatId, @Param("userId") Long userId, @Param("limit") Integer limit);

    @Query(value = "SELECT * FROM restore_messages_for_all(:chatId, :userId, :limit)", nativeQuery = true)
    Integer restoreMessagesForAll(@Param("chatId") Long chatId, @Param("userId") Long userId, @Param("limit") Integer limit);


    // Удаление всех сообщений (для всех в чате) | АДМИН
    @Query(value = "SELECT * FROM clear_chat_history_for_all(:chatId, :userId)", nativeQuery = true)
    Integer clearChatHistoryForAll(@Param("chatId") Long chatId, @Param("userId") Long userId);


    // Удаление и восстановление всех сообщений (для себя в чате)
    @Query(value = "SELECT * FROM clear_chat_history_for_self(:chatId, :userId)", nativeQuery = true)
    Integer clearChatHistoryForSelf(@Param("chatId") Long chatId, @Param("userId") Long userId);

    @Query(value = "SELECT * FROM restore_chat_history_for_self(:chatId, :userId)", nativeQuery = true)
    Integer restoreChatHistoryForSelf(@Param("chatId") Long chatId, @Param("userId") Long userId);


    // ========== ОПЕРАЦИИ С СООБЩЕНИЯМИ ==========

    @Query(value = "SELECT message_id, sender_id, sender_username, text, sent_at, read_count, is_hidden_by_user, is_hidden_by_admin " +
            "FROM get_chat_messages(:chatId, :userId, :limit, :offset)", nativeQuery = true)
    List<MessageResult> getChatMessages(@Param("chatId") Long chatId, @Param("userId") Long userId,
                                        @Param("limit") Integer limit, @Param("offset") Integer offset);

    @Modifying
    @Transactional
    @Query(value = "INSERT INTO message_read_status (message_id, user_id, read_at) " +
            "VALUES (:messageId, :userId, NOW()) ON CONFLICT (message_id, user_id) DO NOTHING", nativeQuery = true)
    void markMessageAsRead(@Param("messageId") Long messageId, @Param("userId") Long userId);

    @Query("SELECT COUNT(m) FROM Message m WHERE m.chatId = :chatId AND m.hiddenByAdmin = false " +
            "AND m.id NOT IN (SELECT uhm.messageId FROM UserHiddenMessage uhm WHERE uhm.userId = :userId)")
    Integer getVisibleMessagesCount(@Param("chatId") Long chatId, @Param("userId") Long userId);


    // ========== ОПЕРАЦИИ С ЧАТОМ ==========

    @Query(value = "SELECT c.id FROM chats c " +
            "JOIN chat_members cm1 ON c.id = cm1.chat_id AND cm1.user_id = :user1Id AND cm1.is_deleted = FALSE " +
            "JOIN chat_members cm2 ON c.id = cm2.chat_id AND cm2.user_id = :user2Id AND cm2.is_deleted = FALSE " +
            "WHERE c.is_group = FALSE AND c.is_deleted = FALSE LIMIT 1", nativeQuery = true)
    Long findExistingPersonalChat(@Param("user1Id") Long user1Id, @Param("user2Id") Long user2Id);

    @Modifying
    @Transactional
    @Query(value = "INSERT INTO chat_members (chat_id, user_id, is_admin, joined_at) " +
            "VALUES (:chatId, :userId, :isAdmin, CURRENT_TIMESTAMP) " +
            "ON CONFLICT (chat_id, user_id) DO UPDATE SET is_deleted = false, joined_at = CURRENT_TIMESTAMP", nativeQuery = true)
    void upsertChatMember(@Param("chatId") Long chatId, @Param("userId") Long userId, @Param("isAdmin") Boolean isAdmin);

    @Modifying
    @Transactional
    @Query("UPDATE ChatMember cm SET cm.isDeleted = true WHERE cm.chatId = :chatId AND cm.userId = :userId")
    void leaveChat(@Param("chatId") Long chatId, @Param("userId") Long userId);

    @Modifying
    @Transactional
    @Query("UPDATE Chat c SET c.isDeleted = true WHERE c.id = :chatId")
    void deleteChat(@Param("chatId") Long chatId);

    @Query(value = "SELECT COUNT(*) FROM chat_members WHERE chat_id = :chatId AND is_deleted = false", nativeQuery = true)
    Integer getChatMemberCount(@Param("chatId") Long chatId);

    @Query(value = "SELECT total_messages, hidden_for_all, hidden_by_user, can_clear_for_all " +
            "FROM get_chat_clear_stats(:chatId, :userId)", nativeQuery = true)
    ChatStatsResult getChatClearStats(@Param("chatId") Long chatId, @Param("userId") Long userId);

    @Query("SELECT c.createdBy FROM Chat c WHERE c.id = :chatId")
    Long getChatCreator(@Param("chatId") Long chatId);

    @Query(value = "SELECT cm.user_id FROM chat_members cm " +
            "WHERE cm.chat_id = :chatId AND cm.user_id != :userId AND cm.is_admin = true AND cm.is_deleted = false " +
            "ORDER BY cm.joined_at ASC LIMIT 1", nativeQuery = true)
    Long findAnotherAdmin(@Param("chatId") Long chatId, @Param("userId") Long userId);

    @Modifying
    @Transactional
    @Query("UPDATE Chat c SET c.createdBy = :newCreatorId WHERE c.id = :chatId")
    void updateChatCreator(@Param("chatId") Long chatId, @Param("newCreatorId") Long newCreatorId);


    // ========== ПРОСТЫЕ ПРОВЕРКИ ==========

    @Query(value = "SELECT EXISTS( SELECT 1 FROM chat_members cm" +
            "WHERE cm.chat_id = :chatId AND cm.user_id = :userId AND cm.is_deleted = false)", nativeQuery = true)
    Boolean isChatMember(@Param("chatId") Long chatId, @Param("userId") Long userId);

    @Query(value = "SELECT COALESCE((" +
            "SELECT cm.is_admin FROM chat_members cm " +
            "WHERE cm.chat_id = :chatId AND cm.user_id = :userId AND cm.is_deleted = false" +
            "), false)", nativeQuery = true)
    Boolean isChatAdmin(@Param("chatId") Long chatId, @Param("userId") Long userId);

    @Query("SELECT c.isGroup FROM Chat c WHERE c.id = :chatId AND c.isDeleted = false")
    Boolean isGroupChat(@Param("chatId") Long chatId);
}