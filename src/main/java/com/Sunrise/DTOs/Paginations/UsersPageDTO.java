package com.Sunrise.DTOs.Paginations;

import com.Sunrise.Entities.DTOs.LightUserDTO;

import java.util.Map;

public record UsersPageDTO(Map<Long, LightUserDTO> users, Long nextCursor) { }