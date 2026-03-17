package com.Sunrise.Entities.Cache;

import lombok.Getter;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Getter
public class CacheChatMembersContainer {
    private final Long chatId;
    private volatile Long chatCreatorId;
    private final AtomicInteger deletedMemberCount;                 // удаленных участников в БД
    private final AtomicInteger totalMemberCount;                   // всего участников в БД
    private final LocalDateTime createdAt = LocalDateTime.now();    // когда создан контейнер

    private final Map<Long, CacheChatMember> members = new ConcurrentHashMap<>();   // userId -> CacheChatMember
    private final Set<Long> adminIds = ConcurrentHashMap.newKeySet();               // userId
    private final Set<Long> deletedMemberIds = ConcurrentHashMap.newKeySet();       // userId

    public CacheChatMembersContainer(CacheChat chat) {
        this.chatId = chat.getId();
        this.chatCreatorId = chat.getCreatedBy();
        this.deletedMemberCount = new AtomicInteger(chat.getDeletedMembersCount());
        this.totalMemberCount = new AtomicInteger(chat.getMembersCount());
        this.adminIds.add(chat.getCreatedBy());
    }

    public List<CacheChatMember> getChatAdmins() {
        return adminIds.stream().map(members::get).toList();
    }
    public Optional<CacheChatMember> getMember(Long userId) {
        return Optional.ofNullable(members.get(userId));
    }
    public Optional<CacheChatMember> getActiveMember(Long userId) {
        return getMember(userId).filter(CacheChatMember::isActive);
    }

    public void addNewMembers(Collection<CacheChatMember> newMembers) {
        addMembers(newMembers);
        totalMemberCount.addAndGet(newMembers.size());
    }
    public void addMembers(Iterable<CacheChatMember> newMembers)  {
        for (CacheChatMember member : newMembers) {
            members.put(member.getUserId(), member);
            if (member.isAdmin()) {
                adminIds.add(member.getUserId());
            }
            if (member.isDeleted()) {
                deletedMemberIds.add(member.getUserId());
                deletedMemberCount.incrementAndGet();  // <-- Добавляем
            }
        }
    }
    public void addNewMember(CacheChatMember member) {
        addMember(member);
        totalMemberCount.incrementAndGet();
    }
    public void addMember(CacheChatMember member) {
        members.put(member.getUserId(), member);
        if (member.isAdmin()) {
            adminIds.add(member.getUserId());
        }
        if (member.isDeleted()) {
            deletedMemberIds.add(member.getUserId());
            deletedMemberCount.incrementAndGet();
        }
    }
    public void updateAdminRights(Long userId, Boolean isAdmin) {
        // Нельзя убрать права у создателя
        if (userId.equals(chatCreatorId))
            return;

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
        // Новый создатель получает права
        this.chatCreatorId = newCreatorId;
        adminIds.add(newCreatorId);

        // Обновляем права в member если он загружен
        getMember(newCreatorId).ifPresent(us -> us.setIsAdmin(true));
    }

    public void deleteAdminRightsForAllAdmins() {
        adminIds.stream() // Удаляем права у всех админов, кроме создателя
                .filter(adminId -> !adminId.equals(chatCreatorId))
                .forEach(adminId -> updateAdminRights(adminId, false));
    }
    public void markMemberAsDeleted(Long userId) {
        // Нельзя удалить создателя
        if (userId.equals(chatCreatorId))
            return;

        CacheChatMember member = members.get(userId);
        if (member != null) {
            member.setIsDeleted(true);
            deletedMemberIds.add(userId);
            deletedMemberCount.incrementAndGet();
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
            deletedMemberCount.decrementAndGet();
            if (isAdmin)
                adminIds.add(userId);
        }
    }

    public boolean hasActiveMember(Long userId) {
        return getActiveMember(userId).isPresent();
    }
    public Optional<Boolean> hasMemberAndGetIsActive(Long userId) {
        return getMember(userId).map(CacheChatMember::isActive);
    }
    public Optional<Boolean> isAdmin(Long userId) {
        if (adminIds.contains(userId) && hasActiveMember(userId))
            return Optional.of(true);

        if (members.containsKey(userId))
            return Optional.of(false);

        return Optional.empty();
    }
    public Optional<Long> getAnotherAdmin(Long excludeUserId) {
        return adminIds.stream().filter(us -> hasActiveMember(us) && !us.equals(excludeUserId)).findFirst();
    }
    public Optional<Boolean> isDeleted(Long userId) {
        if (deletedMemberIds.contains(userId))
            return Optional.of(true);

        if (members.containsKey(userId))
            return Optional.of(false);

        return Optional.empty();
    }

    public int getActiveMemberCount() {
        return totalMemberCount.get() - deletedMemberCount.get();
    }
    public int getActiveLoadedCount() {
        return members.size() - deletedMemberCount.get();
    }

    public boolean isFullyLoaded() {
        return members.size() >= totalMemberCount.get();
    }
    public double getLoadPercentage() {
        return (members.size() * 100.0) / totalMemberCount.get();
    }
}