package com.Sunrise.Services.DataServices.CacheEntities;

import com.Sunrise.Entities.Chat;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@lombok.Getter
@lombok.Setter
public class CacheChat extends Chat {

    private Map<Long, CacheChatMember> chatMembers = new ConcurrentHashMap<>(); // userId -> CacheChatMember

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


    // Вспомогательные методы
    public void addMember(Long userId, Boolean isAdmin) {

        if (chatMembers.get(userId) instanceof CacheChatMember existingMember)
        {
            if (existingMember.getIsDeleted())
            {
                existingMember.restoreMember(isAdmin);
            }
            else existingMember.setIsAdmin(isAdmin);
        }
        else {
            chatMembers.put(userId, new CacheChatMember(userId, isAdmin));
        }
    }
    public void removeMember(Long userId) {
        if (chatMembers.get(userId) instanceof CacheChatMember member)
            member.markAsDeleted();
    }

    public boolean hasActiveMember(Long userId) {
        return chatMembers.get(userId) instanceof CacheChatMember member && !member.getIsDeleted();
    }
    public boolean hasEverBeenMember(Long userId) {
        return chatMembers.containsKey(userId);
    }
    public int getActiveMemberCount() {
        return (int)chatMembers.values().stream()
                .filter(member -> !member.getIsDeleted())
                .count();
    }

    public Set<Long> getAdminIds() {
        return chatMembers.entrySet().stream()
                .filter(entry -> !entry.getValue().getIsDeleted() && entry.getValue().getIsAdmin())
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());
    }
    public Boolean isAdmin(Long userId) {
        return chatMembers.get(userId) instanceof CacheChatMember member && !member.getIsDeleted() ? member.getIsAdmin() : false;
    }
    public void setAdminRights(Long userId, Boolean isAdmin) {
        if (chatMembers.get(userId) instanceof CacheChatMember member && !member.getIsDeleted())
            member.setIsAdmin(isAdmin);
    }

    // Полезные методы
    public CacheChatMember getMemberInfo(Long userId) {
        return chatMembers.get(userId);
    }
    public LocalDateTime getMemberJoinedAt(Long userId) {
        return chatMembers.get(userId) instanceof CacheChatMember member ? member.getJoinedAt() : null;
    }
    public LocalDateTime getMemberCurrentJoinDate(Long userId) {
        return chatMembers.get(userId) instanceof CacheChatMember member ? member.getCurrentJoinDate() : null;
    }

    public void restoreMember(Long userId, Boolean isAdmin) {
        if (chatMembers.get(userId) instanceof CacheChatMember member)
            member.restoreMember(isAdmin);
    }
    public void restoreMember(Long userId) {
        restoreMember(userId, false);
    }

    public Set<Long> getActiveMemberIds() {
        return chatMembers.entrySet().stream()
                .filter(entry -> !entry.getValue().getIsDeleted())
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());
    }
    public Set<Long> getDeletedMemberIds() {
        return chatMembers.entrySet().stream()
                .filter(entry -> entry.getValue().getIsDeleted())
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());
    }

    public int getMembershipPeriodsCount(Long userId) {
        return chatMembers.get(userId) instanceof CacheChatMember member ? member.getMembershipHistory().size() : 0;
    }


    // Метод для очистки старых удаленных пользователей
    public void cleanupOldDeletedMembers(int daysThreshold) {
        LocalDateTime thresholdDate = LocalDateTime.now().minusDays(daysThreshold);
        chatMembers.entrySet().removeIf(entry -> {
            CacheChatMember member = entry.getValue();
            if (member.getIsDeleted()) {
                LocalDateTime leftAt = member.getMembershipHistory().stream()
                        .map(CacheChatMember.MembershipPeriod::getLeftAt)
                        .filter(Objects::nonNull)
                        .max(LocalDateTime::compareTo)
                        .orElse(member.getJoinedAt());
                return leftAt.isBefore(thresholdDate);
            }
            return false;
        });
    }
}
