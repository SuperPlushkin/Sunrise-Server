package com.Sunrise.Services.DataServices.CacheEntities;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@lombok.Setter
@lombok.Getter
@lombok.AllArgsConstructor
@lombok.NoArgsConstructor
public class CacheChatMember {
    private Long id;
    private LocalDateTime joinedAt = LocalDateTime.now();
    private LocalDateTime currentJoinDate;
    private Boolean isAdmin = false;
    private Boolean isDeleted = false;
    private List<MembershipPeriod> membershipHistory = new ArrayList<>();

    public CacheChatMember(Long id, Boolean isAdmin) {
        this.id = id;
        this.isAdmin = isAdmin;
        this.joinedAt = LocalDateTime.now();
        this.isDeleted = false;
    }

    public void markAsDeleted() {
        this.isDeleted = true;

        if (!membershipHistory.isEmpty()) {
            MembershipPeriod lastPeriod = membershipHistory.getLast();
            if (lastPeriod.getJoinedAt() == null)
                lastPeriod.setLeftAt(LocalDateTime.now());
        }
    }
    public void restoreMember(Boolean isAdmin) {
        this.isDeleted = false;
        this.isAdmin = isAdmin;
        this.currentJoinDate = LocalDateTime.now();

        membershipHistory.add(new MembershipPeriod(LocalDateTime.now(), null));
    }

    @lombok.Setter
    @lombok.Getter
    public class MembershipPeriod {
        private LocalDateTime joinedAt;
        private LocalDateTime leftAt;

        public MembershipPeriod(LocalDateTime joinedAt, LocalDateTime leftAt) {
            this.joinedAt = joinedAt;
            this.leftAt = leftAt;
        }
    }
}
