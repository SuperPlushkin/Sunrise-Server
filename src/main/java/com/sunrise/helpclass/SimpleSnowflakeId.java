package com.sunrise.helpclass;

import java.security.SecureRandom;

/*
 * Формат: 64 бита
 * - 45 бит: timestamp - хватит с 2024 до ~3138 года
 * - 19 бит: random - 524288 вариантов
 */
public final class SimpleSnowflakeId {

    private static final long CUSTOM_TIME = 1704067200000L; // ОТ 2024-01-01
    private static final long TIMESTAMP_BITS = 45L;
    private static final long RANDOM_BITS = 64L - TIMESTAMP_BITS;
    private static final long MAX_RANDOM = (1L << RANDOM_BITS) - 1; // сдвигает биты на RANDOM_BITS вправо, чтоб не вышло за пределы
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    public static long nextId() {
        long timestamp = System.currentTimeMillis() - CUSTOM_TIME;
        long random = SECURE_RANDOM.nextLong() & MAX_RANDOM;
        return (timestamp << RANDOM_BITS) | random;
    }
}
