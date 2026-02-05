package com.Sunrise.DTO.ServiceResults;

import com.Sunrise.DTO.Responses.ChatDTO;

import java.util.Set;

@lombok.Getter
public class UserChatsResult extends ServiceResultTemplate {
    private final Set<ChatDTO> userChats;
    private final Integer userChatsCount;

    public UserChatsResult(boolean success, String errorMessage, Set<ChatDTO> userChats, Integer userChatsCount) {
        super(success, errorMessage);
        this.userChats = userChats;
        this.userChatsCount = userChatsCount;
    }

    public static UserChatsResult success(Set<ChatDTO> userChats, Integer userChatsCount) {
        return new UserChatsResult(true, null, userChats, userChatsCount);
    }
    public static UserChatsResult error(String errorMessage) {
        return new UserChatsResult(false, errorMessage, null, null);
    }
}
