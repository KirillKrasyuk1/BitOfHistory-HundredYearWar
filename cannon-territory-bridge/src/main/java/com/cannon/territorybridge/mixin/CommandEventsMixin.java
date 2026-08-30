package com.cannon.territorybridge.mixin;

import com.cannon.territorybridge.server.BridgeServerEvents;
import com.talhanation.recruits.CommandEvents;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = CommandEvents.class, remap = false)
public abstract class CommandEventsMixin {
    @Inject(method = "openCommandScreen", at = @At("HEAD"), cancellable = true, remap = false)
    private static void cannon$blockCommandScreen(Player player, CallbackInfo ci) {
        if (BridgeServerEvents.shouldBlockRecruitCommandUi()) {
            ci.cancel();
        }
    }
}
