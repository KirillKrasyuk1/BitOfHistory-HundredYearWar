package com.cannon.territorybridge.server;

import com.cannon.territorybridge.config.BridgeConfig;
import com.talhanation.recruits.DiplomacyEvent;
import com.talhanation.recruits.FactionEvent;
import com.talhanation.recruits.RecruitEvent;
import com.talhanation.recruits.world.RecruitsDiplomacyManager;
import com.talhanation.recruits.world.RecruitsFaction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.AABB;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Team;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import ydmsama.hundred_years_war.main.entity.entities.BaseCombatEntity;
import ydmsama.hundred_years_war.main.entity.entities.HywHorseEntity;
import ydmsama.hundred_years_war.main.utils.RelationSystem;
import ydmsama.hundred_years_war.main.utils.VanillaTeamUuidCache;

import java.util.UUID;

public final class BridgeServerEvents {
    private int teamSyncCooldown;

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onRecruitHired(RecruitEvent.Hired event) {
        if (BridgeConfig.BLOCK_RECRUIT_HIRE.get()) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onRecruitsEntityJoin(EntityJoinLevelEvent event) {
        if (!BridgeConfig.BLOCK_RECRUITS_ENTITIES.get()) {
            return;
        }
        if (event.getLevel().isClientSide()) {
            return;
        }
        if (isRecruitsNpc(event.getEntity())) {
            event.setCanceled(true);
        }
    }

    /** Recruits mod NPCs (recruits, nobles, assassins, captains, etc.) — not HYW armies. */
    static boolean isRecruitsNpc(Entity entity) {
        if (!(entity instanceof LivingEntity)) {
            return false;
        }
        String className = entity.getClass().getName();
        return className.startsWith("com.talhanation.recruits.entities.")
                && !className.contains(".ai.");
    }

    @SubscribeEvent
    public void onFactionCreated(FactionEvent.Created event) {
        if (!BridgeConfig.SYNC_DIPLOMACY_TO_HYW.get()) {
            return;
        }
        RecruitsFaction faction = event.getFaction();
        if (faction == null) {
            return;
        }
        UUID teamUuid = VanillaTeamUuidCache.get(faction.getStringID());
        String displayName = faction.getTeamDisplayName() != null ? faction.getTeamDisplayName() : faction.getStringID();
        RelationSystem.getOrCreateTeamRelationData(teamUuid, displayName);
    }

    @SubscribeEvent
    public void onFactionDisbanded(FactionEvent.Disbanded event) {
        if (!BridgeConfig.SYNC_DIPLOMACY_TO_HYW.get()) {
            return;
        }
        RecruitsFaction faction = event.getFaction();
        if (faction == null) {
            return;
        }
        UUID teamUuid = VanillaTeamUuidCache.get(faction.getStringID());
        UUID leaderId = faction.getTeamLeaderUUID();
        if (leaderId != null) {
            RelationSystem.disbandTeam(teamUuid, leaderId);
        }
    }

    @SubscribeEvent
    public void onDiplomacyChanged(DiplomacyEvent.RelationChanged event) {
        if (!BridgeConfig.SYNC_DIPLOMACY_TO_HYW.get()) {
            return;
        }
        UUID factionA = VanillaTeamUuidCache.get(event.getFactionA());
        UUID factionB = VanillaTeamUuidCache.get(event.getFactionB());
        RelationSystem.RelationType hywType = toHywRelation(event.getNewStatus());
        RelationSystem.setRelation(factionA, factionB, hywType);
        RelationSystem.setRelation(factionB, factionA, hywType);
    }

    @SubscribeEvent
    public void onServerTick(net.minecraftforge.event.TickEvent.ServerTickEvent event) {
        if (event.phase != net.minecraftforge.event.TickEvent.Phase.END) {
            return;
        }
        if (!BridgeConfig.SYNC_HYW_TEAMS.get()) {
            return;
        }
        if (--teamSyncCooldown > 0) {
            return;
        }
        teamSyncCooldown = BridgeConfig.TEAM_SYNC_INTERVAL_TICKS.get();

        var server = event.getServer();
        if (server == null) {
            return;
        }

        boolean countHorses = BridgeConfig.COUNT_MOUNTED_HORSES_FOR_SIEGE.get();
        for (ServerLevel level : server.getAllLevels()) {
            AABB bounds = new AABB(-3.0E7, level.getMinBuildHeight(), -3.0E7, 3.0E7, level.getMaxBuildHeight(), 3.0E7);
            for (BaseCombatEntity unit : level.getEntitiesOfClass(BaseCombatEntity.class, bounds, LivingEntity::isAlive)) {
                if (!countHorses && unit instanceof HywHorseEntity) {
                    continue;
                }
                syncHywUnitTeam(level, unit);
            }
        }
    }

    private static void syncHywUnitTeam(ServerLevel level, BaseCombatEntity unit) {
        UUID ownerId = unit.getOwnerUUID();
        if (ownerId == null) {
            return;
        }
        ServerPlayer owner = level.getServer().getPlayerList().getPlayer(ownerId);
        if (owner == null) {
            return;
        }
        applyOwnerTeam(level, unit, owner);
    }

    private static void applyOwnerTeam(ServerLevel level, LivingEntity entity, ServerPlayer owner) {
        Team ownerTeam = owner.getTeam();
        if (ownerTeam == null) {
            return;
        }
        Team entityTeam = entity.getTeam();
        if (entityTeam == ownerTeam) {
            return;
        }
        PlayerTeam playerTeam = level.getScoreboard().getPlayerTeam(ownerTeam.getName());
        if (playerTeam != null) {
            level.getScoreboard().addPlayerToTeam(entity.getStringUUID(), playerTeam);
        }
    }

    private static RelationSystem.RelationType toHywRelation(RecruitsDiplomacyManager.DiplomacyStatus status) {
        return switch (status) {
            case ENEMY -> RelationSystem.RelationType.HOSTILE;
            case ALLY -> RelationSystem.RelationType.FRIENDLY;
            case NEUTRAL -> RelationSystem.RelationType.NEUTRAL;
        };
    }

    /** Used by ClaimEventsMixin */
    public static boolean shouldBlockVillagerClaimTakeover() {
        return BridgeConfig.BLOCK_VILLAGER_CLAIM_TAKEOVER.get();
    }

    /** Used by CommandEventsMixin */
    public static boolean shouldBlockRecruitCommandUi() {
        return BridgeConfig.BLOCK_RECRUIT_COMMAND_UI.get();
    }
}
