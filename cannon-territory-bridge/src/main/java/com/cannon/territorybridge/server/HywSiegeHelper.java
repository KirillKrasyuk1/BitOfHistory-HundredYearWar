package com.cannon.territorybridge.server;

import com.cannon.territorybridge.CannonTerritoryBridge;
import com.cannon.territorybridge.config.BridgeConfig;
import com.talhanation.recruits.FactionEvents;
import com.talhanation.recruits.world.RecruitsFaction;
import com.talhanation.recruits.world.RecruitsPlayerInfo;
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
        try {
            if (!BridgeConfig.SYNC_HYW_TEAMS.get() || !entity.isAlive() || entity.isRemoved()) {
                return null;
            }
            if (!(entity instanceof BaseCombatEntity hyw)) {
                return null;
            }
            if (hyw instanceof SiegeUnit) {
                return null;
            }
            if (!BridgeConfig.COUNT_MOUNTED_HORSES_FOR_SIEGE.get() && hyw instanceof HywHorseEntity) {
                return null;
            }
            if (!(entity.level() instanceof ServerLevel level)) {
                return null;
            }
            UUID ownerId = hyw.getOwnerUUID();
            if (ownerId == null) {
                return null;
            }

            ServerPlayer onlineOwner = level.getServer().getPlayerList().getPlayer(ownerId);
            if (onlineOwner != null && onlineOwner.getTeam() != null) {
                return onlineOwner.getTeam();
            }

            RecruitsFaction faction = findRecruitsFactionForOwner(ownerId);
            if (faction == null) {
                return null;
            }
            return level.getScoreboard().getPlayerTeam(faction.getStringID());
        } catch (Throwable t) {
            CannonTerritoryBridge.LOGGER.warn(
                    "HYW siege team lookup failed for {}: {}",
                    entity.getType(),
                    t.toString()
            );
            return null;
        }
    }

    static RecruitsFaction findRecruitsFactionForOwner(UUID ownerId) {
        if (FactionEvents.recruitsFactionManager == null) {
            return null;
        }
        for (RecruitsFaction faction : FactionEvents.recruitsFactionManager.getFactions()) {
            if (ownerId.equals(faction.getTeamLeaderUUID())) {
                return faction;
            }
            for (RecruitsPlayerInfo member : faction.getMembers()) {
                if (ownerId.equals(member.getUUID())) {
                    return faction;
                }
            }
        }
        return null;
    }
}
