package com.cannon.territorybridge.mixin;

import com.cannon.territorybridge.CannonTerritoryBridge;
import com.cannon.territorybridge.server.BridgeServerEvents;
import com.cannon.territorybridge.server.HywSiegeClassifier;
import com.cannon.territorybridge.server.HywSiegeHelper;
import com.cannon.territorybridge.server.SiegeBalance;
import com.talhanation.recruits.ClaimEvents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import com.talhanation.recruits.world.RecruitsClaim;
import net.minecraft.world.scores.Team;
import net.minecraftforge.common.ForgeConfigSpec;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

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
    private void cannon$addHywGarrison(
            List<LivingEntity> entities,
            RecruitsClaim claim,
            List<LivingEntity> attackers,
            List<LivingEntity> defenders,
            CallbackInfo ci
    ) {
        HywSiegeClassifier.supplementHywUnits(entities, claim, attackers, defenders);
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
