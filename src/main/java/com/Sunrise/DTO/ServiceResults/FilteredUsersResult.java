package com.Sunrise.DTO.ServiceResults;

import java.util.Date;
import java.util.List;

@lombok.Getter
public class FilteredUsersResult extends ServiceResult{
    private final List<UserDTO> users;

    public FilteredUsersResult(boolean success, String errorMessage, List<UserDTO> users){
        super(success, errorMessage);

        this.users = users;
    }

    public static FilteredUsersResult success(List<UserDTO> users) {
        return new FilteredUsersResult(true, null, users);
    }
    public static FilteredUsersResult error(String errorMessage) {
        return new FilteredUsersResult(false, errorMessage, null);
    }
}
