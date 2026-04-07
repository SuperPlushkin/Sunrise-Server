package com.sunrise.core.service.result;


@lombok.Getter
public class ChatStatsResult extends ServiceResultTemplate {
    private final Integer totalMessages;
    private final Integer deletedForAll;
    private final Boolean canDeleteForAll;

    public ChatStatsResult(Boolean success, String errorMessage, Integer totalMessages, Integer deletedForAll, Boolean canDeleteForAll) {
        super(success, errorMessage);

        this.totalMessages = totalMessages;
        this.deletedForAll = deletedForAll;
        this.canDeleteForAll = canDeleteForAll;
    }

    public static ChatStatsResult success(Integer totalMessages, Integer deletedForAll, Boolean canClearForAll) {
        return new ChatStatsResult(true, null, totalMessages, deletedForAll, canClearForAll);
    }
    public static ChatStatsResult error(String errorMessage) {
        return new ChatStatsResult(false, errorMessage, null, null, null);
    }
}