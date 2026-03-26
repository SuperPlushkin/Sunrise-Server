package com.Sunrise.DTOs.ServiceResults;

public class SimpleResult extends ServiceResultTemplate {
    public SimpleResult(boolean success, String errorMessage){
        super(success, errorMessage);
    }

    public static SimpleResult success() {
        return new SimpleResult(true, null);
    }
    public static SimpleResult error(String errorMessage) {
        return new SimpleResult(false, errorMessage);
    }
}
