package com.Sunrise.DTO.ServiceResults;

@lombok.Getter
public class VisibleMessagesCountResult extends ServiceResult {
    private final Integer visibleMessagesCount;

    public VisibleMessagesCountResult(boolean success, String errorMessage, Integer visibleMessagesCount) {
        super(success, errorMessage);

        this.visibleMessagesCount = visibleMessagesCount;
    }

    public static VisibleMessagesCountResult success(Integer visibleMessagesCount) {
        return new VisibleMessagesCountResult(true, null, visibleMessagesCount);
    }
    public static VisibleMessagesCountResult error(String errorMessage) {
        return new VisibleMessagesCountResult(false, errorMessage, null);
    }
}
