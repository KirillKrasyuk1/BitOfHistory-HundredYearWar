package com.cannon.territorybridge.mixin;

import com.cannon.territorybridge.server.HywMobilizationGuard;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ydmsama.hundred_years_war.main.network.packets.RecruitmentOrderPacket;
import ydmsama.hundred_years_war.main.recruitment.RecruitmentOrderManager;

import java.util.List;

@Mixin(value = RecruitmentOrderManager.class, remap = false)
public abstract class RecruitmentOrderManagerMixin {
    @Inject(method = "createOrder", at = @At("HEAD"), cancellable = true, remap = false)
    private void cannon$guardCreateOrder(
            ServerPlayer player,
            List<RecruitmentOrderPacket.RecruitmentEntry> entries,
            CallbackInfoReturnable<RecruitmentOrderManager.CreationResult> cir
    ) {
        HywMobilizationGuard.DenyReason reason = HywMobilizationGuard.evaluate(
                player.getUUID(),
                player.blockPosition(),
                player,
                "createOrder"
        );
        if (reason != null) {
            HywMobilizationGuard.denyMobilization(player, reason);
            cir.setReturnValue(RecruitmentOrderManager.CreationResult.fail(
                    HywMobilizationGuard.denyMessage(reason)
            ));
        }
    }
}
