package com.Sunrise.Services;

import com.Sunrise.DTO.ServiceResults.FilteredUsersResult;
import com.Sunrise.DTO.ServiceResults.UserDTO;

import com.Sunrise.Services.DataServices.DataAccessService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserService {
    private final DataAccessService dataAccessService;

    public UserService(DataAccessService dataAccessService){
        this.dataAccessService = dataAccessService;
    }

    public FilteredUsersResult getFilteredUsers(int limit, int offset, String filter) {
        try
        {
            List<UserDTO> users = dataAccessService.getFilteredUsers(filter,  limit, offset);
            return FilteredUsersResult.success(users);
        }
        catch (Exception e) {
            return FilteredUsersResult.error("Error during getFilteredUsers: " + e.getMessage());
        }
    }
}
