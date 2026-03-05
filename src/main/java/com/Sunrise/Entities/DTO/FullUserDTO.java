package com.Sunrise.Entities.DTO;

import java.time.LocalDateTime;

@lombok.Getter
@lombok.AllArgsConstructor
public class FullUserDTO {
    public final Long id;
    public final String username;
    public final String name;
    private String email;
    private String hashPassword;
    private LocalDateTime lastLogin;
    private LocalDateTime createdAt;
    private boolean isEnabled;
    private boolean isDeleted;
}
