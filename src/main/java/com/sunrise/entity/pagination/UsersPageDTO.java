package com.sunrise.entity.pagination;

import com.sunrise.entity.dto.LightUserDTO;

import java.util.Map;

public record UsersPageDTO(Map<Long, LightUserDTO> users, Long nextCursor) { }