package com.cannon.territorybridge.mixin;

import com.cannon.territorybridge.bridge.BridgeClaimAccess;
import com.talhanation.recruits.world.RecruitsClaim;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

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
}
