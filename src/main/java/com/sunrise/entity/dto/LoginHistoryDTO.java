package com.sunrise.entity.dto;

import java.time.LocalDateTime;

@lombok.Getter
@lombok.Setter
@lombok.AllArgsConstructor
public class LoginHistoryDTO {
    private long id;
    private long userId;
    private String ipAddress;
    private String deviceInfo;
    private LocalDateTime loginAt;

    public static LoginHistoryDTO create(long id, long userId, String ipAddress, String deviceInfo) {
        return new LoginHistoryDTO(id, userId, ipAddress, deviceInfo, LocalDateTime.now());
    }
}
