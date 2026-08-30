package com.cannon.territorybridge.server;

import com.cannon.territorybridge.config.BridgeConfig;

/** Recruits siege start vs passive capture (HP drain) rules. */
public final class SiegeBalance {
    private SiegeBalance() {}

    public static int minAttackersToStart() {
        return BridgeConfig.MIN_SIEGE_ATTACKERS.get();
    }

    public static double captureRatio() {
        return BridgeConfig.CAPTURE_ADVANTAGE_RATIO.get();
    }

    /** Attackers needed for passive capture at the current defender count. */
    public static int requiredAttackersForCapture(int defenderCount) {
        if (defenderCount <= 0) {
            return minAttackersToStart();
        }
        return (int) Math.ceil(defenderCount * captureRatio());
    }

    /**
     * Claim HP only drops when attackers meet the minimum headcount and (if enabled) the capture ratio.
     * Zero defenders allows slow baseline capture only when there is no committed garrison left.
     */
    public static boolean canProgressCapture(int attackerCount, int defenderCount) {
        if (attackerCount < minAttackersToStart()) {
            return false;
        }
        if (defenderCount <= 0) {
            return true;
        }
        if (!BridgeConfig.REQUIRE_ATTACKER_ADVANTAGE.get()) {
            return true;
        }
        return attackerCount >= defenderCount * captureRatio();
    }

    /** Whether a siege may transfer ownership — blocks completion while committed garrison UUIDs remain. */
    public static boolean canCompleteCapture(
            int attackerCount,
            int defenderCount,
            boolean hasCommittedDefenders,
            boolean captureInProgress
    ) {
        if (!canProgressCapture(attackerCount, defenderCount)) {
            return false;
        }
        if (captureInProgress && hasCommittedDefenders && defenderCount <= 0) {
            return false;
        }
        return true;
    }

    /**
     * Siege speed multiplier passed to Recruits damage and the client overlay.
     * Below {@link #captureRatio()} capture stalls; above it, speed scales with attacker:defender ratio.
     */
    public static float computeSpeedPercent(int attackerCount, int defenderCount) {
        if (!canProgressCapture(attackerCount, defenderCount)) {
            return 0.0f;
        }
        if (defenderCount <= 0) {
            return (float) captureRatio();
        }
        return (float) attackerCount / defenderCount;
    }

    public static boolean isCaptureProgressing(float siegeSpeedPercent, int health) {
        if (health <= 0) {
            return false;
        }
        return siegeSpeedPercent + 1.0e-4f >= captureRatio();
    }
}
