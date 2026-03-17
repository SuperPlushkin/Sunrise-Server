package com.Sunrise.Entities.Cache;

import lombok.Getter;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

@Getter
public class CacheChatMessagesContainer {

    private static final int MAX_CACHE_SIZE = 1000;

    private final long chatId;
    private final AtomicInteger totalMessagesCount;

    private final ConcurrentNavigableMap<Long, CacheMessage> messages = new ConcurrentSkipListMap<>();
    private final AtomicLong oldestId = new AtomicLong(-1); // локальный максимум (в кеше)
    private final AtomicLong newestId = new AtomicLong(-1); // локальный минимум (в кеше)

    private final LocalDateTime createdAt = LocalDateTime.now();

    public CacheChatMessagesContainer(long chatId, CacheMessage newestMessage, int totalCount) {
        this.chatId = chatId;
        this.totalMessagesCount = new AtomicInteger(totalCount);
        addMessage(newestMessage);
    }


    // ========== ДОБАВЛЕНИЕ СООБЩЕНИЙ ==========

    public void addMessage(CacheMessage message) {
        if (message == null) return;

        long newId = message.getId();
        if (messages.containsKey(newId)) return;

        messages.put(newId, message);

        // Если превысили лимит - удаляем самое старое
        if (messages.size() > MAX_CACHE_SIZE) {
            Map.Entry<Long, CacheMessage> firstEntry = messages.firstEntry();
            if (firstEntry != null && firstEntry.getKey() < newId) {
                messages.remove(firstEntry.getKey());

                // Обновляем oldestId
                Map.Entry<Long, CacheMessage> newFirst = messages.firstEntry();
                oldestId.set(newFirst != null ? newFirst.getKey() : -1);
            }
        }

        // Обновляем индексы
        oldestId.updateAndGet(oldId -> oldId == -1 ? newId : Math.min(oldId, newId));
        newestId.updateAndGet(oldId -> oldId == -1 ? newId : Math.max(oldId, newId));
    }
    public void addMessages(Collection<CacheMessage> newMessages) {
        if (newMessages == null || newMessages.isEmpty()) return;

        List<CacheMessage> sortedMsg = new ArrayList<>(newMessages);
        sortedMsg.sort(Comparator.comparingLong(CacheMessage::getId).reversed()); // сначала новые

        for (CacheMessage message : sortedMsg) {
            addMessage(message);
            if (messages.size() >= MAX_CACHE_SIZE && message.getId() <= oldestId.get()) {
                break; // Если кеш полон и мы дошли до старых сообщений - можно прекратить
            }
        }
    }

    public void addNewMessage(CacheMessage message) {
        addMessage(message);
        totalMessagesCount.incrementAndGet();
    }


    // ========== МЕТОДЫ ДЛЯ ПРОЧТЕНИЯ ==========

    public void markAsRead(long messageId, long userId) {
        CacheMessage msg = messages.get(messageId);
        if (msg == null || msg.isHiddenByAdmin()) return;

        msg.markAsReadByUser(userId);
    }
    public boolean isReadByUser(long messageId, long userId) {
        CacheMessage msg = messages.get(messageId);
        return msg != null && msg.isReadByUser(userId);
    }


    // ========== ПОЛУЧЕНИЕ СООБЩЕНИЙ ==========

    public Optional<CacheMessage> getMessage(long messageId) {
        return Optional.ofNullable(messages.get(messageId));
    }
    public Optional<CacheMessage> getFirstMessage() {
        if (messages.isEmpty())
            return Optional.empty();

        return Optional.ofNullable(messages.descendingMap().firstEntry()).map(Map.Entry::getValue);
    }
    public List<CacheMessage> getFirstMessages(int limit) {
        return messages.descendingMap().values().stream()
                .limit(limit).toList();
    }
    public List<CacheMessage> getMessagesBefore(long beforeId, int limit) {
        return messages.headMap(beforeId, false)
                .descendingMap().values().stream()
                .limit(limit).toList();
    }
    public List<CacheMessage> getMessagesAfter(long afterId, int limit) {
        return messages.tailMap(afterId, false)
                .values().stream()
                .limit(limit).toList();
    }
    public List<CacheMessage> getMessagesInRange(long fromId, long toId) {
        return new ArrayList<>(messages.subMap(fromId, true, toId, true).values());
    }


    // ========== ПРОВЕРКИ ==========

    public boolean isLoadedBefore(long messageId) {
        long oldest = oldestId.get();
        return oldest != -1 && oldest <= messageId;
    }
    public boolean isLoadedAfter(long messageId) {
        long newest = newestId.get();;
        return newest != -1 && newest >= messageId;
    }

    public Long getOldestId() {
        long id = oldestId.get();
        return id != -1 ? id : null;
    }
    public int getTotalMessagesCount() {
        return totalMessagesCount.get();
    }


    // ========== ИНФОРМАЦИЯ ==========

    public double getLoadPercentage() {
        int total = totalMessagesCount.get();
        if (total == 0) return 100.0;
        return (messages.size() * 100.0) / total;
    }
    public boolean isFullyLoaded() {
        return messages.size() >= totalMessagesCount.get();
    }
    public boolean hasSomeMessagesDB() {
        return totalMessagesCount.get() - messages.size() > 0;
    }

    public boolean hasMessages() {
        return totalMessagesCount.get() > 0;
    }
}