package com.cannon.economy;

import com.cannon.economy.command.EconomyCommands;
import com.cannon.economy.config.EconomyConfig;
import com.cannon.economy.deposit.OreDepositEngine;
import com.cannon.economy.farming.FertilitySystem;
import com.cannon.economy.hyw.HywIntegration;
import com.cannon.economy.worldgen.StructureSpawnIntegration;
import com.mojang.logging.LogUtils;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

@Mod(CannonEconomy.MOD_ID)
public class CannonEconomy {
    public static final String MOD_ID = "cannon_economy";
    public static final Logger LOGGER = LogUtils.getLogger();

    public CannonEconomy() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, EconomyConfig.SPEC);
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::commonSetup);
        MinecraftForge.EVENT_BUS.register(new OreDepositEngine());
        MinecraftForge.EVENT_BUS.addListener(this::registerCommands);
        MinecraftForge.EVENT_BUS.addListener(this::onServerStarted);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            HywIntegration.applySupplyMultiplier();
            StructureSpawnIntegration.apply();
        });
        LOGGER.info("Cannon Economy loaded — fertility, ore deposits, HYW balance, structure blocking.");
    }

    private void onServerStarted(ServerStartedEvent event) {
        HywIntegration.applySupplyMultiplier();
        StructureSpawnIntegration.apply();
    }

    private void registerCommands(RegisterCommandsEvent event) {
        EconomyCommands.register(event.getDispatcher());
    }
}
