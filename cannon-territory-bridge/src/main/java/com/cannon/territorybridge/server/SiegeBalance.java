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
     * Claim HP only drops when attackers hold the ratio against defending NPCs.
     * Zero defenders does NOT mean a free capture — that was Recruits default and caused
     * instant wins when the defending player left but garrison was still on the claim.
     */
    public static boolean canProgressCapture(int attackerCount, int defenderCount) {
        if (attackerCount < minAttackersToStart() || defenderCount <= 0) {
            return false;
        }
        return attackerCount >= defenderCount * captureRatio();
    }

    public static boolean isCaptureProgressing(float siegeSpeedPercent, int health) {
        if (health <= 0) {
            return false;
        }
        return siegeSpeedPercent + 1.0e-4f >= captureRatio();
    }
}
