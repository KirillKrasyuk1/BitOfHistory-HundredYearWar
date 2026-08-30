package com.cannon.territorybridge.bridge;

import com.talhanation.recruits.world.RecruitsClaim;

public final class BridgeClaimHelper {
    private BridgeClaimHelper() {}

    public static int attackerCount(RecruitsClaim claim) {
        if (claim instanceof BridgeClaimAccess access) {
            return access.cannon$getBridgeAttackerCount();
        }
        return 0;
    }

    public static int defenderCount(RecruitsClaim claim) {
        if (claim instanceof BridgeClaimAccess access) {
            return access.cannon$getBridgeDefenderCount();
        }
        return 0;
    }
}
