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
import ydmsama.hundred_years_war.main.item.BaseScrollItem;

@Mixin(value = BaseScrollItem.class, remap = false)
public abstract class BaseScrollItemMixin {
    @Inject(
            method = "m_7203_(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/InteractionHand;)Lnet/minecraft/world/InteractionResultHolder;",
            at = @At("HEAD"),
            cancellable = true,
            remap = false
    )
    private void cannon$requireOwnClaimForScroll(
            Level world,
            Player player,
            InteractionHand hand,
            CallbackInfoReturnable<InteractionResultHolder<ItemStack>> cir
    ) {
        if (world.isClientSide() || !(player instanceof ServerPlayer serverPlayer)) {
            return;
        }
        if (!HywMobilizationGuard.canMobilize(serverPlayer, player.blockPosition())) {
            HywMobilizationGuard.denyMobilization(serverPlayer);
            cir.setReturnValue(InteractionResultHolder.fail(player.getItemInHand(hand)));
        }
    }
}
