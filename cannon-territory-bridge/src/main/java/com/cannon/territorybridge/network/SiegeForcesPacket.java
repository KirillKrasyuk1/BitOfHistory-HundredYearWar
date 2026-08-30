package com.cannon.territorybridge.network;

import com.cannon.territorybridge.client.SiegeForceClientCache;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

public record SiegeForcesPacket(UUID claimId, int attackers, int defenders) {
    public static void encode(SiegeForcesPacket packet, FriendlyByteBuf buf) {
        buf.writeUUID(packet.claimId);
        buf.writeVarInt(packet.attackers);
        buf.writeVarInt(packet.defenders);
    }

    public static SiegeForcesPacket decode(FriendlyByteBuf buf) {
        return new SiegeForcesPacket(buf.readUUID(), buf.readVarInt(), buf.readVarInt());
    }

    public static void handle(SiegeForcesPacket packet, Supplier<NetworkEvent.Context> context) {
        NetworkEvent.Context ctx = context.get();
        ctx.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(
                Dist.CLIENT,
                () -> () -> SiegeForceClientCache.update(packet.claimId, packet.attackers, packet.defenders)
        ));
        ctx.setPacketHandled(true);
    }
}
