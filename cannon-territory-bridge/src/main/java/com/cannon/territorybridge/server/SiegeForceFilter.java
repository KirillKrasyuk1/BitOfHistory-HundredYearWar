package com.cannon.territorybridge.server;

import com.cannon.territorybridge.config.BridgeConfig;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import java.util.List;

/** Siege strength is based on NPC armies, not player presence on the claim. */
public final class SiegeForceFilter {
    private SiegeForceFilter() {}

    public static boolean countsForSiege(LivingEntity entity) {
        if (!BridgeConfig.COUNT_PLAYERS_FOR_SIEGE.get() && entity instanceof Player) {
            return false;
        }
        return true;
    }

    public static void stripNonCountingForces(List<LivingEntity> attackers, List<LivingEntity> defenders) {
        attackers.removeIf(entity -> !countsForSiege(entity));
        defenders.removeIf(entity -> !countsForSiege(entity));
    }
}
