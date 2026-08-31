package com.mrbysco.restrictivefarming;

import com.mojang.logging.LogUtils;
import com.mrbysco.restrictivefarming.config.FarmingConfig;
import com.mrbysco.restrictivefarming.loader.CropWhitelistLoader;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

@Mod(RestrictiveFarmingMod.MOD_ID)
public class RestrictiveFarmingMod {
    public static final String MOD_ID = "restrictive_farming";
    public static final Logger LOGGER = LogUtils.getLogger();

    public RestrictiveFarmingMod() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, FarmingConfig.SPEC);
        IEventBus forgeBus = MinecraftForge.EVENT_BUS;
        forgeBus.addListener(this::onAddReloadListeners);
        LOGGER.info("Restrictive Farming (Forge 1.20.1 port) loaded.");
    }

    private void onAddReloadListeners(AddReloadListenerEvent event) {
        event.addListener(CropWhitelistLoader.INSTANCE);
    }

    public static ResourceLocation modLoc(String path) {
        return new ResourceLocation(MOD_ID, path);
    }
}
