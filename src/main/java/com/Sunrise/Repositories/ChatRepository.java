package com.Sunrise.Repositories;

import com.Sunrise.DTO.DBResults.ChatStatsDBResult;
import com.Sunrise.DTO.DBResults.GetChatMemberDBResult;
import com.Sunrise.DTO.DBResults.GetPersonalChatDBResult;
import com.Sunrise.Entities.DB.Chat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface ChatRepository extends JpaRepository<Chat, Long> {

    @Query("SELECT cm.id.chatId as chatId, cm.id.userId as userId, cm.isAdmin as isAdmin FROM ChatMember cm")
    List<GetChatMemberDBResult> getAllChatMembers();

    @Query("SELECT c.id as chatId, cm1.id.userId as userId1, cm2.id.userId as userId2 " +
            "FROM Chat c " +
            "JOIN ChatMember cm1 ON c.id = cm1.id.chatId AND cm1.isDeleted = false " +
            "JOIN ChatMember cm2 ON c.id = cm2.id.chatId AND cm2.isDeleted = false " +
            "WHERE c.isGroup = false AND cm1.id.userId < cm2.id.userId")
    List<GetPersonalChatDBResult> getAllPersonalChats();


    // ========== ДЕЙСТВИЯ С ИСТОРИЕЙ ЧАТОВ ==========


    // Удаление всех сообщений (для всех) | АДМИН
    @Query(value = "SELECT * FROM clear_chat_history_for_all(:chatId, :userId)", nativeQuery = true)
    Integer clearChatHistoryForAll(@Param("chatId") Long chatId, @Param("userId") Long userId);


    // Удаление всех сообщений (для себя)
    @Query(value = "SELECT * FROM clear_chat_history_for_self(:chatId, :userId)", nativeQuery = true)
    Integer clearChatHistoryForSelf(@Param("chatId") Long chatId, @Param("userId") Long userId);


    // ========== ОПЕРАЦИИ С ЧАТОМ ==========

    @Modifying
    @Transactional
    @Query(value = "INSERT INTO chat_members (chat_id, user_id, is_admin, joined_at) " +
            "VALUES (:chatId, :userId, :isAdmin, CURRENT_TIMESTAMP) " +
            "ON CONFLICT (chat_id, user_id) DO UPDATE SET is_deleted = false, joined_at = CURRENT_TIMESTAMP", nativeQuery = true)
    void upsertChatMember(@Param("chatId") Long chatId, @Param("userId") Long userId, @Param("isAdmin") Boolean isAdmin);

    @Modifying
    @Transactional
    @Query("UPDATE ChatMember cm SET cm.isDeleted = true WHERE cm.id.chatId = :chatId AND cm.id.userId = :userId")
    void leaveChat(@Param("chatId") Long chatId, @Param("userId") Long userId);

    @Modifying
    @Transactional
    @Query("UPDATE Chat c SET c.isDeleted = false WHERE c.id = :chatId")
    void restoreChat(@Param("chatId") Long chatId);

    @Modifying
    @Transactional
    @Query("UPDATE Chat c SET c.isDeleted = true WHERE c.id = :chatId")
    void deleteChat(@Param("chatId") Long chatId);

    @Query(value = "SELECT total_messages, hidden_for_all, hidden_by_user, can_clear_for_all " +
            "FROM get_chat_clear_stats(:chatId, :userId)", nativeQuery = true)
    ChatStatsDBResult getChatClearStats(@Param("chatId") Long chatId, @Param("userId") Long userId);

    @Modifying
    @Transactional
    @Query("UPDATE Chat c SET c.createdBy = :newCreatorId WHERE c.id = :chatId")
    void updateChatCreator(@Param("chatId") Long chatId, @Param("newCreatorId") Long newCreatorId);
}