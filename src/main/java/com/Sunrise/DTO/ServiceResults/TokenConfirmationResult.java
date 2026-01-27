package com.Sunrise.DTO.ServiceResults;

import java.util.Date;

@lombok.Getter
@lombok.AllArgsConstructor
public class TokenConfirmationResult {
    private boolean success;
    private String operationText;
}
