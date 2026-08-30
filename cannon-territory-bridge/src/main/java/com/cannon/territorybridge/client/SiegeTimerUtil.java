package com.cannon.territorybridge.client;

import com.cannon.territorybridge.config.BridgeConfig;
import com.talhanation.recruits.world.RecruitsClaim;

/** Estimates remaining siege time from synced claim state (Recruits ticks every 5 s). */
public final class SiegeTimerUtil {
    private static final int SIEGE_TICK_SECONDS = 5;
    private static final int TICKS_PER_MINUTE = 12;
    private static final int BASE_DAMAGE = 3;

    private SiegeTimerUtil() {}

    public static int estimateRemainingSeconds(RecruitsClaim claim) {
        if (claim == null || !claim.isUnderSiege || claim.getHealth() <= 0) {
            return 0;
        }

        int maxHealth = claim.getMaxHealth();
        int health = claim.getHealth();
        float speed = claim.getSiegeSpeedPercent();
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
