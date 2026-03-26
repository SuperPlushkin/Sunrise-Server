package com.Sunrise.DTOs.Paginations;

@lombok.Getter
public final class ChatMembersPagination extends PaginationTemplate {
    private final long chatId;

    public ChatMembersPagination(long id, long chatId, Long cursor, int limit) {
        super(id, cursor, limit);
        this.chatId = chatId;
    }
}