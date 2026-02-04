package com.Sunrise.DTO.ServiceResults;

import com.Sunrise.Services.DataServices.CacheEntities.FullChatMember;
import lombok.Getter;

import java.util.Set;

@Getter
public class GetChatMembersResult extends ServiceResult {
    private final Set<FullChatMember> chatMembers;
    private final Integer chatMembersCount;

    public GetChatMembersResult(boolean success, String errorMessage, Set<FullChatMember> chatMembers, Integer chatMembersCount) {
        super(success, errorMessage);
        this.chatMembers = chatMembers;
        this.chatMembersCount = chatMembersCount;
    }

    public static GetChatMembersResult success(Set<FullChatMember> chatMembers, Integer chatMembersCount) {
        return new GetChatMembersResult(true, null, chatMembers, chatMembersCount);
    }
    public static GetChatMembersResult error(String errorMessage) {
        return new GetChatMembersResult(false, errorMessage, null, null);
    }
}
