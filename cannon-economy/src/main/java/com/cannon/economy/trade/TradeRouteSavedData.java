package com.cannon.economy.trade;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class TradeRouteSavedData extends SavedData {
    private static final String DATA_NAME = "cannon_economy_routes";
    private final List<TradeRoute> routes = new ArrayList<>();

    public static TradeRouteSavedData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(
                TradeRouteSavedData::load,
                TradeRouteSavedData::new,
                DATA_NAME);
    }

    public TradeRouteSavedData() {}

    public static TradeRouteSavedData load(CompoundTag tag) {
        TradeRouteSavedData data = new TradeRouteSavedData();
        ListTag list = tag.getList("Routes", Tag.TAG_COMPOUND);
        for (Tag entry : list) {
            data.routes.add(TradeRoute.load((CompoundTag) entry));
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag list = new ListTag();
        for (TradeRoute route : routes) {
            list.add(route.save());
        }
        tag.put("Routes", list);
        return tag;
    }

    public TradeRoute add(TradeRoute route) {
        routes.add(route);
        setDirty();
        return route;
    }

    public boolean remove(UUID id) {
        boolean removed = routes.removeIf(r -> r.id.equals(id));
        if (removed) {
            setDirty();
        }
        return removed;
    }

    public List<TradeRoute> all() {
        return List.copyOf(routes);
    }

    public void updateRoute(TradeRoute updated) {
        for (int i = 0; i < routes.size(); i++) {
            if (routes.get(i).id.equals(updated.id)) {
                routes.set(i, updated);
                setDirty();
                return;
            }
        }
    }
}
