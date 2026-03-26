package com.Sunrise.DTOs.DBResults;

public interface ChatStatsDBResult {
    Integer getTotalMessages();
    Integer getDeletedForAll();
    Integer getDeletedForUser();
    Boolean getCanDeleteForAll();
}
