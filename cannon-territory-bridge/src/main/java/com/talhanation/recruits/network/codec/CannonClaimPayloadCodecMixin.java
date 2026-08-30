package com.talhanation.recruits.network.codec;

import com.cannon.territorybridge.bridge.BridgeClaimAccess;
import com.talhanation.recruits.world.RecruitsClaim;
import net.minecraft.network.FriendlyByteBuf;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = ClaimPayloadCodec.class, remap = false)
public abstract class CannonClaimPayloadCodecMixin {
    @Inject(method = "writeState", at = @At("RETURN"), remap = false)
    private static void cannon$writeBridgeForces(
            FriendlyByteBuf buf,
            RecruitsClaim claim,
            CallbackInfo ci
    ) {
        BridgeClaimAccess access = (BridgeClaimAccess) claim;
        buf.writeVarInt(access.cannon$getBridgeAttackerCount());
        buf.writeVarInt(access.cannon$getBridgeDefenderCount());
    }

    @Inject(method = "readState", at = @At("RETURN"), remap = false)
    private static void cannon$readBridgeForces(
            FriendlyByteBuf buf,
            CallbackInfoReturnable<RecruitsClaim> cir
    ) {
        RecruitsClaim claim = cir.getReturnValue();
        if (claim == null || !buf.isReadable()) {
            return;
        }
        BridgeClaimAccess access = (BridgeClaimAccess) claim;
        access.cannon$setBridgeAttackerCount(buf.readVarInt());
        access.cannon$setBridgeDefenderCount(buf.readVarInt());
    }
}
