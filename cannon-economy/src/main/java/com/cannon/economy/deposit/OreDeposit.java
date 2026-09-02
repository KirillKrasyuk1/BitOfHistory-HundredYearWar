package com.cannon.economy.deposit;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.LongArrayTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public final class OreDeposit {
    public final UUID id;
    public final BlockPos center;
    public final ResourceKey<Level> dimension;
    public final ResourceLocation oreBlockId;
    public final ResourceLocation deepslateOreBlockId;
    /** Horizontal radius of the vein in blocks (XZ). */
    public final int blockRadius;
    /** Chance (0–100) to replace stone with ore during initial vein layout. */
    public final int replacePercent;
    /** Vertical thickness of the vein, centered on {@link #center} Y. */
    public final int depth;
    /** Seconds between regeneration attempts at fixed vein positions. */
    public final int regenIntervalSeconds;
    public final String label;
    public final Set<Long> veinPositions;
    public boolean conversionDone;
    /** Linear scan index through the deposit volume during conversion. */
    public int conversionIndex;
    public long lastRegenTick;

    public OreDeposit(
            UUID id,
            BlockPos center,
            ResourceKey<Level> dimension,
            ResourceLocation oreBlockId,
            ResourceLocation deepslateOreBlockId,
            int blockRadius,
            int replacePercent,
            int depth,
            int regenIntervalSeconds,
            String label,
            Set<Long> veinPositions,
            boolean conversionDone,
            int conversionIndex,
            long lastRegenTick) {
        this.id = id;
        this.center = center;
        this.dimension = dimension;
        this.oreBlockId = oreBlockId;
        this.deepslateOreBlockId = deepslateOreBlockId;
        this.blockRadius = blockRadius;
        this.replacePercent = replacePercent;
        this.depth = depth;
        this.regenIntervalSeconds = regenIntervalSeconds;
        this.label = label == null ? "" : label;
        this.veinPositions = veinPositions == null ? new HashSet<>() : veinPositions;
        this.conversionDone = conversionDone;
        this.conversionIndex = conversionIndex;
        this.lastRegenTick = lastRegenTick;
    }

    public int minY() {
        return center.getY() - depth / 2;
    }

    public int maxY() {
        return center.getY() + (depth - 1) / 2;
    }

    public int regenIntervalTicks() {
        return Math.max(20, regenIntervalSeconds * 20);
    }

    public int volumeBlockCount() {
        int diameter = blockRadius * 2 + 1;
        return diameter * diameter * depth;
    }

    public boolean containsBlock(BlockPos pos, ResourceKey<Level> dim) {
        if (!dim.equals(dimension)) {
            return false;
        }
        int dx = pos.getX() - center.getX();
        int dz = pos.getZ() - center.getZ();
        if (dx * dx + dz * dz > blockRadius * blockRadius) {
            return false;
        }
        return pos.getY() >= minY() && pos.getY() <= maxY();
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putUUID("Id", id);
        tag.putLong("X", center.getX());
        tag.putLong("Y", center.getY());
        tag.putLong("Z", center.getZ());
        tag.putString("Dimension", dimension.location().toString());
        tag.putString("Ore", oreBlockId.toString());
        tag.putString("DeepslateOre", deepslateOreBlockId.toString());
        tag.putInt("BlockRadius", blockRadius);
        tag.putInt("ReplacePercent", replacePercent);
        tag.putInt("Depth", depth);
        tag.putInt("RegenIntervalSeconds", regenIntervalSeconds);
        tag.putString("Label", label);
        tag.putBoolean("ConversionDone", conversionDone);
        tag.putInt("ConversionIndex", conversionIndex);
        tag.putLong("LastRegenTick", lastRegenTick);
        long[] packed = veinPositions.stream().mapToLong(Long::longValue).toArray();
        tag.put("VeinPositions", new LongArrayTag(packed));
        return tag;
    }

    public static OreDeposit load(CompoundTag tag) {
        ResourceKey<Level> dim = ResourceKey.create(
                net.minecraft.core.registries.Registries.DIMENSION,
                ResourceLocation.tryParse(tag.getString("Dimension")));

        Set<Long> positions = new HashSet<>();
        if (tag.contains("VeinPositions", net.minecraft.nbt.Tag.TAG_LONG_ARRAY)) {
            for (long value : tag.getLongArray("VeinPositions")) {
                positions.add(value);
            }
        }

        int blockRadius = tag.contains("BlockRadius")
                ? tag.getInt("BlockRadius")
                : tag.contains("ChunkRadius")
                ? Math.max(8, tag.getInt("ChunkRadius") * 16)
                : 24;
        int replacePercent = tag.contains("ReplacePercent") ? tag.getInt("ReplacePercent") : 3;
        int depth = tag.contains("Depth") ? tag.getInt("Depth") : 16;
        int regenSeconds = tag.contains("RegenIntervalSeconds")
                ? tag.getInt("RegenIntervalSeconds")
                : 300;

        return new OreDeposit(
                tag.getUUID("Id"),
                new BlockPos(tag.getInt("X"), tag.getInt("Y"), tag.getInt("Z")),
                dim,
                ResourceLocation.tryParse(tag.getString("Ore")),
                ResourceLocation.tryParse(tag.getString("DeepslateOre")),
                blockRadius,
                replacePercent,
                depth,
                regenSeconds,
                tag.getString("Label"),
                positions,
                tag.getBoolean("ConversionDone"),
                tag.contains("ConversionIndex") ? tag.getInt("ConversionIndex") : 0,
                tag.getLong("LastRegenTick"));
    }

    public OreDeposit withConversionDone(boolean done) {
        return copy(done, conversionIndex, lastRegenTick, veinPositions);
    }

    public OreDeposit withConversionIndex(int index) {
        return copy(conversionDone, index, lastRegenTick, veinPositions);
    }

    public OreDeposit withLastRegenTick(long tick) {
        return copy(conversionDone, conversionIndex, tick, veinPositions);
    }

    public OreDeposit withVeinPositions(Set<Long> positions) {
        return copy(conversionDone, conversionIndex, lastRegenTick, positions);
    }

    private OreDeposit copy(boolean done, int index, long regenTick, Set<Long> positions) {
        return new OreDeposit(
                id, center, dimension, oreBlockId, deepslateOreBlockId,
                blockRadius, replacePercent, depth, regenIntervalSeconds, label,
                positions, done, index, regenTick);
    }
}
