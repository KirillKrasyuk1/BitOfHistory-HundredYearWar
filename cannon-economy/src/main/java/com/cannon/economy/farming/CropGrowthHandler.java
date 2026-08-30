package com.cannon.economy.farming;

import com.cannon.economy.config.EconomyConfig;
import com.cannon.economy.deposit.DepositSavedData;
import com.cannon.economy.deposit.DepositType;
import com.cannon.economy.deposit.ResourceDeposit;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.Optional;

public class CropGrowthHandler {

    private static final TagKey<Biome> PLAINS_LIKE = TagKey.create(
            Registries.BIOME, new ResourceLocation("cannon_economy", "plains_farmland"));

    @SubscribeEvent
    public void onCropPlace(BlockEvent.EntityPlaceEvent event) {
        if (!EconomyConfig.ENABLE_CROP_RULES.get() || event.getLevel().isClientSide()) {
            return;
        }
        BlockState state = event.getPlacedBlock();
        if (!state.is(BlockTags.CROPS) && !(state.getBlock() instanceof CropBlock)) {
            return;
        }
        if (!canGrowAt((ServerLevel) event.getLevel(), event.getPos())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void onCropGrow(BlockEvent.CropGrowEvent.Pre event) {
        if (!EconomyConfig.ENABLE_CROP_RULES.get() || event.getLevel().isClientSide()) {
            return;
        }
        ServerLevel level = (ServerLevel) event.getLevel();
        BlockPos pos = event.getPos();

        if (!canGrowAt(level, pos)) {
            event.setResult(net.minecraftforge.eventbus.api.Event.Result.DENY);
            return;
        }

        Optional<ResourceDeposit> fertile = DepositSavedData.get(level)
                .ofTypeAt(pos, level.dimension(), DepositType.FERTILE);
        if (fertile.isPresent()) {
            double mult = EconomyConfig.CROP_GROWTH_MULTIPLIER_FERTILE.get();
            for (int i = 1; i < (int) mult; i++) {
                BlockState state = level.getBlockState(pos);
                if (state.getBlock() instanceof CropBlock crop && !crop.isMaxAge(state)) {
                    level.setBlock(pos, crop.getStateForAge(crop.getAge(state) + 1), Block.UPDATE_ALL);
                }
            }
        }
    }

    public static boolean canGrowAt(ServerLevel level, BlockPos pos) {
        ResourceKey<Level> dim = level.dimension();
        if (DepositSavedData.get(level).ofTypeAt(pos, dim, DepositType.FERTILE).isPresent()) {
            return true;
        }

        Biome biome = level.getBiome(pos).value();
        ResourceLocation biomeId = level.registryAccess().registryOrThrow(Registries.BIOME).getKey(biome);
        if (biomeId == null) {
            return true;
        }

        String path = biomeId.getPath();
        boolean plainsLike = path.contains("plains") || path.contains("river") || path.contains("meadow")
                || level.getBiome(pos).is(PLAINS_LIKE);
        if (!plainsLike) {
            // Other biomes: allow desert cactus, taiga beetroot-style via permissive default
            if (path.contains("desert") || path.contains("badlands")) {
                return false; // no wheat in desert without FERTILE deposit
            }
            if (path.contains("taiga") || path.contains("snowy")) {
                return false;
            }
            // forest/jungle — limited farming
            return path.contains("forest") || path.contains("jungle") || path.contains("swamp");
        }

        return isNearWater(level, pos, EconomyConfig.FERTILE_WATER_RADIUS.get());
    }

    private static boolean isNearWater(ServerLevel level, BlockPos origin, int radius) {
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                for (int dy = -2; dy <= 2; dy++) {
                    cursor.set(origin.getX() + dx, origin.getY() + dy, origin.getZ() + dz);
                    if (level.getFluidState(cursor).isSource()) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
