package com.Sunrise.DTO.ServiceResults;


@lombok.Getter
public class ChatStatsResult extends ServiceResultTemplate {
    private final Integer totalMessages;
    private final Integer deletedForAll;
    private final Integer hiddenByUser;
    private final Boolean canClearForAll;

    public ChatStatsResult(Boolean success, String errorMessage, Integer totalMessages, Integer deletedForAll, Integer hiddenByUser, Boolean canClearForAll) {
        super(success, errorMessage);

        this.totalMessages = totalMessages;
        this.deletedForAll = deletedForAll;
        this.hiddenByUser = hiddenByUser;
        this.canClearForAll = canClearForAll;
    }

    public static ChatStatsResult success(Integer totalMessages, Integer deletedForAll, Integer hiddenByUser, Boolean canClearForAll) {
        return new ChatStatsResult(true, null, totalMessages, deletedForAll, hiddenByUser, canClearForAll);
    }
    public static ChatStatsResult error(String errorMessage) {
        return new ChatStatsResult(false, errorMessage, null, null, null, null);
    }
}