package com.sunrise.core.dataservice.type;

import lombok.Getter;

public enum ChatType {
    PERSONAL(2, 2, false),
    SMALL_GROUP(1, 50, true),
    BIG_GROUP(1, 1000, true);

    private final int minMemberCount;
    private final int maxMemberCount;
    @Getter
    private final boolean isChangeable; // можно ли сменить тип после создания

    ChatType(int minMemberCount, int maxMemberCount, boolean isChangeable) {
        this.minMemberCount = minMemberCount;
        this.maxMemberCount = maxMemberCount;
        this.isChangeable = isChangeable;
    }

    public boolean isMembersInBound(int memberCount) { return minMemberCount <= memberCount && memberCount <= maxMemberCount; }
    public boolean isPersonal() { return this == PERSONAL; }
    public boolean isNotPersonal() { return this == PERSONAL; }
    public boolean isActionsEnabled() { return this != BIG_GROUP; } // только в больших группах отключаем действия
}