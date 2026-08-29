package com.cannon.territorybridge.server;

import com.cannon.territorybridge.CannonTerritoryBridge;
import com.cannon.territorybridge.config.BridgeConfig;
import com.talhanation.recruits.DiplomacyEvent;
import com.talhanation.recruits.FactionEvent;
import com.talhanation.recruits.RecruitEvent;
import com.talhanation.recruits.world.RecruitsDiplomacyManager;
import com.talhanation.recruits.world.RecruitsFaction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
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
    public void onEntityJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide()) {
            return;
        }
        Entity entity = event.getEntity();

        if (isRecruitsNpc(entity)) {
            if (BridgeConfig.BLOCK_RECRUITS_ENTITIES.get()) {
                event.setCanceled(true);
            }
            return;
        }

        if (!BridgeConfig.SYNC_HYW_TEAMS.get() || !(entity instanceof BaseCombatEntity unit)) {
            return;
        }
        if (!BridgeConfig.COUNT_MOUNTED_HORSES_FOR_SIEGE.get() && unit instanceof HywHorseEntity) {
            return;
        }
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }

        // Defer team assignment until after spawn completes — avoids integrated-server stalls/crashes.
        level.getServer().execute(() -> scheduleHywTeamSync(level, unit));
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

        UUID leaderId = faction.getTeamLeaderUUID();
        if (leaderId != null && BridgeConfig.SYNC_HYW_TEAMS.get()) {
            var server = net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer();
            if (server != null) {
                server.execute(() -> syncHywUnitsForOwner(server, leaderId));
            }
        }
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
        if (server == null || server.getPlayerList().getPlayerCount() == 0) {
            return;
        }

        boolean countHorses = BridgeConfig.COUNT_MOUNTED_HORSES_FOR_SIEGE.get();
        int radius = BridgeConfig.TEAM_SYNC_RADIUS.get();

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (!(player.level() instanceof ServerLevel level)) {
                continue;
            }
            AABB bounds = player.getBoundingBox().inflate(radius);
            for (BaseCombatEntity unit : level.getEntitiesOfClass(BaseCombatEntity.class, bounds, LivingEntity::isAlive)) {
                if (!countHorses && unit instanceof HywHorseEntity) {
                    continue;
                }
                syncHywUnitTeam(level, unit);
            }
        }
    }

    private static void scheduleHywTeamSync(ServerLevel level, BaseCombatEntity unit) {
        if (!unit.isAlive() || unit.level() != level) {
            return;
        }
        syncHywUnitTeam(level, unit);
    }

    private static void syncHywUnitsForOwner(net.minecraft.server.MinecraftServer server, UUID ownerId) {
        ServerPlayer owner = server.getPlayerList().getPlayer(ownerId);
        if (owner == null || !(owner.level() instanceof ServerLevel level)) {
            return;
        }
        int radius = BridgeConfig.TEAM_SYNC_RADIUS.get();
        AABB bounds = owner.getBoundingBox().inflate(radius);
        for (BaseCombatEntity unit : level.getEntitiesOfClass(BaseCombatEntity.class, bounds, LivingEntity::isAlive)) {
            if (ownerId.equals(unit.getOwnerUUID())) {
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
        try {
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
        } catch (RuntimeException e) {
            CannonTerritoryBridge.LOGGER.warn("HYW team sync failed for {}: {}", entity.getStringUUID(), e.toString());
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
