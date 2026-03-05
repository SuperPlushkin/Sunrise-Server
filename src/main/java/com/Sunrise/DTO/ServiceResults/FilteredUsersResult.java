package com.Sunrise.DTO.ServiceResults;

import com.Sunrise.DTO.DBResults.UsersPageResult;

@lombok.Getter
public class FilteredUsersResult extends ServiceResultTemplate {
    private final UsersPageResult page;

    public FilteredUsersResult(boolean success, String errorMessage, UsersPageResult page){
        super(success, errorMessage);
        this.page = page;
    }

    public static FilteredUsersResult success(UsersPageResult page) {
        return new FilteredUsersResult(true, null, page);
    }
    public static FilteredUsersResult error(String errorMessage) {
        return new FilteredUsersResult(false, errorMessage, null);
    }
}
