package com.Sunrise.Entities.DB;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Objects;

@NoArgsConstructor
@AllArgsConstructor
public class MessageReadStatusId implements Serializable {
    private Long message;
    private Long user;

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof MessageReadStatusId that))
            return false;
        return Objects.equals(message, that.message) && Objects.equals(user, that.user);
    }

    @Override
    public int hashCode() {
        return Objects.hash(message, user);
    }
}
