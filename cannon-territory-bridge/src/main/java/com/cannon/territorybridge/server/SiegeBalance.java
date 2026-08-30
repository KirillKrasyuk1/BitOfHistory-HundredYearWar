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

    /** Siege may exist, but claim HP only drops at this attacker:defender ratio (default 2:1). */
    public static boolean canProgressCapture(int attackerCount, int defenderCount) {
        if (attackerCount < minAttackersToStart()) {
            return false;
        }
        if (defenderCount <= 0) {
            return true;
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
