package com.cannon.territorybridge.network;

import com.cannon.territorybridge.client.SiegeForceClientCache;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

/** Syncs siege army counts and claim HP to clients (Recruits only updates players in-party). */
public record SiegeForcesPacket(
        UUID claimId,
        int attackers,
        int defenders,
        int health,
        int maxHealth,
        float speedPercent
) {
    public static void encode(SiegeForcesPacket packet, FriendlyByteBuf buf) {
        buf.writeUUID(packet.claimId);
        buf.writeVarInt(packet.attackers);
        buf.writeVarInt(packet.defenders);
        buf.writeVarInt(packet.health);
        buf.writeVarInt(packet.maxHealth);
        buf.writeFloat(packet.speedPercent);
    }

    public static SiegeForcesPacket decode(FriendlyByteBuf buf) {
        return new SiegeForcesPacket(
                buf.readUUID(),
                buf.readVarInt(),
                buf.readVarInt(),
                buf.readVarInt(),
                buf.readVarInt(),
                buf.readFloat()
        );
    }

    public static void handle(SiegeForcesPacket packet, Supplier<NetworkEvent.Context> context) {
        NetworkEvent.Context ctx = context.get();
        ctx.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(
                Dist.CLIENT,
                () -> () -> SiegeForceClientCache.update(
                        packet.claimId,
                        packet.attackers,
                        packet.defenders,
                        packet.health,
                        packet.maxHealth,
                        packet.speedPercent
                )
        ));
        ctx.setPacketHandled(true);
    }
}
