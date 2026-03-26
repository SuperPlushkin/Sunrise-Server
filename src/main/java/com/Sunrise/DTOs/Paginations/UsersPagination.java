package com.Sunrise.DTOs.Paginations;

@lombok.Getter
public class UsersPagination extends PaginationTemplate {
    private final String filter;
    public UsersPagination(long id, String filter, Long cursor, int limit) {
        super(id, cursor, limit);
        this.filter = filter;
    }
}
