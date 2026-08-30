package com.cannon.territorybridge.client;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class SiegeForceClientCache {
    private static final Map<UUID, ForceSnapshot> SNAPSHOTS = new ConcurrentHashMap<>();

    private SiegeForceClientCache() {}

    public static void update(UUID claimId, int attackers, int defenders) {
        if (claimId == null) {
            return;
        }
        if (attackers <= 0 && defenders <= 0) {
            SNAPSHOTS.remove(claimId);
            return;
        }
        SNAPSHOTS.put(claimId, new ForceSnapshot(attackers, defenders));
    }

    public static void clear(UUID claimId) {
        if (claimId != null) {
            SNAPSHOTS.remove(claimId);
        }
    }

    public static int attackers(UUID claimId) {
        ForceSnapshot snapshot = SNAPSHOTS.get(claimId);
        return snapshot != null ? snapshot.attackers : -1;
    }

    public static int defenders(UUID claimId) {
        ForceSnapshot snapshot = SNAPSHOTS.get(claimId);
        return snapshot != null ? snapshot.defenders : -1;
    }

    private record ForceSnapshot(int attackers, int defenders) {}
}
