package com.sunrise.core.service.result;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public final class ResultOneArg<T> {
    private final boolean success;
    private final String error;
    private final T result;

    public static <T> ResultOneArg<T> success(T arg) {
        return new ResultOneArg<>(true, null, arg);
    }

    public static <T> ResultOneArg<T> error(String error) {
        return new ResultOneArg<>(false, error, null);
    }
}