package com.Sunrise.DTO.ServiceResults;

import com.Sunrise.Entities.DTO.FullChatMemberDTO;
import lombok.Getter;

import java.util.Map;

@Getter
public class ChatMembersResult extends ServiceResultTemplate {
    private final Map<Long, FullChatMemberDTO> chatMembers;
    private final Integer chatMembersCount;

    public ChatMembersResult(boolean success, String errorMessage, Map<Long, FullChatMemberDTO> chatMembers, Integer chatMembersCount) {
        super(success, errorMessage);
        this.chatMembers = chatMembers;
        this.chatMembersCount = chatMembersCount;
    }

    public static ChatMembersResult success(Map<Long, FullChatMemberDTO> chatMembers, Integer chatMembersCount) {
        return new ChatMembersResult(true, null, chatMembers, chatMembersCount);
    }
    public static ChatMembersResult error(String errorMessage) {
        return new ChatMembersResult(false, errorMessage, null, null);
    }
}
