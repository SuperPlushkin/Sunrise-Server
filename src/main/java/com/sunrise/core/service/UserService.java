package com.sunrise.core.service;

import com.sunrise.core.dataservice.LockManager;
import com.sunrise.core.service.result.*;
import com.sunrise.entity.dto.UsersPageDTO;

import com.sunrise.core.dataservice.DataOrchestrator;
import com.sunrise.core.dataservice.DataValidator;

import com.sunrise.entity.dto.FullUserDTO;
import com.sunrise.entity.dto.UserProfileDTO;
import com.sunrise.helpclass.ValidationException;

import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Slf4j
@RequiredArgsConstructor
@Service
public class UserService {

    private final DataOrchestrator dataOrchestrator;
    private final LockManager lockManager;
    private final DataValidator validator;

    public ResultOneArg<UserProfileDTO> updateProfile(long userId, String newUsername, String newName) {
        // LOCK на username
        if (!lockManager.tryLockUsername(newUsername))
            return ResultOneArg.error("Try again later");

        try {
            validator.validateActiveUser(userId);

            FullUserDTO user = dataOrchestrator.getUser(userId)
                    .orElseThrow(() -> new ValidationException("User not found"));

            boolean usernameNotChanged = user.getUsername().equals(newUsername);
            boolean nameNotChanged = user.getName().equals(newName);

            // Проверяем, что данные изменились
            if (usernameNotChanged && nameNotChanged) {
                throw new ValidationException("Data has not changed");
            }

            if (!usernameNotChanged && dataOrchestrator.existsUserByUsername(newUsername)) {
                throw new ValidationException("Username already taken");
            }

            // Обновляем профиль
            UserProfileDTO profile = new UserProfileDTO(userId, newUsername, newName, user.getCreatedAt());
            dataOrchestrator.updateUserProfile(profile);

            log.info("[🔧] ✅ User {} updated profile", userId);
            return ResultOneArg.success(profile);
        }
        catch (ValidationException e) {
            log.warn("[🔧] ☝️ Failed to update profile for user {}: {}", userId, e.getMessage());
            return ResultOneArg.error(e.getMessage());
        }
        catch (Exception e) {
            log.error("[🔧] ⚠️ Error updating profile for user {}: {}", userId, e.getMessage());
            return ResultOneArg.error("Update profile failed due to server error");
        }
        finally {
            lockManager.unLockUsername(newUsername);
        }
    }
    public ResultNoArgs deleteProfile(long userIdToDeleted, long userWhoDelete) {
        try {
            if (userIdToDeleted != userWhoDelete) {
                validator.validateActiveUser(userWhoDelete);
            }
            validator.validateActiveUser(userIdToDeleted);

            // удаляем
            dataOrchestrator.deleteUser(userIdToDeleted);

            log.info("[🔧] ✅ User {} deleted profile {}", userWhoDelete, userIdToDeleted);
            return ResultNoArgs.success();
        }
        catch (ValidationException e) {
            log.warn("[🔧] ☝️ Failed to delete profile for user {}: {}", userWhoDelete, e.getMessage());
            return ResultNoArgs.error(e.getMessage());
        }
        catch (Exception e) {
            log.error("[🔧] ⚠️ Error deleting profile for user {}: {}", userWhoDelete, e.getMessage());
            return ResultNoArgs.error("Update profile failed due to server error");
        }
    }

    public ResultOneArg<UserProfileDTO> getMyProfile(long userId) {
        try {
            validator.validateActiveUser(userId);

            Optional<UserProfileDTO> profileOpt = dataOrchestrator.getUserProfile(userId);
            if (profileOpt.isEmpty()) {
                throw new ValidationException("User not found or is deleted");
            }

            log.debug("[🔧] ✅ Loaded profile for user {}", userId);
            return ResultOneArg.success(profileOpt.get());
        }
        catch (ValidationException e) {
            log.warn("[🔧] ☝️ Failed to get self profile for user {}: {}", userId, e.getMessage());
            return ResultOneArg.error(e.getMessage());
        }
        catch (Exception e) {
            log.error("[🔧] ⚠️ Error getting self profile for user {}: {}", userId, e.getMessage());
            return ResultOneArg.error("Get profile failed due to server error");
        }
    }
    public ResultOneArg<UserProfileDTO> getOtherProfile(long currentUserId, long otherUserId) {
        try {
            validator.validateActiveUser(currentUserId);
            validator.validateActiveUser(otherUserId);

            Optional<UserProfileDTO> profileOpt = dataOrchestrator.getUserProfile(otherUserId);
            if (profileOpt.isEmpty()) {
                throw new ValidationException("User not found or is deleted");
            }

            log.debug("[🔧] ✅ User {} retrieved profile of user {}", currentUserId, otherUserId);
            return ResultOneArg.success(profileOpt.get());
        }
        catch (ValidationException e) {
            log.warn("[🔧] ☝️ Failed to get other profile for user {}: {}", otherUserId, e.getMessage());
            return ResultOneArg.error(e.getMessage());
        }
        catch (Exception e) {
            log.error("[🔧] ⚠️ Error getting other profile for user {}: {}", otherUserId, e.getMessage());
            return ResultOneArg.error("Get profile failed due to server error");
        }
    }

    public ResultOneArg<UsersPageDTO> getActiveUsersPage(long userId, String filter, Long cursor, int limit) {
        try {
            validator.validateActiveUser(userId);

            UsersPageDTO usersPage = dataOrchestrator.getActiveUsersPage(filter, cursor, limit);

            log.debug("[🔧] ✅ Get {} users with filter='{}', nextCursor={}, limit={}", usersPage.users().size(), filter, cursor, limit);
            return ResultOneArg.success(usersPage);
        }
        catch (ValidationException e) {
            log.warn("[🔧] ☝️ Failed to getFilteredUsers: {}", e.getMessage());
            return ResultOneArg.error(e.getMessage());
        }
        catch (Exception e) {
            log.error("[🔧] ⚠️ Error during getFilteredUsers: {}", e.getMessage());
            return ResultOneArg.error("Get filtered users failed due to server error");
        }
    }
}