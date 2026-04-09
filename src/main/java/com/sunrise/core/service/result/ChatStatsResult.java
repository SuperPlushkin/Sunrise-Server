package com.sunrise.core.service.result;

public record ChatStatsResult(int totalMessages, int deletedForAll, boolean canDeleteForAll) { }