package com.Sunrise.Entities.Cache;

import com.Sunrise.Entities.DB.ChatMember;

public class CacheChatMember extends ChatMember {
    public CacheChatMember(ChatMember chatMember){
        super(chatMember.getChatId(), chatMember.getUserId(), chatMember.getJoinedAt(), chatMember.getIsAdmin(), chatMember.getIsDeleted());
    }
}
