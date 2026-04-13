package com.sunrise.entity.cache;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@lombok.Getter
@lombok.Setter
@lombok.AllArgsConstructor
public class CacheChatMembersContainer {
    private final long chatId;
    private final Map<Long, CacheChatMember> members = new ConcurrentHashMap<>();   // userId -> CacheChatMember
    private final Set<Long> adminIds = ConcurrentHashMap.newKeySet();               // userId
    private final Set<Long> deletedMemberIds = ConcurrentHashMap.newKeySet();       // userId

    public List<CacheChatMember> getChatAdmins() {
        return adminIds.stream().map(key -> CacheChatMember.copy(members.get(key))).toList();
    }
    public Optional<CacheChatMember> getMember(long userId) {
        return Optional.ofNullable(CacheChatMember.copy(members.get(userId)));
    }
    public Optional<CacheChatMember> getMemberLink(long userId) {
        return Optional.ofNullable(members.get(userId));
    }

    public void addBatch(Iterable<CacheChatMember> newMembers)  {
        for (CacheChatMember member : newMembers) {
            CacheChatMember copyMember = CacheChatMember.copy(member);
            members.put(copyMember.getUserId(), copyMember);
            if (copyMember.isAdmin()) {
                adminIds.add(copyMember.getUserId());
            }
            if (copyMember.isDeleted()) {
                deletedMemberIds.add(copyMember.getUserId());
            }
        }
    }
    public void add(CacheChatMember member) {
        CacheChatMember copyMember = CacheChatMember.copy(member);
        members.put(copyMember.getUserId(), copyMember);
        if (copyMember.isAdmin()) {
            adminIds.add(copyMember.getUserId());
        }
        if (copyMember.isDeleted()) {
            deletedMemberIds.add(copyMember.getUserId());
        }
    }
    public void updateInfo(long userId, String tag, LocalDateTime updatedAt) {
        getMemberLink(userId).filter(CacheChatMember::isActive).ifPresent(member -> {
            member.setTag(tag);
            member.setUpdatedAt(updatedAt);
        });
    }
    public void updateAdminRights(long userId, boolean isAdmin, LocalDateTime updatedAt) {
        getMemberLink(userId).filter(CacheChatMember::isActive).ifPresent(member -> {
            member.setAdmin(isAdmin);
            member.setUpdatedAt(updatedAt);
        });
        if (isAdmin) {
            adminIds.add(userId);
        } else {
            adminIds.remove(userId);
        }
    }
    public void updateSettings(long userId, boolean isPinned, LocalDateTime updatedAt) {
        getMemberLink(userId).filter(CacheChatMember::isActive).ifPresent(member -> {
            member.setPinned(isPinned);
            member.setSettingsUpdatedAt(updatedAt);
            member.setUpdatedAt(updatedAt);
        });
    }
    public void markMemberAsDeleted(long userId, LocalDateTime updatedAt) {
        getMember(userId).ifPresent(member -> {
            member.setDeleted(true);
            member.setUpdatedAt(updatedAt);
        });
        deletedMemberIds.add(userId);
    }
    public void restoreMember(long userId, boolean isAdmin, LocalDateTime updatedAt) {
        getMember(userId).ifPresent(member ->{
            member.setAdmin(isAdmin);
            member.setDeleted(false);
            member.setUpdatedAt(updatedAt);
        });

        deletedMemberIds.remove(userId);
        if (isAdmin) {
            adminIds.add(userId);
        }
    }

    public Optional<Boolean> hasMemberAndIsActive(long userId) {
        return getMemberLink(userId).map(CacheChatMember::isActive);
    }

    public Optional<Boolean> isAdmin(long userId) {
        if (adminIds.contains(userId) && getMemberLink(userId).filter(CacheChatMember::isActive).isPresent())
            return Optional.of(true);

        if (members.containsKey(userId))
            return Optional.of(false);

        return Optional.empty();
    }

    public Optional<Boolean> isDeleted(long userId) {
        if (deletedMemberIds.contains(userId))
            return Optional.of(true);

        if (members.containsKey(userId))
            return Optional.of(false);

        return Optional.empty();
    }
}