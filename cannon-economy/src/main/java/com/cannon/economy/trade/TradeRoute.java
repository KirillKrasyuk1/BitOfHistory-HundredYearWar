package com.cannon.economy.trade;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.UUID;

public final class TradeRoute {
    public final UUID id;
    public final String name;
    public final BlockPos from;
    public final BlockPos to;
    public final ResourceKey<Level> dimension;
    public final ItemStack cargo;
    public final int intervalTicks;
    public final int tariffPercent;
    public final UUID ownerFaction;
    public int ticksUntilNext;

    public TradeRoute(UUID id, String name, BlockPos from, BlockPos to, ResourceKey<Level> dimension,
                      ItemStack cargo, int intervalTicks, int tariffPercent, UUID ownerFaction, int ticksUntilNext) {
        this.id = id;
        this.name = name;
        this.from = from;
        this.to = to;
        this.dimension = dimension;
        this.cargo = cargo;
        this.intervalTicks = intervalTicks;
        this.tariffPercent = Math.max(0, Math.min(100, tariffPercent));
        this.ownerFaction = ownerFaction;
        this.ticksUntilNext = ticksUntilNext;
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putUUID("Id", id);
        tag.putString("Name", name);
        tag.putLong("FromX", from.getX());
        tag.putLong("FromY", from.getY());
        tag.putLong("FromZ", from.getZ());
        tag.putLong("ToX", to.getX());
        tag.putLong("ToY", to.getY());
        tag.putLong("ToZ", to.getZ());
        tag.putString("Dimension", dimension.location().toString());
        tag.put("Cargo", cargo.save(new CompoundTag()));
        tag.putInt("Interval", intervalTicks);
        tag.putInt("Tariff", tariffPercent);
        if (ownerFaction != null) {
            tag.putUUID("OwnerFaction", ownerFaction);
        }
        tag.putInt("TicksUntilNext", ticksUntilNext);
        return tag;
    }

    public static TradeRoute load(CompoundTag tag) {
        UUID id = tag.getUUID("Id");
        String name = tag.getString("Name");
        BlockPos from = new BlockPos(tag.getInt("FromX"), tag.getInt("FromY"), tag.getInt("FromZ"));
        BlockPos to = new BlockPos(tag.getInt("ToX"), tag.getInt("ToY"), tag.getInt("ToZ"));
        ResourceKey<Level> dim = ResourceKey.create(
                net.minecraft.core.registries.Registries.DIMENSION,
                new ResourceLocation(tag.getString("Dimension")));
        ItemStack cargo = ItemStack.of(tag.getCompound("Cargo"));
        int interval = tag.getInt("Interval");
        int tariff = tag.getInt("Tariff");
        UUID owner = tag.hasUUID("OwnerFaction") ? tag.getUUID("OwnerFaction") : null;
        int ticks = tag.contains("TicksUntilNext") ? tag.getInt("TicksUntilNext") : interval;
        return new TradeRoute(id, name, from, to, dim, cargo, interval, tariff, owner, ticks);
    }

    public TradeRoute withTicksUntilNext(int ticks) {
        return new TradeRoute(id, name, from, to, dimension, cargo, intervalTicks, tariffPercent, ownerFaction, ticks);
    }
}
