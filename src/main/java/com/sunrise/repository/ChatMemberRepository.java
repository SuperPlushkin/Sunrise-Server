package com.sunrise.repository;

import com.sunrise.entity.db.ChatMember;
import com.sunrise.entity.db.ChatMemberId;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChatMemberRepository extends JpaRepository<ChatMember, ChatMemberId> {

    @Modifying
    @Transactional
    @Query(value = "SELECT add_or_restore_chat_member(:chatId, :userId, :isAdmin)", nativeQuery = true)
    void saveOrRestoreChatMember(@Param("chatId") long chatId, @Param("userId") long userId, @Param("isAdmin") boolean isAdmin);

    @Modifying
    @Transactional
    @Query(value = "SELECT add_or_restore_chat_member(:chatId, user_id, is_admin) " +
                    "FROM unnest(:userIds, :isAdminFlags) AS t(user_id, is_admin)", nativeQuery = true)
    void saveOrRestoreChatMembers(@Param("chatId") long chatId, @Param("userIds") Long[] userIds, @Param("isAdminFlags") Boolean[] isAdminFlags);

    @Modifying
    @Query("UPDATE ChatMember cm SET cm.isAdmin = :isAdmin " +
            "WHERE cm.id.chatId = :chatId AND cm.id.userId = :userId AND cm.isDeleted = false")
    int updateAdminRights(@Param("chatId") long chatId, @Param("userId") long userId, @Param("isAdmin") boolean isAdmin);

    @Modifying
    @Transactional
    @Query(value = "SELECT remove_chat_member(:chatId, :userId)", nativeQuery = true)
    boolean removeChatMember(@Param("chatId") long chatId, @Param("userId") long userId); // удален или нет

    @Query("SELECT cm FROM ChatMember cm " +
            "WHERE cm.id.chatId = :chatId AND cm.id.userId = :userId AND cm.isDeleted = false")
    Optional<ChatMember> getActiveChatMember(@Param("chatId") long chatId, @Param("userId") long userId);

    @Query("SELECT cm FROM ChatMember cm " +
            "WHERE cm.id.chatId = :chatId AND cm.id.userId IN :userIds AND cm.isDeleted = false")
    List<ChatMember> getActiveChatMembersByIds(@Param("chatId") long chatId, @Param("userIds") List<Long> userIds);

    @Query("""
           SELECT cm.id.userId FROM ChatMember cm
           WHERE cm.id.chatId = :chatId AND cm.isDeleted = false
           AND (:cursor IS NULL OR cm.id.userId < :cursor)
           ORDER BY cm.id.userId DESC""")
    List<Long> getChatMemberIdsPage(@Param("chatId") long chatId, @Param("cursor") Long cursor, Pageable pageable);
}
