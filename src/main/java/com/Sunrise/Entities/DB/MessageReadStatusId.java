package com.Sunrise.Entities.DB;
import java.io.Serializable;
import java.util.Objects;

public class MessageReadStatusId implements Serializable {
    private Long message;
    private Long user;

    public MessageReadStatusId() {}
    public MessageReadStatusId(Long message, Long user) {
        this.message = message;
        this.user = user;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MessageReadStatusId)) return false;
        MessageReadStatusId that = (MessageReadStatusId) o;
        return Objects.equals(message, that.message) && Objects.equals(user, that.user);
    }

    @Override
    public int hashCode() {
        return Objects.hash(message, user);
    }
}
