package com.sunrise.core.service;

import com.sunrise.core.service.result.FilteredUsersResult;
import com.sunrise.core.service.result.GetProfileResult;
import com.sunrise.core.service.result.SimpleResult;
import com.sunrise.core.service.result.UpdateProfileResult;
import com.sunrise.entity.dto.UsersPageDTO;

import com.sunrise.core.dataservice.DataOrchestrator;
import com.sunrise.core.dataservice.DataValidator;

import com.sunrise.entity.dto.FullUserDTO;
import com.sunrise.entity.dto.UserProfileDTO;
import com.sunrise.helpclass.ValidationException;

import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;

import java.util.Optional;

@Slf4j
@Service
public class UserService {
    private final DataOrchestrator dataOrchestrator;
    private final DataValidator validator;

    public UserService(DataOrchestrator dataOrchestrator, DataValidator validator){
        this.dataOrchestrator = dataOrchestrator;
        this.validator = validator;
    }

    public UpdateProfileResult updateProfile(long userId, String newUsername, String newName) {
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
            return UpdateProfileResult.success(profile);

        } catch (ValidationException e) {
            log.warn("[🔧] ☝️ Failed to update profile for user {}: {}", userId, e.getMessage());
            return UpdateProfileResult.error(e.getMessage());
        } catch (Exception e) {
            log.error("[🔧] ⚠️ Error updating profile for user {}: {}", userId, e.getMessage());
            return UpdateProfileResult.error("Update profile failed due to server error");
        }
    }
    public SimpleResult deleteProfile(long userIdToDeleted, long userWhoDelete) {
        try {
            if (userIdToDeleted != userWhoDelete) {
                validator.validateActiveUser(userWhoDelete);
            }
            validator.validateActiveUser(userIdToDeleted);

            // удаляем
            dataOrchestrator.deleteUser(userIdToDeleted);

            log.info("[🔧] ✅ User {} deleted profile {}", userWhoDelete, userIdToDeleted);
            return SimpleResult.success();

        } catch (ValidationException e) {
            log.warn("[🔧] ☝️ Failed to delete profile for user {}: {}", userWhoDelete, e.getMessage());
            return SimpleResult.error(e.getMessage());
        } catch (Exception e) {
            log.error("[🔧] ⚠️ Error deleting profile for user {}: {}", userWhoDelete, e.getMessage());
            return SimpleResult.error("Update profile failed due to server error");
        }
    }

    public GetProfileResult getMyProfile(long userId) {
        try {
            validator.validateActiveUser(userId);

            Optional<UserProfileDTO> profileOpt = dataOrchestrator.getUserProfile(userId);
            if (profileOpt.isEmpty()) {
                throw new ValidationException("User not found or is deleted");
            }

            log.debug("[🔧] ✅ Loaded profile for user {}", userId);
            return GetProfileResult.success(profileOpt.get());

        } catch (ValidationException e) {
            log.warn("[🔧] ☝️ Failed to get self profile for user {}: {}", userId, e.getMessage());
            return GetProfileResult.error(e.getMessage());
        } catch (Exception e) {
            log.error("[🔧] ⚠️ Error getting self profile for user {}: {}", userId, e.getMessage());
            return GetProfileResult.error("Get profile failed due to server error");
        }
    }
    public GetProfileResult getOtherProfile(long currentUserId, long otherUserId) {
        try {
            validator.validateActiveUser(currentUserId);
            validator.validateActiveUser(otherUserId);

            Optional<UserProfileDTO> profileOpt = dataOrchestrator.getUserProfile(otherUserId);
            if (profileOpt.isEmpty()) {
                throw new ValidationException("User not found or is deleted");
            }

            log.debug("[🔧] ✅ User {} retrieved profile of user {}", currentUserId, otherUserId);
            return GetProfileResult.success(profileOpt.get());

        } catch (ValidationException e) {
            log.warn("[🔧] ☝️ Failed to get other profile for user {}: {}", otherUserId, e.getMessage());
            return GetProfileResult.error(e.getMessage());
        } catch (Exception e) {
            log.error("[🔧] ⚠️ Error getting other profile for user {}: {}", otherUserId, e.getMessage());
            return GetProfileResult.error("Get profile failed due to server error");
        }
    }

    public FilteredUsersResult getFilteredUsers(long userId, String filter, Long cursor, int limit) {
        try {
            validator.validateActiveUser(userId);

            UsersPageDTO usersPage = dataOrchestrator.getUsersPage(filter, cursor, limit);

            log.debug("[🔧] ✅ Get {} users with filter='{}', nextCursor={}, limit={}", usersPage.users().size(), filter, cursor, limit);
            return FilteredUsersResult.success(usersPage);
        }
        catch (ValidationException e) {
            log.warn("[🔧] ☝️ Failed to getFilteredUsers: {}", e.getMessage());
            return FilteredUsersResult.error(e.getMessage());
        }
        catch (Exception e) {
            log.error("[🔧] ⚠️ Error during getFilteredUsers: {}", e.getMessage());
            return FilteredUsersResult.error("Get filtered users failed due to server error");
        }
    }
}