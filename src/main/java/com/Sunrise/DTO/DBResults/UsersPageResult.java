package com.Sunrise.DTO.DBResults;

import com.Sunrise.Entities.DTO.LightUserDTO;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public record UsersPageResult(Map<Long, LightUserDTO> users, int totalCount, boolean hasMore) {
    public Set<Long> getUsersId(){
        return new HashSet<>(users.keySet());
    }
}