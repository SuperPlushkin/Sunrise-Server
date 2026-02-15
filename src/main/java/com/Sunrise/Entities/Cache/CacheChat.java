package com.Sunrise.Entities.Cache;

import com.Sunrise.Entities.DB.Chat;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@lombok.Getter
@lombok.Setter
public class CacheChat extends Chat {

    private final Map<Long, CacheChatMember> members = new ConcurrentHashMap<>(); // userId -> CacheChatMember

    public CacheChat(Long id, String name, Long createdBy, Boolean isGroup){
        super();
        this.id = id;
        this.name = name;
        this.createdBy = createdBy;
        this.isGroup = isGroup;
    }
    public CacheChat(Chat chat) {
        super();
        this.setId(chat.getId());
        this.setName(chat.getName());
        this.setCreatedBy(chat.getCreatedBy());
        this.setIsGroup(chat.getIsGroup());
        this.setCreatedAt(chat.getCreatedAt());
        this.setIsDeleted(chat.getIsDeleted());
    }

    public boolean isPersonalChat() {
        return !getIsGroup() && getMembersSize() == 2;
    }

    public void addMember(CacheUser user, Boolean isAdmin) {
        if (members.get(user.getId()) instanceof CacheChatMember existingMember) {
            existingMember.setUsername(user.getUsername());
            existingMember.setName(user.getName());
            existingMember.setIsAdmin(isAdmin);
            if (existingMember.getIsDeleted())
                existingMember.restoreMember(isAdmin);
        }
        else members.put(user.getId(), new CacheChatMember(user, isAdmin));
    }
    public void removeMember(Long userId) {
        if (members.get(userId) instanceof CacheChatMember member)
            member.markAsDeleted();
    }
    public void restoreMember(Long userId, Boolean isAdmin) {
        if (members.get(userId) instanceof CacheChatMember member)
            member.restoreMember(isAdmin);
    }
    public void clearMembers() {
        members.clear();
    }

    public Long getOtherMemberId(Long userId) {
        return members.values().stream().filter(user -> !user.getUserId().equals(userId)).findFirst().map(CacheChatMember::getUserId).orElse(null);
    }
    public boolean hasNotDeletedMember(Long userId) {
        return members.get(userId) instanceof CacheChatMember member && !member.getIsDeleted();
    }
    public boolean hasEverBeenMember(Long userId) {
        return members.containsKey(userId);
    }

    public Boolean isMemberAdmin(Long userId) {
        return members.get(userId) instanceof CacheChatMember member && !member.getIsDeleted() ? member.getIsAdmin() : false;
    }
    public void setAdminRights(Long userId, Boolean isAdmin) {
        if (members.get(userId) instanceof CacheChatMember member && !member.getIsDeleted())
            member.setIsAdmin(isAdmin);
    }

    public Set<Long> getAdminIds() {
        return members.entrySet().stream()
                .filter(entry -> !entry.getValue().getIsDeleted() && entry.getValue().getIsAdmin())
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());
    }
    public Set<Long> getMemberIds() {
        return new HashSet<>(members.keySet());
    }
    public int getNotDeletedMemberCount() {
        return (int) members.values().stream()
                .filter(member -> !member.getIsDeleted())
                .count();
    }
    public Integer getMembersSize() {
        return members.size();
    }

    public void updateFromEntity(Chat chat) {
        this.setName(chat.getName());
        this.setCreatedBy(chat.getCreatedBy());
        this.setIsGroup(chat.getIsGroup());
        this.setCreatedAt(chat.getCreatedAt());
        this.setIsDeleted(chat.getIsDeleted());
    }
}
