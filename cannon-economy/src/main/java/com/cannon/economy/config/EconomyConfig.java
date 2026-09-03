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
    public static final ForgeConfigSpec.BooleanValue FARM_CHARM_SPRINKLER_FERTILITY_BONUS;
    public static final ForgeConfigSpec.IntValue SPRINKLER_FERTILITY_BONUS;

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

    // Recruits claim pricing
    public static final ForgeConfigSpec.BooleanValue OVERRIDE_RECRUITS_CLAIMS;
    public static final ForgeConfigSpec.ConfigValue<String> RECRUITS_CURRENCY;
    public static final ForgeConfigSpec.IntValue RECRUITS_CLAIMING_COST;
    public static final ForgeConfigSpec.IntValue RECRUITS_CHUNK_COST;

    // Worldgen / ambient structures
    public static final ForgeConfigSpec.BooleanValue BLOCK_RECRUITS_WORLDGEN;
    public static final ForgeConfigSpec.BooleanValue BLOCK_HYW_NEARBY_STRUCTURES;

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
                .comment("Deprecated — kept for config compat. Growth on dry soil is always blocked when fertility is enabled.")
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
        FARM_CHARM_SPRINKLER_FERTILITY_BONUS = b
                .comment("Raise displayed fertility near Farm & Charm water sprinklers (in addition to irrigation).")
                .define("farmCharmSprinklerFertilityBonus", true);
        SPRINKLER_FERTILITY_BONUS = b
                .comment("Extra fertility levels within sprinkler range (stacks with biome, capped at 5).")
                .defineInRange("sprinklerFertilityBonus", 1, 0, 2);
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

        b.comment("Override Recruits claim pricing and currency.").push("claims");
        OVERRIDE_RECRUITS_CLAIMS = b
                .comment("Apply custom claim currency and costs to Recruits on server start.")
                .define("overrideRecruitsClaims", true);
        RECRUITS_CURRENCY = b
                .comment("Item id used as currency for Recruits hiring and claims.")
                .define("recruitsCurrency", "minecraft:gold_ingot");
        RECRUITS_CLAIMING_COST = b
                .comment("Cost in currency items to claim a 5×5 chunk territory.")
                .defineInRange("claimingCost", 10, 0, 1453);
        RECRUITS_CHUNK_COST = b
                .comment("Cost in currency items to expand a claim by one chunk (0 = free).")
                .defineInRange("chunkCost", 0, 0, 1453);
        b.pop();

        b.comment("Block ambient military structures from Recruits addons and HYW worldgen.").push("worldgen");
        BLOCK_RECRUITS_WORLDGEN = b
                .comment("Disable Recruits patrols, Village Recruits tower/sky villages (datapack + config), Warium mercenaries.")
                .define("blockRecruitsStructures", true);
        BLOCK_HYW_NEARBY_STRUCTURES = b
                .comment("Disable HYW auto-generated nearby structures (bandit camps, etc.).")
                .define("blockHywNearbyStructures", true);
        b.pop();

        SPEC = b.build();
    }

    private EconomyConfig() {}
}
