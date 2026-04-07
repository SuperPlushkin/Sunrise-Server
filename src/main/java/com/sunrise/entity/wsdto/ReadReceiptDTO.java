package com.sunrise.entity.wsdto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReadReceiptDTO {
    private long chatId;
    private long userId;
    private String username;
    private long lastReadMessageId;
    private LocalDateTime readAt;
}
