package com.cannon.territorybridge.mixin;

import com.cannon.territorybridge.CannonTerritoryBridge;
import com.cannon.territorybridge.server.BridgeServerEvents;
import com.cannon.territorybridge.server.HywSiegeHelper;
import com.talhanation.recruits.ClaimEvents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import com.talhanation.recruits.world.RecruitsClaim;
import net.minecraft.world.scores.Team;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ClaimEvents.class, remap = false)
public abstract class ClaimEventsMixin {
    @Inject(method = "takeOverVillager", at = @At("HEAD"), cancellable = true, remap = false)
    private void cannon$skipVillagerTakeover(ServerLevel level, RecruitsClaim claim, LivingEntity livingEntity, CallbackInfo ci) {
        if (BridgeServerEvents.shouldBlockVillagerClaimTakeover()) {
            ci.cancel();
        }
    }

    @Redirect(
            method = "classifyEntities",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;getTeam()Lnet/minecraft/world/scores/Team;"),
            remap = true
    )
    private Team cannon$resolveSiegeTeam(LivingEntity entity) {
        try {
            Team team = entity.getTeam();
            return team != null ? team : HywSiegeHelper.resolveOwnerTeam(entity);
        } catch (Throwable t) {
            CannonTerritoryBridge.LOGGER.warn(
                    "Recruits siege classify failed for {}: {}",
                    entity.getType(),
                    t.toString()
            );
            return null;
        }
    }
}
