package com.sunrise.controller.request;

import com.sunrise.config.annotation.ValidId;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@lombok.Getter
@lombok.Setter
@lombok.AllArgsConstructor
public class PublicMessageRequest {
    @ValidId
    private Long tempId;

    @NotBlank
    @Size(max = 10000)
    private String text;
}
