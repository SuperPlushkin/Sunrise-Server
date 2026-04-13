package com.sunrise.controller.request;

import com.sunrise.core.dataservice.type.ChatType;
import jakarta.validation.constraints.NotNull;

@lombok.Getter
@lombok.AllArgsConstructor
@lombok.NoArgsConstructor
public class UpdateChatTypeRequest {
    @NotNull(message = "groupType is required")
    private ChatType groupType;
}
