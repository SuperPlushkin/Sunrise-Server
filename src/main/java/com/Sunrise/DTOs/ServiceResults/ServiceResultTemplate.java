package com.Sunrise.DTOs.ServiceResults;

@lombok.Getter
@lombok.AllArgsConstructor
public abstract class ServiceResultTemplate {
    private boolean success;
    private String errorMessage;
}
