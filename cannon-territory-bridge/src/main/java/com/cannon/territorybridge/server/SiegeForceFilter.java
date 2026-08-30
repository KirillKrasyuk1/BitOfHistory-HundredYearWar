package com.cannon.territorybridge.server;

import com.cannon.territorybridge.config.BridgeConfig;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import ydmsama.hundred_years_war.main.entity.entities.BaseCombatEntity;
import ydmsama.hundred_years_war.main.entity.entities.HywHorseEntity;
import ydmsama.hundred_years_war.main.entity.entities.tags.SiegeUnit;

import java.util.List;

/** Siege strength is HYW NPC armies only — never player presence on the claim. */
public final class SiegeForceFilter {
    private SiegeForceFilter() {}

    public static boolean countsForSiege(LivingEntity entity) {
        if (entity == null || !entity.isAlive() || entity.isRemoved()) {
            return false;
        }
        if (entity instanceof Player) {
            return BridgeConfig.COUNT_PLAYERS_FOR_SIEGE.get();
        }
        if (!(entity instanceof BaseCombatEntity)) {
            return false;
        }
        if (entity instanceof HywHorseEntity && !BridgeConfig.COUNT_MOUNTED_HORSES_FOR_SIEGE.get()) {
            return false;
        }
        if (entity instanceof SiegeUnit && !BridgeConfig.COUNT_HYW_SIEGE_WEAPONS.get()) {
            return false;
        }
        return true;
    }

    public static void stripNonCountingForces(List<LivingEntity> attackers, List<LivingEntity> defenders) {
        attackers.removeIf(entity -> !countsForSiege(entity));
        defenders.removeIf(entity -> !countsForSiege(entity));
    }
}
