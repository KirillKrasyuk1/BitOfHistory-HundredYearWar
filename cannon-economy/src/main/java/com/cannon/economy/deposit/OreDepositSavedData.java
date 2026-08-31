package com.cannon.economy.deposit;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class OreDepositSavedData extends SavedData {
    private static final String DATA_NAME = "cannon_economy_ore_deposits";
    private final List<OreDeposit> deposits = new ArrayList<>();

    public static OreDepositSavedData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(OreDepositSavedData::load, OreDepositSavedData::new, DATA_NAME);
    }

    public OreDepositSavedData() {}

    public static OreDepositSavedData load(CompoundTag tag) {
        OreDepositSavedData data = new OreDepositSavedData();
        ListTag list = tag.getList("Deposits", Tag.TAG_COMPOUND);
        for (Tag entry : list) {
            data.deposits.add(OreDeposit.load((CompoundTag) entry));
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag list = new ListTag();
        for (OreDeposit d : deposits) {
            list.add(d.save());
        }
        tag.put("Deposits", list);
        return tag;
    }

    public OreDeposit add(OreDeposit deposit) {
        deposits.add(deposit);
        setDirty();
        return deposit;
    }

    public void update(OreDeposit deposit) {
        for (int i = 0; i < deposits.size(); i++) {
            if (deposits.get(i).id.equals(deposit.id)) {
                deposits.set(i, deposit);
                setDirty();
                return;
            }
        }
    }

    public boolean remove(UUID id) {
        boolean removed = deposits.removeIf(d -> d.id.equals(id));
        if (removed) {
            setDirty();
        }
        return removed;
    }

    public List<OreDeposit> all() {
        return List.copyOf(deposits);
    }

    public Optional<OreDeposit> atBlock(net.minecraft.core.BlockPos pos, net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> dim) {
        return deposits.stream().filter(d -> d.containsBlock(pos, dim)).findFirst();
    }
}
