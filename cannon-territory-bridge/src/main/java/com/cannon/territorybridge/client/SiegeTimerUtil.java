package com.cannon.territorybridge.client;

import com.cannon.territorybridge.bridge.BridgeClaimHelper;
import com.cannon.territorybridge.config.BridgeConfig;
import com.cannon.territorybridge.server.SiegeBalance;
import com.talhanation.recruits.world.RecruitsClaim;

/** Estimates remaining siege time from server-synced claim state (Recruits ticks every 5 s). */
public final class SiegeTimerUtil {
    private static final int SIEGE_TICK_SECONDS = 5;
    private static final int TICKS_PER_MINUTE = 12;
    private static final int BASE_DAMAGE = 3;

    private SiegeTimerUtil() {}

    public static int overlayAttackerCount(RecruitsClaim claim) {
        if (claim == null) {
            return 0;
        }
        int cached = SiegeForceClientCache.attackers(claim.getUUID());
        return cached >= 0 ? cached : BridgeClaimHelper.attackerCount(claim);
    }

    public static int overlayDefenderCount(RecruitsClaim claim) {
        if (claim == null) {
            return 0;
        }
        int cached = SiegeForceClientCache.defenders(claim.getUUID());
        return cached >= 0 ? cached : BridgeClaimHelper.defenderCount(claim);
    }

    public static int overlayHealth(RecruitsClaim claim) {
        if (claim == null) {
            return 0;
        }
        int cached = SiegeForceClientCache.health(claim.getUUID());
        return cached >= 0 ? cached : claim.getHealth();
    }

    public static int overlayMaxHealth(RecruitsClaim claim) {
        if (claim == null) {
            return 1;
        }
        int cached = SiegeForceClientCache.maxHealth(claim.getUUID());
        return cached > 0 ? cached : Math.max(1, claim.getMaxHealth());
    }

    public static boolean isCaptureProgressing(RecruitsClaim claim) {
        if (claim == null || !claim.isUnderSiege || overlayHealth(claim) <= 0) {
            return false;
        }
        int attackers = overlayAttackerCount(claim);
        int defenders = overlayDefenderCount(claim);
        if (attackers > 0 || defenders > 0) {
            return SiegeBalance.computeSpeedPercent(attackers, defenders) > 0.0f;
        }
        return SiegeBalance.isCaptureProgressing(claim.getSiegeSpeedPercent(), overlayHealth(claim));
    }

    public static int requiredAttackers(RecruitsClaim claim) {
        return SiegeBalance.requiredAttackersForCapture(overlayDefenderCount(claim));
    }

    public static int estimateRemainingSeconds(RecruitsClaim claim) {
        if (!isCaptureProgressing(claim)) {
            return 0;
        }

        int health = overlayHealth(claim);
        int maxHealth = overlayMaxHealth(claim);
        int attackers = overlayAttackerCount(claim);
        int defenders = overlayDefenderCount(claim);
        float speed = SiegeForceClientCache.speedPercent(claim.getUUID());
        if (speed <= 0.0f) {
            speed = SiegeBalance.computeSpeedPercent(attackers, defenders);
        }
        if (speed <= 0.0f) {
            speed = claim.getSiegeSpeedPercent();
        }
        if (speed <= 0.0f) {
            speed = 1.0f;
        }

        int damage = Math.max(1, Math.round(BASE_DAMAGE * speed));
        if (BridgeConfig.APPLY_SIEGE_SPEED_TO_DAMAGE.get()) {
            int minMinutes = BridgeConfig.MIN_CAPTURE_MINUTES.get();
            int maxPerTick = Math.max(
                    1,
                    (int) Math.ceil(maxHealth / (double) (minMinutes * TICKS_PER_MINUTE))
            );
            damage = Math.min(damage, maxPerTick);
        }

        int ticks = (int) Math.ceil(health / (double) damage);
        return ticks * SIEGE_TICK_SECONDS;
    }

    public static String formatDuration(int totalSeconds) {
        if (totalSeconds <= 0) {
            return "0:00";
        }
        int minutes = totalSeconds / 60;
        int seconds = totalSeconds % 60;
        return minutes + ":" + String.format("%02d", seconds);
    }
}
