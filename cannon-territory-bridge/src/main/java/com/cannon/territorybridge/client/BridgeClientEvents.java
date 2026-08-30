package com.cannon.territorybridge.client;

import com.cannon.territorybridge.config.BridgeConfig;
import com.cannon.territorybridge.server.BridgeServerEvents;
import com.talhanation.recruits.client.gui.AssassinLeaderScreen;
import com.talhanation.recruits.client.gui.CommandScreen;
import com.talhanation.recruits.client.gui.DebugInvScreen;
import com.talhanation.recruits.client.gui.NobleTradeScreen;
import com.talhanation.recruits.client.gui.PatrolLeaderScreen;
import com.talhanation.recruits.client.gui.PromoteScreen;
import com.talhanation.recruits.client.gui.RecruitHireScreen;
import com.talhanation.recruits.client.gui.RecruitInventoryScreen;
import com.talhanation.recruits.client.gui.RecruitMoreScreen;
import com.talhanation.recruits.client.gui.RenameRecruitScreen;
import com.talhanation.recruits.client.gui.ScoutScreen;
import com.talhanation.recruits.client.gui.SiegeEngineerScreen;
import com.talhanation.recruits.client.gui.group.EditOrAddGroupScreen;
import com.talhanation.recruits.client.gui.group.RecruitsGroupListScreen;
import com.talhanation.recruits.client.gui.group.SelectGroupScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = com.cannon.territorybridge.CannonTerritoryBridge.MOD_ID, value = Dist.CLIENT)
public final class BridgeClientEvents {
    private BridgeClientEvents() {}

    @SubscribeEvent
    public static void onScreenOpen(ScreenEvent.Opening event) {
        if (!BridgeServerEvents.shouldBlockRecruitCommandUi()) {
            return;
        }
        Screen screen = event.getScreen();
        if (isBlockedRecruitsScreen(screen)) {
            event.setCanceled(true);
        }
    }

    private static boolean isBlockedRecruitsScreen(Screen screen) {
        return screen instanceof CommandScreen
                || screen instanceof RecruitHireScreen
                || screen instanceof RecruitInventoryScreen
                || screen instanceof RecruitMoreScreen
                || screen instanceof RenameRecruitScreen
                || screen instanceof NobleTradeScreen
                || screen instanceof PatrolLeaderScreen
                || screen instanceof PromoteScreen
                || screen instanceof ScoutScreen
                || screen instanceof SiegeEngineerScreen
                || screen instanceof AssassinLeaderScreen
                || screen instanceof DebugInvScreen
                || screen instanceof RecruitsGroupListScreen
                || screen instanceof EditOrAddGroupScreen
                || screen instanceof SelectGroupScreen;
    }
}
