package com.Sunrise.Core.Services;

import com.Sunrise.DTOs.Paginations.UsersPageDTO;
import com.Sunrise.DTOs.ServiceResults.FilteredUsersResult;
import com.Sunrise.Core.DataServices.DataOrchestrator;
import com.Sunrise.Core.DataServices.DataValidator;
import com.Sunrise.Subclasses.ValidationException;

import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;

@Slf4j
@Service
public class UserService {
    private final DataOrchestrator dataOrchestrator;
    private final DataValidator validator;

    public UserService(DataOrchestrator dataOrchestrator, DataValidator validator){
        this.dataOrchestrator = dataOrchestrator;
        this.validator = validator;
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