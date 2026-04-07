package com.sunrise.controller.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@lombok.Getter
@lombok.Setter
@lombok.AllArgsConstructor
public class PublicMessageRequest {
    @NotBlank
    @Size(max = 10000)
    private String text;
}
