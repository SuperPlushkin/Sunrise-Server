package com.Sunrise.DTO.Paginations;

import lombok.Builder;

import java.time.LocalDateTime;
import java.util.Set;

@Builder
public record ChatMembersPagination(long id, long chatId, int offset, int limit, Set<Long> chatMembersIds, LocalDateTime createdAt, boolean hasMore, int totalCount) {
    public boolean isEmptyChatMembersIds() {
        return chatMembersIds.isEmpty();
    }
    public int getSizeChatMembersIds() {
        return chatMembersIds.size();
    }
}
