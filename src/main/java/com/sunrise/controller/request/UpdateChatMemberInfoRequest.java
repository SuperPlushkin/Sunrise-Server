package com.sunrise.controller.request;

import jakarta.validation.constraints.Size;

@lombok.Getter
@lombok.AllArgsConstructor
@lombok.NoArgsConstructor
public class UpdateChatMemberInfoRequest {
    @Size(max = 20, message = "tag mustn`t be more than 20 characters")
    private String tag;
}
