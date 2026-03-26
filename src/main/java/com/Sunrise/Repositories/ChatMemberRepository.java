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
import java.util.Set;

@Repository
public interface ChatMemberRepository extends JpaRepository<ChatMember, ChatMemberId> {

    @Modifying
    @Transactional
    @Query(value = "SELECT add_or_restore_chat_member(:chatId, :userId, :isAdmin)", nativeQuery = true)
    void addChatMember(@Param("chatId") long chatId, @Param("userId") long userId, @Param("isAdmin") boolean isAdmin);

    @Modifying
    @Transactional
    @Query(value = "SELECT remove_chat_member(:chatId, :userId)", nativeQuery = true)
    void removeChatMember(@Param("chatId") long chatId, @Param("userId") long userId);


    @Query("SELECT cm.id.chatId as chatId, u as user FROM ChatMember cm " +
            "JOIN User u ON u.id = cm.id.userId " +
            "WHERE cm.id.chatId IN :chatIds " +
            "AND cm.id.userId != :currentUserId " +
            "AND cm.isDeleted = false")
    List<ChatOpponentResult> findOpponentsForChats(@Param("chatIds") Set<Long> chatIds, @Param("currentUserId") Long currentUserId);

    @Query("SELECT cm.id.userId FROM ChatMember cm WHERE cm.id.chatId = :chatId")
    List<Long> findChatMemberIds(@Param("chatId") long chatId);

    @Query("SELECT cm FROM ChatMember cm " +
            "WHERE cm.id.chatId = :chatId AND cm.id.userId IN :userIds AND cm.isDeleted = false")
    List<ChatMember> findActiveChatMembersByIds(@Param("chatId") long chatId, @Param("userIds") List<Long> userIds);

    @Query(value = "SELECT * FROM get_chat_members_page(:chatId, :afterUserId, :limit)", nativeQuery = true)
    List<ChatMemberResult> findFullChatMembersPage(@Param("chatId") long chatId, @Param("afterUserId") Long afterUserId, @Param("limit") int limit);
}
