package com.Sunrise.Entities.DTOs;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
public class UserProfileDTO {
    private long userId;
    private String username;
    private String name;
    private LocalDateTime createdAt;
}
