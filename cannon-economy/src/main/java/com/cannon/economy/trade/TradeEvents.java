package com.cannon.economy.trade;

import com.cannon.economy.CannonEconomy;
import com.cannon.economy.config.EconomyConfig;
import com.cannon.economy.recruits.RecruitsTradeBridge;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

public class TradeEvents {
    private static final List<ActiveCaravan> ACTIVE = new ArrayList<>();

    @SubscribeEvent
    public void onLevelTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.level.isClientSide()) {
            return;
        }
        ServerLevel level = (ServerLevel) event.level;
        TradeRouteSavedData data = TradeRouteSavedData.get(level);

        for (TradeRoute route : data.all()) {
            if (!route.dimension.equals(level.dimension())) {
                continue;
            }
            int ticks = route.ticksUntilNext - 1;
            if (ticks <= 0) {
                trySpawnCaravan(level, route);
                ticks = route.intervalTicks;
            }
            data.updateRoute(route.withTicksUntilNext(ticks));
        }

        tickCaravans(level);
    }

    private void trySpawnCaravan(ServerLevel level, TradeRoute route) {
        if (!(level.getBlockEntity(route.from) instanceof TradePostBlockEntity fromPost)) {
            return;
        }
        if (!(level.getBlockEntity(route.to) instanceof TradePostBlockEntity toPost)) {
            return;
        }

        ItemStack cargo = route.cargo.copy();
        if (cargo.isEmpty()) {
            return;
        }

        if (!fromPost.tryExtract(cargo)) {
            return;
        }

        if (EconomyConfig.CHECK_RECRUITS_EMBARGO.get() && route.ownerFaction != null) {
            if (RecruitsTradeBridge.isRouteBlocked(level, route)) {
                fromPost.insert(cargo);
                broadcast(level, route.from, Component.translatable("message.cannon_economy.trade_embargo", route.name));
                return;
            }
        }

        ACTIVE.add(new ActiveCaravan(route.id, route.from, route.to, cargo, route.tariffPercent, false));
        broadcast(level, route.from, Component.translatable("message.cannon_economy.caravan_departed", route.name));
    }

    private void tickCaravans(ServerLevel level) {
        int speed = EconomyConfig.CARAVAN_SPEED_BLOCKS_PER_TICK.get();
        Iterator<ActiveCaravan> it = ACTIVE.iterator();
        while (it.hasNext()) {
            ActiveCaravan caravan = it.next();
            BlockPos target = caravan.returning ? caravan.from : caravan.to;
            BlockPos current = caravan.position;

            int dx = Integer.compare(target.getX(), current.getX());
            int dy = Integer.compare(target.getY(), current.getY());
            int dz = Integer.compare(target.getZ(), current.getZ());

            for (int step = 0; step < speed; step++) {
                if (dx == 0 && dy == 0 && dz == 0) {
                    break;
                }
                int nx = current.getX() + (dx != 0 ? dx : 0);
                int ny = current.getY() + (dy != 0 ? dy : 0);
                int nz = current.getZ() + (dz != 0 ? dz : 0);
                current = new BlockPos(nx, ny, nz);
                caravan.position = current;

                if (EconomyConfig.CHECK_RECRUITS_EMBARGO.get()) {
                    if (RecruitsTradeBridge.isChunkEmbargoed(level, current)) {
                        caravan.returning = true;
                        broadcast(level, current, Component.translatable("message.cannon_economy.caravan_turned_back"));
                        break;
                    }
                }

                if (current.equals(target)) {
                    break;
                }
            }

            if (caravan.position.equals(caravan.to) && !caravan.returning) {
                deliver(level, caravan);
                caravan.returning = true;
                caravan.position = caravan.to;
            } else if (caravan.returning && caravan.position.equals(caravan.from)) {
                if (!caravan.cargo.isEmpty()) {
                    BlockEntity be = level.getBlockEntity(caravan.from);
                    if (be instanceof TradePostBlockEntity post) {
                        post.insert(caravan.cargo);
                    }
                }
                it.remove();
            }
        }
    }

    private void deliver(ServerLevel level, ActiveCaravan caravan) {
        BlockEntity be = level.getBlockEntity(caravan.to);
        if (!(be instanceof TradePostBlockEntity post)) {
            return;
        }
        ItemStack cargo = caravan.cargo.copy();
        if (caravan.tariffPercent > 0) {
            int toll = Math.max(1, cargo.getCount() * caravan.tariffPercent / 100);
            cargo.shrink(toll);
        }
        post.insert(cargo);
        caravan.cargo = ItemStack.EMPTY;
        broadcast(level, caravan.to, Component.translatable("message.cannon_economy.caravan_arrived"));
    }

    private static void broadcast(ServerLevel level, BlockPos pos, Component message) {
        for (ServerPlayer player : level.players()) {
            if (player.blockPosition().distSqr(pos) < 4096) {
                player.sendSystemMessage(message);
            }
        }
    }

    private static final class ActiveCaravan {
        final UUID routeId;
        final BlockPos from;
        final BlockPos to;
        ItemStack cargo;
        final int tariffPercent;
        BlockPos position;
        boolean returning;

        ActiveCaravan(UUID routeId, BlockPos from, BlockPos to, ItemStack cargo, int tariffPercent, boolean returning) {
            this.routeId = routeId;
            this.from = from;
            this.to = to;
            this.cargo = cargo;
            this.tariffPercent = tariffPercent;
            this.position = from;
            this.returning = returning;
        }
    }
}
