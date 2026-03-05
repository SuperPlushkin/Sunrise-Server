package com.Sunrise.Services;

import com.Sunrise.DTO.DBResults.UsersPageResult;
import com.Sunrise.DTO.ServiceResults.FilteredUsersResult;
import com.Sunrise.Services.DataServices.DataAccessService;
import com.Sunrise.Services.DataServices.DataValidator;
import com.Sunrise.Subclasses.ValidationException;

import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;

@Slf4j
@Service
public class UserService {
    private final DataAccessService dataAccessService;
    private final DataValidator validator;

    public UserService(DataAccessService dataAccessService, DataValidator validator){
        this.dataAccessService = dataAccessService;
        this.validator = validator;
    }

    public FilteredUsersResult getFilteredUsers(Long userId, String filter, int offset, int limit) {
        try {
            validator.validateActiveUser(userId);

            UsersPageResult usersPage = dataAccessService.getUsersPage(filter, offset, limit);

            log.debug("[🔧] ✅ Get {} users with filter='{}' (offset={}, limit={})", usersPage.totalCount(), filter, offset, limit);
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