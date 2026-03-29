package com.Sunrise.DTOs.ServiceResults;

import com.Sunrise.Entities.DTOs.UserProfileDTO;
import lombok.Getter;

@Getter
public class UpdateProfileResult {
    private final boolean success;
    private final String errorMessage;
    private final UserProfileDTO profile;

    private UpdateProfileResult(boolean success, String errorMessage, UserProfileDTO profile) {
        this.success = success;
        this.errorMessage = errorMessage;
        this.profile = profile;
    }

    public static UpdateProfileResult success(UserProfileDTO profile) {
        return new UpdateProfileResult(true, null, profile);
    }

    public static UpdateProfileResult error(String errorMessage) {
        return new UpdateProfileResult(false, errorMessage, null);
    }
}