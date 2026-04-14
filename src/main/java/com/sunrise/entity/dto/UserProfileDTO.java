package com.sunrise.entity.dto;

import java.time.LocalDateTime;

@lombok.Setter
@lombok.Getter
@lombok.AllArgsConstructor
public class UserProfileDTO {
    private long userId;
    private String username;
    private String name;
    private LocalDateTime profileUpdatedAt;
    private LocalDateTime createdAt;
    private boolean isEnabled;
    private LocalDateTime deletedAt;
    private boolean isDeleted;
}
