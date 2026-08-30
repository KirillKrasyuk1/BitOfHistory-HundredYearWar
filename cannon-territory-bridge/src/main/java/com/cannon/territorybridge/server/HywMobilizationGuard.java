package com.cannon.territorybridge.server;

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
        return evaluate(player.getUUID(), pos, player) == null;
    }

    public static DenyReason evaluate(UUID playerId, BlockPos pos, ServerPlayer messagingPlayer) {
        if (!BridgeConfig.REQUIRE_OWN_CLAIM_TO_MOBILIZE.get()) {
            return null;
        }
        if (messagingPlayer != null && messagingPlayer.getAbilities().instabuild) {
            return null;
        }
        if (ClaimEvents.recruitsClaimManager == null) {
            return DenyReason.NO_CLAIM;
        }

        RecruitsFaction playerFaction = findPlayerFaction(playerId, messagingPlayer);
        if (playerFaction == null) {
            return DenyReason.NO_CLAIM;
        }

        if (!ownsAnyClaim(playerFaction.getStringID())) {
            return DenyReason.NO_CLAIM;
        }

        if (!HywSquadHelper.hasSquad(playerId)) {
            return DenyReason.NO_SQUAD;
        }

        RecruitsClaim claim = ClaimEvents.recruitsClaimManager.getClaim(new ChunkPos(pos));
        if (claim == null || claim.isAdmin || claim.isRemoved) {
            return DenyReason.NOT_ON_OWN_CLAIM;
        }

        RecruitsFaction ownerFaction = claim.getOwnerFaction();
        if (ownerFaction == null || !isMemberOfFaction(playerId, ownerFaction)) {
            return DenyReason.NOT_ON_OWN_CLAIM;
        }

        return null;
    }

    public static void denyMobilization(ServerPlayer player) {
        denyMobilization(player, evaluate(player.getUUID(), player.blockPosition(), player));
    }

    public static void denyMobilization(ServerPlayer player, DenyReason reason) {
        if (reason == null) {
            reason = DenyReason.NOT_ON_OWN_CLAIM;
        }
        String key = switch (reason) {
            case NO_CLAIM -> "message.cannon_territory_bridge.mobilize_denied_no_claim";
            case NO_SQUAD -> "message.cannon_territory_bridge.mobilize_denied_no_squad";
            case NOT_ON_OWN_CLAIM -> "message.cannon_territory_bridge.mobilize_denied";
        };
        player.displayClientMessage(
                Component.translatable(key).withStyle(ChatFormatting.RED),
                true
        );
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
