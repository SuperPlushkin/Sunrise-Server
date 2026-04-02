package com.Sunrise.Entities.Caches;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

@lombok.Getter
@lombok.Setter
@lombok.AllArgsConstructor
public class CacheChat {
    private static final int MAX_MESSAGE_CACHE_SIZE = 1000;

    // Chat fields
    private long id;
    private String name;
    private boolean isGroup;
    private Long opponentId;
    private int membersCount;
    private int deletedMembersCount;
    private LocalDateTime createdAt;
    private long createdBy;
    private LocalDateTime deletedAt;
    private boolean isDeleted;

    // Message fields
    private final ConcurrentNavigableMap<Long, CacheMessage> messages = new ConcurrentSkipListMap<>();
    private final Map<Long, AtomicLong> lastReadMessageIdByUserIds = new ConcurrentHashMap<>();

    private final AtomicInteger messagesCount;
    private final AtomicInteger deletedMessagesCount;
    private final AtomicLong oldestMessageId = new AtomicLong(-1); // локальный максимум (в кеше)
    private final AtomicLong newestMessageId = new AtomicLong(-1); // локальный минимум (в кеше)

    // Metadata
    private final LocalDateTime cachedAt = LocalDateTime.now();

    public CacheChat(long id, String name, boolean isGroup, Long opponentId, int membersCount, int deletedMembersCount, LocalDateTime createdAt, long createdBy,
                     LocalDateTime deletedAt, boolean isDeleted, CacheMessage newestMessage, int messagesCount, int deletedMessagesCount) {
        this.id = id;
        this.name = name;
        this.isGroup = isGroup;
        this.opponentId = opponentId;
        this.membersCount = membersCount;
        this.deletedMembersCount = deletedMembersCount;
        this.createdAt = createdAt;
        this.createdBy = createdBy;
        this.deletedAt = deletedAt;
        this.isDeleted = isDeleted;
        this.messagesCount = new AtomicInteger(messagesCount);
        this.deletedMessagesCount = new AtomicInteger(deletedMessagesCount);
        addMessage(newestMessage);
    }

    public boolean isActive() {
        return !isDeleted;
    }
    public void delete(){
        this.deletedAt = LocalDateTime.now();
        this.isDeleted = true;
    }
    public void restore(){
        this.deletedAt = null;
        this.isDeleted = false;
    }

    public void updateFromCache(CacheChat cacheChat){
        this.name = cacheChat.getName();
        this.deletedMembersCount = cacheChat.getDeletedMembersCount();
        this.membersCount = cacheChat.getMembersCount();
        this.createdBy = cacheChat.getCreatedBy();
        this.isGroup = cacheChat.isGroup();
        this.createdAt = cacheChat.getCreatedAt();
        this.deletedAt = cacheChat.getDeletedAt();
        this.isDeleted = cacheChat.isDeleted();
    }

    // ========== ДОБАВЛЕНИЕ СООБЩЕНИЙ ==========

    public void addMessage(CacheMessage message) {
        if (message == null) return;

        long newId = message.getId();
        if (messages.containsKey(newId)) return;

        messages.put(newId, message);

        // Если превысили лимит - удаляем самое старое
        if (messages.size() > MAX_MESSAGE_CACHE_SIZE) {
            Map.Entry<Long, CacheMessage> firstEntry = messages.firstEntry();
            if (firstEntry != null && firstEntry.getKey() < newId) {
                messages.remove(firstEntry.getKey());

                // Обновляем oldestId
                Map.Entry<Long, CacheMessage> newFirst = messages.firstEntry();
                oldestMessageId.set(newFirst != null ? newFirst.getKey() : -1);
            }
        }

        // Обновляем индексы
        oldestMessageId.updateAndGet(oldId -> oldId == -1 ? newId : Math.min(oldId, newId));
        newestMessageId.updateAndGet(oldId -> oldId == -1 ? newId : Math.max(oldId, newId));
    }
    public void addMessages(Collection<CacheMessage> newMessages) {
        if (newMessages == null || newMessages.isEmpty()) return;

        List<CacheMessage> sortedMsg = new ArrayList<>(newMessages);
        sortedMsg.sort(Comparator.comparingLong(CacheMessage::getId).reversed()); // сначала новые

        for (CacheMessage message : sortedMsg) {
            addMessage(message);
            if (messages.size() >= MAX_MESSAGE_CACHE_SIZE && message.getId() <= oldestMessageId.get()) {
                break; // Если кеш полон и мы дошли до старых сообщений - можно прекратить
            }
        }
    }

    public void addNewMessage(CacheMessage message) {
        addMessage(message);
        messagesCount.incrementAndGet();
    }


    // ========== МЕТОДЫ ДЛЯ ПРОЧТЕНИЯ ==========

    public void updateLastReadByUser(long userId, long messageId) {
        lastReadMessageIdByUserIds.computeIfAbsent(userId, k -> new AtomicLong(0))
                .updateAndGet(current -> Math.max(current, messageId));
    }
    public Optional<Boolean> isReadByUserOptional(long messageId, long userId) {
        AtomicLong read = lastReadMessageIdByUserIds.get(userId);
        if (read == null) return Optional.empty();
        return Optional.of(read.get() > messageId);
    }
    public boolean isReadByUser(long messageId, long userId) {
        AtomicLong read = lastReadMessageIdByUserIds.get(userId);
        return read != null && read.get() > messageId;
    }
    public boolean hasUserReadStatus(long userId) {
        return lastReadMessageIdByUserIds.get(userId) != null;
    }


    // ========== ПОЛУЧЕНИЕ СООБЩЕНИЙ ==========

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


    // ========== ДЕЙСТВИЯ С СООБЩЕНИЯМИ ==========

    public boolean hasMessages() {
        return messagesCount.get() > 0;
    }

    public void deleteMessage(long messageId) {
        CacheMessage message = messages.get(messageId);
        if (message == null) return;

        message.delete();
        deletedMessagesCount.incrementAndGet();
    }


    // ========== ПРОВЕРКИ ==========

    public boolean isLoadedBefore(long messageId) {
        long oldest = oldestMessageId.get();
        return oldest != -1 && oldest <= messageId;
    }
    public boolean isLoadedAfter(long messageId) {
        long newest = newestMessageId.get();;
        return newest != -1 && newest >= messageId;
    }

    public Long getOldestId() {
        long id = oldestMessageId.get();
        return id != -1 ? id : null;
    }
    public Long getNewestId() {
        long id = newestMessageId.get();
        return id != -1 ? id : null;
    }

    public int getDeletedMessagesCount() {
        return deletedMessagesCount.get();
    }
    public int getMessagesCount() {
        return messagesCount.get();
    }


    // ========== ИНФОРМАЦИЯ ==========

    public double getLoadPercentage() {
        int total = messagesCount.get();
        if (total == 0) return 100.0;
        return (messages.size() * 100.0) / total;
    }
    public boolean isFullyLoaded() {
        return messages.size() >= messagesCount.get();
    }
}
