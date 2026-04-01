package com.Sunrise.Repositories;

import com.Sunrise.DTOs.Paginations.ChatMemberResult;
import com.Sunrise.DTOs.DBResults.ChatOpponentResult;
import com.Sunrise.Entities.DBs.ChatMember;
import com.Sunrise.Entities.DBs.ChatMemberId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface ChatMemberRepository extends JpaRepository<ChatMember, ChatMemberId> {

    @Modifying
    @Transactional
    @Query(value = "SELECT add_or_restore_chat_member(:chatId, :userId, :isAdmin)", nativeQuery = true)
    void addChatMember(@Param("chatId") long chatId, @Param("userId") long userId, @Param("isAdmin") boolean isAdmin);

    @Modifying
    @Transactional
    @Query(value = "SELECT add_or_restore_chat_member(:chatId, user_id, is_admin) " +
                    "FROM unnest(:userIds, :isAdminFlags) AS t(user_id, is_admin)", nativeQuery = true)
    void addChatMembers(@Param("chatId") long chatId, @Param("userIds") Long[] userIds, @Param("isAdminFlags") Boolean[] isAdminFlags);

    @Modifying
    @Transactional
    @Query(value = "SELECT remove_chat_member(:chatId, :userId)", nativeQuery = true)
    void removeChatMember(@Param("chatId") long chatId, @Param("userId") long userId);


    @Query(value = """
                   SELECT 
                       cm.chat_id,
                       u.id as user_id,
                       u.username, 
                       u.name,
                       u.email,
                       u.hash_password,
                       u.last_login,
                       u.created_at,
                       u.is_enabled,
                       u.is_deleted
                   FROM unnest(:chatIds, :userIds) AS arr(chat_id, user_id)
                   JOIN chat_members cm ON cm.chat_id = arr.chat_id AND cm.user_id = arr.user_id
                   JOIN users u ON u.id = cm.user_id
                   WHERE cm.is_deleted = false
                   """, nativeQuery = true)
    List<ChatOpponentResult> findOpponentsForChats(@Param("chatIds") Long[] chatIds, @Param("userIds") Long[] userIds);

    @Query("SELECT cm.id.userId FROM ChatMember cm WHERE cm.id.chatId = :chatId")
    List<Long> findChatMemberIds(@Param("chatId") long chatId);

    @Query("SELECT cm FROM ChatMember cm " +
            "WHERE cm.id.chatId = :chatId AND cm.id.userId IN :userIds AND cm.isDeleted = false")
    List<ChatMember> findActiveChatMembersByIds(@Param("chatId") long chatId, @Param("userIds") List<Long> userIds);

    @Query(value = "SELECT * FROM get_chat_members_page(:chatId, :afterUserId, :limit)", nativeQuery = true)
    List<ChatMemberResult> findFullChatMembersPage(@Param("chatId") long chatId, @Param("afterUserId") Long afterUserId, @Param("limit") int limit);
}
