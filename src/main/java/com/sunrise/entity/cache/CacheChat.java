package com.sunrise.entity.cache;

import com.sunrise.core.dataservice.type.ChatType;

import java.time.LocalDateTime;

@lombok.Getter
@lombok.Setter
@lombok.AllArgsConstructor
public class CacheChat {

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

    private final LocalDateTime cachedAt = LocalDateTime.now();

    public void onAddMember(){
        membersCount++;
    }
    public void onAddMembers(int membersToAdd){
        membersCount += membersToAdd;
    }
    public void onDeleteMember(){
        membersCount--;
        deletedMembersCount++;
    }

    public void setChatType(ChatType chatType, LocalDateTime updatedAt) {
        this.chatType = chatType;
        this.updatedAt = updatedAt;
    }
    public void setChatInfo(String name, String description, LocalDateTime updatedAt) {
        this.name = name;
        this.description = description;
        this.updatedAt = updatedAt;
    }
    public void delete(LocalDateTime updatedAt) {
        this.deletedAt = updatedAt;
        this.isDeleted = true;
        this.updatedAt = updatedAt;
    }
    public void restore(LocalDateTime updatedAt) {
        this.deletedAt = null;
        this.isDeleted = false;
        this.updatedAt = updatedAt;
    }

    public boolean isActive() {
        return !isDeleted;
    }
    public boolean isPersonal(){
        return chatType.isPersonal();
    }
    public boolean isNotPersonal(){
        return !chatType.isPersonal();
    }

    public void updateFromCache(CacheChat cacheChat) {
        this.name = cacheChat.getName();
        this.chatType = cacheChat.getChatType();
        this.opponentId = cacheChat.getOpponentId();
        this.deletedMembersCount = cacheChat.getDeletedMembersCount();
        this.membersCount = cacheChat.getMembersCount();
        this.createdBy = cacheChat.getCreatedBy();
        this.createdAt = cacheChat.getCreatedAt();
        this.deletedAt = cacheChat.getDeletedAt();
        this.isDeleted = cacheChat.isDeleted();
    }

    public static CacheChat copy(CacheChat chat) {
        if (chat == null) return null;

        return new CacheChat(
            chat.getId(),
            chat.getName(),
            chat.getDescription(),
            chat.getChatType(),
            chat.getOpponentId(),
            chat.getMembersCount(),
            chat.getDeletedMembersCount(),
            chat.getUpdatedAt(),
            chat.getCreatedAt(),
            chat.getCreatedBy(),
            chat.getDeletedAt(),
            chat.isDeleted()
        );
    }
}