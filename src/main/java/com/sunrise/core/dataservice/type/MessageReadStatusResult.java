package com.sunrise.core.dataservice.type;

import java.time.LocalDateTime;

public interface MessageReadStatusResult {
    Long getUserId();
    LocalDateTime getReadAt();
}
