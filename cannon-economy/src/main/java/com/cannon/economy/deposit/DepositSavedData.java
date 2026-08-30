package com.cannon.economy.deposit;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class DepositSavedData extends SavedData {
    private static final String DATA_NAME = "cannon_economy_deposits";
    private final List<ResourceDeposit> deposits = new ArrayList<>();

    public static DepositSavedData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(
                DepositSavedData::load,
                DepositSavedData::new,
                DATA_NAME);
    }

    public DepositSavedData() {}

    public static DepositSavedData load(CompoundTag tag) {
        DepositSavedData data = new DepositSavedData();
        ListTag list = tag.getList("Deposits", Tag.TAG_COMPOUND);
        for (Tag entry : list) {
            data.deposits.add(ResourceDeposit.load((CompoundTag) entry));
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag list = new ListTag();
        for (ResourceDeposit deposit : deposits) {
            list.add(deposit.save());
        }
        tag.put("Deposits", list);
        return tag;
    }

    public ResourceDeposit add(BlockPos center, ResourceKey<Level> dimension, DepositType type, int radius, String label) {
        ResourceDeposit deposit = new ResourceDeposit(UUID.randomUUID(), center, dimension, type, radius, label);
        deposits.add(deposit);
        setDirty();
        return deposit;
    }

    public boolean remove(UUID id) {
        boolean removed = deposits.removeIf(d -> d.id.equals(id));
        if (removed) {
            setDirty();
        }
        return removed;
    }

    public List<ResourceDeposit> all() {
        return List.copyOf(deposits);
    }

    public Optional<ResourceDeposit> at(BlockPos pos, ResourceKey<Level> dimension) {
        return deposits.stream()
                .filter(d -> d.contains(pos, dimension))
                .findFirst();
    }

    public Optional<ResourceDeposit> ofTypeAt(BlockPos pos, ResourceKey<Level> dimension, DepositType type) {
        return deposits.stream()
                .filter(d -> d.type == type && d.contains(pos, dimension))
                .findFirst();
    }
}
