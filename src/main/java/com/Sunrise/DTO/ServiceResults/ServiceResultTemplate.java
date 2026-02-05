package com.Sunrise.DTO.ServiceResults;

@lombok.Getter
@lombok.AllArgsConstructor
public abstract class ServiceResultTemplate {
    private boolean success;
    private String errorMessage;
}
