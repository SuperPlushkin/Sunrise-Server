package com.sunrise.core.service.result;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;

@lombok.Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public final class ResultNoArgs {
    private final boolean success;
    private final String error;
    private final String operationText = "Operation successful";

    public static ResultNoArgs success() {
        return new ResultNoArgs(true, null);
    }
    public static ResultNoArgs error(String error) {
        return new ResultNoArgs(false, error);
    }
}