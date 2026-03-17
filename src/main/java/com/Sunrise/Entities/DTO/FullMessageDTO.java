package com.Sunrise.Entities.DTO;

import java.time.LocalDateTime;
import java.util.Set;

@lombok.Setter
@lombok.Getter
@lombok.AllArgsConstructor
public class FullMessageDTO {
    private long id;
    private long senderId;
    private long chatId;
    private String text;
    private LocalDateTime sentAt;
    private long readCount;
    private Set<Long> readByUsers;
    private boolean hiddenByAdmin;

    public static FullMessageDTO create(long id, long senderId, long chatId, String text){
        return new FullMessageDTO(id, senderId, chatId, text, LocalDateTime.now(), 1, Set.of(senderId), false);
    }
}

