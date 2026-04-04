package com.sunrise.core.service.result;

@lombok.Getter
@lombok.AllArgsConstructor
public abstract class ServiceResultTemplate {
    private boolean success;
    private String errorMessage;
}
