package com.cannon.territorybridge.server;

import com.cannon.territorybridge.bridge.BridgeClaimAccess;
import com.cannon.territorybridge.config.BridgeConfig;
import com.cannon.territorybridge.network.SiegeForceBroadcaster;
import com.talhanation.recruits.ClaimEvents;
import com.talhanation.recruits.world.RecruitsClaim;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.AABB;
import ydmsama.hundred_years_war.main.entity.entities.BaseCombatEntity;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Keeps siege attacker/defender counts stable while NPC armies stay alive, even if they leave the claim
 * or the owning player dies. Home-position garrison on the claim is always counted for defenders/attackers.
 */
public final class ClaimSiegeTracker {
    private static final Map<UUID, Commitment> COMMITMENTS = new ConcurrentHashMap<>();

    private ClaimSiegeTracker() {}

    private static final int HOME_SCAN_MARGIN = 2048;

    public static void clear(RecruitsClaim claim) {
        if (claim != null) {
            COMMITMENTS.remove(claim.getUUID());
            resetBridgeCounts(claim);
            if (ClaimEvents.server != null) {
                SiegeForceBroadcaster.clearOnAll(ClaimEvents.server, claim.getUUID());
            }
        }
    }

    public static void supplementForcesInsideClaim(
            ServerLevel level,
            RecruitsClaim claim,
            List<LivingEntity> attackers,
            List<LivingEntity> defenders
    ) {
        if (claim == null || level == null || !claim.isUnderSiege) {
            return;
        }
        for (net.minecraft.world.level.ChunkPos chunk : claim.getClaimedChunks()) {
            int minX = chunk.getMinBlockX();
            int minZ = chunk.getMinBlockZ();
            AABB box = new AABB(
                    minX,
                    level.getMinBuildHeight(),
                    minZ,
                    chunk.getMaxBlockX() + 1,
                    level.getMaxBuildHeight(),
                    chunk.getMaxBlockZ() + 1
            );
            for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, box, Entity::isAlive)) {
                if (!SiegeForceFilter.countsForSiege(entity)) {
                    continue;
                }
                if (attackers.contains(entity) || defenders.contains(entity)) {
                    continue;
                }
                HywSiegeClassifier.Role role = HywSiegeClassifier.classify(entity, claim);
                if (role == HywSiegeClassifier.Role.ATTACKER) {
                    attackers.add(entity);
                } else if (role == HywSiegeClassifier.Role.DEFENDER) {
                    defenders.add(entity);
                }
            }
        }
    }

    public static void applyStickyForces(
            ServerLevel level,
            RecruitsClaim claim,
            List<LivingEntity> inClaimEntities,
            List<LivingEntity> attackers,
            List<LivingEntity> defenders
    ) {
        if (!BridgeConfig.STICKY_SIEGE_FORCES.get() || claim == null || level == null || !claim.isUnderSiege) {
            syncBridgeCounts(claim, attackers, defenders);
            return;
        }

        Commitment commitment = COMMITMENTS.computeIfAbsent(claim.getUUID(), ignored -> new Commitment());
        rememberEntities(commitment.attackers, attackers);
        rememberEntities(commitment.defenders, defenders);

        supplementHomeGarrison(level, claim, commitment);
        pruneCommitment(level, claim, commitment);

        attackers.clear();
        defenders.clear();
        resolveEntities(level, claim, commitment.attackers, attackers);
        resolveEntities(level, claim, commitment.defenders, defenders);

        SiegeForceFilter.stripNonCountingForces(attackers, defenders);
        syncBridgeCounts(claim, attackers, defenders);
    }

    private static void rememberEntities(Set<UUID> bucket, List<LivingEntity> entities) {
        for (LivingEntity entity : entities) {
            if (entity != null && entity.isAlive() && !entity.isRemoved()) {
                bucket.add(entity.getUUID());
            }
        }
    }

    private static void supplementHomeGarrison(ServerLevel level, RecruitsClaim claim, Commitment commitment) {
        AABB scanBox = expandedClaimBounds(claim, HOME_SCAN_MARGIN);
        if (scanBox == null) {
            return;
        }
        List<BaseCombatEntity> hywUnits = level.getEntitiesOfClass(BaseCombatEntity.class, scanBox, Entity::isAlive);
        for (BaseCombatEntity hyw : hywUnits) {
            if (!isHomeOnClaim(hyw, claim)) {
                continue;
            }
            HywSiegeClassifier.Role role = HywSiegeClassifier.classify(hyw, claim);
            if (role == HywSiegeClassifier.Role.ATTACKER) {
                commitment.attackers.add(hyw.getUUID());
            } else if (role == HywSiegeClassifier.Role.DEFENDER) {
                commitment.defenders.add(hyw.getUUID());
            }
        }
    }

    private static void pruneCommitment(ServerLevel level, RecruitsClaim claim, Commitment commitment) {
        pruneBucket(level, claim, commitment.attackers, HywSiegeClassifier.Role.ATTACKER);
        pruneBucket(level, claim, commitment.defenders, HywSiegeClassifier.Role.DEFENDER);
    }

    private static void pruneBucket(
            ServerLevel level,
            RecruitsClaim claim,
            Set<UUID> bucket,
            HywSiegeClassifier.Role expectedRole
    ) {
        Iterator<UUID> iterator = bucket.iterator();
        while (iterator.hasNext()) {
            UUID entityId = iterator.next();
            Entity entity = level.getEntity(entityId);
            if (entity == null) {
                continue;
            }
            if (!(entity instanceof LivingEntity living) || !living.isAlive() || living.isRemoved()) {
                iterator.remove();
                continue;
            }
            if (!SiegeForceFilter.countsForSiege(living)) {
                iterator.remove();
                continue;
            }
            HywSiegeClassifier.Role role = HywSiegeClassifier.classify(living, claim);
            if (role != expectedRole) {
                iterator.remove();
            }
        }
    }

    private static void resolveEntities(
            ServerLevel level,
            RecruitsClaim claim,
            Set<UUID> source,
            List<LivingEntity> target
    ) {
        for (UUID entityId : source) {
            Entity entity = level.getEntity(entityId);
            if (!(entity instanceof LivingEntity living) || !living.isAlive() || living.isRemoved()) {
                continue;
            }
            if (!SiegeForceFilter.countsForSiege(living)) {
                continue;
            }
            HywSiegeClassifier.Role role = HywSiegeClassifier.classify(living, claim);
            if (role == HywSiegeClassifier.Role.NONE) {
                continue;
            }
            if (!target.contains(living)) {
                target.add(living);
            }
        }
    }

    private static boolean isHomeOnClaim(BaseCombatEntity hyw, RecruitsClaim claim) {
        if (hyw.shouldIgnoreHomePosition()) {
            return false;
        }
        var home = hyw.getHomePosition();
        if (home == null) {
            return false;
        }
        return claim.containsChunk(new ChunkPos(home));
    }

    private static AABB expandedClaimBounds(RecruitsClaim claim, int margin) {
        List<ChunkPos> chunks = claim.getClaimedChunks();
        if (chunks.isEmpty()) {
            return null;
        }
        int minX = Integer.MAX_VALUE;
        int minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int maxZ = Integer.MIN_VALUE;
        for (ChunkPos chunk : chunks) {
            minX = Math.min(minX, chunk.getMinBlockX());
            minZ = Math.min(minZ, chunk.getMinBlockZ());
            maxX = Math.max(maxX, chunk.getMaxBlockX());
            maxZ = Math.max(maxZ, chunk.getMaxBlockZ());
        }
        return new AABB(
                minX - margin,
                -64,
                minZ - margin,
                maxX + margin,
                320,
                maxZ + margin
        );
    }

    private static void syncBridgeCounts(RecruitsClaim claim, List<LivingEntity> attackers, List<LivingEntity> defenders) {
        int attackerCount = attackers != null ? attackers.size() : 0;
        int defenderCount = defenders != null ? defenders.size() : 0;
        if (claim instanceof BridgeClaimAccess access) {
            access.cannon$setBridgeAttackerCount(attackerCount);
            access.cannon$setBridgeDefenderCount(defenderCount);
        }
        if (ClaimEvents.server != null && claim != null && claim.isUnderSiege) {
            SiegeForceBroadcaster.syncClaim(ClaimEvents.server, claim);
        }
    }

    private static void resetBridgeCounts(RecruitsClaim claim) {
        if (claim instanceof BridgeClaimAccess access) {
            access.cannon$setBridgeAttackerCount(0);
            access.cannon$setBridgeDefenderCount(0);
        }
    }

    private static final class Commitment {
        private final Set<UUID> attackers = new HashSet<>();
        private final Set<UUID> defenders = new HashSet<>();
    }
}
