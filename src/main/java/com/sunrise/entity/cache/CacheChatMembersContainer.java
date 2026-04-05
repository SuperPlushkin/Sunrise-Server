package com.sunrise.entity.cache;

import lombok.Getter;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Getter
public class CacheChatMembersContainer {
    private final long chatId;
    private final Map<Long, CacheChatMember> members = new ConcurrentHashMap<>();   // userId -> CacheChatMember
    private final Set<Long> adminIds = ConcurrentHashMap.newKeySet();               // userId
    private final Set<Long> deletedMemberIds = ConcurrentHashMap.newKeySet();       // userId

    public CacheChatMembersContainer(long chatId) {
        this.chatId = chatId;
    }

    public List<CacheChatMember> getChatAdmins() {
        return adminIds.stream().map(key -> CacheChatMember.copy(members.get(key))).toList();
    }
    public Optional<CacheChatMember> getMember(Long userId) {
        return Optional.ofNullable(CacheChatMember.copy(members.get(userId)));
    }
    public Optional<CacheChatMember> getMemberLink(Long userId) {
        return Optional.ofNullable(members.get(userId));
    }

    public void addMembers(Iterable<CacheChatMember> newMembers)  {
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
    public void addMember(CacheChatMember member) {
        CacheChatMember copyMember = CacheChatMember.copy(member);
        members.put(copyMember.getUserId(), copyMember);
        if (copyMember.isAdmin()) {
            adminIds.add(copyMember.getUserId());
        }
        if (copyMember.isDeleted()) {
            deletedMemberIds.add(copyMember.getUserId());
        }
    }

    public void updateAdminRights(Long userId, Boolean isAdmin) {
        getMemberLink(userId).filter(CacheChatMember::isActive).ifPresent(m -> m.setIsAdmin(isAdmin));
        if (isAdmin) {
            adminIds.add(userId);
        } else {
            adminIds.remove(userId);
        }
    }
    public void updateChatCreator(Long newCreatorId) {
        getMember(newCreatorId).ifPresent(m -> m.setIsAdmin(true));
        adminIds.add(newCreatorId);
    }

    public void markMemberAsDeleted(Long userId) {
        getMember(userId).ifPresent(m -> m.setIsDeleted(true));
        deletedMemberIds.add(userId);
    }
    public void restoreMember(Long userId, Boolean isAdmin) {
        getMember(userId).ifPresent(member ->{
            member.setIsAdmin(isAdmin);
            member.setIsDeleted(false);
        });

        deletedMemberIds.remove(userId);
        if (isAdmin) {
            adminIds.add(userId);
        }
    }

    public Optional<Boolean> hasMemberAndIsActive(Long userId) {
        return getMemberLink(userId).map(CacheChatMember::isActive);
    }

    public Optional<Boolean> isAdmin(Long userId) {
        if (adminIds.contains(userId) && getMemberLink(userId).filter(CacheChatMember::isActive).isPresent())
            return Optional.of(true);

        if (members.containsKey(userId))
            return Optional.of(false);

        return Optional.empty();
    }

    public Optional<Boolean> isDeleted(Long userId) {
        if (deletedMemberIds.contains(userId))
            return Optional.of(true);

        if (members.containsKey(userId))
            return Optional.of(false);

        return Optional.empty();
    }
}