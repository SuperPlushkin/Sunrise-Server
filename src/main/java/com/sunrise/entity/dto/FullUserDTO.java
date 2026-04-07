package com.sunrise.entity.dto;

import java.time.LocalDateTime;

@lombok.Setter
@lombok.Getter
@lombok.AllArgsConstructor
public class FullUserDTO {
    private long id;
    private String username;
    private String name;
    private String email;
    private String hashPassword;
    private LocalDateTime lastLogin;
    private LocalDateTime createdAt;
    private boolean isEnabled;
    private boolean isDeleted;

    public static FullUserDTO create(Long id, String username, String name, String email, String hashPassword) {
        return new FullUserDTO(id, username, name, email, hashPassword, null, LocalDateTime.now(), false, false);
    }
}
