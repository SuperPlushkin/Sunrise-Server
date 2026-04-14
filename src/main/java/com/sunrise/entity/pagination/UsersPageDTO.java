package com.sunrise.entity.pagination;

import com.sunrise.entity.dto.UserProfileDTO;

import java.util.Map;

public record UsersPageDTO(Map<Long, UserProfileDTO> users, Long nextCursor) { }