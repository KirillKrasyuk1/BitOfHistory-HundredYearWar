package com.cannon.territorybridge.server;

import com.cannon.territorybridge.config.BridgeConfig;
import com.talhanation.recruits.DiplomacyEvent;
import com.talhanation.recruits.FactionEvent;
import com.talhanation.recruits.RecruitEvent;
import com.talhanation.recruits.world.RecruitsDiplomacyManager;
import com.talhanation.recruits.world.RecruitsFaction;
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
