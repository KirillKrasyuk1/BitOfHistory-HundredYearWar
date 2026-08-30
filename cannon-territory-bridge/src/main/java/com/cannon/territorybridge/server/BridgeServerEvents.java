package com.cannon.territorybridge.server;

import com.cannon.territorybridge.bridge.BridgeClaimHelper;
import com.cannon.territorybridge.config.BridgeConfig;
import com.cannon.territorybridge.network.SiegeForceBroadcaster;
import com.talhanation.recruits.ClaimEvent;
import com.talhanation.recruits.ClaimEvents;
import com.talhanation.recruits.DiplomacyEvent;
import com.talhanation.recruits.FactionEvent;
import com.talhanation.recruits.RecruitEvent;
import com.talhanation.recruits.SiegeEvent;
import com.talhanation.recruits.world.RecruitsDiplomacyManager;
import com.talhanation.recruits.world.RecruitsFaction;
import com.talhanation.recruits.world.RecruitsClaim;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import ydmsama.hundred_years_war.main.entity.entities.BaseCombatEntity;
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

    /** Blocks Conqueror's Staff army spawn when charge completes outside own claim. */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onHywStaffUseFinish(LivingEntityUseItemEvent.Finish event) {
        if (event.getEntity().level().isClientSide()) {
            return;
        }
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        HywMobilizationGuard.DenyReason reason = HywMobilizationGuard.evaluate(
                player.getUUID(),
                player.blockPosition(),
                player,
                "staffUseFinish"
        );
        if (reason != null) {
            HywMobilizationGuard.denyMobilization(player, reason);
            event.setCanceled(true);
        }
    }

    /** Blocks HYW staff / scroll use outside own claim. */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onHywMobilizationRightClick(PlayerInteractEvent.RightClickItem event) {
        if (event.getLevel().isClientSide()) {
            return;
        }
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        ItemStack stack = event.getItemStack();
        if (!HywMobilizationItems.isMobilizationItem(stack)) {
            return;
        }
        HywMobilizationGuard.DenyReason reason = HywMobilizationGuard.evaluate(
                player.getUUID(),
                player.blockPosition(),
                player,
                "rightClickItem"
        );
        if (reason != null) {
            HywMobilizationGuard.denyMobilization(player, reason);
            event.setCanceled(true);
            event.setCancellationResult(net.minecraft.world.InteractionResult.FAIL);
        }
    }

    /** Safety net: reject HYW combat units and recruitment flags spawned outside allowed territory. */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onHywCombatEntityJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide() || !BridgeConfig.REQUIRE_OWN_CLAIM_TO_MOBILIZE.get()) {
            return;
        }
        Entity entity = event.getEntity();
        if (!(entity instanceof BaseCombatEntity hyw)) {
            return;
        }
        UUID ownerId = HywEntityAccess.getOwnerUuid(hyw);
        if (ownerId == null) {
            return;
        }
        ServerPlayer owner = event.getLevel().getServer().getPlayerList().getPlayer(ownerId);
        HywMobilizationGuard.DenyReason reason = HywMobilizationGuard.evaluate(
                ownerId,
                entity.blockPosition(),
                owner,
                "entityJoin:" + entity.getType()
        );
        if (reason != null) {
            event.setCanceled(true);
            if (owner != null) {
                HywMobilizationGuard.denyMobilization(owner, reason);
            }
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

    /** Recruits sieges tick every 100 server ticks (~5 s). */
    private static final int RECRUITS_SIEGE_TICKS_PER_MINUTE = 12;

    @SubscribeEvent
    public void onSiegeStart(SiegeEvent.Start event) {
        RecruitsClaim claim = event.getClaim();
        if (claim == null || event.getLevel().isClientSide()) {
            return;
        }
        if (claim.getHealth() < claim.getMaxHealth()) {
            claim.resetHealth();
        }
    }

    @SubscribeEvent
    public void onSiegeTick(SiegeEvent.Tick event) {
        if (event.getLevel().isClientSide()) {
            return;
        }
        RecruitsClaim claim = event.getClaim();
        if (claim == null) {
            return;
        }

        int attackerCount = ClaimSiegeTracker.effectiveAttackerCount(claim, bridgeSiegeAttackerCount(claim, event.getAttackerCount()));
        int defenderCount = ClaimSiegeTracker.effectiveDefenderCount(claim, bridgeSiegeDefenderCount(claim, event.getDefenderCount()));
        float speed = SiegeBalance.computeSpeedPercent(attackerCount, defenderCount);

        if (speed <= 0.0f) {
            event.setDamage(0);
            claim.setSiegeSpeedPercent(0.0f);
            return;
        }

        claim.setSiegeSpeedPercent(speed);

        int damage = event.getDamage();
        if (BridgeConfig.APPLY_SIEGE_SPEED_TO_DAMAGE.get()) {
            damage = Math.max(1, Math.round(damage * speed));
        }

        int minMinutes = BridgeConfig.MIN_CAPTURE_MINUTES.get();
        int maxHealth = claim.getMaxHealth();
        int maxDamagePerTick = Math.max(
                1,
                (int) Math.ceil(maxHealth / (double) (minMinutes * RECRUITS_SIEGE_TICKS_PER_MINUTE))
        );
        event.setDamage(Math.min(damage, maxDamagePerTick));

        if (ClaimEvents.server != null) {
            SiegeForceBroadcaster.syncClaim(ClaimEvents.server, claim);
        }
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

    private static int bridgeSiegeAttackerCount(RecruitsClaim claim, int fallback) {
        if (claim != null && claim.isUnderSiege) {
            return BridgeClaimHelper.attackerCount(claim);
        }
        return fallback;
    }

    private static int bridgeSiegeDefenderCount(RecruitsClaim claim, int fallback) {
        if (claim != null && claim.isUnderSiege) {
            return BridgeClaimHelper.defenderCount(claim);
        }
        return fallback;
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
