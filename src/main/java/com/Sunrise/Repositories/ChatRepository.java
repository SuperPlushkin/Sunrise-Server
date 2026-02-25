package com.Sunrise.Repositories;

import com.Sunrise.DTO.DBResults.ChatStatsDBResult;
import com.Sunrise.Entities.DB.Chat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChatRepository extends JpaRepository<Chat, Long> {


    // ========== ОПЕРАЦИИ С ЧАТОМ ==========


    @Query(value = "SELECT create_personal_chat(:chatId, :user1Id, :user2Id)", nativeQuery = true)
    void createPersonalChat(@Param("chatId") Long chatId, @Param("user1Id") Long user1Id, @Param("user2Id") Long user2Id);

    @Query(value = "SELECT create_group_chat(:chatId, :name, :creatorId, :memberIds)", nativeQuery = true)
    void createGroupChat(@Param("chatId") Long chatId, @Param("name") String name,
                         @Param("creatorId") Long creatorId, @Param("memberIds") Long[] memberIds);

    @Modifying
    @Transactional
    @Query("UPDATE Chat c SET c.createdBy = :newCreatorId WHERE c.id = :chatId")
    void updateChatCreator(@Param("chatId") Long chatId, @Param("newCreatorId") Long newCreatorId);

    @Modifying
    @Transactional
    @Query("UPDATE Chat c SET c.isDeleted = false, c.deletedAt = null WHERE c.id = :chatId")
    void restoreChat(@Param("chatId") Long chatId);

    @Modifying
    @Transactional
    @Query("UPDATE Chat c SET c.isDeleted = true, c.deletedAt = CURRENT_TIMESTAMP WHERE c.id = :chatId")
    void deleteChat(@Param("chatId") Long chatId);

    @Query(value = "SELECT sync_chat_counters(:chatId)", nativeQuery = true)
    void syncChatCounters(@Param("chatId") Long chatId);

    @Query(value = "SELECT COUNT(*) FROM chats WHERE is_deleted = false", nativeQuery = true)
    int countActiveChats();


    // ========== ПОИСК ==========


    @Query("SELECT c FROM Chat c WHERE c.id IN " +
            "(SELECT cm.id.chatId FROM ChatMember cm WHERE cm.id.userId = :userId AND cm.isDeleted = false)")
    List<Chat> findUserChats(@Param("userId") Long userId);

    @Query("SELECT c FROM Chat c " +
            "WHERE c.isGroup = false AND c.isDeleted = false AND EXISTS (" +
            "   SELECT cm1 FROM ChatMember cm1 WHERE cm1.id.chatId = c.id " +
            "   AND cm1.id.userId = :userId1 AND cm1.isDeleted = false" +
            ") AND EXISTS (" +
            "   SELECT cm2 FROM ChatMember cm2 WHERE cm2.id.chatId = c.id " +
            "   AND cm2.id.userId = :userId2 AND cm2.isDeleted = false" +
            ")")
    Optional<Chat> findPersonalChat(@Param("userId1") Long userId1, @Param("userId2") Long userId2);

    @Query("SELECT c.id FROM Chat c WHERE c.isDeleted = false")
    List<Long> findAllActiveChatIds();

    @Query("SELECT c.id FROM Chat c WHERE c.createdBy = :userId AND c.isDeleted = false")
    List<Long> findChatIdsByCreator(@Param("userId") Long userId);


    // ========== ДЕЙСТВИЯ С ИСТОРИЕЙ ЧАТОВ ==========


    // Удаление всех сообщений (для всех) | админ
    @Query(value = "SELECT * FROM clear_chat_history_for_all(:chatId, :userId)", nativeQuery = true)
    Integer clearChatHistoryForAll(@Param("chatId") Long chatId, @Param("userId") Long userId);


    // Удаление всех сообщений (для себя)
    @Query(value = "SELECT * FROM clear_chat_history_for_self(:chatId, :userId)", nativeQuery = true)
    Integer clearChatHistoryForSelf(@Param("chatId") Long chatId, @Param("userId") Long userId);


    // Статистика
    @Query(value = "SELECT total_messages, hidden_for_all, hidden_by_user, can_clear_for_all " +
            "FROM get_chat_clear_stats(:chatId, :userId)", nativeQuery = true)
    ChatStatsDBResult getChatClearStats(@Param("chatId") Long chatId, @Param("userId") Long userId);
}