package com.sunrise.repository;

import com.sunrise.core.dataservice.type.UserResult;
import com.sunrise.entity.db.User;

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
public interface UserRepository extends JpaRepository<User, Long> {

    // ========== ОБНОВЛЕНИЯ ==========

    @Modifying
    @Transactional
    @Query("UPDATE User u SET u.lastLogin = :lastLogin WHERE u.username = :username")
    void updateLastLogin(@Param("username") String username, @Param("lastLogin") LocalDateTime lastLogin);

    @Modifying
    @Transactional
    @Query("UPDATE User u SET u.username = :username, u.name = :name WHERE u.id = :userId")
    int updateProfile(@Param("userId") long userId, @Param("username") String username, @Param("name") String name);

    @Modifying
    @Transactional
    @Query("UPDATE User u SET u.isEnabled = true WHERE u.id = :userId")
    int enableUser(@Param("userId") long userId);

    @Modifying
    @Transactional
    @Query("UPDATE User u SET u.isDeleted = true WHERE u.id = :userId")
    int deleteUser(@Param("userId") long userId);

    @Modifying
    @Transactional
    @Query("UPDATE User u SET u.isDeleted = false WHERE u.id = :userId")
    int restoreUser(@Param("userId") long userId);


    // ========== ПОИСК ==========

    @Query("SELECT u FROM User u WHERE u.isDeleted = false AND u.id IN :userIds")
    List<User> getActiveUserByIds(@Param("userIds")List<Long> userIds);
    Optional<User> getByUsername(String username);
    Optional<User> getByEmail(String email);


    // ========== ПОИСК И ФИЛЬТРАЦИЯ С ПАГИНАЦИЕЙ ==========

    @Query("""
           SELECT
               u.id,
               u.username,
               u.name
           FROM User u
           WHERE u.isDeleted = false
               AND (:filter = ''
                   OR LOWER(u.username) LIKE LOWER(CONCAT('%', :filter, '%'))
                   OR LOWER(u.name) LIKE LOWER(CONCAT('%', :filter, '%')))
               AND (:cursor IS NULL OR u.id < :cursor)
           ORDER BY u.id DESC
           """)
    List<UserResult> getActiveUsersPage(@Param("filter") String filter, @Param("cursor") Long cursor, Pageable pageable);
}
