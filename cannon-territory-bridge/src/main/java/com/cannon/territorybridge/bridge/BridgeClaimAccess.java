package com.cannon.territorybridge.bridge;

/** Extra siege force fields synced with Recruits claim network payloads. */
public interface BridgeClaimAccess {
    int cannon$getBridgeAttackerCount();

    int cannon$getBridgeDefenderCount();

    void cannon$setBridgeAttackerCount(int count);

    void cannon$setBridgeDefenderCount(int count);
}
