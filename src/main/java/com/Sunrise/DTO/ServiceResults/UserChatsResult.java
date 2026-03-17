package com.Sunrise.DTO.ServiceResults;

import com.Sunrise.DTO.DBResults.UserChatsPageResult;

@lombok.Getter
public class UserChatsResult extends ServiceResultTemplate {
    private final UserChatsPageResult pagination;

    public UserChatsResult(boolean success, String errorMessage, UserChatsPageResult pagination) {
        super(success, errorMessage);
        this.pagination = pagination;
    }

    public static UserChatsResult success(UserChatsPageResult pagination) {
        return new UserChatsResult(true, null, pagination);
    }
    public static UserChatsResult error(String errorMessage) {
        return new UserChatsResult(false, errorMessage, null);
    }
}
