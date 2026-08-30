package com.cannon.territorybridge;

import com.cannon.territorybridge.config.BridgeConfig;
import com.cannon.territorybridge.server.BridgeServerEvents;
import com.mojang.logging.LogUtils;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

@Mod(CannonTerritoryBridge.MOD_ID)
public class CannonTerritoryBridge {
    public static final String MOD_ID = "cannon_territory_bridge";
    public static final Logger LOGGER = LogUtils.getLogger();

    public CannonTerritoryBridge() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, BridgeConfig.SPEC);
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::commonSetup);
        MinecraftForge.EVENT_BUS.register(new BridgeServerEvents());
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        LOGGER.info("Cannon Territory Bridge loaded — HYW armies + Recruits claims/diplomacy only.");
        LOGGER.info("Config file: config/cannon_territory_bridge-common.toml (created on first launch if mod is active).");
    }
}
