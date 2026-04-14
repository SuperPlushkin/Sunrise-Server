package com.sunrise.controller.request;

import jakarta.validation.constraints.NotNull;

@lombok.Data
@lombok.AllArgsConstructor
@lombok.NoArgsConstructor
public class UpdateSelfChatSettingsRequest {
    @NotNull(message = "isPinned is required")
    public Boolean isPinned;
}
