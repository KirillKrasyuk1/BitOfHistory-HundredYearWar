package com.cannon.economy.config;

import net.minecraftforge.common.ForgeConfigSpec;

public final class EconomyConfig {
    public static final ForgeConfigSpec SPEC;

    // Fertility
    public static final ForgeConfigSpec.BooleanValue ENABLE_FERTILITY;
    public static final ForgeConfigSpec.IntValue FERTILITY_CELL_SIZE;
    public static final ForgeConfigSpec.BooleanValue SHOW_FERTILITY_ON_HOE;
    public static final ForgeConfigSpec.BooleanValue REQUIRE_WATER_FOR_CROPS;
    public static final ForgeConfigSpec.IntValue WATER_RADIUS;
    public static final ForgeConfigSpec.IntValue RIVER_FLOODPLAIN_RADIUS;
    public static final ForgeConfigSpec.IntValue RIVER_FERTILITY;
    public static final ForgeConfigSpec.BooleanValue FARM_CHARM_SPRINKLER_COUNTS_AS_WATER;
    public static final ForgeConfigSpec.IntValue FARM_CHARM_SPRINKLER_RADIUS;
    public static final ForgeConfigSpec.BooleanValue FARM_CHARM_FERTILIZED_BONUS;
    public static final ForgeConfigSpec.IntValue FERTILIZED_SOIL_FERTILITY_BONUS;

    // Deposits
    public static final ForgeConfigSpec.IntValue DEFAULT_BLOCK_RADIUS;
    public static final ForgeConfigSpec.IntValue DEFAULT_REPLACE_PERCENT;
    public static final ForgeConfigSpec.IntValue DEFAULT_DEPTH;
    public static final ForgeConfigSpec.IntValue DEFAULT_REGEN_SECONDS;
    public static final ForgeConfigSpec.IntValue ORES_PER_REGEN;
    public static final ForgeConfigSpec.IntValue CONVERT_BLOCKS_PER_TICK;

    // HYW
    public static final ForgeConfigSpec.DoubleValue SUPPLY_CONSUMPTION_MULTIPLIER;
    public static final ForgeConfigSpec.IntValue DIAMONDS_PER_NETHERITE;

    static {
        ForgeConfigSpec.Builder b = new ForgeConfigSpec.Builder();

        b.comment("Soil fertility (1–5) controls crop growth speed.").push("fertility");
        ENABLE_FERTILITY = b.define("enableFertility", true);
        FERTILITY_CELL_SIZE = b
                .comment("Horizontal cell size in blocks for random fertility within a biome.")
                .defineInRange("cellSize", 8, 4, 32);
        SHOW_FERTILITY_ON_HOE = b
                .comment("Show fertility when right-clicking farmland/dirt with a hoe.")
                .define("showFertilityOnHoe", true);
        REQUIRE_WATER_FOR_CROPS = b
                .comment("Crops can only be planted within waterRadius of water (or a Farm & Charm sprinkler).")
                .define("requireWaterForCrops", true);
        WATER_RADIUS = b
                .comment("Max horizontal distance to a water source block for planting/growing.")
                .defineInRange("waterRadius", 4, 1, 16);
        RIVER_FLOODPLAIN_RADIUS = b
                .comment("Blocks around a position to detect river biomes for Nile-like 2× floodplains.")
                .defineInRange("riverFloodplainRadius", 8, 2, 32);
        RIVER_FERTILITY = b
                .comment("Fertility level near rivers (5 = 2× growth).")
                .defineInRange("riverFertility", 5, 1, 5);
        FARM_CHARM_SPRINKLER_COUNTS_AS_WATER = b
                .comment("Treat Farm & Charm water sprinklers as irrigation (optional mod).")
                .define("farmCharmSprinklerCountsAsWater", true);
        FARM_CHARM_SPRINKLER_RADIUS = b
                .comment("Horizontal range to detect Farm & Charm sprinklers (mod default is 8).")
                .defineInRange("farmCharmSprinklerRadius", 8, 1, 32);
        FARM_CHARM_FERTILIZED_BONUS = b
                .comment("Add a fertility bonus on Farm & Charm fertilized soil/farmland.")
                .define("farmCharmFertilizedBonus", true);
        FERTILIZED_SOIL_FERTILITY_BONUS = b
                .comment("Extra fertility levels on fertilized soil (stacks with biome, capped at 5).")
                .defineInRange("fertilizedSoilFertilityBonus", 1, 0, 2);
        b.pop();

        b.comment("Admin ore deposits: fixed-position veins with regeneration.").push("deposits");
        DEFAULT_BLOCK_RADIUS = b
                .comment("Default horizontal vein radius in blocks (XZ).")
                .defineInRange("defaultBlockRadius", 24, 4, 128);
        DEFAULT_REPLACE_PERCENT = b
                .comment("Default percent of stone replaced when laying out a vein.")
                .defineInRange("defaultReplacePercent", 3, 1, 25);
        DEFAULT_DEPTH = b
                .comment("Default vertical thickness of a vein, centered on admin Y.")
                .defineInRange("defaultDepth", 12, 1, 64);
        DEFAULT_REGEN_SECONDS = b
                .comment("Default seconds before mined vein blocks regenerate.")
                .defineInRange("defaultRegenSeconds", 300, 5, 86400);
        ORES_PER_REGEN = b
                .comment("Max vein blocks restored per regen tick across each deposit.")
                .defineInRange("oresPerRegen", 2, 1, 32);
        CONVERT_BLOCKS_PER_TICK = b
                .comment("Blocks scanned per tick while laying out a new vein.")
                .defineInRange("convertBlocksPerTick", 8000, 500, 100000);
        b.pop();

        b.comment("HYW balance (Nether/End closed — netherite replaced in bundled HYW JSON overrides).").push("hyw");
        SUPPLY_CONSUMPTION_MULTIPLIER = b
                .comment("Multiplier for HYW army food/supply consumption (2.0 = armies eat twice as much).")
                .defineInRange("supplyConsumptionMultiplier", 2.0, 1.0, 10.0);
        DIAMONDS_PER_NETHERITE = b
                .comment("Each netherite_ingot in recruitment costs becomes this many extra diamonds (baked into JSON).")
                .defineInRange("diamondsPerNetherite", 3, 1, 16);
        b.pop();

        SPEC = b.build();
    }

    private EconomyConfig() {}
}
