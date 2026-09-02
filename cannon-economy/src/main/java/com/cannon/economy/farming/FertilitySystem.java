package com.cannon.economy.farming;

import com.cannon.economy.CannonEconomy;
import com.cannon.economy.config.EconomyConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.FarmBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Soil fertility (1–5) controls crop growth speed. No block placement restrictions —
 * crops simply won't grow on dry (non-irrigated) soil.
 */
@Mod.EventBusSubscriber(modid = CannonEconomy.MOD_ID)
public final class FertilitySystem {

    public record FertilityResult(int displayLevel, float growthMultiplier, boolean irrigated, int biomeBase) {}

    private FertilitySystem() {}

    public static FertilityResult evaluate(ServerLevel level, BlockPos pos) {
        if (!EconomyConfig.ENABLE_FERTILITY.get()) {
            return new FertilityResult(3, 1.0f, true, 3);
        }

        BlockState soil = level.getBlockState(pos);
        if (soil.getBlock() instanceof CropBlock || soil.is(net.minecraft.tags.BlockTags.CROPS)) {
            soil = level.getBlockState(pos.below());
        }

        Holder<net.minecraft.world.level.biome.Biome> biome = level.getBiome(pos);
        int biomeBase = computeBiomeBase(level, pos, biome);
        boolean irrigated = hasIrrigation(level, pos, soil);
        int bonus = FarmCharmIntegration.fertilityBonus(soil);

        boolean river = isRiverBiome(biome) || hasRiverBiomeNearby(level, pos);
        if (river) {
            float mult = isAridBiome(biome)
                    ? growthMultiplier(biomeBase) * 2.0f
                    : growthMultiplier(5);
            return new FertilityResult(5, mult, irrigated, biomeBase);
        }

        int display = Mth.clamp(biomeBase + bonus, 1, 5);
        return new FertilityResult(display, growthMultiplier(display), irrigated, biomeBase);
    }

    public static int getFertility(ServerLevel level, BlockPos pos) {
        return evaluate(level, pos).displayLevel();
    }

    public static float growthMultiplier(int fertility) {
        return switch (fertility) {
            case 1 -> 0.25f;
            case 2 -> 0.75f;
            case 3 -> 1.0f;
            case 4 -> 1.25f;
            case 5 -> 1.5f;
            default -> 1.0f;
        };
    }

    /** Sprinkler, fertilized soil, or natural (world-gen) water — not player-placed buckets. */
    public static boolean hasIrrigation(ServerLevel level, BlockPos cropPos, BlockState soil) {
        if (FarmCharmIntegration.isFertilizedSoil(soil)) {
            return true;
        }
        int sprinklerRadius = EconomyConfig.FARM_CHARM_SPRINKLER_RADIUS.get();
        if (FarmCharmIntegration.hasSprinklerNearby(level, cropPos, sprinklerRadius)) {
            return true;
        }
        return NaturalWaterTracker.hasNaturalWaterNearby(level, cropPos);
    }

    private static boolean hasIrrigation(ServerLevel level, BlockPos cropPos) {
        BlockState soil = level.getBlockState(cropPos);
        if (soil.getBlock() instanceof CropBlock || soil.is(net.minecraft.tags.BlockTags.CROPS)) {
            soil = level.getBlockState(cropPos.below());
        }
        return hasIrrigation(level, cropPos, soil);
    }

    private static int computeBiomeBase(ServerLevel level, BlockPos pos, Holder<net.minecraft.world.level.biome.Biome> biome) {
        int cell = EconomyConfig.FERTILITY_CELL_SIZE.get();
        int cx = Math.floorDiv(pos.getX(), cell);
        int cz = Math.floorDiv(pos.getZ(), cell);
        long seed = level.getSeed()
                ^ ((long) cx * 341873128712L)
                ^ ((long) cz * 132897987541L)
                ^ 0xC4FE07111L;
        RandomSource random = RandomSource.create(seed);

        boolean nearNaturalWater = NaturalWaterTracker.hasNaturalWaterNearby(level, pos);

        if (isAridBiome(biome)) {
            return Mth.clamp(1 + random.nextInt(2), 1, 2);
        }
        if (isPoorBiome(biome)) {
            return Mth.clamp(1 + random.nextInt(2), 1, 2);
        }
        if (isRichBiome(biome)) {
            if (nearNaturalWater) {
                return Mth.clamp(3 + random.nextInt(3), 3, 5);
            }
            return Mth.clamp(2 + random.nextInt(2), 2, 3);
        }
        return Mth.clamp(2 + random.nextInt(2), 2, 3);
    }

    private static boolean hasRiverBiomeNearby(ServerLevel level, BlockPos origin) {
        int radius = EconomyConfig.RIVER_FLOODPLAIN_RADIUS.get();
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                if (dx * dx + dz * dz > radius * radius) {
                    continue;
                }
                cursor.set(origin.getX() + dx, origin.getY(), origin.getZ() + dz);
                if (isRiverBiome(level.getBiome(cursor))) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean isRiverBiome(Holder<net.minecraft.world.level.biome.Biome> biome) {
        if (biome.is(BiomeTags.IS_RIVER)) {
            return true;
        }
        ResourceLocation id = biome.unwrapKey().map(k -> k.location()).orElse(null);
        return id != null && id.getPath().contains("river");
    }

    private static boolean isAridBiome(Holder<net.minecraft.world.level.biome.Biome> biome) {
        if (biome.is(EconomyBiomeTags.ARID)) {
            return true;
        }
        ResourceLocation id = biome.unwrapKey().map(k -> k.location()).orElse(null);
        if (id == null) {
            return false;
        }
        String path = id.getPath();
        return path.contains("savanna") || path.contains("desert") || path.contains("badlands")
                || path.contains("arid") || path.contains("outback") || path.contains("scrub");
    }

    private static boolean isPoorBiome(Holder<net.minecraft.world.level.biome.Biome> biome) {
        if (isAridBiome(biome)) {
            return true;
        }
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
        String path = id.getPath();
        return path.contains("frozen") || path.contains("snowy") || path.contains("ice")
                || path.contains("peak") || path.contains("grove") || path.contains("forest")
                || path.contains("taiga") || path.contains("windswept") || path.contains("stony");
    }

    private static boolean isRichBiome(Holder<net.minecraft.world.level.biome.Biome> biome) {
        if (biome.is(EconomyBiomeTags.PLAINS_FARMLAND)) {
            return true;
        }
        ResourceLocation id = biome.unwrapKey().map(k -> k.location()).orElse(null);
        if (id == null) {
            return false;
        }
        String path = id.getPath();
        return path.contains("plains") || path.contains("meadow") || path.contains("prairie");
    }

    @SubscribeEvent
    public static void onCropGrow(BlockEvent.CropGrowEvent.Pre event) {
        if (!EconomyConfig.ENABLE_FERTILITY.get() || event.getLevel().isClientSide()) {
            return;
        }
        ServerLevel level = (ServerLevel) event.getLevel();
        BlockPos pos = event.getPos();
        FertilityResult fertility = evaluate(level, pos);

        if (!fertility.irrigated()) {
            event.setResult(Event.Result.DENY);
            return;
        }

        float mult = fertility.growthMultiplier();
        RandomSource random = level.getRandom();

        if (mult <= 0f) {
            event.setResult(Event.Result.DENY);
            return;
        }

        if (mult < 1.0f) {
            if (random.nextFloat() > mult) {
                event.setResult(Event.Result.DENY);
            }
            return;
        }

        if (mult > 1.0f) {
            event.setResult(Event.Result.ALLOW);
            scheduleExtraGrowthTick(level, pos, mult - 1.0f, random);
        }
    }

    private static void scheduleExtraGrowthTick(
            ServerLevel level, BlockPos pos, float extraChance, RandomSource random) {
        if (extraChance <= 0f) {
            return;
        }
        BlockState state = level.getBlockState(pos);
        Block block = state.getBlock();
        if (block instanceof CropBlock crop && crop.isMaxAge(state)) {
            return;
        }
        while (extraChance > 0f) {
            if (extraChance >= 1.0f || random.nextFloat() < extraChance) {
                level.scheduleTick(pos, block, 1);
            }
            extraChance -= 1.0f;
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
                && !FarmCharmIntegration.isFertilizedSoil(state)
                && !(state.getBlock() instanceof CropBlock)
                && !(state.getBlock() instanceof BushBlock)) {
            if (!state.is(net.minecraft.world.level.block.Blocks.DIRT)
                    && !state.is(net.minecraft.world.level.block.Blocks.GRASS_BLOCK)
                    && !state.is(net.minecraft.world.level.block.Blocks.FARMLAND)) {
                return;
            }
        }
        FertilityResult fertility = evaluate(level, pos);
        Component line1 = Component.translatable(
                "message.cannon_economy.fertility",
                fertility.displayLevel(),
                String.format("%.2f", fertility.growthMultiplier()));
        Component line2 = fertility.irrigated()
                ? Component.translatable("message.cannon_economy.irrigated")
                : Component.translatable("message.cannon_economy.not_irrigated");
        event.getEntity().displayClientMessage(line1, true);
        event.getEntity().displayClientMessage(line2, true);
        if (isRiverBiome(level.getBiome(pos)) || hasRiverBiomeNearby(level, pos)) {
            if (isAridBiome(level.getBiome(pos))) {
                event.getEntity().displayClientMessage(
                        Component.translatable("message.cannon_economy.river_arid_bonus"), true);
            }
        }
        if (FarmCharmIntegration.isLoaded()) {
            event.getEntity().displayClientMessage(Component.translatable("message.cannon_economy.farm_charm_hint"), true);
        }
    }
}
