package com.sunrise.core.service.result;

import com.sunrise.entity.dto.UsersPageDTO;

@lombok.Getter
public class FilteredUsersResult extends ServiceResultTemplate {
    private final UsersPageDTO pagination;

    public FilteredUsersResult(boolean success, String errorMessage, UsersPageDTO pagination){
        super(success, errorMessage);
        this.pagination = pagination;
    }

    public static FilteredUsersResult success(UsersPageDTO page) {
        return new FilteredUsersResult(true, null, page);
    }
    public static FilteredUsersResult error(String errorMessage) {
        return new FilteredUsersResult(false, errorMessage, null);
    }
}
