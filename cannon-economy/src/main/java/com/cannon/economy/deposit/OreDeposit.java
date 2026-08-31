package com.cannon.economy.deposit;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

import java.util.UUID;

public final class OreDeposit {
    public final UUID id;
    public final BlockPos center;
    public final ResourceKey<Level> dimension;
    public final ResourceLocation oreBlockId;
    public final ResourceLocation deepslateOreBlockId;
    public final int chunkRadius;
    public final String label;
    public boolean conversionDone;
    public int conversionChunkIndex;
    public long lastRegenTick;

    public OreDeposit(UUID id, BlockPos center, ResourceKey<Level> dimension,
                      ResourceLocation oreBlockId, ResourceLocation deepslateOreBlockId,
                      int chunkRadius, String label, boolean conversionDone, int conversionChunkIndex, long lastRegenTick) {
        this.id = id;
        this.center = center;
        this.dimension = dimension;
        this.oreBlockId = oreBlockId;
        this.deepslateOreBlockId = deepslateOreBlockId;
        this.chunkRadius = chunkRadius;
        this.label = label == null ? "" : label;
        this.conversionDone = conversionDone;
        this.conversionChunkIndex = conversionChunkIndex;
        this.lastRegenTick = lastRegenTick;
    }

    public boolean containsChunk(int chunkX, int chunkZ, ResourceKey<Level> dim) {
        if (!dim.equals(dimension)) {
            return false;
        }
        int cx = center.getX() >> 4;
        int cz = center.getZ() >> 4;
        return Math.abs(chunkX - cx) <= chunkRadius && Math.abs(chunkZ - cz) <= chunkRadius;
    }

    public boolean containsBlock(BlockPos pos, ResourceKey<Level> dim) {
        return containsChunk(pos.getX() >> 4, pos.getZ() >> 4, dim);
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
        tag.putInt("ChunkRadius", chunkRadius);
        tag.putString("Label", label);
        tag.putBoolean("ConversionDone", conversionDone);
        tag.putInt("ConversionChunkIndex", conversionChunkIndex);
        tag.putLong("LastRegenTick", lastRegenTick);
        return tag;
    }

    public static OreDeposit load(CompoundTag tag) {
        ResourceKey<Level> dim = ResourceKey.create(
                net.minecraft.core.registries.Registries.DIMENSION,
                ResourceLocation.tryParse(tag.getString("Dimension")));
        return new OreDeposit(
                tag.getUUID("Id"),
                new BlockPos(tag.getInt("X"), tag.getInt("Y"), tag.getInt("Z")),
                dim,
                ResourceLocation.tryParse(tag.getString("Ore")),
                ResourceLocation.tryParse(tag.getString("DeepslateOre")),
                tag.getInt("ChunkRadius"),
                tag.getString("Label"),
                tag.getBoolean("ConversionDone"),
                tag.contains("ConversionChunkIndex") ? tag.getInt("ConversionChunkIndex") : 0,
                tag.getLong("LastRegenTick"));
    }

    public OreDeposit withConversionDone(boolean done) {
        return new OreDeposit(id, center, dimension, oreBlockId, deepslateOreBlockId, chunkRadius, label, done, conversionChunkIndex, lastRegenTick);
    }

    public OreDeposit withConversionChunkIndex(int index) {
        return new OreDeposit(id, center, dimension, oreBlockId, deepslateOreBlockId, chunkRadius, label, conversionDone, index, lastRegenTick);
    }

    public OreDeposit withLastRegenTick(long tick) {
        return new OreDeposit(id, center, dimension, oreBlockId, deepslateOreBlockId, chunkRadius, label, conversionDone, conversionChunkIndex, tick);
    }
}
