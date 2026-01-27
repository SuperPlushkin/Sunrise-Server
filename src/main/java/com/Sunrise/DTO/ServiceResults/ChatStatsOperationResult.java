package com.Sunrise.DTO.ServiceResults;


@lombok.Getter
public final class ChatStatsOperationResult extends ServiceResult {
    private final Integer totalMessages;
    private final Integer deletedForAll;
    private final Integer hiddenByUser;
    private final Boolean canClearForAll;

    public ChatStatsOperationResult(Boolean success, String errorMessage, Integer totalMessages, Integer deletedForAll, Integer hiddenByUser, Boolean canClearForAll) {
        super(success, errorMessage);

        this.totalMessages = totalMessages;
        this.deletedForAll = deletedForAll;
        this.hiddenByUser = hiddenByUser;
        this.canClearForAll = canClearForAll;
    }

    public static ChatStatsOperationResult success(Integer totalMessages, Integer deletedForAll, Integer hiddenByUser, Boolean canClearForAll) {
        return new ChatStatsOperationResult(true, null, totalMessages, deletedForAll, hiddenByUser, canClearForAll);
    }
    public static ChatStatsOperationResult error(String errorMessage) {
        return new ChatStatsOperationResult(false, errorMessage, null, null, null, null);
    }
}