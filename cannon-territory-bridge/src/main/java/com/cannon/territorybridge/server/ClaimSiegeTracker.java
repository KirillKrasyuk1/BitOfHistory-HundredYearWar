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
 * Live HYW counts refresh every tick for the overlay.
 * During active capture, defender UUIDs survive chunk-unload until confirmed dead —
 * ratio/completion uses live + reserve, not zero just because the owner walked off.
 */
public final class ClaimSiegeTracker {
    private static final Map<UUID, Commitment> COMMITMENTS = new ConcurrentHashMap<>();
    private static final Set<UUID> CONFIRMED_DEAD = ConcurrentHashMap.newKeySet();

    private ClaimSiegeTracker() {}

    private static final int HOME_SCAN_MARGIN = 2048;

    private static final ThreadLocal<RecruitsClaim> ACTIVE_TICK_CLAIM = new ThreadLocal<>();

    public static void bindActiveTickClaim(RecruitsClaim claim) {
        ACTIVE_TICK_CLAIM.set(claim);
    }

    public static RecruitsClaim activeTickClaim() {
        return ACTIVE_TICK_CLAIM.get();
    }

    public static void clearActiveTickClaim() {
        ACTIVE_TICK_CLAIM.remove();
    }

    public static void markDead(UUID entityId) {
        if (entityId != null) {
            CONFIRMED_DEAD.add(entityId);
        }
    }

    public static void clear(RecruitsClaim claim) {
        if (claim != null) {
            Commitment removed = COMMITMENTS.remove(claim.getUUID());
            if (removed != null) {
                removed.attackers.forEach(CONFIRMED_DEAD::remove);
                removed.defenders.forEach(CONFIRMED_DEAD::remove);
            }
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
        for (ChunkPos chunk : claim.getClaimedChunks()) {
            AABB box = new AABB(
                    chunk.getMinBlockX(),
                    level.getMinBuildHeight(),
                    chunk.getMinBlockZ(),
                    chunk.getMaxBlockX() + 1,
                    level.getMaxBuildHeight(),
                    chunk.getMaxBlockZ() + 1
            );
            for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, box, Entity::isAlive)) {
                addClassifiedEntity(entity, claim, attackers, defenders);
            }
        }
    }

    public static void finalizeSiegeForces(
            ServerLevel level,
            RecruitsClaim claim,
            List<LivingEntity> attackers,
            List<LivingEntity> defenders
    ) {
        if (claim == null || level == null || !claim.isUnderSiege) {
            syncBridgeCounts(claim, 0, 0);
            return;
        }

        SiegeForceFilter.stripNonCountingForces(attackers, defenders);

        if (BridgeConfig.STICKY_SIEGE_FORCES.get()) {
            supplementHomeGarrisonInLists(level, claim, attackers, defenders);
        }

        SiegeForceFilter.stripNonCountingForces(attackers, defenders);

        Commitment commitment = COMMITMENTS.computeIfAbsent(claim.getUUID(), ignored -> new Commitment());
        rebuildCommitmentFromLists(commitment, attackers, defenders, claim);
        pruneCommitment(level, claim, commitment);

        int liveAttackers = attackers.size();
        int liveDefenders = defenders.size();
        syncBridgeCounts(claim, liveAttackers, liveDefenders);
    }

    /** Overlay / packet — units currently resolved in loaded chunks. */
    public static int liveAttackerCount(RecruitsClaim claim) {
        return bridgeFieldCount(claim, true);
    }

    public static int liveDefenderCount(RecruitsClaim claim) {
        return bridgeFieldCount(claim, false);
    }

    /** Recruits ratio, damage, and capture completion — includes unloaded garrison during active capture. */
    public static int ratioAttackerCount(RecruitsClaim claim, ServerLevel level) {
        return liveAttackerCount(claim);
    }

    public static int ratioDefenderCount(RecruitsClaim claim, ServerLevel level) {
        int live = liveDefenderCount(claim);
        if (!BridgeConfig.STICKY_SIEGE_FORCES.get() || !isCaptureInProgress(claim)) {
            return live;
        }
        Commitment commitment = COMMITMENTS.get(claim.getUUID());
        if (commitment == null) {
            return live;
        }
        return live + countUnloadedReserve(level, commitment.defenders);
    }

    public static boolean isCaptureInProgress(RecruitsClaim claim) {
        return claim != null && claim.isUnderSiege && claim.getHealth() < claim.getMaxHealth();
    }

    /** Ownership transfer only when every committed defender is confirmed dead (or there never was a garrison). */
    public static boolean canTransferOwnership(RecruitsClaim claim, ServerLevel level, int attackers, int defendersForRatio) {
        if (!SiegeBalance.canProgressCapture(attackers, defendersForRatio)) {
            return false;
        }
        Commitment commitment = claim != null ? COMMITMENTS.get(claim.getUUID()) : null;
        if (commitment == null || !commitment.captureStarted || commitment.captureBaselineDefenders <= 0) {
            return true;
        }
        return allDefendersConfirmedDead(claim, level);
    }

    public static void onCaptureDamageTick(RecruitsClaim claim, ServerLevel level, int ratioDefenders) {
        if (claim == null || !claim.isUnderSiege) {
            return;
        }
        Commitment commitment = COMMITMENTS.computeIfAbsent(claim.getUUID(), ignored -> new Commitment());
        if (commitment.captureStarted) {
            return;
        }
        commitment.captureStarted = true;
        commitment.captureBaselineDefenders = Math.max(ratioDefenders, commitment.defenders.size());
    }

    public static boolean allDefendersConfirmedDead(RecruitsClaim claim, ServerLevel level) {
        Commitment commitment = claim != null ? COMMITMENTS.get(claim.getUUID()) : null;
        if (commitment == null || commitment.defenders.isEmpty()) {
            return true;
        }
        for (UUID entityId : commitment.defenders) {
            if (CONFIRMED_DEAD.contains(entityId)) {
                continue;
            }
            if (level == null) {
                return false;
            }
            Entity entity = level.getEntity(entityId);
            if (entity == null) {
                return false;
            }
            if (entity instanceof LivingEntity living && living.isAlive() && !living.isRemoved()) {
                return false;
            }
        }
        return true;
    }

    private static void addClassifiedEntity(
            LivingEntity entity,
            RecruitsClaim claim,
            List<LivingEntity> attackers,
            List<LivingEntity> defenders
    ) {
        if (!SiegeForceFilter.countsForSiege(entity)) {
            return;
        }
        if (attackers.contains(entity) || defenders.contains(entity)) {
            return;
        }
        HywSiegeClassifier.Role role = HywSiegeClassifier.classify(entity, claim);
        if (role == HywSiegeClassifier.Role.ATTACKER) {
            attackers.add(entity);
        } else if (role == HywSiegeClassifier.Role.DEFENDER) {
            defenders.add(entity);
        }
    }

    private static void rebuildCommitmentFromLists(
            Commitment commitment,
            List<LivingEntity> attackers,
            List<LivingEntity> defenders,
            RecruitsClaim claim
    ) {
        Set<UUID> previousDefenders = new HashSet<>(commitment.defenders);

        commitment.attackers.clear();
        commitment.defenders.clear();
        rememberEntities(commitment.attackers, attackers);
        rememberEntities(commitment.defenders, defenders);

        if (isCaptureInProgress(claim) || commitment.captureStarted) {
            for (UUID entityId : previousDefenders) {
                if (!CONFIRMED_DEAD.contains(entityId)) {
                    commitment.defenders.add(entityId);
                }
            }
        }
    }

    private static int countUnloadedReserve(ServerLevel level, Set<UUID> defenderIds) {
        int reserve = 0;
        for (UUID entityId : defenderIds) {
            if (CONFIRMED_DEAD.contains(entityId)) {
                continue;
            }
            Entity entity = level.getEntity(entityId);
            if (entity == null) {
                reserve++;
            }
        }
        return reserve;
    }

    private static void rememberEntities(Set<UUID> bucket, List<LivingEntity> entities) {
        for (LivingEntity entity : entities) {
            if (entity != null && entity.isAlive() && !entity.isRemoved()) {
                bucket.add(entity.getUUID());
            }
        }
    }

    private static void supplementHomeGarrisonInLists(
            ServerLevel level,
            RecruitsClaim claim,
            List<LivingEntity> attackers,
            List<LivingEntity> defenders
    ) {
        AABB scanBox = expandedClaimBounds(claim, HOME_SCAN_MARGIN);
        if (scanBox == null) {
            return;
        }
        for (BaseCombatEntity hyw : level.getEntitiesOfClass(BaseCombatEntity.class, scanBox, Entity::isAlive)) {
            if (!isHomeOnClaim(hyw, claim)) {
                continue;
            }
            addClassifiedEntity(hyw, claim, attackers, defenders);
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
            if (CONFIRMED_DEAD.contains(entityId)) {
                iterator.remove();
                continue;
            }
            Entity entity = level.getEntity(entityId);
            if (entity == null) {
                continue;
            }
            if (!(entity instanceof LivingEntity living) || !living.isAlive() || living.isRemoved()) {
                iterator.remove();
                CONFIRMED_DEAD.add(entityId);
                continue;
            }
            if (!SiegeForceFilter.countsForSiege(living)) {
                iterator.remove();
                continue;
            }
            if (HywSiegeClassifier.classify(living, claim) != expectedRole) {
                iterator.remove();
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

    private static void syncBridgeCounts(RecruitsClaim claim, int attackerCount, int defenderCount) {
        if (claim instanceof BridgeClaimAccess access) {
            access.cannon$setBridgeAttackerCount(attackerCount);
            access.cannon$setBridgeDefenderCount(defenderCount);
        }
        if (ClaimEvents.server != null && claim != null && claim.isUnderSiege) {
            SiegeForceBroadcaster.syncClaim(ClaimEvents.server, claim);
        }
    }

    private static int bridgeFieldCount(RecruitsClaim claim, boolean attackers) {
        if (claim instanceof BridgeClaimAccess access) {
            return attackers ? access.cannon$getBridgeAttackerCount() : access.cannon$getBridgeDefenderCount();
        }
        return 0;
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
        private boolean captureStarted;
        private int captureBaselineDefenders;
    }
}
