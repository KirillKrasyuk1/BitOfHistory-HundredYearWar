package com.cannon.territorybridge.mixin;

import com.cannon.territorybridge.server.BridgeServerEvents;
import com.talhanation.recruits.CommandEvents;
import com.talhanation.recruits.client.events.KeyEvents;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = KeyEvents.class, remap = false)
public abstract class KeyEventsMixin {
    @Redirect(
            method = "onKeyInput",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/talhanation/recruits/CommandEvents;openCommandScreen(Lnet/minecraft/world/entity/player/Player;)V"
            ),
            remap = false
    )
    private static void cannon$redirectCommandScreen(Player player) {
        if (!BridgeServerEvents.shouldBlockRecruitCommandUi()) {
            CommandEvents.openCommandScreen(player);
        }
    }
}
