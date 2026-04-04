package com.sunrise.entity.dto;

import java.util.Map;

public record UsersPageDTO(Map<Long, LightUserDTO> users, Long nextCursor) { }