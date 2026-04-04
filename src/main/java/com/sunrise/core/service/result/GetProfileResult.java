package com.sunrise.core.service.result;

import com.sunrise.entity.dto.UserProfileDTO;
import lombok.Getter;

@Getter
public class GetProfileResult {
    private final boolean success;
    private final String errorMessage;
    private final UserProfileDTO profile;

    private GetProfileResult(boolean success, String errorMessage, UserProfileDTO profile) {
        this.success = success;
        this.errorMessage = errorMessage;
        this.profile = profile;
    }

    public static GetProfileResult success(UserProfileDTO profile) {
        return new GetProfileResult(true, null, profile);
    }

    public static GetProfileResult error(String errorMessage) {
        return new GetProfileResult(false, errorMessage, null);
    }
}