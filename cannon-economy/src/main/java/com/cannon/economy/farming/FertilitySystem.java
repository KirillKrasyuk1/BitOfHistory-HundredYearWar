package com.cannon.economy.farming;

import com.cannon.economy.CannonEconomy;
import com.cannon.economy.config.EconomyConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.FarmBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Soil fertility (1–5) controls crop growth speed.
 * Most land is poor; arid biomes need irrigation; river floodplains give strict 2× (Nile-like).
 * Compatible with [Let's Do] Farm &amp; Charm sprinklers and fertilized soil.
 */
@Mod.EventBusSubscriber(modid = CannonEconomy.MOD_ID)
public final class FertilitySystem {

    private FertilitySystem() {}

    public static int getFertility(ServerLevel level, BlockPos pos) {
        if (!EconomyConfig.ENABLE_FERTILITY.get()) {
            return 3;
        }
        int base = computeBaseFertility(level, pos);
        BlockState soil = level.getBlockState(pos);
        if (soil.getBlock() instanceof CropBlock || soil.is(net.minecraft.tags.BlockTags.CROPS)) {
            soil = level.getBlockState(pos.below());
        }
        int bonus = FarmCharmIntegration.fertilityBonus(soil);
        return Mth.clamp(base + bonus, 1, 5);
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

    public static boolean hasIrrigation(ServerLevel level, BlockPos cropPos) {
        if (!EconomyConfig.REQUIRE_WATER_FOR_CROPS.get()) {
            return true;
        }
        BlockPos soilPos = cropPos;
        BlockState soil = level.getBlockState(soilPos);
        if (soil.getBlock() instanceof CropBlock || soil.is(net.minecraft.tags.BlockTags.CROPS)) {
            soilPos = cropPos.below();
            soil = level.getBlockState(soilPos);
        }
        if (FarmCharmIntegration.isFertilizedSoil(soil)) {
            return true;
        }
        return hasWaterNearby(level, soilPos);
    }

    private static int computeBaseFertility(ServerLevel level, BlockPos pos) {
        int cell = EconomyConfig.FERTILITY_CELL_SIZE.get();
        int cx = Math.floorDiv(pos.getX(), cell);
        int cz = Math.floorDiv(pos.getZ(), cell);
        long seed = level.getSeed()
                ^ ((long) cx * 341873128712L)
                ^ ((long) cz * 132897987541L)
                ^ 0xC4FE07111L;
        RandomSource random = RandomSource.create(seed);

        Holder<net.minecraft.world.level.biome.Biome> biome = level.getBiome(pos);
        boolean nearWater = hasWaterNearby(level, pos);
        boolean riverBiome = isRiverBiome(biome);
        boolean nearRiver = riverBiome || hasRiverBiomeNearby(level, pos);

        if (riverBiome || (nearRiver && nearWater)) {
            return EconomyConfig.RIVER_FERTILITY.get();
        }

        if (isAridBiome(biome)) {
            if (nearWater) {
                return EconomyConfig.RIVER_FERTILITY.get();
            }
            return Mth.clamp(1 + random.nextInt(2), 1, 2);
        }

        if (isPoorBiome(biome)) {
            return Mth.clamp(1 + random.nextInt(2), 1, 2);
        }

        if (isRichBiome(biome)) {
            if (nearWater) {
                return Mth.clamp(3 + random.nextInt(3), 3, 5);
            }
            return Mth.clamp(2 + random.nextInt(2), 2, 3);
        }

        return Mth.clamp(2 + random.nextInt(2), 2, 3);
    }

    private static boolean hasWaterNearby(ServerLevel level, BlockPos origin) {
        int waterRadius = EconomyConfig.WATER_RADIUS.get();
        if (hasFluidWaterNearby(level, origin, waterRadius)) {
            return true;
        }
        int sprinklerRadius = EconomyConfig.FARM_CHARM_SPRINKLER_RADIUS.get();
        return FarmCharmIntegration.hasSprinklerNearby(level, origin, sprinklerRadius);
    }

    private static boolean hasFluidWaterNearby(ServerLevel level, BlockPos origin, int radius) {
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (dx * dx + dz * dz > radius * radius) {
                        continue;
                    }
                    cursor.set(origin.getX() + dx, origin.getY() + dy, origin.getZ() + dz);
                    FluidState fluid = level.getFluidState(cursor);
                    if (fluid.is(FluidTags.WATER) && fluid.isSource()) {
                        return true;
                    }
                }
            }
        }
        return false;
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

    private static boolean isPlantableCrop(Block block) {
        if (block instanceof CropBlock || block instanceof BushBlock) {
            return true;
        }
        if (block instanceof BonemealableBlock) {
            return true;
        }
        BlockState defaultState = block.defaultBlockState();
        return defaultState.is(net.minecraft.tags.BlockTags.CROPS);
    }

    private enum PlantDenyReason {
        NONE,
        NO_WATER,
        INFERTILE
    }

    private static PlantDenyReason validatePlanting(ServerLevel level, BlockPos cropPos) {
        if (!EconomyConfig.ENABLE_FERTILITY.get()) {
            return PlantDenyReason.NONE;
        }
        if (!hasIrrigation(level, cropPos)) {
            return PlantDenyReason.NO_WATER;
        }
        if (getFertility(level, cropPos) <= 1) {
            return PlantDenyReason.INFERTILE;
        }
        return PlantDenyReason.NONE;
    }

    private static Component denyMessage(PlantDenyReason reason) {
        return switch (reason) {
            case NO_WATER -> Component.translatable("message.cannon_economy.no_water");
            case INFERTILE -> Component.translatable("message.cannon_economy.cannot_plant");
            default -> Component.empty();
        };
    }

    @SubscribeEvent
    public static void onCropPlace(BlockEvent.EntityPlaceEvent event) {
        if (!EconomyConfig.ENABLE_FERTILITY.get() || event.getLevel().isClientSide()) {
            return;
        }
        BlockState state = event.getPlacedBlock();
        if (!isPlantableCrop(state.getBlock())) {
            return;
        }
        ServerLevel level = (ServerLevel) event.getLevel();
        BlockPos cropPos = event.getPos();
        PlantDenyReason reason = validatePlanting(level, cropPos);
        if (reason != PlantDenyReason.NONE) {
            event.setCanceled(true);
            if (event.getEntity() != null) {
                event.getEntity().sendSystemMessage(denyMessage(reason));
            }
        }
    }

    @SubscribeEvent
    public static void onSeedUse(PlayerInteractEvent.RightClickBlock event) {
        if (!EconomyConfig.ENABLE_FERTILITY.get() || event.getLevel().isClientSide()) {
            return;
        }
        ItemStack stack = event.getItemStack();
        if (!(stack.getItem() instanceof BlockItem blockItem)) {
            return;
        }
        if (!isPlantableCrop(blockItem.getBlock())) {
            return;
        }
        ServerLevel level = (ServerLevel) event.getLevel();
        BlockPos cropPos = event.getPos().above();
        PlantDenyReason reason = validatePlanting(level, cropPos);
        if (reason != PlantDenyReason.NONE) {
            event.setCanceled(true);
            event.setCancellationResult(net.minecraft.world.InteractionResult.FAIL);
            if (event.getEntity() != null) {
                event.getEntity().displayClientMessage(denyMessage(reason), true);
            }
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
            event.setResult(Event.Result.ALLOW);
            scheduleExtraGrowthTick(level, pos, mult - 1.0f, random);
        }
    }

    /**
     * Schedules extra random ticks instead of jumping multiple ages in one tick
     * (which skips visual growth stages).
     */
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
        int fertility = getFertility(level, pos);
        float mult = growthMultiplier(fertility);
        boolean irrigated = hasIrrigation(level, pos);
        Component line1 = Component.translatable("message.cannon_economy.fertility", fertility, String.format("%.2f", mult));
        Component line2 = irrigated
                ? Component.translatable("message.cannon_economy.irrigated")
                : Component.translatable("message.cannon_economy.not_irrigated");
        event.getEntity().displayClientMessage(line1, true);
        event.getEntity().displayClientMessage(line2, true);
        if (FarmCharmIntegration.isLoaded()) {
            event.getEntity().displayClientMessage(Component.translatable("message.cannon_economy.farm_charm_hint"), true);
        }
    }
}
