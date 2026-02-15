package com.Sunrise.Services;

import com.Sunrise.DTO.ServiceResults.FilteredUsersResult;

import com.Sunrise.DTO.Responses.UserDTO;
import com.Sunrise.Entities.DB.User;
import com.Sunrise.Services.DataServices.DataAccessService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class UserService {
    private final DataAccessService dataAccessService;

    public UserService(DataAccessService dataAccessService){
        this.dataAccessService = dataAccessService;
    }

    public FilteredUsersResult getFilteredUsers(int limit, int offset, String filter) {
        try
        {
            List<User> users = dataAccessService.getFilteredUsers(filter,  limit, offset);

            List<UserDTO> userDTOSet = users.stream().map(UserDTO::new).toList();

            return FilteredUsersResult.success(userDTOSet);
        }
        catch (Exception e) {
            return FilteredUsersResult.error("Error during getFilteredUsers: " + e.getMessage());
        }
    }
}
