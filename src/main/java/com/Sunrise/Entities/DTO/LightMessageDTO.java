package com.Sunrise.Entities.DTO;

import java.time.LocalDateTime;

@lombok.Setter
@lombok.Getter
@lombok.AllArgsConstructor
public class LightMessageDTO {
    private long id;
    private long senderId;
    private long chatId;
    private String text;
    private LocalDateTime sentAt;
    private long readCount;
    private boolean readByUser;
    private boolean readByOpponent;
    private boolean hiddenByAdmin;
}
