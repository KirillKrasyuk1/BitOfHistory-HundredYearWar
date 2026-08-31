package com.cannon.economy.deposit;

import com.cannon.economy.CannonEconomy;
import com.cannon.economy.config.EconomyConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class OreDepositEngine {
    private static final Map<Long, Long> MINED_COOLDOWN = new HashMap<>();

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
        BlockState state = event.getState();
        if (isTargetOre(state, deposit) || isReplaceableStone(state)) {
            long key = event.getPos().asLong();
            long expire = level.getGameTime() + EconomyConfig.MINED_COOLDOWN_TICKS.get();
            MINED_COOLDOWN.put(key, expire);
        }
    }

    @SubscribeEvent
    public void onLevelTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.level.isClientSide()) {
            return;
        }
        ServerLevel level = (ServerLevel) event.level;
        long now = level.getGameTime();
        purgeCooldown(now);

        int budget = EconomyConfig.CONVERT_BLOCKS_PER_TICK.get();
        int placed = 0;

        for (OreDeposit deposit : OreDepositSavedData.get(level).all()) {
            if (!deposit.dimension.equals(level.dimension())) {
                continue;
            }
            if (!deposit.conversionDone) {
                budget = runConversion(level, deposit, budget);
                continue;
            }
            if (now - deposit.lastRegenTick < EconomyConfig.REGEN_INTERVAL_TICKS.get()) {
                continue;
            }
            placed += runRegen(level, deposit, EconomyConfig.ORES_PER_REGEN.get());
            OreDepositSavedData.get(level).update(deposit.withLastRegenTick(now));
        }
    }

    private int runConversion(ServerLevel level, OreDeposit deposit, int budget) {
        int side = deposit.chunkRadius * 2 + 1;
        int totalChunks = side * side;
        if (deposit.conversionChunkIndex >= totalChunks) {
            OreDepositSavedData.get(level).update(deposit.withConversionDone(true));
            CannonEconomy.LOGGER.info("Ore deposit {} conversion complete at {}", deposit.id, deposit.center);
            return budget;
        }

        int centerCx = deposit.center.getX() >> 4;
        int centerCz = deposit.center.getZ() >> 4;
        int minY = EconomyConfig.ORE_MIN_Y.get();
        int maxY = EconomyConfig.ORE_MAX_Y.get();
        RandomSource random = RandomSource.create(deposit.id.getMostSignificantBits() ^ level.getSeed() ^ deposit.conversionChunkIndex);

        int idx = deposit.conversionChunkIndex;
        int dcx = (idx % side) - deposit.chunkRadius;
        int dcz = (idx / side) - deposit.chunkRadius;
        LevelChunk chunk = level.getChunk(centerCx + dcx, centerCz + dcz);

        for (int x = 0; x < 16 && budget > 0; x++) {
            for (int z = 0; z < 16 && budget > 0; z++) {
                for (int y = minY; y <= maxY && budget > 0; y++) {
                    budget--;
                    BlockPos pos = new BlockPos(chunk.getPos().getMinBlockX() + x, y, chunk.getPos().getMinBlockZ() + z);
                    BlockState state = level.getBlockState(pos);
                    if (isOreBlock(state) || (isReplaceableStone(state) && random.nextFloat() < 0.12f)) {
                        level.setBlock(pos, pickOreState(deposit, y), Block.UPDATE_ALL);
                    }
                }
            }
        }

        OreDeposit updated = deposit.withConversionChunkIndex(deposit.conversionChunkIndex + 1);
        if (updated.conversionChunkIndex >= totalChunks) {
            updated = updated.withConversionDone(true);
            CannonEconomy.LOGGER.info("Ore deposit {} conversion complete at {}", deposit.id, deposit.center);
        }
        OreDepositSavedData.get(level).update(updated);
        return budget;
    }

    private int runRegen(ServerLevel level, OreDeposit deposit, int maxPlacements) {
        int minY = EconomyConfig.ORE_MIN_Y.get();
        int maxY = EconomyConfig.ORE_MAX_Y.get();
        RandomSource random = level.getRandom();
        int placed = 0;
        int attempts = maxPlacements * 8;

        while (placed < maxPlacements && attempts-- > 0) {
            int cx = (deposit.center.getX() >> 4) + random.nextInt(deposit.chunkRadius * 2 + 1) - deposit.chunkRadius;
            int cz = (deposit.center.getZ() >> 4) + random.nextInt(deposit.chunkRadius * 2 + 1) - deposit.chunkRadius;
            if (!deposit.containsChunk(cx, cz, level.dimension())) {
                continue;
            }
            LevelChunk chunk = level.getChunk(cx, cz);
            int x = chunk.getPos().getMinBlockX() + random.nextInt(16);
            int z = chunk.getPos().getMinBlockZ() + random.nextInt(16);
            int y = minY + random.nextInt(Math.max(1, maxY - minY + 1));
            BlockPos pos = new BlockPos(x, y, z);
            if (isOnCooldown(level.getGameTime(), pos)) {
                continue;
            }
            BlockState state = level.getBlockState(pos);
            if (!isReplaceableStone(state)) {
                continue;
            }
            level.setBlock(pos, pickOreState(deposit, y), Block.UPDATE_ALL);
            placed++;
        }
        return placed;
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

    private static boolean isOnCooldown(long now, BlockPos pos) {
        Long expire = MINED_COOLDOWN.get(pos.asLong());
        return expire != null && expire > now;
    }

    private static void purgeCooldown(long now) {
        Iterator<Map.Entry<Long, Long>> it = MINED_COOLDOWN.entrySet().iterator();
        while (it.hasNext()) {
            if (it.next().getValue() <= now) {
                it.remove();
            }
        }
    }
}
