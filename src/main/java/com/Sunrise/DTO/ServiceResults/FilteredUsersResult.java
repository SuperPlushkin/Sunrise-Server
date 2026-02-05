package com.Sunrise.DTO.ServiceResults;

import com.Sunrise.DTO.Responses.UserDTO;

import java.util.Set;

@lombok.Getter
public class FilteredUsersResult extends ServiceResultTemplate {
    private final Set<UserDTO> users;

    public FilteredUsersResult(boolean success, String errorMessage, Set<UserDTO> users){
        super(success, errorMessage);
        this.users = users;
    }

    public static FilteredUsersResult success(Set<UserDTO> users) {
        return new FilteredUsersResult(true, null, users);
    }
    public static FilteredUsersResult error(String errorMessage) {
        return new FilteredUsersResult(false, errorMessage, null);
    }
}
