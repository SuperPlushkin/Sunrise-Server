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
        return adminIds.stream().map(members::get).toList();
    }
    public Optional<CacheChatMember> getMember(Long userId) {
        return Optional.ofNullable(members.get(userId));
    }

    public void addMembers(Iterable<CacheChatMember> newMembers)  {
        for (CacheChatMember member : newMembers) {
            members.put(member.getUserId(), member);
            if (member.isAdmin()) {
                adminIds.add(member.getUserId());
            }
            if (member.isDeleted()) {
                deletedMemberIds.add(member.getUserId());
            }
        }
    }
    public void addMember(CacheChatMember member) {
        members.put(member.getUserId(), member);
        if (member.isAdmin()) {
            adminIds.add(member.getUserId());
        }
        if (member.isDeleted()) {
            deletedMemberIds.add(member.getUserId());
        }
    }

    public void updateAdminRights(Long userId, Boolean isAdmin) {
        CacheChatMember member = members.get(userId);
        if (member != null && !member.isDeleted()) {
            member.setIsAdmin(isAdmin);
            if (isAdmin) {
                adminIds.add(userId);
            } else {
                adminIds.remove(userId);
            }
        }
    }
    public void updateChatCreator(Long newCreatorId) {
        adminIds.add(newCreatorId);
        getMember(newCreatorId).ifPresent(us -> us.setIsAdmin(true));
    }

    public void markMemberAsDeleted(Long userId) {
        CacheChatMember member = members.get(userId);
        if (member != null) {
            member.setIsDeleted(true);
            deletedMemberIds.add(userId);
            if (member.isAdmin())
                adminIds.remove(userId);
        }
    }
    public void restoreMember(Long userId, Boolean isAdmin) {
        CacheChatMember member = members.get(userId);
        if (member != null) {
            member.setIsAdmin(isAdmin);
            member.setIsDeleted(false);
            deletedMemberIds.remove(userId);
            if (isAdmin)
                adminIds.add(userId);
        }
    }

    public Optional<Boolean> hasMemberAndIsActive(Long userId) {
        return getMember(userId).map(CacheChatMember::isActive);
    }

    public Optional<Boolean> isAdmin(Long userId) {
        if (adminIds.contains(userId) && getMember(userId).filter(CacheChatMember::isActive).isPresent())
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