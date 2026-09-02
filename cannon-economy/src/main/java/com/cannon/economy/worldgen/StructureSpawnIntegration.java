package com.cannon.economy.worldgen;

import com.cannon.economy.CannonEconomy;
import com.cannon.economy.config.EconomyConfig;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModList;

/**
 * Disables ambient world structures from Recruits addons and HYW that clash with Cannon's atmosphere.
 * Datapack overrides handle new chunk generation; reflection disables runtime patrols and HYW nearby structures.
 */
public final class StructureSpawnIntegration {
    private StructureSpawnIntegration() {}

    public static void apply() {
        if (EconomyConfig.BLOCK_RECRUITS_WORLDGEN.get()) {
            disableRecruitsPatrols();
            disableVillageRecruitsExpansion();
            disableWariumMercenaries();
        }
        if (EconomyConfig.BLOCK_HYW_NEARBY_STRUCTURES.get()) {
            disableHywNearbyStructures();
        }
    }

    private static void disableHywNearbyStructures() {
        if (!ModList.get().isLoaded("hundred_years_war")) {
            return;
        }
        try {
            Class<?> cfg = Class.forName("ydmsama.hundred_years_war.main.config.ServerModConfig");
            Object instance = cfg.getField("INSTANCE").get(null);
            cfg.getField("enableNearStructureGeneration").setBoolean(instance, false);
            cfg.getMethod("save").invoke(null);
            CannonEconomy.LOGGER.info("HYW nearby structure generation disabled");
        } catch (ReflectiveOperationException e) {
            CannonEconomy.LOGGER.warn("Could not disable HYW structure generation: {}", e.getMessage());
        }
    }

    private static void disableRecruitsPatrols() {
        if (!ModList.get().isLoaded("recruits")) {
            return;
        }
        try {
            Class<?> cfg = Class.forName("com.talhanation.recruits.config.RecruitsServerConfig");
            setBoolean(cfg, "ShouldRecruitPatrolsSpawn", false);
            setBoolean(cfg, "ShouldPillagerPatrolsSpawn", false);
            setBoolean(cfg, "NobleVillagerSpawn", false);
            setBoolean(cfg, "PillagerSpawn", false);
            setInt(cfg, "MaxSpawnRecruitsInVillage", 0);
            CannonEconomy.LOGGER.info("Recruits ambient patrol and village spawns disabled");
        } catch (ReflectiveOperationException e) {
            CannonEconomy.LOGGER.warn("Could not disable Recruits spawns: {}", e.getMessage());
        }
    }

    private static void disableVillageRecruitsExpansion() {
        if (!ModList.get().isLoaded("village_recruits")) {
            return;
        }
        try {
            Class<?> cfg = Class.forName("com.example.villagerecruits.config.VRConfig$Common");
            Object common = Class.forName("com.example.villagerecruits.config.VRConfig")
                    .getField("COMMON")
                    .get(null);
            setBooleanOnInstance(cfg, common, "NATURAL_VILLAGES_ENABLED", false);
            setBooleanOnInstance(cfg, common, "SKY_VILLAGES_ENABLED", false);
            setBooleanOnInstance(cfg, common, "SECONDARY_VILLAGES_BUILD", false);
            setBooleanOnInstance(cfg, common, "SKY_VILLAGES_BUILD", false);
            CannonEconomy.LOGGER.info("Village Recruits natural and sky village generation disabled");
        } catch (ReflectiveOperationException e) {
            CannonEconomy.LOGGER.warn("Could not disable Village Recruits spawns: {}", e.getMessage());
        }
    }

    private static void disableWariumMercenaries() {
        if (!ModList.get().isLoaded("recruitswr")) {
            return;
        }
        try {
            Class<?> cfg = Class.forName("com.logic.recruitswr.config.RecruitsWariumConfig");
            setBoolean(cfg, "SHOULD_MERCENARIES_SPAWN", false);
            CannonEconomy.LOGGER.info("Recruits Warium mercenary patrol spawns disabled");
        } catch (ReflectiveOperationException e) {
            CannonEconomy.LOGGER.warn("Could not disable Recruits Warium spawns: {}", e.getMessage());
        }
    }

    private static void setBoolean(Class<?> cfg, String fieldName, boolean value) throws ReflectiveOperationException {
        ForgeConfigSpec.BooleanValue spec = (ForgeConfigSpec.BooleanValue) cfg.getField(fieldName).get(null);
        spec.set(value);
    }

    private static void setInt(Class<?> cfg, String fieldName, int value) throws ReflectiveOperationException {
        ForgeConfigSpec.IntValue spec = (ForgeConfigSpec.IntValue) cfg.getField(fieldName).get(null);
        spec.set(value);
    }

    private static void setBooleanOnInstance(Class<?> cfg, Object instance, String fieldName, boolean value)
            throws ReflectiveOperationException {
        ForgeConfigSpec.BooleanValue spec = (ForgeConfigSpec.BooleanValue) cfg.getField(fieldName).get(instance);
        spec.set(value);
    }
}
