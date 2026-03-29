package com.Sunrise.Core.Services;

import com.Sunrise.DTOs.Paginations.UsersPageDTO;
import com.Sunrise.DTOs.ServiceResults.FilteredUsersResult;
import com.Sunrise.Core.DataServices.DataOrchestrator;
import com.Sunrise.Core.DataServices.DataValidator;
import com.Sunrise.DTOs.ServiceResults.GetProfileResult;
import com.Sunrise.DTOs.ServiceResults.UpdateProfileResult;
import com.Sunrise.Entities.DTOs.FullUserDTO;
import com.Sunrise.Entities.DTOs.UserProfileDTO;
import com.Sunrise.Subclasses.ValidationException;

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

    public UpdateProfileResult updateProfile(long userId, String username, String name) {
        try {
            validator.validateActiveUser(userId);

            // Проверяем, что новый username не занят другим пользователем
            Optional<FullUserDTO> existingUser = dataOrchestrator.getUserByUsername(username);
            if (existingUser.isPresent() && existingUser.get().getId() != userId) {
                return UpdateProfileResult.error("Username already taken");
            }

            // Обновляем профиль
            dataOrchestrator.updateUserProfile(userId, username, name);

            // Получаем обновленного пользователя
            Optional<UserProfileDTO> profileOpt = dataOrchestrator.getUserProfile(userId);
            if (profileOpt.isEmpty()) {
                return UpdateProfileResult.error("User not found after update");
            }

            log.info("[🔧] ✅ User {} updated profile", userId);
            return UpdateProfileResult.success(profileOpt.get());

        } catch (ValidationException e) {
            log.warn("[🔧] ☝️ Failed to update profile for user {}: {}", userId, e.getMessage());
            return UpdateProfileResult.error(e.getMessage());
        } catch (Exception e) {
            log.error("[🔧] ⚠️ Error updating profile for user {}: {}", userId, e.getMessage());
            return UpdateProfileResult.error("Update profile failed due to server error");
        }
    }

    public GetProfileResult getMyProfile(long userId) {
        try {
            validator.validateActiveUser(userId);

            Optional<UserProfileDTO> profileOpt = dataOrchestrator.getUserProfile(userId);
            if (profileOpt.isEmpty()) {
                return GetProfileResult.error("User not found or is deleted");
            }

            log.debug("[🔧] ✅ Loaded profile for user {}", userId);
            return GetProfileResult.success(profileOpt.get());

        } catch (ValidationException e) {
            log.warn("[🔧] ☝️ Failed to get profile for user {}: {}", userId, e.getMessage());
            return GetProfileResult.error(e.getMessage());
        } catch (Exception e) {
            log.error("[🔧] ⚠️ Error getting profile for user {}: {}", userId, e.getMessage());
            return GetProfileResult.error("Get profile failed due to server error");
        }
    }
    public GetProfileResult getOtherProfile(long currentUserId, long otherUserId) {
        try {
            validator.validateActiveUser(currentUserId);
            validator.validateActiveUser(otherUserId);

            Optional<UserProfileDTO> profileOpt = dataOrchestrator.getUserProfile(otherUserId);
            if (profileOpt.isEmpty()) {
                return GetProfileResult.error("User not found or is deleted");
            }

            log.debug("[🔧] ✅ User {} retrieved profile of user {}", currentUserId, otherUserId);
            return GetProfileResult.success(profileOpt.get());

        } catch (ValidationException e) {
            log.warn("[🔧] ☝️ Failed to get profile for user {}: {}", otherUserId, e.getMessage());
            return GetProfileResult.error(e.getMessage());
        } catch (Exception e) {
            log.error("[🔧] ⚠️ Error getting profile for user {}: {}", otherUserId, e.getMessage());
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
            return FilteredUsersResult.error("Error during getFilteredUsers: " + e.getMessage());
        }
    }
}