package com.cannon.territorybridge.server;

import com.cannon.territorybridge.CannonTerritoryBridge;
import net.minecraft.server.level.ServerPlayer;
import ydmsama.hundred_years_war.main.entity.entities.BaseCombatEntity;

import java.lang.reflect.Method;
import java.util.UUID;

/** Resolves HYW unit owner UUID across 0.3.x (getOwnerUUID) and 0.7.x (m_21805_) APIs. */
public final class HywEntityAccess {
    private static final Method OWNER_UUID_METHOD = resolveOwnerUuidMethod();

    private HywEntityAccess() {}

    public static UUID getOwnerUuid(BaseCombatEntity entity) {
        if (entity == null) {
            return null;
        }
        if (OWNER_UUID_METHOD != null) {
            try {
                Object value = OWNER_UUID_METHOD.invoke(entity);
                if (value instanceof UUID uuid) {
                    return uuid;
                }
            } catch (ReflectiveOperationException t) {
                CannonTerritoryBridge.LOGGER.debug(
                        "HYW owner UUID lookup via reflection failed for {}: {}",
                        entity.getType(),
                        t.toString()
                );
            }
        }
        ServerPlayer owner = entity.getOwner();
        return owner != null ? owner.getUUID() : null;
    }

    private static Method resolveOwnerUuidMethod() {
        for (String methodName : new String[] {"getOwnerUUID", "m_21805_"}) {
            try {
                return BaseCombatEntity.class.getMethod(methodName);
            } catch (NoSuchMethodException ignored) {
                // try next name
            }
        }
        CannonTerritoryBridge.LOGGER.warn(
                "Could not find HYW BaseCombatEntity owner UUID accessor; falling back to getOwner()"
        );
        return null;
    }
}
