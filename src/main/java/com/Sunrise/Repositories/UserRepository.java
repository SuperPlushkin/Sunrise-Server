package com.Sunrise.Repositories;

import com.Sunrise.DTO.DBResults.ChatStatsDBResult;
import com.Sunrise.Entities.DB.User;

import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    @Query("SELECT u.id, u.username, u.name FROM User u " +
            "WHERE LOWER(u.username) LIKE LOWER(CONCAT('%', :prefix, '%')) " +
            "AND u.isEnabled = true AND u.isDeleted = false")
    List<ChatStatsDBResult.GetUserResult> findFilteredUsers(@Param("prefix") String prefix, Pageable pageable);

    @Modifying
    @Transactional
    @Query("UPDATE User u SET u.lastLogin = :lastLogin WHERE u.username = :username")
    void updateLastLogin(@Param("username") String username, @Param("lastLogin") LocalDateTime lastLogin);

    @Modifying
    @Transactional
    @Query("UPDATE User u SET u.isEnabled = true WHERE u.id = :userId")
    void enableUser(@Param("userId") Long userId);
}
