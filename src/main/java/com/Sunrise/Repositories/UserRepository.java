package com.Sunrise.Repositories;

import com.Sunrise.DTOs.Paginations.UserResult;
import com.Sunrise.Entities.DBs.User;

import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

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
    @Query("UPDATE User u SET u.isEnabled = true WHERE u.id = :userId")
    void enableUser(@Param("userId") Long userId);

    @Modifying
    @Transactional
    @Query("UPDATE User u SET u.isDeleted = true WHERE u.id = :userId")
    void softDeleteUser(@Param("userId") Long userId);

    @Modifying
    @Transactional
    @Query("UPDATE User u SET u.isDeleted = false WHERE u.id = :userId")
    void restoreUser(@Param("userId") Long userId);

    // ========== ПОИСК ==========

    @Query("SELECT u FROM User u WHERE u.isDeleted = false AND u.id IN :userIds")
    List<User> findActiveUserByIds(@Param("userIds")List<Long> userIds);
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);


    // ========== ПОИСК И ФИЛЬТРАЦИЯ С ПАГИНАЦИЕЙ (ПОКА ЧТО НОРМ, НО ПОТОМ НЕ НОРМ) ==========

    @Query(value = "SELECT * FROM get_users_page(:filter, :cursor, :limit)", nativeQuery = true)
    List<UserResult> getFullFilteredUsersPage(@Param("filter") String filter, @Param("cursor") Long cursor, @Param("limit") int limit);
}
