package com.Sunrise.Entities.Cache;

import java.time.LocalDateTime;

@lombok.Setter
@lombok.Getter
@lombok.AllArgsConstructor
@lombok.NoArgsConstructor
public class CacheChatMember {
    private Long userId;
    private String username;
    private String name;
    private LocalDateTime joinedAt; // TODO: БАГ, НАДО ИЗ БД БРАТЬ, НО ПОКА ЧТО ПОХЕР
    private Boolean isAdmin = false;
    private Boolean isDeleted = false;

    public CacheChatMember(CacheUser user, Boolean isAdmin) {
        this.userId = user.getId();
        this.username = user.getUsername();
        this.name = user.getName();
        this.isAdmin = isAdmin;
        this.joinedAt = LocalDateTime.now(); // TODO: БАГ, НАДО ИЗ БД БРАТЬ, НО ПОКА ЧТО ПОХЕР
        this.isDeleted = false;
    }

    public void markAsDeleted() {
        this.isDeleted = true;
    }
    public void restoreMember(Boolean isAdmin) {
        this.isDeleted = false;
        this.isAdmin = isAdmin;
    }
}
