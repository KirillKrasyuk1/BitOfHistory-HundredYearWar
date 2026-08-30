package com.cannon.territorybridge.mixin;

import com.cannon.territorybridge.bridge.BridgeClaimAccess;
import com.cannon.territorybridge.bridge.BridgeClaimHelper;
import com.cannon.territorybridge.server.ClaimSiegeTracker;
import com.cannon.territorybridge.server.SiegeBalance;
import com.talhanation.recruits.world.RecruitsClaim;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
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

    /** Recruits ends sieges when player headcount in claim drops — ignore if HYW armies remain. */
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

    /** Do not wipe siege HP while capture is underway and committed garrison may still be alive. */
    @Inject(method = "resetHealth", at = @At("HEAD"), cancellable = true, remap = false)
    private void cannon$guardCaptureTimerReset(CallbackInfo ci) {
        RecruitsClaim self = (RecruitsClaim) (Object) this;
        if (!self.isUnderSiege) {
            return;
        }
        if (ClaimSiegeTracker.isCaptureInProgress(self) && ClaimSiegeTracker.hasCommittedDefenders(self)) {
            ci.cancel();
        }
    }

    /** Capture only when bridge army counts satisfy the ratio — not because a player walked off. */
    @Inject(method = "setSiegeSuccess", at = @At("HEAD"), cancellable = true, remap = false)
    private void cannon$guardInstantCapture(ServerLevel level, CallbackInfo ci) {
        RecruitsClaim self = (RecruitsClaim) (Object) this;
        int attackers = BridgeClaimHelper.attackerCount(self);
        int defenders = BridgeClaimHelper.defenderCount(self);
        boolean committedDefenders = ClaimSiegeTracker.hasCommittedDefenders(self);
        boolean captureInProgress = ClaimSiegeTracker.isCaptureInProgress(self);

        if (!SiegeBalance.canCompleteCapture(attackers, defenders, committedDefenders, captureInProgress)) {
            ci.cancel();
            if (self.getHealth() <= 0) {
                self.setHealth(1);
            }
        }
    }
}
