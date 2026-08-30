package com.cannon.territorybridge.network;

import com.cannon.territorybridge.CannonTerritoryBridge;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public final class SiegeForceNetworking {
    private static final String PROTOCOL = "1";

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(CannonTerritoryBridge.MOD_ID, "siege_forces"),
            () -> PROTOCOL,
            PROTOCOL::equals,
            PROTOCOL::equals
    );

    private SiegeForceNetworking() {}

    public static void register() {
        CHANNEL.registerMessage(
                0,
                SiegeForcesPacket.class,
                SiegeForcesPacket::encode,
                SiegeForcesPacket::decode,
                SiegeForcesPacket::handle
        );
    }
}
