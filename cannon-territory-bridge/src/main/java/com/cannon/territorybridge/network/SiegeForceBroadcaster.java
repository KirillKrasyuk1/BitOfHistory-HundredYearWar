package com.cannon.territorybridge.network;

import com.cannon.territorybridge.bridge.BridgeClaimHelper;
import com.talhanation.recruits.world.RecruitsClaim;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.PacketDistributor;

import java.util.UUID;

public final class SiegeForceBroadcaster {
    private SiegeForceBroadcaster() {}

    public static void syncClaim(MinecraftServer server, RecruitsClaim claim) {
        if (server == null || claim == null || !claim.isUnderSiege) {
            return;
        }
        SiegeForcesPacket packet = new SiegeForcesPacket(
                claim.getUUID(),
                BridgeClaimHelper.attackerCount(claim),
                BridgeClaimHelper.defenderCount(claim),
                claim.getHealth(),
                claim.getMaxHealth(),
                claim.getSiegeSpeedPercent()
        );
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            SiegeForceNetworking.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
        }
    }

    public static void syncToAll(MinecraftServer server, UUID claimId, int attackers, int defenders) {
        if (server == null || claimId == null) {
            return;
        }
        RecruitsClaim claim = findClaim(server, claimId);
        if (claim != null) {
            syncClaim(server, claim);
        } else {
            SiegeForcesPacket packet = new SiegeForcesPacket(claimId, attackers, defenders, 0, 0, 0.0f);
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                SiegeForceNetworking.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
            }
        }
    }

    public static void clearOnAll(MinecraftServer server, UUID claimId) {
        if (server == null || claimId == null) {
            return;
        }
        SiegeForcesPacket packet = new SiegeForcesPacket(claimId, 0, 0, 0, 0, 0.0f);
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            SiegeForceNetworking.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
        }
    }

    private static RecruitsClaim findClaim(MinecraftServer server, UUID claimId) {
        if (com.talhanation.recruits.ClaimEvents.recruitsClaimManager == null) {
            return null;
        }
        return com.talhanation.recruits.ClaimEvents.recruitsClaimManager.getClaim(claimId);
    }
}
