package com.cannon.economy.deposit;

import com.cannon.economy.CannonEconomy;
import com.cannon.economy.config.EconomyConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class OreDepositEngine {
    private static final Map<Long, Long> MINED_AT = new HashMap<>();

    @SubscribeEvent
    public void onBreak(BlockEvent.BreakEvent event) {
        if (event.getLevel().isClientSide()) {
            return;
        }
        ServerLevel level = (ServerLevel) event.getLevel();
        OreDeposit deposit = OreDepositSavedData.get(level).atBlock(event.getPos(), level.dimension()).orElse(null);
        if (deposit == null) {
            return;
        }
        long key = event.getPos().asLong();
        if (deposit.veinPositions.contains(key) || isTargetOre(event.getState(), deposit)) {
            MINED_AT.put(key, level.getGameTime());
        }
    }

    @SubscribeEvent
    public void onLevelTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.level.isClientSide()) {
            return;
        }
        ServerLevel level = (ServerLevel) event.level;
        long now = level.getGameTime();
        purgeMined(now);

        int budget = EconomyConfig.CONVERT_BLOCKS_PER_TICK.get();

        for (OreDeposit deposit : OreDepositSavedData.get(level).all()) {
            if (!deposit.dimension.equals(level.dimension())) {
                continue;
            }
            if (!deposit.conversionDone) {
                budget = runConversion(level, deposit, budget);
                continue;
            }
            if (now - deposit.lastRegenTick < deposit.regenIntervalTicks()) {
                continue;
            }
            runRegen(level, deposit, EconomyConfig.ORES_PER_REGEN.get());
            OreDepositSavedData.get(level).update(deposit.withLastRegenTick(now));
        }
    }

    private int runConversion(ServerLevel level, OreDeposit deposit, int budget) {
        int total = deposit.volumeBlockCount();
        if (deposit.conversionIndex >= total) {
            finishConversion(level, deposit);
            return budget;
        }

        int diameter = deposit.blockRadius * 2 + 1;
        int minY = deposit.minY();
        Set<Long> positions = new HashSet<>(deposit.veinPositions);
        RandomSource random = RandomSource.create(deposit.id.getMostSignificantBits() ^ deposit.id.getLeastSignificantBits() ^ level.getSeed());

        while (budget > 0 && deposit.conversionIndex < total) {
            BlockPos pos = indexToPos(deposit, deposit.conversionIndex, diameter, minY);
            deposit.conversionIndex++;
            budget--;

            if (!deposit.containsBlock(pos, level.dimension())) {
                continue;
            }

            BlockState state = level.getBlockState(pos);
            long packed = pos.asLong();
            if (isOreBlock(state)) {
                level.setBlock(pos, pickOreState(deposit, pos.getY()), Block.UPDATE_ALL);
                positions.add(packed);
                continue;
            }

            if (!isReplaceableStone(state)) {
                continue;
            }

            float chance = rollChance(pos, random);
            if (chance < deposit.replacePercent / 100.0f) {
                level.setBlock(pos, pickOreState(deposit, pos.getY()), Block.UPDATE_ALL);
                positions.add(packed);
            }
        }

        OreDeposit updated = deposit.withVeinPositions(positions).withConversionIndex(deposit.conversionIndex);
        if (updated.conversionIndex >= total) {
            updated = updated.withConversionDone(true);
            CannonEconomy.LOGGER.info(
                    "Ore deposit {} ready: {} vein blocks in r={} depth={} @ {}",
                    updated.id, updated.veinPositions.size(), updated.blockRadius, updated.depth, updated.center);
        }
        OreDepositSavedData.get(level).update(updated);
        return budget;
    }

    private static void finishConversion(ServerLevel level, OreDeposit deposit) {
        OreDeposit done = deposit.withConversionDone(true);
        OreDepositSavedData.get(level).update(done);
        CannonEconomy.LOGGER.info(
                "Ore deposit {} ready: {} vein blocks @ {}",
                done.id, done.veinPositions.size(), done.center);
    }

    private static BlockPos indexToPos(OreDeposit deposit, int index, int diameter, int minY) {
        int layerVolume = diameter * diameter;
        int yOffset = index / layerVolume;
        int remainder = index % layerVolume;
        int xOffset = remainder % diameter;
        int zOffset = remainder / diameter;
        int x = deposit.center.getX() - deposit.blockRadius + xOffset;
        int z = deposit.center.getZ() - deposit.blockRadius + zOffset;
        int y = minY + yOffset;
        return new BlockPos(x, y, z);
    }

    private static float rollChance(BlockPos pos, RandomSource seed) {
        RandomSource random = RandomSource.create(seed.nextLong() ^ pos.asLong());
        return random.nextFloat();
    }

    private void runRegen(ServerLevel level, OreDeposit deposit, int maxPlacements) {
        if (deposit.veinPositions.isEmpty()) {
            return;
        }
        long now = level.getGameTime();
        List<Long> candidates = new ArrayList<>();
        for (long packed : deposit.veinPositions) {
            BlockPos pos = BlockPos.of(packed);
            if (!deposit.containsBlock(pos, level.dimension())) {
                continue;
            }
            BlockState state = level.getBlockState(pos);
            if (isTargetOre(state, deposit)) {
                continue;
            }
            if (!isReplaceableStone(state)) {
                continue;
            }
            Long minedAt = MINED_AT.get(packed);
            if (minedAt != null && now - minedAt < deposit.regenIntervalTicks()) {
                continue;
            }
            candidates.add(packed);
        }

        if (candidates.isEmpty()) {
            return;
        }

        RandomSource random = level.getRandom();
        int placed = 0;
        int attempts = Math.min(maxPlacements * 4, candidates.size());
        while (placed < maxPlacements && attempts-- > 0) {
            long packed = candidates.get(random.nextInt(candidates.size()));
            BlockPos pos = BlockPos.of(packed);
            BlockState state = level.getBlockState(pos);
            if (isTargetOre(state, deposit) || !isReplaceableStone(state)) {
                continue;
            }
            level.setBlock(pos, pickOreState(deposit, pos.getY()), Block.UPDATE_ALL);
            MINED_AT.remove(packed);
            placed++;
        }
    }

    private static BlockState pickOreState(OreDeposit deposit, int y) {
        ResourceLocation id = y < 0 ? deposit.deepslateOreBlockId : deposit.oreBlockId;
        Block block = BuiltInRegistries.BLOCK.get(id);
        if (block == null || block.defaultBlockState().isAir()) {
            block = BuiltInRegistries.BLOCK.get(deposit.oreBlockId);
        }
        return block.defaultBlockState();
    }

    private static boolean isTargetOre(BlockState state, OreDeposit deposit) {
        Block block = state.getBlock();
        ResourceLocation id = BuiltInRegistries.BLOCK.getKey(block);
        return id.equals(deposit.oreBlockId) || id.equals(deposit.deepslateOreBlockId);
    }

    private static boolean isOreBlock(BlockState state) {
        return state.is(BlockTags.COAL_ORES) || state.is(BlockTags.IRON_ORES) || state.is(BlockTags.GOLD_ORES)
                || state.is(BlockTags.COPPER_ORES) || state.is(BlockTags.DIAMOND_ORES) || state.is(BlockTags.EMERALD_ORES)
                || state.is(BlockTags.LAPIS_ORES) || state.is(BlockTags.REDSTONE_ORES)
                || BuiltInRegistries.BLOCK.getKey(state.getBlock()).getPath().contains("_ore");
    }

    private static boolean isReplaceableStone(BlockState state) {
        return state.is(BlockTags.BASE_STONE_OVERWORLD) || state.is(BlockTags.DEEPSLATE_ORE_REPLACEABLES)
                || state.is(BlockTags.STONE_ORE_REPLACEABLES);
    }

    private static void purgeMined(long now) {
        Iterator<Map.Entry<Long, Long>> it = MINED_AT.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Long, Long> entry = it.next();
            if (now - entry.getValue() > 24000L * 60L) {
                it.remove();
            }
        }
    }
}
