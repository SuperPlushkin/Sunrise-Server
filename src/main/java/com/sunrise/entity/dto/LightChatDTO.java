package com.sunrise.entity.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.time.LocalDateTime;

@lombok.Setter
@lombok.Getter
@lombok.AllArgsConstructor
public class LightChatDTO {
    private long id;
    private String name;
    private boolean isGroup;
    private Long opponentId;
    private int membersCount;
    private int deletedMembersCount;
    private LocalDateTime createdAt;
    private long createdBy;
    private LocalDateTime deletedAt;
    private boolean isDeleted;

    @JsonIgnore
    public boolean isMoreThenOneMember() {
        return membersCount > 1;
    }
    @JsonIgnore
    public boolean isActive() {
        return !isDeleted;
    }

    public static LightChatDTO createPrivate(long id, long createdBy, long opponentId){
        return new LightChatDTO(id, null, false, opponentId, 0, 0, LocalDateTime.now(), createdBy, null, false);
    }
    public static LightChatDTO createGroup(long id, String name, long createdBy){
        return new LightChatDTO(id, name, true, null, 0, 0, LocalDateTime.now(), createdBy, null, false);
    }
}
