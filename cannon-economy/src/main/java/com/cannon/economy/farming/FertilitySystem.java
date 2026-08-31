package com.cannon.economy.farming;

import com.cannon.economy.config.EconomyConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BiomeTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.level.block.FarmBlock;
import net.minecraft.world.level.block.BushBlock;

/**
 * Fertility 1–5:
 * 1 = 0.5× growth (2× slower), 2 = ~0.67× (1.5× slower),
 * 3 = 1× vanilla, 4 = 1.5×, 5 = 2×.
 * Frozen / forest / mountain → 1–2; plains / river floodplain → 3–5.
 */
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = com.cannon.economy.CannonEconomy.MOD_ID)
public final class FertilitySystem {

    private FertilitySystem() {}

    public static int getFertility(ServerLevel level, BlockPos pos) {
        if (!EconomyConfig.ENABLE_FERTILITY.get()) {
            return 3;
        }
        int cell = EconomyConfig.FERTILITY_CELL_SIZE.get();
        int cx = Math.floorDiv(pos.getX(), cell);
        int cz = Math.floorDiv(pos.getZ(), cell);
        long seed = level.getSeed()
                ^ ((long) cx * 341873128712L)
                ^ ((long) cz * 132897987541L)
                ^ 0xC4FE07111L;
        RandomSource random = RandomSource.create(seed);

        Holder<Biome> biome = level.getBiome(pos);
        int min;
        int max;
        if (isPoorBiome(biome)) {
            min = 1;
            max = 2;
        } else if (isRichBiome(biome, level, pos)) {
            min = 3;
            max = 5;
        } else {
            // temperate forests etc. — mediocre
            min = 2;
            max = 3;
        }
        return Mth.clamp(min + random.nextInt(max - min + 1), 1, 5);
    }

    public static float growthMultiplier(int fertility) {
        return switch (fertility) {
            case 1 -> 0.5f;
            case 2 -> 2f / 3f;
            case 3 -> 1.0f;
            case 4 -> 1.5f;
            case 5 -> 2.0f;
            default -> 1.0f;
        };
    }

    private static boolean isPoorBiome(Holder<Biome> biome) {
        if (biome.is(BiomeTags.IS_MOUNTAIN) || biome.is(BiomeTags.IS_HILL)) {
            return true;
        }
        if (biome.is(BiomeTags.IS_FOREST) || biome.is(BiomeTags.IS_TAIGA) || biome.is(BiomeTags.IS_JUNGLE)) {
            return true;
        }
        ResourceLocation id = biome.unwrapKey().map(k -> k.location()).orElse(null);
        if (id == null) {
            return false;
        }
        String p = id.getPath();
        return p.contains("frozen") || p.contains("snowy") || p.contains("ice")
                || p.contains("peak") || p.contains("grove") || p.contains("forest")
                || p.contains("taiga") || p.contains("windswept") || p.contains("stony");
    }

    private static boolean isRichBiome(Holder<Biome> biome, ServerLevel level, BlockPos pos) {
        ResourceLocation id = biome.unwrapKey().map(k -> k.location()).orElse(null);
        if (id != null) {
            String p = id.getPath();
            if (p.contains("plains") || p.contains("meadow") || p.contains("river")
                    || p.contains("flood") || p.contains("swamp") && p.contains("mangrove")) {
                return true;
            }
            if (p.contains("river") || p.equals("beach")) {
                return true;
            }
        }
        // Floodplain heuristic: plains-like near water
        if (id != null && id.getPath().contains("plains")) {
            return true;
        }
        return false;
    }

    @SubscribeEvent
    public static void onCropPlace(BlockEvent.EntityPlaceEvent event) {
        if (!EconomyConfig.ENABLE_FERTILITY.get() || event.getLevel().isClientSide()) {
            return;
        }
        BlockState state = event.getPlacedBlock();
        if (!(state.getBlock() instanceof CropBlock) && !state.is(net.minecraft.tags.BlockTags.CROPS)) {
            return;
        }
        ServerLevel level = (ServerLevel) event.getLevel();
        int fertility = getFertility(level, event.getPos());
        if (fertility <= 1) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onCropGrow(BlockEvent.CropGrowEvent.Pre event) {
        if (!EconomyConfig.ENABLE_FERTILITY.get() || event.getLevel().isClientSide()) {
            return;
        }
        ServerLevel level = (ServerLevel) event.getLevel();
        BlockPos pos = event.getPos();
        int fertility = getFertility(level, pos);
        float mult = growthMultiplier(fertility);
        RandomSource random = level.getRandom();

        if (mult < 1.0f) {
            if (random.nextFloat() > mult) {
                event.setResult(Event.Result.DENY);
            }
            return;
        }

        if (mult > 1.0f) {
            // Allow this tick, then optionally advance extra ages (1.5× / 2×)
            event.setResult(Event.Result.ALLOW);
            float extra = mult - 1.0f;
            while (extra > 0f) {
                if (extra >= 1.0f || random.nextFloat() < extra) {
                    BlockState state = level.getBlockState(pos);
                    if (state.getBlock() instanceof CropBlock crop && !crop.isMaxAge(state)) {
                        level.setBlock(pos, crop.getStateForAge(crop.getAge(state) + 1), Block.UPDATE_ALL);
                    }
                }
                extra -= 1.0f;
            }
        }
    }

    @SubscribeEvent
    public static void onHoeUse(PlayerInteractEvent.RightClickBlock event) {
        if (!EconomyConfig.SHOW_FERTILITY_ON_HOE.get() || event.getLevel().isClientSide()) {
            return;
        }
        if (!(event.getItemStack().getItem() instanceof HoeItem)
                && !event.getItemStack().is(ItemTags.HOES)) {
            return;
        }
        ServerLevel level = (ServerLevel) event.getLevel();
        BlockPos pos = event.getPos();
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof FarmBlock)
                && !(state.getBlock() instanceof CropBlock)
                && !(state.getBlock() instanceof BushBlock)) {
            // still allow checking dirt under feet of crop
            if (!state.is(net.minecraft.world.level.block.Blocks.DIRT)
                    && !state.is(net.minecraft.world.level.block.Blocks.GRASS_BLOCK)
                    && !state.is(net.minecraft.world.level.block.Blocks.FARMLAND)) {
                return;
            }
        }
        int fertility = getFertility(level, pos);
        float mult = growthMultiplier(fertility);
        event.getEntity().displayClientMessage(
                Component.translatable("message.cannon_economy.fertility", fertility, String.format("%.2f", mult)),
                true);
    }
}
