package com.sunrise.core.dataservice.type;

public interface ChatStatsDBResult {
    Integer getTotalMessages();
    Integer getDeletedForAll();
    Boolean getCanDeleteForAll();
}
