package com.Sunrise.DTO.ServiceResults;

import com.Sunrise.DTO.Responses.ChatDTO;

import java.util.List;

@lombok.Getter
public class UserChatsResult extends ServiceResultTemplate {
    private final List<ChatDTO> userChats;
    private final Integer userChatsCount;

    public UserChatsResult(boolean success, String errorMessage, List<ChatDTO> userChats, Integer userChatsCount) {
        super(success, errorMessage);
        this.userChats = userChats;
        this.userChatsCount = userChatsCount;
    }

    public static UserChatsResult success(List<ChatDTO> userChats, Integer userChatsCount) {
        return new UserChatsResult(true, null, userChats, userChatsCount);
    }
    public static UserChatsResult error(String errorMessage) {
        return new UserChatsResult(false, errorMessage, null, null);
    }
}
