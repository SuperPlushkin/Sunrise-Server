package com.Sunrise.DTOs.ServiceResults;

import com.Sunrise.DTOs.Paginations.UsersPageDTO;

@lombok.Getter
public class FilteredUsersResult extends ServiceResultTemplate {
    private final UsersPageDTO page;

    public FilteredUsersResult(boolean success, String errorMessage, UsersPageDTO page){
        super(success, errorMessage);
        this.page = page;
    }

    public static FilteredUsersResult success(UsersPageDTO page) {
        return new FilteredUsersResult(true, null, page);
    }
    public static FilteredUsersResult error(String errorMessage) {
        return new FilteredUsersResult(false, errorMessage, null);
    }
}
