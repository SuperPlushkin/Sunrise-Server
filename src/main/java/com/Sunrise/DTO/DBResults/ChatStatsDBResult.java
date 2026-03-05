package com.Sunrise.DTO.DBResults;

public interface ChatStatsDBResult {
    Integer getTotalMessages();
    Integer getDeletedForAll();
    Integer getDeletedForUser();
    Boolean getCanDeleteForAll();
}
