package com.Sunrise.DTO.ServiceResults;

import com.Sunrise.DTO.Responses.ChatMemberDTO;
import lombok.Getter;

import java.util.Set;

@Getter
public class ChatMembersResult extends ServiceResultTemplate {
    private final Set<ChatMemberDTO> chatMembers;
    private final Integer chatMembersCount;

    public ChatMembersResult(boolean success, String errorMessage, Set<ChatMemberDTO> chatMembers, Integer chatMembersCount) {
        super(success, errorMessage);
        this.chatMembers = chatMembers;
        this.chatMembersCount = chatMembersCount;
    }

    public static ChatMembersResult success(Set<ChatMemberDTO> chatMembers, Integer chatMembersCount) {
        return new ChatMembersResult(true, null, chatMembers, chatMembersCount);
    }
    public static ChatMembersResult error(String errorMessage) {
        return new ChatMembersResult(false, errorMessage, null, null);
    }
}
