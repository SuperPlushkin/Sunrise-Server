package com.Sunrise.DTO.DBResults;

import com.Sunrise.Entities.DB.Chat;
import com.Sunrise.Entities.DB.Message;

public interface FullChatResult {
    Chat getChat();
    Message getNewestMessage();
    Integer getVisibleCount();
    Integer getHiddenCount();
    Integer getTotalCount();
}
