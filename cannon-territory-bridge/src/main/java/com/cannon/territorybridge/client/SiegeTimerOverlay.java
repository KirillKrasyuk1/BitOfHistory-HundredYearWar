package com.cannon.territorybridge.client;

import com.cannon.territorybridge.CannonTerritoryBridge;
import com.cannon.territorybridge.config.BridgeConfig;
import com.talhanation.recruits.client.ClientManager;
import com.talhanation.recruits.world.RecruitsClaim;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.ChunkPos;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

@Mod.EventBusSubscriber(modid = CannonTerritoryBridge.MOD_ID, value = Dist.CLIENT)
public final class SiegeTimerOverlay {
    private SiegeTimerOverlay() {}

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        if (!BridgeConfig.SHOW_SIEGE_TIMER.get()) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null || minecraft.options.hideGui) {
            return;
        }

        RecruitsClaim claim = findClaimAtPlayer(minecraft);
        if (claim == null || !claim.isUnderSiege) {
            return;
        }

        int remaining = SiegeTimerUtil.estimateRemainingSeconds(claim);
        int health = claim.getHealth();
        int maxHealth = Math.max(1, claim.getMaxHealth());

        Component line1 = Component.literal("§c⚔ Осада: §f" + SiegeTimerUtil.formatDuration(remaining));
        Component line2 = Component.literal(
                "§7Прочность: §f" + health + "/" + maxHealth
                        + " §7(§f" + (health * 100 / maxHealth) + "%§7)"
        );

        GuiGraphics graphics = event.getGuiGraphics();
        int width = minecraft.getWindow().getGuiScaledWidth();
        int x = width / 2;
        int y = minecraft.getWindow().getGuiScaledHeight() / 2 + 28;

        graphics.drawCenteredString(minecraft.font, line1, x, y, 0xFFFFFF);
        graphics.drawCenteredString(minecraft.font, line2, x, y + 10, 0xAAAAAA);
    }

    private static RecruitsClaim findClaimAtPlayer(Minecraft minecraft) {
        List<RecruitsClaim> claims = ClientManager.recruitsClaims;
        if (claims == null || claims.isEmpty()) {
            return null;
        }
        ChunkPos chunk = minecraft.player.chunkPosition();
        for (RecruitsClaim claim : claims) {
            if (claim != null && claim.containsChunk(chunk)) {
                return claim;
            }
        }
        return null;
    }
}
