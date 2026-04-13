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

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ChatMemberRepository extends JpaRepository<ChatMember, ChatMemberId> {

    @Modifying
    @Transactional
    @Query(value = "SELECT add_or_restore_chat_member(:chatId, :userId, :isAdmin, :joinedAt, TRUE)", nativeQuery = true)
    void saveOrRestore(@Param("chatId") long chatId, @Param("userId") long userId, @Param("isAdmin") boolean isAdmin, @Param("joinedAt") LocalDateTime joinedAt);

    @Modifying
    @Transactional
    @Query(value = "SELECT add_or_restore_chat_member(:chatId, user_id, is_admin, :joinedAt, TRUE) " +
                    "FROM unnest(:userIds, :isAdminFlags) AS t(user_id, is_admin)", nativeQuery = true)
    void saveOrRestoreBatch(@Param("chatId") long chatId, @Param("userIds") Long[] userIds, @Param("joinedAt") LocalDateTime joinedAt, @Param("isAdminFlags") Boolean[] isAdminFlags);

    @Modifying
    @Transactional
    @Query("""
           UPDATE ChatMember cm
           SET cm.tag = :tag, cm.updatedAt = :updatedAt
           WHERE cm.id.chatId = :chatId AND cm.id.userId = :userId""")
    int updateInfo(@Param("chatId") long chatId, @Param("userId") long userId, @Param("tag") String tag, @Param("updatedAt") LocalDateTime updatedAt);

    @Modifying
    @Transactional
    @Query("""
           UPDATE ChatMember cm
           SET cm.isAdmin = :isAdmin, cm.updatedAt = :updatedAt
           WHERE cm.id.chatId = :chatId AND cm.id.userId = :userId""")
    int updateAdminRights(@Param("chatId") long chatId, @Param("userId") long userId, @Param("isAdmin") boolean isAdmin, @Param("updatedAt") LocalDateTime updatedAt);

    @Modifying
    @Transactional
    @Query("""
           UPDATE ChatMember cm
           SET cm.isPinned = :isPinned, cm.settingsUpdatedAt = :updatedAt, cm.updatedAt = :updatedAt
           WHERE cm.id.chatId = :chatId AND cm.id.userId = :userId""")
    int updateSettings(@Param("chatId") long chatId, @Param("userId") long userId, @Param("isPinned") boolean isPinned, @Param("updatedAt") LocalDateTime updatedAt);

    @Modifying
    @Transactional
    @Query(value = "SELECT remove_chat_member(:chatId, :userId, :updatedAt)", nativeQuery = true)
    boolean remove(@Param("chatId") long chatId, @Param("userId") long userId, @Param("updatedAt") LocalDateTime updatedAt); // удален или нет

    @Query("SELECT cm FROM ChatMember cm " +
            "WHERE cm.id.chatId = :chatId AND cm.id.userId = :userId AND cm.isDeleted = false")
    Optional<ChatMember> getActive(@Param("chatId") long chatId, @Param("userId") long userId);

    @Query("SELECT cm FROM ChatMember cm " +
            "WHERE cm.id.chatId = :chatId AND cm.id.userId IN :userIds AND cm.isDeleted = false")
    List<ChatMember> getActiveByIds(@Param("chatId") long chatId, @Param("userIds") List<Long> userIds);

    @Query("""
           SELECT cm.id.userId FROM ChatMember cm
           WHERE cm.id.chatId = :chatId AND cm.isDeleted = false
           AND (:cursor IS NULL OR cm.id.userId < :cursor)
           ORDER BY cm.id.userId DESC""")
    List<Long> getIdsPage(@Param("chatId") long chatId, @Param("cursor") Long cursor, Pageable pageable);
}
