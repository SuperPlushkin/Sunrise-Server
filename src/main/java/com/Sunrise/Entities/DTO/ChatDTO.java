package com.Sunrise.Entities.DTO;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.time.LocalDateTime;

@lombok.Setter
@lombok.Getter
@lombok.AllArgsConstructor
public class ChatDTO {
    private long id;
    private String name;
    private int membersCount;
    private int deletedMembersCount;
    private long createdBy;
    private LocalDateTime createdAt;
    private boolean isGroup;
    private LocalDateTime deletedAt;
    private boolean isDeleted;

    @JsonIgnore
    public boolean isMoreThenOneMember() {
        return membersCount > 1;
    }
}
