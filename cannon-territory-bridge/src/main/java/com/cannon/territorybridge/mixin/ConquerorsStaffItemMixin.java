package com.cannon.territorybridge.mixin;

import com.cannon.territorybridge.server.HywMobilizationGuard;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ydmsama.hundred_years_war.main.item.ConquerorsStaffItem;

@Mixin(value = ConquerorsStaffItem.class, remap = false)
public abstract class ConquerorsStaffItemMixin {
    @Inject(method = "spawnArmy", at = @At("HEAD"), cancellable = true, remap = false)
    private void cannon$requireOwnClaimToMobilize(Level world, Player player, CallbackInfo ci) {
        if (world.isClientSide() || !(player instanceof ServerPlayer serverPlayer)) {
            return;
        }
        if (!HywMobilizationGuard.canMobilize(serverPlayer, player.blockPosition())) {
            HywMobilizationGuard.denyMobilization(serverPlayer);
            ci.cancel();
        }
    }
}
