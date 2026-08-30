package com.cannon.territorybridge.client;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class SiegeForceClientCache {
    private static final Map<UUID, SiegeSnapshot> SNAPSHOTS = new ConcurrentHashMap<>();

    private SiegeForceClientCache() {}

    public static void update(
            UUID claimId,
            int attackers,
            int defenders,
            int health,
            int maxHealth,
            float speedPercent
    ) {
        if (claimId == null) {
            return;
        }
        if (attackers <= 0 && defenders <= 0 && health <= 0) {
            SNAPSHOTS.remove(claimId);
            return;
        }
        SNAPSHOTS.put(claimId, new SiegeSnapshot(attackers, defenders, health, maxHealth, speedPercent));
    }

    public static void clear(UUID claimId) {
        if (claimId != null) {
            SNAPSHOTS.remove(claimId);
        }
    }

    public static int attackers(UUID claimId) {
        SiegeSnapshot snapshot = SNAPSHOTS.get(claimId);
        return snapshot != null ? snapshot.attackers : -1;
    }

    public static int defenders(UUID claimId) {
        SiegeSnapshot snapshot = SNAPSHOTS.get(claimId);
        return snapshot != null ? snapshot.defenders : -1;
    }

    public static int health(UUID claimId) {
        SiegeSnapshot snapshot = SNAPSHOTS.get(claimId);
        return snapshot != null ? snapshot.health : -1;
    }

    public static int maxHealth(UUID claimId) {
        SiegeSnapshot snapshot = SNAPSHOTS.get(claimId);
        return snapshot != null ? snapshot.maxHealth : -1;
    }

    public static float speedPercent(UUID claimId) {
        SiegeSnapshot snapshot = SNAPSHOTS.get(claimId);
        return snapshot != null ? snapshot.speedPercent : -1.0f;
    }

    private record SiegeSnapshot(
            int attackers,
            int defenders,
            int health,
            int maxHealth,
            float speedPercent
    ) {}
}
