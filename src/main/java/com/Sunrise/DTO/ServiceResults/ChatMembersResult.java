package com.Sunrise.DTO.ServiceResults;

import com.Sunrise.DTO.Responses.ChatMemberDTO;
import lombok.Getter;

import java.util.List;

@Getter
public class ChatMembersResult extends ServiceResultTemplate {
    private final List<ChatMemberDTO> chatMembers;
    private final Integer chatMembersCount;

    public ChatMembersResult(boolean success, String errorMessage, List<ChatMemberDTO> chatMembers, Integer chatMembersCount) {
        super(success, errorMessage);
        this.chatMembers = chatMembers;
        this.chatMembersCount = chatMembersCount;
    }

    public static ChatMembersResult success(List<ChatMemberDTO> chatMembers, Integer chatMembersCount) {
        return new ChatMembersResult(true, null, chatMembers, chatMembersCount);
    }
    public static ChatMembersResult error(String errorMessage) {
        return new ChatMembersResult(false, errorMessage, null, null);
    }
}
