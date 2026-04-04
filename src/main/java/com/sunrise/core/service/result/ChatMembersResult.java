package com.sunrise.core.service.result;

import com.sunrise.entity.dto.ChatMembersPageDTO;
import lombok.Getter;

@Getter
public class ChatMembersResult extends ServiceResultTemplate {
    private final ChatMembersPageDTO pagination;

    public ChatMembersResult(boolean success, String errorMessage, ChatMembersPageDTO pagination) {
        super(success, errorMessage);
        this.pagination = pagination;
    }

    public static ChatMembersResult success(ChatMembersPageDTO chatMemberPage) {
        return new ChatMembersResult(true, null, chatMemberPage);
    }
    public static ChatMembersResult error(String errorMessage) {
        return new ChatMembersResult(false, errorMessage, null);
    }
}
