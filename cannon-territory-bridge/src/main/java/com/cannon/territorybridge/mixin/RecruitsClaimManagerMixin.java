package com.cannon.territorybridge.mixin;

import com.cannon.territorybridge.server.ClaimSiegeTracker;
import com.talhanation.recruits.world.RecruitsClaim;
import com.talhanation.recruits.world.RecruitsClaimManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = RecruitsClaimManager.class, remap = false)
public abstract class RecruitsClaimManagerMixin {
    @Inject(method = "removeActiveSiege", at = @At("HEAD"), remap = false)
    private void cannon$clearSiegeTracker(RecruitsClaim claim, CallbackInfo ci) {
        ClaimSiegeTracker.clear(claim);
    }
}
