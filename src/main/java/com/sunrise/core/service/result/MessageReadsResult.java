package com.sunrise.core.service.result;

import com.sunrise.entity.dto.MessageReadStatusDTO;

import java.util.Map;

@lombok.Getter
public class MessageReadsResult extends ServiceResultTemplate {
    private final Map<Long, MessageReadStatusDTO> reads;

    public MessageReadsResult(boolean success, String errorMessage, Map<Long, MessageReadStatusDTO> reads) {
        super(success, errorMessage);

        this.reads = reads;
    }

    public static MessageReadsResult success(Map<Long, MessageReadStatusDTO> reads) {
        return new MessageReadsResult(true, null, reads);
    }
    public static MessageReadsResult error(String errorMessage) {
        return new MessageReadsResult(false, errorMessage, null);
    }
}
