package com.cannon.territorybridge.server;

import com.cannon.territorybridge.CannonTerritoryBridge;
import com.cannon.territorybridge.config.BridgeConfig;
import com.talhanation.recruits.world.RecruitsFaction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import ydmsama.hundred_years_war.main.utils.RelationSystem;
import ydmsama.hundred_years_war.main.utils.TeamRelationData;
import ydmsama.hundred_years_war.main.utils.VanillaTeamUuidCache;

import java.util.UUID;

/** Safe, deferred Recruits faction ↔ HYW RelationSystem linking. */
public final class HywRelationBridge {
    private HywRelationBridge() {}

    public static void linkRecruitsFactionDeferred(ServerLevel level, RecruitsFaction faction, ServerPlayer player) {
        if (!BridgeConfig.SYNC_DIPLOMACY_TO_HYW.get() || faction == null || level == null) {
            return;
        }
        level.getServer().execute(() -> linkRecruitsFaction(faction, player));
    }

    public static void linkRecruitsFaction(RecruitsFaction faction, ServerPlayer player) {
        if (!BridgeConfig.SYNC_DIPLOMACY_TO_HYW.get() || faction == null) {
            return;
        }
        try {
            UUID teamUuid = VanillaTeamUuidCache.get(faction.getStringID());
            String displayName = faction.getTeamDisplayName() != null
                    ? faction.getTeamDisplayName()
                    : faction.getStringID();
            RelationSystem.getOrCreateTeamRelationData(teamUuid, displayName);

            UUID memberId = resolveMemberId(faction, player);
            if (memberId == null) {
                return;
            }

            UUID existingTeam = RelationSystem.getPlayerTeamUUID(memberId);
            if (existingTeam != null && !existingTeam.equals(teamUuid)) {
                RelationSystem.setRelation(
                        existingTeam,
                        teamUuid,
                        RelationSystem.RelationType.FRIENDLY
                );
            }

            RelationSystem.joinTeam(memberId, teamUuid, TeamRelationData.MemberType.OWNER);
        } catch (Throwable t) {
            CannonTerritoryBridge.LOGGER.warn(
                    "HYW relation link failed for faction {}: {}",
                    faction.getStringID(),
                    t.toString()
            );
        }
    }

    public static void mirrorDiplomacy(UUID factionA, UUID factionB, RelationSystem.RelationType relation) {
        if (!BridgeConfig.SYNC_DIPLOMACY_TO_HYW.get()) {
            return;
        }
        try {
            RelationSystem.setRelation(factionA, factionB, relation);
            RelationSystem.setRelation(factionB, factionA, relation);
        } catch (Throwable t) {
            CannonTerritoryBridge.LOGGER.warn(
                    "HYW diplomacy mirror failed ({} ↔ {}): {}",
                    factionA,
                    factionB,
                    t.toString()
            );
        }
    }

    public static void disbandFaction(RecruitsFaction faction) {
        if (!BridgeConfig.SYNC_DIPLOMACY_TO_HYW.get() || faction == null) {
            return;
        }
        try {
            UUID teamUuid = VanillaTeamUuidCache.get(faction.getStringID());
            UUID leaderId = faction.getTeamLeaderUUID();
            if (leaderId != null) {
                RelationSystem.disbandTeam(teamUuid, leaderId);
            }
        } catch (Throwable t) {
            CannonTerritoryBridge.LOGGER.warn(
                    "HYW faction disband failed for {}: {}",
                    faction.getStringID(),
                    t.toString()
            );
        }
    }

    private static UUID resolveMemberId(RecruitsFaction faction, ServerPlayer player) {
        if (player != null) {
            return player.getUUID();
        }
        return faction.getTeamLeaderUUID();
    }
}
