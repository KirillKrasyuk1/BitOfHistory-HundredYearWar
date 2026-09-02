package com.cannon.economy.deposit;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class PendingOreRegen {
    public final UUID depositId;
    public final BlockPos pos;
    public final long regenAtTick;

    public PendingOreRegen(UUID depositId, BlockPos pos, long regenAtTick) {
        this.depositId = depositId;
        this.pos = pos;
        this.regenAtTick = regenAtTick;
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putUUID("DepositId", depositId);
        tag.putLong("X", pos.getX());
        tag.putLong("Y", pos.getY());
        tag.putLong("Z", pos.getZ());
        tag.putLong("RegenAt", regenAtTick);
        return tag;
    }

    public static PendingOreRegen load(CompoundTag tag) {
        return new PendingOreRegen(
                tag.getUUID("DepositId"),
                new BlockPos(tag.getInt("X"), tag.getInt("Y"), tag.getInt("Z")),
                tag.getLong("RegenAt"));
    }

    public static ListTag saveAll(List<PendingOreRegen> entries) {
        ListTag list = new ListTag();
        for (PendingOreRegen entry : entries) {
            list.add(entry.save());
        }
        return list;
    }

    public static List<PendingOreRegen> loadAll(ListTag list) {
        List<PendingOreRegen> entries = new ArrayList<>();
        for (Tag tag : list) {
            entries.add(load((CompoundTag) tag));
        }
        return entries;
    }
}
