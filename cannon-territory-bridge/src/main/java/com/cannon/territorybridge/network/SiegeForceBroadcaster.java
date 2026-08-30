package com.cannon.territorybridge.network;

import com.cannon.territorybridge.CannonTerritoryBridge;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.PacketDistributor;

import java.util.UUID;

public final class SiegeForceBroadcaster {
    private SiegeForceBroadcaster() {}

    public static void syncToAll(MinecraftServer server, UUID claimId, int attackers, int defenders) {
        if (server == null || claimId == null) {
            return;
        }
        SiegeForcesPacket packet = new SiegeForcesPacket(claimId, attackers, defenders);
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            SiegeForceNetworking.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
        }
    }

    public static void clearOnAll(MinecraftServer server, UUID claimId) {
        if (server == null || claimId == null) {
            return;
        }
        syncToAll(server, claimId, 0, 0);
    }
}
