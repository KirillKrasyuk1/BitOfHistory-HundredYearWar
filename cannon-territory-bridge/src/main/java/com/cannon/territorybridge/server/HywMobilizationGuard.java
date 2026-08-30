package com.cannon.territorybridge.server;

import com.cannon.territorybridge.CannonTerritoryBridge;
import com.cannon.territorybridge.config.BridgeConfig;
import com.talhanation.recruits.ClaimEvents;
import com.talhanation.recruits.FactionEvents;
import com.talhanation.recruits.world.RecruitsClaim;
import com.talhanation.recruits.world.RecruitsFaction;
import com.talhanation.recruits.world.RecruitsPlayerInfo;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.scores.Team;

import java.util.UUID;

public final class HywMobilizationGuard {
    public enum DenyReason {
        NO_CLAIM,
        NO_SQUAD,
        NOT_ON_OWN_CLAIM
    }

    private HywMobilizationGuard() {}

    public static boolean canMobilize(ServerPlayer player, BlockPos pos) {
        return evaluate(player.getUUID(), pos, player, "canMobilize") == null;
    }

    public static DenyReason evaluate(UUID playerId, BlockPos pos, ServerPlayer messagingPlayer) {
        return evaluate(playerId, pos, messagingPlayer, "evaluate");
    }

    public static DenyReason evaluate(UUID playerId, BlockPos pos, ServerPlayer messagingPlayer, String source) {
        boolean creative = messagingPlayer != null && messagingPlayer.getAbilities().instabuild;
        boolean hasSquad = HywSquadHelper.hasSquad(playerId);
        ChunkPos chunk = pos == null ? null : new ChunkPos(pos);
        RecruitsFaction playerFaction = findPlayerFaction(playerId, messagingPlayer);
        RecruitsClaim standingClaim = null;
        if (ClaimEvents.recruitsClaimManager != null && chunk != null) {
            standingClaim = ClaimEvents.recruitsClaimManager.getClaim(chunk);
        }

        DenyReason reason = resolve(playerId, pos, messagingPlayer, playerFaction, standingClaim, creative, hasSquad);
        boolean logAllow = source == null || !source.startsWith("entityJoin:");
        if (reason != null || logAllow) {
            CannonTerritoryBridge.LOGGER.info(
                    "[CTB-MOBILIZE] source={} player={} uuid={} pos={} chunk={} creative={} configClaim={} configSquad={} creativeBypass={} faction={} standingClaim={} squad={} result={}",
                    source,
                    messagingPlayer != null ? messagingPlayer.getGameProfile().getName() : "offline",
                    playerId,
                    pos,
                    chunk,
                    creative,
                    BridgeConfig.REQUIRE_OWN_CLAIM_TO_MOBILIZE.get(),
                    BridgeConfig.REQUIRE_SQUAD_TO_MOBILIZE.get(),
                    BridgeConfig.ALLOW_CREATIVE_BYPASS.get(),
                    playerFaction != null ? playerFaction.getStringID() : "none",
                    standingClaim != null ? standingClaim.getOwnerFactionStringID() : "none",
                    hasSquad,
                    reason == null ? "ALLOW" : reason
            );
        }
        return reason;
    }

    private static DenyReason resolve(
            UUID playerId,
            BlockPos pos,
            ServerPlayer messagingPlayer,
            RecruitsFaction playerFaction,
            RecruitsClaim standingClaim,
            boolean creative,
            boolean hasSquad
    ) {
        if (!BridgeConfig.REQUIRE_OWN_CLAIM_TO_MOBILIZE.get() && !BridgeConfig.REQUIRE_SQUAD_TO_MOBILIZE.get()) {
            return null;
        }
        if (creative && BridgeConfig.ALLOW_CREATIVE_BYPASS.get()) {
            return null;
        }
        if (ClaimEvents.recruitsClaimManager == null) {
            return DenyReason.NO_CLAIM;
        }
        if (playerFaction == null) {
            return DenyReason.NO_CLAIM;
        }
        if (BridgeConfig.REQUIRE_OWN_CLAIM_TO_MOBILIZE.get() && !ownsAnyClaim(playerFaction.getStringID())) {
            return DenyReason.NO_CLAIM;
        }
        if (BridgeConfig.REQUIRE_SQUAD_TO_MOBILIZE.get() && !hasSquad) {
            return DenyReason.NO_SQUAD;
        }
        if (!BridgeConfig.REQUIRE_OWN_CLAIM_TO_MOBILIZE.get()) {
            return null;
        }
        if (standingClaim == null || standingClaim.isAdmin || standingClaim.isRemoved) {
            return DenyReason.NOT_ON_OWN_CLAIM;
        }
        RecruitsFaction ownerFaction = standingClaim.getOwnerFaction();
        if (ownerFaction == null || !isMemberOfFaction(playerId, ownerFaction)) {
            return DenyReason.NOT_ON_OWN_CLAIM;
        }
        return null;
    }

    public static void denyMobilization(ServerPlayer player) {
        denyMobilization(player, evaluate(player.getUUID(), player.blockPosition(), player, "deny"));
    }

    public static void denyMobilization(ServerPlayer player, DenyReason reason) {
        if (reason == null) {
            reason = DenyReason.NOT_ON_OWN_CLAIM;
        }
        player.displayClientMessage(denyMessage(reason).copy().withStyle(ChatFormatting.RED), true);
    }

    public static Component denyMessage(DenyReason reason) {
        if (reason == null) {
            reason = DenyReason.NOT_ON_OWN_CLAIM;
        }
        String key = switch (reason) {
            case NO_CLAIM -> "message.cannon_territory_bridge.mobilize_denied_no_claim";
            case NO_SQUAD -> "message.cannon_territory_bridge.mobilize_denied_no_squad";
            case NOT_ON_OWN_CLAIM -> "message.cannon_territory_bridge.mobilize_denied";
        };
        return Component.translatable(key);
    }

    static boolean ownsAnyClaim(String factionId) {
        for (RecruitsClaim claim : ClaimEvents.recruitsClaimManager.getAllClaims()) {
            if (claim.isAdmin || claim.isRemoved) {
                continue;
            }
            if (factionId.equals(claim.getOwnerFactionStringID())) {
                return true;
            }
        }
        return false;
    }

    static RecruitsFaction findPlayerFaction(UUID playerId, ServerPlayer player) {
        RecruitsFaction faction = HywSiegeHelper.findRecruitsFactionForOwner(playerId);
        if (faction != null) {
            return faction;
        }
        if (player != null) {
            String factionId = resolvePlayerFactionId(player);
            if (factionId != null && FactionEvents.recruitsFactionManager != null) {
                return FactionEvents.recruitsFactionManager.getFactionByStringID(factionId);
            }
        }
        return null;
    }

    static boolean isMemberOfFaction(UUID playerId, RecruitsFaction faction) {
        if (playerId.equals(faction.getTeamLeaderUUID())) {
            return true;
        }
        for (RecruitsPlayerInfo member : faction.getMembers()) {
            if (playerId.equals(member.getUUID())) {
                return true;
            }
        }
        return false;
    }

    /** Scoreboard team name for a player's Recruits faction, if any. */
    public static String resolvePlayerFactionId(ServerPlayer player) {
        Team team = player.getTeam();
        if (team != null) {
            return team.getName();
        }
        RecruitsFaction faction = HywSiegeHelper.findRecruitsFactionForOwner(player.getUUID());
        return faction != null ? faction.getStringID() : null;
    }
}
