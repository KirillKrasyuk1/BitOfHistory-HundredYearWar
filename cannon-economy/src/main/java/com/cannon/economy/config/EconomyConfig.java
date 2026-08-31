package com.cannon.economy.config;

import net.minecraftforge.common.ForgeConfigSpec;

public final class EconomyConfig {
    public static final ForgeConfigSpec SPEC;

    // Fertility
    public static final ForgeConfigSpec.BooleanValue ENABLE_FERTILITY;
    public static final ForgeConfigSpec.IntValue FERTILITY_CELL_SIZE;
    public static final ForgeConfigSpec.BooleanValue SHOW_FERTILITY_ON_HOE;

    // Deposits
    public static final ForgeConfigSpec.IntValue DEFAULT_CHUNK_RADIUS;
    public static final ForgeConfigSpec.IntValue ORE_MIN_Y;
    public static final ForgeConfigSpec.IntValue ORE_MAX_Y;
    public static final ForgeConfigSpec.IntValue REGEN_INTERVAL_TICKS;
    public static final ForgeConfigSpec.IntValue ORES_PER_REGEN;
    public static final ForgeConfigSpec.IntValue MINED_COOLDOWN_TICKS;
    public static final ForgeConfigSpec.DoubleValue TARGET_ORE_RATIO;
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
        b.pop();

        b.comment("Admin ore deposits: replace + regenerate ores in a chunk radius.").push("deposits");
        DEFAULT_CHUNK_RADIUS = b.defineInRange("defaultChunkRadius", 5, 1, 32);
        ORE_MIN_Y = b.defineInRange("oreMinY", -64, -64, 320);
        ORE_MAX_Y = b.defineInRange("oreMaxY", 64, -64, 320);
        REGEN_INTERVAL_TICKS = b
                .comment("Ticks between regeneration attempts (200 = 10 seconds).")
                .defineInRange("regenIntervalTicks", 200, 20, 72000);
        ORES_PER_REGEN = b
                .comment("Max new ore blocks placed per regen tick per deposit.")
                .defineInRange("oresPerRegen", 4, 1, 64);
        MINED_COOLDOWN_TICKS = b
                .comment("How long a mined position stays blacklisted for regen (12000 = 10 min).")
                .defineInRange("minedCooldownTicks", 12000, 200, 240000);
        TARGET_ORE_RATIO = b
                .comment("Target fraction of stone-like blocks that should be deposit ore (0.02 = 2%).")
                .defineInRange("targetOreRatio", 0.02, 0.001, 0.2);
        CONVERT_BLOCKS_PER_TICK = b
                .comment("Blocks scanned per tick during initial deposit conversion.")
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
