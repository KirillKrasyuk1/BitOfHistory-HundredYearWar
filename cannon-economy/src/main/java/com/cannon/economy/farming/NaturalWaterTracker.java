package com.cannon.economy.farming;

import com.cannon.economy.config.EconomyConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.LongArrayTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashSet;
import java.util.Set;

/**
 * Distinguishes world-generated water from buckets. Only natural sources count for fertility buffs.
 */
@Mod.EventBusSubscriber(modid = com.cannon.economy.CannonEconomy.MOD_ID)
public final class NaturalWaterTracker extends SavedData {
    private static final String DATA_NAME = "cannon_economy_player_water";
    private final Set<Long> playerPlacedWater = new HashSet<>();

    public static NaturalWaterTracker get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(NaturalWaterTracker::load, NaturalWaterTracker::new, DATA_NAME);
    }

    public NaturalWaterTracker() {}

    private static NaturalWaterTracker load(CompoundTag tag) {
        NaturalWaterTracker data = new NaturalWaterTracker();
        if (tag.contains("PlayerWater", Tag.TAG_LONG_ARRAY)) {
            for (long packed : tag.getLongArray("PlayerWater")) {
                data.playerPlacedWater.add(packed);
            }
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        tag.put("PlayerWater", new LongArrayTag(playerPlacedWater.stream().mapToLong(Long::longValue).toArray()));
        return tag;
    }

    public static boolean hasNaturalWaterNearby(ServerLevel level, BlockPos origin) {
        int radius = EconomyConfig.WATER_RADIUS.get();
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        NaturalWaterTracker tracker = get(level);
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (dx * dx + dz * dz > radius * radius) {
                        continue;
                    }
                    cursor.set(origin.getX() + dx, origin.getY() + dy, origin.getZ() + dz);
                    FluidState fluid = level.getFluidState(cursor);
                    if (fluid.is(FluidTags.WATER) && fluid.isSource()) {
                        if (!tracker.playerPlacedWater.contains(cursor.asLong())) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    public void markPlayerPlaced(BlockPos pos) {
        if (playerPlacedWater.add(pos.asLong())) {
            setDirty();
        }
    }

    public void unmark(BlockPos pos) {
        if (playerPlacedWater.remove(pos.asLong())) {
            setDirty();
        }
    }

    @SubscribeEvent
    public static void onBucketPlace(PlayerInteractEvent.RightClickBlock event) {
        if (event.getLevel().isClientSide() || event.getEntity() == null) {
            return;
        }
        if (!event.getItemStack().is(Items.WATER_BUCKET)) {
            return;
        }
        BlockPos placePos = event.getPos().relative(event.getFace());
        NaturalWaterTracker.get((ServerLevel) event.getLevel()).markPlayerPlaced(placePos);
    }

    @SubscribeEvent
    public static void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
        if (event.getLevel().isClientSide() || event.getEntity() == null) {
            return;
        }
        BlockState placed = event.getPlacedBlock();
        if (placed.is(Blocks.WATER)) {
            NaturalWaterTracker.get((ServerLevel) event.getLevel()).markPlayerPlaced(event.getPos());
        }
    }

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (event.getLevel().isClientSide()) {
            return;
        }
        ServerLevel level = (ServerLevel) event.getLevel();
        BlockPos pos = event.getPos();
        FluidState fluid = event.getState().getFluidState();
        if (fluid.is(FluidTags.WATER)) {
            NaturalWaterTracker.get(level).unmark(pos);
        }
    }
}
