package com.cannon.economy.deposit;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

import java.util.UUID;

public final class ResourceDeposit {
    public final UUID id;
    public final BlockPos center;
    public final ResourceKey<Level> dimension;
    public final DepositType type;
    public final int radius;
    public final String label;

    public ResourceDeposit(UUID id, BlockPos center, ResourceKey<Level> dimension, DepositType type, int radius, String label) {
        this.id = id;
        this.center = center;
        this.dimension = dimension;
        this.type = type;
        this.radius = radius;
        this.label = label == null ? "" : label;
    }

    public boolean contains(BlockPos pos, ResourceKey<Level> dim) {
        if (!dim.equals(dimension)) {
            return false;
        }
        return center.distSqr(pos) <= (long) radius * radius;
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putUUID("Id", id);
        tag.putLong("X", center.getX());
        tag.putLong("Y", center.getY());
        tag.putLong("Z", center.getZ());
        tag.putString("Dimension", dimension.location().toString());
        tag.putString("Type", type.getSerializedName());
        tag.putInt("Radius", radius);
        tag.putString("Label", label);
        return tag;
    }

    public static ResourceDeposit load(CompoundTag tag) {
        UUID id = tag.getUUID("Id");
        BlockPos center = new BlockPos(tag.getInt("X"), tag.getInt("Y"), tag.getInt("Z"));
        ResourceKey<Level> dim = ResourceKey.create(
                net.minecraft.core.registries.Registries.DIMENSION,
                new ResourceLocation(tag.getString("Dimension")));
        DepositType type = DepositType.byId(tag.getString("Type"));
        int radius = tag.getInt("Radius");
        String label = tag.contains("Label") ? tag.getString("Label") : "";
        return new ResourceDeposit(id, center, dim, type, radius, label);
    }
}
