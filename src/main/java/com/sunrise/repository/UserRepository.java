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
    @Query("UPDATE User u SET u.lastLogin = :lastLogin, u.updatedAt = :lastLogin WHERE u.username = :username")
    void updateLastLogin(@Param("username") String username, @Param("lastLogin") LocalDateTime lastLogin);

    @Modifying
    @Transactional
    @Query("UPDATE User u SET u.username = :username, u.name = :name, u.profileUpdatedAt = :updatedAt, u.updatedAt = :updatedAt WHERE u.id = :userId")
    int updateProfile(@Param("userId") long userId, @Param("username") String username, @Param("name") String name, @Param("updatedAt") LocalDateTime updatedAt);

    @Modifying
    @Transactional
    @Query(value = "UPDATE users SET email = :email, jwt_version = jwt_version + 1, updated_at = :updatedAt WHERE id = :userId RETURNING jwt_version", nativeQuery = true)
    int updateUserEmailAndGetJwtVersion(@Param("userId") long userId, @Param("email") String email, @Param("updatedAt") LocalDateTime updatedAt);

    @Modifying
    @Transactional
    @Query(value = "UPDATE users SET hash_password = :password, jwt_version = jwt_version + 1, updated_at = :updatedAt WHERE id = :userId RETURNING jwt_version", nativeQuery = true)
    int updateUserPasswordAndGetJwtVersion(@Param("userId") long userId, @Param("password") String password, @Param("updatedAt") LocalDateTime updatedAt);

    @Modifying
    @Transactional
    @Query(value = "UPDATE users SET is_enabled = true, jwt_version = jwt_version + 1, profile_updated_at = :updatedAt, updated_at = :updatedAt WHERE id = :userId RETURNING jwt_version", nativeQuery = true)
    int enableUserAndGetJwtVersion(@Param("userId") long userId, @Param("updatedAt") LocalDateTime updatedAt);

    @Modifying
    @Transactional
    @Query(value = "UPDATE users SET is_enabled = false, jwt_version = jwt_version + 1, profile_updated_at = :updatedAt, updated_at = :updatedAt WHERE id = :userId RETURNING jwt_version", nativeQuery = true)
    int disableUserAndGetJwtVersion(@Param("userId") long userId, @Param("updatedAt") LocalDateTime updatedAt);

    @Modifying
    @Transactional
    @Query(value = "UPDATE users SET is_deleted = true, jwt_version = jwt_version + 1, profile_updated_at = :updatedAt, updated_at = :updatedAt WHERE id = :userId RETURNING jwt_version", nativeQuery = true)
    int deleteUserAndGetJwtVersion(@Param("userId") long userId, @Param("updatedAt") LocalDateTime updatedAt);

    @Modifying
    @Transactional
    @Query(value = "UPDATE users SET is_deleted = false, jwt_version = jwt_version + 1, profile_updated_at = :updatedAt, updated_at = :updatedAt WHERE id = :userId RETURNING jwt_version", nativeQuery = true)
    int restoreUserAndGetJwtVersion(@Param("userId") long userId, @Param("updatedAt") LocalDateTime updatedAt);


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
               u.name,
               u.profileUpdatedAt,
               u.createdAt,
               u.isEnabled,
               u.deletedAt,
               u.isDeleted
           FROM User u
           WHERE u.isDeleted = false AND u.isEnabled = true
               AND (:filter = ''
                   OR LOWER(u.username) LIKE LOWER(CONCAT('%', :filter, '%'))
                   OR LOWER(u.name) LIKE LOWER(CONCAT('%', :filter, '%')))
               AND (:cursor IS NULL OR u.id < :cursor)
           ORDER BY u.id DESC
           """)
    List<UserResult> getActiveUsersPage(@Param("filter") String filter, @Param("cursor") Long cursor, Pageable pageable);
}
