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
        int attackers = SiegeTimerUtil.overlayAttackerCount(claim);
        int defenders = SiegeTimerUtil.overlayDefenderCount(claim);
        int required = SiegeTimerUtil.requiredAttackers(claim);
        boolean capturing = SiegeTimerUtil.isCaptureProgressing(claim);

        Component line1 = capturing
                ? Component.translatable("overlay.cannon_territory_bridge.siege_timer", SiegeTimerUtil.formatDuration(remaining))
                : Component.translatable(
                        "overlay.cannon_territory_bridge.siege_paused",
                        attackers,
                        defenders,
                        required
                );
        Component line2 = Component.translatable(
                "overlay.cannon_territory_bridge.siege_forces",
                attackers,
                defenders,
                required
        );
        Component line3 = Component.translatable(
                "overlay.cannon_territory_bridge.siege_hp",
                health,
                maxHealth,
                health * 100 / maxHealth
        );

        GuiGraphics graphics = event.getGuiGraphics();
        int width = minecraft.getWindow().getGuiScaledWidth();
        int x = width / 2;
        int y = minecraft.getWindow().getGuiScaledHeight() / 2 + 28;

        graphics.drawCenteredString(minecraft.font, line1, x, y, 0xFFFFFF);
        graphics.drawCenteredString(minecraft.font, line2, x, y + 10, 0xCCCCCC);
        graphics.drawCenteredString(minecraft.font, line3, x, y + 20, 0xAAAAAA);
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
