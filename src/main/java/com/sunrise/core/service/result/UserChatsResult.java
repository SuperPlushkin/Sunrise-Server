package com.sunrise.core.service.result;

import com.sunrise.entity.dto.UserChatsPageDTO;

@lombok.Getter
public class UserChatsResult extends ServiceResultTemplate {
    private final UserChatsPageDTO pagination;

    public UserChatsResult(boolean success, String errorMessage, UserChatsPageDTO pagination) {
        super(success, errorMessage);
        this.pagination = pagination;
    }

    public static UserChatsResult success(UserChatsPageDTO pagination) {
        return new UserChatsResult(true, null, pagination);
    }
    public static UserChatsResult error(String errorMessage) {
        return new UserChatsResult(false, errorMessage, null);
    }
}
