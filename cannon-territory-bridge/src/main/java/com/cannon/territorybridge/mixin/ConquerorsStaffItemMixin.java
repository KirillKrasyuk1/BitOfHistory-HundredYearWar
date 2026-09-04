package com.cannon.territorybridge.mixin;

import com.cannon.territorybridge.server.HywMobilizationGuard;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ydmsama.hundred_years_war.main.item.ConquerorsStaffItem;

@Mixin(value = ConquerorsStaffItem.class, remap = false)
public abstract class ConquerorsStaffItemMixin {
    @Inject(method = "m_7203_", at = @At("HEAD"), cancellable = true, remap = false)
    private void cannon$guardRecruitmentOpen(
            Level level,
            Player player,
            InteractionHand hand,
            CallbackInfoReturnable<InteractionResultHolder<ItemStack>> cir
    ) {
        if (level.isClientSide() || !(player instanceof ServerPlayer serverPlayer)) {
            return;
        }
        HywMobilizationGuard.DenyReason reason = HywMobilizationGuard.evaluate(
                serverPlayer.getUUID(),
                serverPlayer.blockPosition(),
                serverPlayer,
                "conquerorsStaff"
        );
        if (reason != null) {
            HywMobilizationGuard.denyMobilization(serverPlayer, reason);
            cir.setReturnValue(InteractionResultHolder.fail(player.getItemInHand(hand)));
        }
    }
}
