package com.Sunrise.DTOs.Paginations;

import lombok.Getter;

import java.time.LocalDateTime;
import java.util.Set;

@Getter
public abstract class PaginationTemplate {
    protected final long id;
    protected final Long cursor;
    protected final int limit;
    protected final LocalDateTime createdAt;

    protected Set<Long> itemIds;
    protected Long nextCursor;

    public PaginationTemplate(long id, Long cursor, int limit) {
        this.id = id;
        this.cursor = cursor;
        this.limit = limit;
        this.createdAt = LocalDateTime.now();
    }

    public void setPaginationData(Set<Long> itemIds, Long nextCursor){
        this.itemIds = itemIds;
        this.nextCursor = nextCursor;
    }

    public int getItemIdsSize() {
        return itemIds.size();
    }
}
