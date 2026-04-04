package com.sunrise.core.dataservice.type;

public interface ChatStatsDBResult {
    Integer getTotalMessages();
    Integer getDeletedForAll();
    Integer getDeletedForUser();
    Boolean getCanDeleteForAll();
}
