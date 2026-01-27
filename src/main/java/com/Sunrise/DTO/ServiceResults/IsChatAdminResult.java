package com.Sunrise.DTO.ServiceResults;

@lombok.Getter
public class IsChatAdminResult extends ServiceResult {
    private final Boolean isChatAdmin;

    public IsChatAdminResult(boolean success, String errorMessage, Boolean isChatAdmin) {
        super(success, errorMessage);
        this.isChatAdmin = isChatAdmin;
    }

    public static IsChatAdminResult success(Boolean isChatAdmin) {
        return new IsChatAdminResult(true, null, isChatAdmin);
    }
    public static IsChatAdminResult error(String errorMessage) {
        return new IsChatAdminResult(false, errorMessage, null);
    }
}
