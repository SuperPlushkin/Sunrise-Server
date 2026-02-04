package com.Sunrise.Services.DataServices.CacheEntities;

import java.time.LocalDateTime;
import java.util.List;

@lombok.Setter
@lombok.Getter
@lombok.AllArgsConstructor
@lombok.NoArgsConstructor
public class CacheChatMember {
    private Long userId;
    private LocalDateTime joinedAt;
    private LocalDateTime currentJoinDate;
    private Boolean isAdmin = false;
    private Boolean isDeleted = false;
    private List<MembershipPeriod> membershipHistory;

    public CacheChatMember(Long userId, Boolean isAdmin) {
        this.userId = userId;
        this.isAdmin = isAdmin;
        this.joinedAt = LocalDateTime.now();
        this.currentJoinDate = LocalDateTime.now();
        this.isDeleted = false;
        this.membershipHistory = List.of(new MembershipPeriod(this.joinedAt, null));
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
