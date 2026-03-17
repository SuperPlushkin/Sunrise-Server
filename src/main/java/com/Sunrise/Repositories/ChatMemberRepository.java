package com.Sunrise.Repositories;

import com.Sunrise.DTO.DBResults.ChatMemberResult;
import com.Sunrise.DTO.DBResults.ChatMembersPageResult;
import com.Sunrise.DTO.DBResults.ChatOpponentResult;
import com.Sunrise.DTO.DBResults.ChatsPageResult;
import com.Sunrise.Entities.DB.ChatMember;
import com.Sunrise.Entities.DB.ChatMemberId;
import com.Sunrise.Entities.DB.User;
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



    @Query("SELECT cm.id.userId FROM ChatMember cm " +
            "WHERE cm.id.chatId = :chatId AND cm.isAdmin = true AND cm.isDeleted = false " +
            "AND cm.id.userId != :excludeUserId")
    Optional<Long> findAnotherAdminId(@Param("chatId") Long chatId, @Param("excludeUserId") Long excludeUserId);

    @Query("SELECT cm FROM ChatMember cm " +
            "WHERE cm.id.chatId = :chatId AND cm.isAdmin = true AND cm.isDeleted = false " +
            "AND cm.id.userId != :excludeUserId")
    Optional<ChatMember> findAnotherActiveAdmin(@Param("chatId") Long chatId, @Param("excludeUserId") Long excludeUserId);

    @Query("SELECT cm.id.chatId FROM ChatMember cm WHERE cm.id.userId = :userId AND cm.isDeleted = false")
    List<Long> getUserChatIds(@Param("userId") Long userId);

    @Query(value = "SELECT * FROM get_chat_members_page(:chatId, :offset, :limit)", nativeQuery = true)
    ChatMembersPageResult getChatMembersPage(@Param("chatId") Long chatId, @Param("offset") int offset, @Param("limit") int limit);

    @Query(value = "SELECT * FROM get_user_chats_page(:userId, :offset, :limit)", nativeQuery = true)
    ChatsPageResult getUserChatsPage(@Param("userId") Long userId, @Param("offset") int offset, @Param("limit") int limit);




    @Query("SELECT cm.id.userId FROM ChatMember cm WHERE cm.id.chatId = :chatId")
    List<Long> findChatMemberIds(@Param("chatId") Long chatId);

    @Query("SELECT cm FROM ChatMember cm WHERE cm.id.chatId = :chatId AND cm.isDeleted = false")
    List<ChatMember> findActiveChatMembers(@Param("chatId") Long chatId);

    @Query("SELECT cm FROM ChatMember cm " +
            "WHERE cm.id.chatId = :chatId AND cm.isDeleted = false AND cm.id.userId != :excludeUserId")
    List<ChatMember> findActiveChatMemberExcludeOne(@Param("chatId") Long chatId, @Param("excludeUserId") Long excludeUserId);

    @Query(value = "SELECT DISTINCT ON (cm.chat_id) cm.* " +
            "FROM chat_members cm " +
            "WHERE cm.chat_id IN :chatIds " +
            "AND cm.is_deleted = false " +
            "AND cm.user_id != :excludeUserId " +
            "ORDER BY cm.chat_id, cm.joined_at DESC",
            nativeQuery = true)
    List<ChatMember> findOneActiveMemberPerChatExcluding(@Param("chatIds") List<Long> chatIds, @Param("excludeUserId") Long excludeUserId);

    @Query("SELECT cm.id.chatId as chatId, u as user FROM ChatMember cm " +
            "JOIN User u ON u.id = cm.id.userId " +
            "WHERE cm.id.chatId IN :chatIds " +
            "AND cm.id.userId != :currentUserId " +
            "AND cm.isDeleted = false")
    List<ChatOpponentResult> findOpponentsForChats(@Param("chatIds") Set<Long> chatIds, @Param("currentUserId") Long currentUserId);

    @Query("SELECT cm FROM ChatMember cm " +
            "WHERE cm.id.chatId = :chatId AND cm.id.userId IN :userIds AND cm.isDeleted = false")
    List<ChatMember> findActiveChatMembersByIds(@Param("chatId") Long chatId, @Param("userIds") List<Long> userIds);

    @Query(value = "SELECT * FROM get_chat_members_page(:chatId, :offset, :limit)", nativeQuery = true)
    List<ChatMemberResult> findFullChatMembersPage(@Param("chatId") Long chatId, @Param("offset") int offset, @Param("limit") int limit);

    @Query("SELECT COUNT(cm) FROM ChatMember cm " +
            "WHERE cm.id.chatId = :chatId AND cm.isDeleted = false")
    int countActiveMembers(@Param("chatId") Long chatId);
}
