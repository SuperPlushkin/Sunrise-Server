package com.sunrise.entity.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.sunrise.core.dataservice.type.ChatType;

import java.time.LocalDateTime;

@lombok.Setter
@lombok.Getter
@lombok.AllArgsConstructor
public class ChatDTO {
    private long id;
    private String name;
    private String description;
    private ChatType chatType;
    private Long opponentId;
    private int membersCount;
    private int deletedMembersCount;
    private LocalDateTime updatedAt;
    private LocalDateTime createdAt;
    private long createdBy;
    private LocalDateTime deletedAt;
    private boolean isDeleted;

    public static ChatDTO createGroup(long id, String name, String description, ChatType type, int membersCount, LocalDateTime createdAt, long createdBy){
        return new ChatDTO(id, name, description, type, null, membersCount, 0, createdAt, createdAt, createdBy, null, false);
    }
    public static ChatDTO createPersonal(long id, long opponentId, LocalDateTime createdAt, long createdBy){
        return new ChatDTO(id, null, null, ChatType.PERSONAL, opponentId, 2, 0, createdAt, createdAt, createdBy, null, false);
    }

    @JsonIgnore
    public boolean isMoreThenOneMember() {
        return membersCount > 1;
    }
    @JsonIgnore
    public boolean isPersonal() {
        return chatType.isPersonal();
    }
    @JsonIgnore
    public boolean isChangeable() {
        return chatType.isChangeable();
    }
    @JsonIgnore
    public boolean isActionsEnabled() {
        return chatType.isActionsEnabled();
    }
}
