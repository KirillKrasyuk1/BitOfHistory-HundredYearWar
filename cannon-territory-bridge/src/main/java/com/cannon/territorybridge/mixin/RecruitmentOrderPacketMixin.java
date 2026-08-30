package com.cannon.territorybridge.mixin;

import com.cannon.territorybridge.server.HywMobilizationGuard;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ydmsama.hundred_years_war.main.network.packets.RecruitmentOrderPacket;

@Mixin(value = RecruitmentOrderPacket.class, remap = false)
public abstract class RecruitmentOrderPacketMixin {
    @Inject(method = "handlePacket", at = @At("HEAD"), cancellable = true, remap = false)
    private static void cannon$guardRecruitmentOrder(
            ServerPlayer player,
            RecruitmentOrderPacket packet,
            CallbackInfo ci
    ) {
        HywMobilizationGuard.DenyReason reason = HywMobilizationGuard.evaluate(
                player.getUUID(),
                player.blockPosition(),
                player
        );
        if (reason != null) {
            HywMobilizationGuard.denyMobilization(player, reason);
            ci.cancel();
        }
    }
}
