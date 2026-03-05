package com.Sunrise.DTO.DBResults;

import com.Sunrise.Entities.DTO.LightUserDTO;

import java.util.Map;

public record UsersPageResult(Map<Long, LightUserDTO> users, Integer totalCount, Boolean hasMore) { }