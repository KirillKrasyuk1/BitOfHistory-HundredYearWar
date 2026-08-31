package com.mrbysco.restrictivefarming.config;

import net.minecraftforge.common.ForgeConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

public final class FarmingConfig {
    public static final ForgeConfigSpec SPEC;
    public static final Common COMMON;

    public static final class Common {
        public final ForgeConfigSpec.BooleanValue defaultRestrictions;
        public final ForgeConfigSpec.BooleanValue restrictPlacement;
        public final ForgeConfigSpec.BooleanValue reduceGrowth;
        public final ForgeConfigSpec.DoubleValue growthReduction;
        public final ForgeConfigSpec.BooleanValue showRestrictedMessage;

        Common(ForgeConfigSpec.Builder builder) {
            builder.comment("General settings").push("general");

            defaultRestrictions = builder
                    .comment("Apply built-in dimension defaults (nether wart in nether, overworld crops in overworld).")
                    .define("defaultRestrictions", true);

            restrictPlacement = builder
                    .comment("Restrict crop placement to whitelisted biomes.")
                    .define("restrictPlacement", true);

            reduceGrowth = builder
                    .comment("Reduce crop growth speed outside whitelisted biomes.")
                    .define("reduceGrowth", true);

            growthReduction = builder
                    .comment("Growth skip chance outside whitelist (0.0 = none, 1.0 = never grows).")
                    .defineInRange("growthReduction", 0.5, 0.0, 1.0);

            showRestrictedMessage = builder
                    .comment("Notify players when placement is blocked.")
                    .define("showRestrictedMessage", true);

            builder.pop();
        }
    }

    static {
        Pair<Common, ForgeConfigSpec> pair = new ForgeConfigSpec.Builder().configure(Common::new);
        COMMON = pair.getLeft();
        SPEC = pair.getRight();
    }

    private FarmingConfig() {}
}
