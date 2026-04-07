package com.sunrise.controller.request;

import com.sunrise.config.annotation.ValidId;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PrivateMessageRequest {
    @NotBlank
    @Size(max = 10000)
    private String text;

    @ValidId
    private Long userToSendId;
}