package com.Sunrise.Entities.Cache;

import com.Sunrise.Entities.DB.Chat;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Getter
public class ChatMembersContainer {
    private final Long chatId;
    private Long chatCreatorId;
    private final AtomicInteger deletedMemberCount;         // удаленных участников в БД
    private final AtomicInteger totalMemberCount;           // всего участников в БД
    private final AtomicInteger loadedCount;               // сколько загружено в кэш
    private final LocalDateTime createdAt;                  // когда создан контейнер

    private final Map<Long, CacheChatMember> members;      // userId -> CacheChatMember
    private final Set<Long> adminIds;                      // userId
    private final Set<Long> deletedMemberIds;              // userId

    public ChatMembersContainer(Chat chat) {
        this.chatId = chat.getId();
        this.chatCreatorId = chat.getCreatedBy();
        this.deletedMemberCount = new AtomicInteger(chat.getDeletedMembersCount());
        this.totalMemberCount = new AtomicInteger(chat.getMembersCount());
        this.loadedCount = new AtomicInteger(0);
        this.createdAt = LocalDateTime.now();
        this.members = new ConcurrentHashMap<>();
        this.adminIds = ConcurrentHashMap.newKeySet();
        this.deletedMemberIds = ConcurrentHashMap.newKeySet();
        this.adminIds.add(chat.getCreatedBy());
    }

    public List<CacheChatMember> getMembersPage(int offset, int limit) {
        return members.values().stream()
                .skip(offset)
                .filter(m -> !m.getIsDeleted())
                .limit(limit)
                .toList();
    }
    public List<CacheChatMember> getActiveMembers() {
        return members.values().stream()
                .filter(m -> !m.getIsDeleted())
                .toList();
    }
    public Set<Long> getChatAdminsIds(Long chatId) {
        return new HashSet<>(adminIds);
    }
    public List<CacheChatMember> getChatAdmins() {
        return adminIds.stream().map(members::get).toList();
    }
    public Optional<CacheChatMember> getMember(Long userId) {
        return Optional.ofNullable(members.get(userId));
    }

    public void addNewMembers(Collection<CacheChatMember> newMembers) {
        addMembers(newMembers);
        totalMemberCount.addAndGet(newMembers.size());
    }
    public void addMembers(Collection<CacheChatMember> newMembers)  {
        for (CacheChatMember member : newMembers) {
            members.put(member.getUserId(), member);
            if (member.getIsAdmin()) {
                adminIds.add(member.getUserId());
            }
            if (member.getIsDeleted()) {
                deletedMemberIds.add(member.getUserId());
                deletedMemberCount.incrementAndGet();  // <-- Добавляем
            }
        }
        loadedCount.addAndGet(newMembers.size());
    }
    public void addNewMember(CacheChatMember member) {
        addMember(member);
        totalMemberCount.incrementAndGet();
    }
    public void addMember(CacheChatMember member) {
        members.put(member.getUserId(), member);
        if (member.getIsAdmin()) {
            adminIds.add(member.getUserId());
        }
        if (member.getIsDeleted()) {
            deletedMemberIds.add(member.getUserId());
            deletedMemberCount.incrementAndGet();
        }
        loadedCount.incrementAndGet();
    }
    public void updateAdminRights(Long userId, Boolean isAdmin) {
        // Нельзя убрать права у создателя
        if (userId.equals(chatCreatorId))
            return;

        CacheChatMember member = members.get(userId);
        if (member != null && !member.getIsDeleted()) {
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
            if (member.getIsAdmin())
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

    public boolean hasMember(Long userId) {
        return members.containsKey(userId);
    }
    public Optional<Boolean> isAdmin(Long userId) {
        if (adminIds.contains(userId))
            return Optional.of(true);

        if (members.containsKey(userId))
            return Optional.of(false);

        return Optional.empty();
    }
    public Optional<Long> getAnotherAdmin(Long excludeUserId) {
        return adminIds.stream().filter(us -> !us.equals(excludeUserId)).findFirst();
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
        return loadedCount.get() - deletedMemberCount.get();
    }

    public boolean isFullyLoaded() {
        return loadedCount.get() >= totalMemberCount.get();
    }
    public double getLoadPercentage() {
        return (loadedCount.get() * 100.0) / totalMemberCount.get();
    }
}