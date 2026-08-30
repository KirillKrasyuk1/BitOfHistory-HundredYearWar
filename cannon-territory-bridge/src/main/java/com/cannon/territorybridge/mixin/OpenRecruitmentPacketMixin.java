package com.cannon.territorybridge.mixin;

import com.cannon.territorybridge.server.HywMobilizationGuard;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ydmsama.hundred_years_war.main.network.packets.OpenRecruitmentPacket;

@Mixin(value = OpenRecruitmentPacket.class, remap = false)
public abstract class OpenRecruitmentPacketMixin {
    @Inject(method = "send", at = @At("HEAD"), cancellable = true, remap = false)
    private static void cannon$guardOpenRecruitment(ServerPlayer player, CallbackInfo ci) {
        HywMobilizationGuard.DenyReason reason = HywMobilizationGuard.evaluate(
                player.getUUID(),
                player.blockPosition(),
                player,
                "openRecruitment"
        );
        if (reason != null) {
            HywMobilizationGuard.denyMobilization(player, reason);
            ci.cancel();
        }
    }
}
