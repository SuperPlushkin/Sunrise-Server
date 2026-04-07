package com.sunrise.entity.wsdto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WebSocketMessageDTO {
    private long id;
    private long chatId;
    private long senderId;
    private String senderName;
    private String text;
    private LocalDateTime sentAt;
    private String type; // "NEW_MESSAGE", "MESSAGE_DELETED", "MESSAGE_EDITED", "READ_RECEIPT"
}
