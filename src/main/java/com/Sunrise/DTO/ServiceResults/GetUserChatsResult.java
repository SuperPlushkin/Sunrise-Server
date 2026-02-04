package com.Sunrise.DTO.ServiceResults;

import com.Sunrise.Entities.Chat;

import java.util.List;

@lombok.Getter
public class GetUserChatsResult extends ServiceResult {
    private final List<Chat> userChats;
    private final Integer userChatsCount;

    public GetUserChatsResult(boolean success, String errorMessage, List<Chat> userChats, Integer userChatsCount) {
        super(success, errorMessage);
        this.userChats = userChats;
        this.userChatsCount = userChatsCount;
    }

    public static GetUserChatsResult success(List<Chat> userChats, Integer userChatsCount) {
        return new GetUserChatsResult(true, null, userChats, userChatsCount);
    }
    public static GetUserChatsResult error(String errorMessage) {
        return new GetUserChatsResult(false, errorMessage, null, null);
    }
}
