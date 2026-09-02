package com.cannon.economy.farming;

import com.cannon.economy.config.EconomyConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Optional integration with [Let's Do] Farm &amp; Charm — no hard dependency.
 */
public final class FarmCharmIntegration {

    private static final String MOD_ID = "farm_and_charm";
    private static final ResourceLocation WATER_SPRINKLER = new ResourceLocation(MOD_ID, "water_sprinkler");
    private static final ResourceLocation FERTILIZED_FARMLAND = new ResourceLocation(MOD_ID, "fertilized_farmland");
    private static final ResourceLocation FERTILIZED_SOIL = new ResourceLocation(MOD_ID, "fertilized_soil");

    private static Block sprinklerBlock;
    private static Block fertilizedFarmlandBlock;
    private static Block fertilizedSoilBlock;
    private static boolean resolved;

    private FarmCharmIntegration() {}

    public static boolean isLoaded() {
        resolve();
        return sprinklerBlock != null;
    }

    public static boolean hasSprinklerNearby(ServerLevel level, BlockPos origin, int radius) {
        if (!EconomyConfig.FARM_CHARM_SPRINKLER_COUNTS_AS_WATER.get()) {
            return false;
        }
        resolve();
        if (sprinklerBlock == null) {
            return false;
        }
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        int r = radius;
        for (int dx = -r; dx <= r; dx++) {
            for (int dy = -2; dy <= 2; dy++) {
                for (int dz = -r; dz <= r; dz++) {
                    if (dx * dx + dz * dz > r * r) {
                        continue;
                    }
                    cursor.set(origin.getX() + dx, origin.getY() + dy, origin.getZ() + dz);
                    if (level.getBlockState(cursor).is(sprinklerBlock)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public static int fertilityBonus(BlockState soil) {
        if (!EconomyConfig.FARM_CHARM_FERTILIZED_BONUS.get()) {
            return 0;
        }
        resolve();
        if (fertilizedFarmlandBlock != null && soil.is(fertilizedFarmlandBlock)) {
            return EconomyConfig.FERTILIZED_SOIL_FERTILITY_BONUS.get();
        }
        if (fertilizedSoilBlock != null && soil.is(fertilizedSoilBlock)) {
            return EconomyConfig.FERTILIZED_SOIL_FERTILITY_BONUS.get();
        }
        return 0;
    }

    public static boolean isFertilizedSoil(BlockState soil) {
        resolve();
        return (fertilizedFarmlandBlock != null && soil.is(fertilizedFarmlandBlock))
                || (fertilizedSoilBlock != null && soil.is(fertilizedSoilBlock));
    }

    private static void resolve() {
        if (resolved) {
            return;
        }
        resolved = true;
        sprinklerBlock = ForgeRegistries.BLOCKS.getValue(WATER_SPRINKLER);
        fertilizedFarmlandBlock = ForgeRegistries.BLOCKS.getValue(FERTILIZED_FARMLAND);
        fertilizedSoilBlock = ForgeRegistries.BLOCKS.getValue(FERTILIZED_SOIL);
    }
}
