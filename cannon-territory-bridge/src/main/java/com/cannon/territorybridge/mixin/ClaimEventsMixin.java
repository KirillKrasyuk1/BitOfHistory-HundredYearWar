package com.cannon.territorybridge.mixin;

import com.cannon.territorybridge.server.BridgeServerEvents;
import com.talhanation.recruits.ClaimEvents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import com.talhanation.recruits.world.RecruitsClaim;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ClaimEvents.class, remap = false)
public abstract class ClaimEventsMixin {
    @Inject(method = "takeOverVillager", at = @At("HEAD"), cancellable = true, remap = false)
    private void cannon$skipVillagerTakeover(ServerLevel level, RecruitsClaim claim, LivingEntity livingEntity, CallbackInfo ci) {
        if (BridgeServerEvents.shouldBlockVillagerClaimTakeover()) {
            ci.cancel();
        }
    }
}
