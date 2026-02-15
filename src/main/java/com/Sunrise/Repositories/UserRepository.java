package com.Sunrise.Repositories;

import com.Sunrise.DTO.DBResults.UserDBResult;
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
import java.util.Optional;
import java.util.Set;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);

    boolean existsByUsername(String username);
    boolean existsByEmail(String email);

    // ========== ПОИСК АКТИВНЫХ ПОЛЬЗОВАТЕЛЕЙ ==========

    @Query("SELECT u FROM User u WHERE u.isEnabled = true AND u.isDeleted = false")
    List<User> findAllActive();

    @Query("SELECT u.id FROM User u WHERE u.isEnabled = true AND u.isDeleted = false")
    Set<Long> findAllActiveUserIds();

    // ========== ФИЛЬТРАЦИЯ С ПАГИНАЦИЕЙ (ПОКА ЧТО НОРМ, НО ПОТОМ НЕ НОРМ) ==========

    @Query("SELECT u FROM User u " +
            "WHERE LOWER(u.username) LIKE LOWER(CONCAT('%', :prefix, '%')) " +
            "AND u.isEnabled = true AND u.isDeleted = false")
    List<User> findFilteredUsers(@Param("prefix") String prefix, Pageable pageable);


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
    @Query("UPDATE User u SET u.isEnabled = false WHERE u.id = :userId")
    void disableUser(@Param("userId") Long userId);

    @Modifying
    @Transactional
    @Query("UPDATE User u SET u.isDeleted = true WHERE u.id = :userId")
    void softDeleteUser(@Param("userId") Long userId);

    @Modifying
    @Transactional
    @Query("UPDATE User u SET u.isDeleted = false WHERE u.id = :userId")
    void restoreUser(@Param("userId") Long userId);
}
