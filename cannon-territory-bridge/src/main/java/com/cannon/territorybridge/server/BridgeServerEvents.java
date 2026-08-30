package com.cannon.territorybridge.server;

import com.cannon.territorybridge.config.BridgeConfig;
import com.talhanation.recruits.ClaimEvent;
import com.talhanation.recruits.DiplomacyEvent;
import com.talhanation.recruits.FactionEvent;
import com.talhanation.recruits.RecruitEvent;
import com.talhanation.recruits.world.RecruitsDiplomacyManager;
import com.talhanation.recruits.world.RecruitsFaction;
import com.talhanation.recruits.world.RecruitsClaim;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import ydmsama.hundred_years_war.main.utils.RelationSystem;
import ydmsama.hundred_years_war.main.utils.VanillaTeamUuidCache;

import java.util.UUID;

public final class BridgeServerEvents {
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onRecruitHired(RecruitEvent.Hired event) {
        if (BridgeConfig.BLOCK_RECRUIT_HIRE.get()) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onEntityJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide() || !BridgeConfig.BLOCK_RECRUITS_ENTITIES.get()) {
            return;
        }
        if (isRecruitsNpc(event.getEntity())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void onFactionCreated(FactionEvent.Created event) {
        ServerLevel level = event.getLevel();
        if (level == null) {
            return;
        }
        HywRelationBridge.linkRecruitsFactionDeferred(level, event.getFaction(), event.getCreator());
    }

    @SubscribeEvent
    public void onFactionDisbanded(FactionEvent.Disbanded event) {
        HywRelationBridge.disbandFaction(event.getFaction());
    }

    @SubscribeEvent
    public void onClaimUpdated(ClaimEvent.Updated event) {
        if (!event.isNew()) {
            return;
        }
        ServerLevel level = event.getLevel();
        if (level == null) {
            return;
        }
        RecruitsClaim claim = event.getClaim();
        if (claim == null) {
            return;
        }
        RecruitsFaction faction = claim.getOwnerFaction();
        ServerPlayer player = resolveClaimPlayer(level, claim);
        HywRelationBridge.linkRecruitsFactionDeferred(level, faction, player);
    }

    @SubscribeEvent
    public void onDiplomacyChanged(DiplomacyEvent.RelationChanged event) {
        UUID factionA = VanillaTeamUuidCache.get(event.getFactionA());
        UUID factionB = VanillaTeamUuidCache.get(event.getFactionB());
        HywRelationBridge.mirrorDiplomacy(factionA, factionB, toHywRelation(event.getNewStatus()));
    }

    private static ServerPlayer resolveClaimPlayer(ServerLevel level, RecruitsClaim claim) {
        if (claim.getPlayerInfo() != null) {
            ServerPlayer byInfo = level.getServer().getPlayerList().getPlayer(claim.getPlayerInfo().getUUID());
            if (byInfo != null) {
                return byInfo;
            }
        }
        RecruitsFaction faction = claim.getOwnerFaction();
        if (faction != null && faction.getTeamLeaderUUID() != null) {
            return level.getServer().getPlayerList().getPlayer(faction.getTeamLeaderUUID());
        }
        return null;
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
