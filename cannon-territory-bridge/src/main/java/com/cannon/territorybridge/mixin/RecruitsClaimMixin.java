package com.cannon.territorybridge.mixin;

import com.cannon.territorybridge.bridge.BridgeClaimAccess;
import com.cannon.territorybridge.bridge.BridgeClaimHelper;
import com.cannon.territorybridge.server.ClaimSiegeTracker;
import com.cannon.territorybridge.server.SiegeBalance;
import com.talhanation.recruits.ClaimEvents;
import com.talhanation.recruits.world.RecruitsClaim;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = RecruitsClaim.class, remap = false)
public class RecruitsClaimMixin implements BridgeClaimAccess {
    @Unique
    private int cannon$bridgeAttackerCount;

    @Unique
    private int cannon$bridgeDefenderCount;

    @Override
    public int cannon$getBridgeAttackerCount() {
        return cannon$bridgeAttackerCount;
    }

    @Override
    public int cannon$getBridgeDefenderCount() {
        return cannon$bridgeDefenderCount;
    }

    @Override
    public void cannon$setBridgeAttackerCount(int count) {
        cannon$bridgeAttackerCount = Math.max(0, count);
    }

    @Override
    public void cannon$setBridgeDefenderCount(int count) {
        cannon$bridgeDefenderCount = Math.max(0, count);
    }

    /** Recruits ends sieges when scanned headcount drops — ignore if bridge armies remain. */
    @Inject(method = "setUnderSiege", at = @At("HEAD"), cancellable = true, remap = false)
    private void cannon$guardPrematureSiegeEnd(boolean underSiege, ServerLevel level, CallbackInfo ci) {
        if (underSiege) {
            return;
        }
        RecruitsClaim self = (RecruitsClaim) (Object) this;
        if (!self.isUnderSiege) {
            return;
        }
        if (BridgeClaimHelper.attackerCount(self) >= SiegeBalance.minAttackersToStart()) {
            ci.cancel();
        }
    }

    /** Do not reset siege HP during an active capture tick. */
    @Inject(method = "resetHealth", at = @At("HEAD"), cancellable = true, remap = false)
    private void cannon$guardCaptureTimerReset(CallbackInfo ci) {
        RecruitsClaim self = (RecruitsClaim) (Object) this;
        if (self.isUnderSiege && ClaimSiegeTracker.isCaptureInProgress(self)) {
            ci.cancel();
        }
    }

    /** Block claim HP from reaching zero unless attackers still meet the capture ratio (sticky reserves). */
    @ModifyVariable(method = "setHealth", at = @At("HEAD"), argsOnly = true, remap = false)
    private int cannon$clampZeroHealthWithoutRatio(int health) {
        RecruitsClaim self = (RecruitsClaim) (Object) this;
        if (!self.isUnderSiege || health > 0) {
            return health;
        }
        ServerLevel level = ClaimEvents.server != null ? ClaimEvents.server.overworld() : null;
        int attackers = ClaimSiegeTracker.ratioAttackerCount(self, level);
        int defenders = ClaimSiegeTracker.ratioDefenderCount(self, level);
        if (!ClaimSiegeTracker.canTransferOwnership(self, level, attackers, defenders)) {
            return 1;
        }
        return health;
    }

    /**
     * Transfer when HP hit 0 and ratio still holds. Do not require defender NPCs dead or
     * the defending player to leave the claim — hiding on the claim must not stall capture.
     */
    @Inject(method = "setSiegeSuccess", at = @At("HEAD"), cancellable = true, remap = false)
    private void cannon$guardInstantCapture(ServerLevel level, CallbackInfo ci) {
        RecruitsClaim self = (RecruitsClaim) (Object) this;
        int attackers = ClaimSiegeTracker.ratioAttackerCount(self, level);
        int defenders = ClaimSiegeTracker.ratioDefenderCount(self, level);

        if (!ClaimSiegeTracker.canTransferOwnership(self, level, attackers, defenders)) {
            ci.cancel();
            if (self.getHealth() <= 0) {
                self.setHealth(1);
            }
        }
    }
}
