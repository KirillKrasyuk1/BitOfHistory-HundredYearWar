package com.cannon.territorybridge.server;

import com.cannon.territorybridge.config.BridgeConfig;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.scores.Team;
import ydmsama.hundred_years_war.main.entity.entities.BaseCombatEntity;
import ydmsama.hundred_years_war.main.entity.entities.HywHorseEntity;
import ydmsama.hundred_years_war.main.entity.entities.tags.SiegeUnit;

import java.util.UUID;

/** Resolves Recruits siege faction for HYW units without touching the scoreboard. */
public final class HywSiegeHelper {
    private HywSiegeHelper() {}

    public static Team resolveOwnerTeam(LivingEntity entity) {
        if (!BridgeConfig.SYNC_HYW_TEAMS.get() || !(entity instanceof BaseCombatEntity hyw)) {
            return null;
        }
        if (hyw instanceof SiegeUnit) {
            return null;
        }
        if (!BridgeConfig.COUNT_MOUNTED_HORSES_FOR_SIEGE.get() && hyw instanceof HywHorseEntity) {
            return null;
        }
        UUID ownerId = hyw.getOwnerUUID();
        if (ownerId == null || !(entity.level() instanceof ServerLevel level)) {
            return null;
        }
        ServerPlayer owner = level.getServer().getPlayerList().getPlayer(ownerId);
        if (owner == null) {
            return null;
        }
        return owner.getTeam();
    }
}
