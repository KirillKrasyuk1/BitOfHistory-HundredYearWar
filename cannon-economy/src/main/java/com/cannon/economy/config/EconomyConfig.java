package com.cannon.economy.config;

import net.minecraftforge.common.ForgeConfigSpec;

public final class EconomyConfig {
    public static final ForgeConfigSpec SPEC;

    public static final ForgeConfigSpec.BooleanValue ENABLE_CROP_RULES;
    public static final ForgeConfigSpec.IntValue FERTILE_WATER_RADIUS;
    public static final ForgeConfigSpec.DoubleValue CROP_GROWTH_MULTIPLIER_FERTILE;
    public static final ForgeConfigSpec.DoubleValue DEPOSIT_BONUS_MULTIPLIER;
    public static final ForgeConfigSpec.IntValue DEFAULT_DEPOSIT_RADIUS;
    public static final ForgeConfigSpec.BooleanValue CHECK_RECRUITS_EMBARGO;
    public static final ForgeConfigSpec.IntValue DEFAULT_ROUTE_INTERVAL_TICKS;
    public static final ForgeConfigSpec.IntValue CARAVAN_SPEED_BLOCKS_PER_TICK;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

        builder.comment("Crop growth restricted by biome and proximity to water (GoT-style river valleys).")
                .push("farming");
        ENABLE_CROP_RULES = builder.define("enableCropRules", true);
        FERTILE_WATER_RADIUS = builder
                .comment("Max horizontal distance to water for crops on plains/river biomes.")
                .defineInRange("fertileWaterRadius", 4, 1, 16);
        CROP_GROWTH_MULTIPLIER_FERTILE = builder
                .comment("Extra random growth attempts multiplier inside admin FERTILE deposits.")
                .defineInRange("fertileDepositGrowthMultiplier", 2.0, 1.0, 5.0);
        builder.pop();

        builder.comment("Admin-placed strategic resource deposits.").push("deposits");
        DEPOSIT_BONUS_MULTIPLIER = builder
                .comment("Extra drop multiplier when mining matching ores inside a deposit.")
                .defineInRange("depositBonusMultiplier", 1.5, 1.0, 5.0);
        DEFAULT_DEPOSIT_RADIUS = builder
                .defineInRange("defaultDepositRadius", 32, 8, 128);
        builder.pop();

        builder.comment("Automated trade routes between Trade Posts.").push("trade");
        CHECK_RECRUITS_EMBARGO = builder
                .comment("Block caravans when Recruits faction embargo applies to route owner.")
                .define("checkRecruitsEmbargo", true);
        DEFAULT_ROUTE_INTERVAL_TICKS = builder
                .comment("Default ticks between caravan departures (6000 = 5 min).")
                .defineInRange("defaultRouteIntervalTicks", 6000, 200, 72000);
        CARAVAN_SPEED_BLOCKS_PER_TICK = builder
                .comment("How many blocks a caravan moves per server tick (integer >= 1).")
                .defineInRange("caravanSpeedBlocksPerTick", 1, 1, 5);
        builder.pop();

        SPEC = builder.build();
    }

    private EconomyConfig() {}
}
