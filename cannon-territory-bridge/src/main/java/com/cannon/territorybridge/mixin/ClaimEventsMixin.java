package com.cannon.territorybridge.mixin;

import com.cannon.territorybridge.CannonTerritoryBridge;
import com.cannon.territorybridge.bridge.BridgeClaimHelper;
import com.cannon.territorybridge.server.BridgeServerEvents;
import com.cannon.territorybridge.server.ClaimSiegeTracker;
import com.cannon.territorybridge.server.HywSiegeClassifier;
import com.cannon.territorybridge.server.HywSiegeHelper;
import com.cannon.territorybridge.server.SiegeBalance;
import com.cannon.territorybridge.server.SiegeForceFilter;
import com.talhanation.recruits.ClaimEvents;
import com.talhanation.recruits.util.ClaimUtil;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import com.talhanation.recruits.world.RecruitsClaim;
import net.minecraft.world.level.Level;
import net.minecraft.world.scores.Team;
import net.minecraftforge.common.ForgeConfigSpec;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.function.Predicate;

@Mixin(value = ClaimEvents.class, remap = false)
public abstract class ClaimEventsMixin {
    @Inject(method = "takeOverVillager", at = @At("HEAD"), cancellable = true, remap = false)
    private void cannon$skipVillagerTakeover(ServerLevel level, RecruitsClaim claim, LivingEntity livingEntity, CallbackInfo ci) {
        if (BridgeServerEvents.shouldBlockVillagerClaimTakeover()) {
            ci.cancel();
        }
    }

    @Redirect(
            method = {"tickDetection", "tickActiveSieges", "updateParties"},
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraftforge/common/ForgeConfigSpec$IntValue;get()Ljava/lang/Object;",
                    remap = false
            ),
            remap = false
    )
    private Object cannon$minSiegeAttackers(ForgeConfigSpec.IntValue instance) {
        return Integer.valueOf(SiegeBalance.minAttackersToStart());
    }

    @Inject(
            method = "classifyEntities",
            at = @At("RETURN"),
            remap = false
    )
    private void cannon$finalizeSiegeForces(
            List<LivingEntity> entities,
            RecruitsClaim claim,
            List<LivingEntity> attackers,
            List<LivingEntity> defenders,
            CallbackInfo ci
    ) {
        HywSiegeClassifier.supplementHywUnits(entities, claim, attackers, defenders);
        SiegeForceFilter.stripNonCountingForces(attackers, defenders);
        ServerLevel level = resolveServerLevel(entities);
        if (level != null) {
            ClaimSiegeTracker.supplementForcesInsideClaim(level, claim, attackers, defenders);
            ClaimSiegeTracker.applyStickyForces(level, claim, entities, attackers, defenders);
        }
    }

    private static ServerLevel resolveServerLevel(List<LivingEntity> entities) {
        for (LivingEntity entity : entities) {
            if (entity != null && entity.level() instanceof ServerLevel serverLevel) {
                return serverLevel;
            }
        }
        MinecraftServer server = ClaimEvents.server;
        return server != null ? server.overworld() : null;
    }

    /**
     * Recruits only scans {@link net.minecraft.server.level.ServerPlayer} inside the claim.
     * Replace with HYW armies so player movement does not change force totals.
     * Instance + static handlers are split — Mixin rejects one @Redirect for both contexts.
     */
    private static List<LivingEntity> cannon$hywArmiesInClaim(Level level, RecruitsClaim claim) {
        return ClaimUtil.getLivingEntitiesInClaim(level, claim, SiegeForceFilter::countsForSiege);
    }

    @Redirect(
            method = {"tickActiveSieges", "tickDetection"},
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/talhanation/recruits/util/ClaimUtil;getLivingEntitiesInClaim(Lnet/minecraft/world/level/Level;Lcom/talhanation/recruits/world/RecruitsClaim;Ljava/util/function/Predicate;)Ljava/util/List;"
            ),
            remap = false
    )
    private List<LivingEntity> cannon$scanHywArmiesInstance(
            Level level,
            RecruitsClaim claim,
            Predicate<LivingEntity> ignored
    ) {
        return cannon$hywArmiesInClaim(level, claim);
    }

    @Redirect(
            method = "onRelationChanged",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/talhanation/recruits/util/ClaimUtil;getLivingEntitiesInClaim(Lnet/minecraft/world/level/Level;Lcom/talhanation/recruits/world/RecruitsClaim;Ljava/util/function/Predicate;)Ljava/util/List;"
            ),
            remap = false
    )
    private static List<LivingEntity> cannon$scanHywArmiesStatic(
            Level level,
            RecruitsClaim claim,
            Predicate<LivingEntity> ignored
    ) {
        return cannon$hywArmiesInClaim(level, claim);
    }

    /** Use bridge / committed counts so chunk-unloaded garrison still affects speed and siege end checks. */
    @ModifyVariable(
            method = "tickActiveSieges",
            at = @At("STORE"),
            ordinal = 0,
            remap = false
    )
    private int cannon$effectiveAttackers(int scannedAttackers, RecruitsClaim claim) {
        return ClaimSiegeTracker.effectiveAttackerCount(claim, scannedAttackers);
    }

    @ModifyVariable(
            method = "tickActiveSieges",
            at = @At("STORE"),
            ordinal = 1,
            remap = false
    )
    private int cannon$effectiveDefenders(int scannedDefenders, RecruitsClaim claim) {
        return ClaimSiegeTracker.effectiveDefenderCount(claim, scannedDefenders);
    }

    @Redirect(
            method = "tickActiveSieges",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/talhanation/recruits/ClaimEvents;calculateSiegeSpeedPercent(II)F"
            ),
            remap = false
    )
    private float cannon$bridgeSiegeSpeed(int attackerCount, int defenderCount, RecruitsClaim claim) {
        return SiegeBalance.computeSpeedPercent(
                ClaimSiegeTracker.effectiveAttackerCount(claim, attackerCount),
                ClaimSiegeTracker.effectiveDefenderCount(claim, defenderCount)
        );
    }

    /**
     * Recruits calls resetHealth after a cancelled setUnderSiege(false) when scanned attackers dip —
     * skip timer wipe while bridge armies keep the siege alive.
     */
    @Redirect(
            method = "tickActiveSieges",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/talhanation/recruits/world/RecruitsClaim;resetHealth()V"
            ),
            remap = false
    )
    private void cannon$guardSiegeResetHealth(RecruitsClaim claim) {
        if (BridgeClaimHelper.attackerCount(claim) >= SiegeBalance.minAttackersToStart()) {
            return;
        }
        claim.resetHealth();
    }

    /** Do not tear down the active siege while bridge attackers still meet the minimum. */
    @Redirect(
            method = "tickActiveSieges",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/talhanation/recruits/world/RecruitsClaimManager;removeActiveSiege(Lcom/talhanation/recruits/world/RecruitsClaim;)V"
            ),
            remap = false
    )
    private void cannon$guardRemoveActiveSiege(
            com.talhanation.recruits.world.RecruitsClaimManager manager,
            RecruitsClaim claim
    ) {
        if (BridgeClaimHelper.attackerCount(claim) >= SiegeBalance.minAttackersToStart()) {
            return;
        }
        manager.removeActiveSiege(claim);
    }

    @Redirect(
            method = {"classifyEntities", "updateParties"},
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;getTeam()Lnet/minecraft/world/scores/Team;"),
            remap = true
    )
    private Team cannon$resolveSiegeTeam(LivingEntity entity) {
        try {
            return HywSiegeHelper.resolveSiegeTeam(entity);
        } catch (Throwable t) {
            CannonTerritoryBridge.LOGGER.warn(
                    "Recruits siege classify failed for {}: {}",
                    entity.getType(),
                    t.toString()
            );
            return entity.getTeam();
        }
    }
}
