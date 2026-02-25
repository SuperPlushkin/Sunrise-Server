package com.Sunrise.Repositories;

import com.Sunrise.DTO.DBResults.ChatMembersPageResult;
import com.Sunrise.DTO.DBResults.ChatsPageResult;
import com.Sunrise.Entities.DB.ChatMember;
import com.Sunrise.Entities.DB.ChatMemberId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface ChatMemberRepository extends JpaRepository<ChatMember, ChatMemberId> {

    @Modifying
    @Transactional
    @Query(value = "SELECT add_or_restore_chat_member(:chatId, :userId, :isAdmin)", nativeQuery = true)
    void addChatMember(@Param("chatId") Long chatId, @Param("userId") Long userId, @Param("isAdmin") Boolean isAdmin);

    @Modifying
    @Transactional
    @Query(value = "SELECT remove_chat_member(:chatId, :userId)", nativeQuery = true)
    void removeChatMember(@Param("chatId") Long chatId, @Param("userId") Long userId);

    @Query("SELECT cm FROM ChatMember cm WHERE cm.id.chatId = :chatId")
    List<ChatMember> getChatMembers(@Param("chatId") Long chatId);

    @Query("SELECT cm.isAdmin FROM ChatMember cm " +
            "WHERE cm.id.chatId = :chatId AND cm.id.userId = :userId AND cm.isDeleted = false")
    Optional<Boolean> isChatAdmin(@Param("chatId") Long chatId, @Param("userId") Long userId);

    @Query("SELECT EXISTS (SELECT 1 FROM ChatMember cm " +
            "WHERE cm.id.chatId = :chatId AND cm.id.userId = :userId AND cm.isDeleted = false)")
    boolean isUserInChat(@Param("chatId") Long chatId, @Param("userId") Long userId);

    @Query("SELECT cm.id.userId FROM ChatMember cm " +
            "WHERE cm.id.chatId = :chatId AND cm.isAdmin = true AND cm.isDeleted = false " +
            "AND cm.id.userId != :excludeUserId")
    Optional<Long> findAnotherAdminId(@Param("chatId") Long chatId, @Param("excludeUserId") Long excludeUserId);

    @Query("SELECT cm FROM ChatMember cm " +
            "WHERE cm.id.chatId = :chatId AND cm.isAdmin = true AND cm.isDeleted = false " +
            "AND cm.id.userId != :excludeUserId")
    Optional<ChatMember> findAnotherAdmin(@Param("chatId") Long chatId, @Param("excludeUserId") Long excludeUserId);

    @Query("SELECT cm.id.chatId FROM ChatMember cm WHERE cm.id.userId = :userId AND cm.isDeleted = false")
    List<Long> getUserChatIds(@Param("userId") Long userId);

    @Query(value = "SELECT * FROM get_chat_members_page(:chatId, :offset, :limit)", nativeQuery = true)
    ChatMembersPageResult getChatMembersPage(@Param("chatId") Long chatId, @Param("offset") int offset, @Param("limit") int limit);

    @Query(value = "SELECT * FROM get_user_chats_page(:userId, :offset, :limit)", nativeQuery = true)
    ChatsPageResult getUserChatsPage(@Param("userId") Long userId, @Param("offset") int offset, @Param("limit") int limit);

    @Query("SELECT cm.id.userId FROM ChatMember cm WHERE cm.id.chatId = :chatId")
    List<Long> findChatMemberIds(@Param("chatId") Long chatId);

    @Query("SELECT cm FROM ChatMember cm " +
            "WHERE cm.id.chatId = :chatId AND cm.id.userId IN :userIds")
    List<ChatMember> findChatMembersByIds(@Param("chatId") Long chatId, @Param("userIds") List<Long> userIds);

    @Query(value = "SELECT cm.chat_id FROM chat_members cm " +
                    "WHERE cm.user_id = :userId " +
                    "AND cm.is_deleted = false AND cm.chat_id NOT IN :chatIds", nativeQuery = true)
    List<Long> findMissingUserChatIds(@Param("userId") Long userId, @Param("chatIds") Set<Long> chatIds);

    @Query(value = "SELECT COUNT(*) FROM chat_members WHERE chat_id = :chatId AND is_deleted = false",
            nativeQuery = true)
    int countChatMembers(@Param("chatId") Long chatId);

    @Query("SELECT COUNT(cm) FROM ChatMember cm " +
            "WHERE cm.id.chatId = :chatId AND cm.isDeleted = false")
    int countActiveMembers(@Param("chatId") Long chatId);
}
