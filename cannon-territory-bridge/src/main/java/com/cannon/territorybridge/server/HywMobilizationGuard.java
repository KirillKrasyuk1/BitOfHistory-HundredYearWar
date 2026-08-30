package com.cannon.territorybridge.server;

import com.cannon.territorybridge.config.BridgeConfig;
import com.talhanation.recruits.ClaimEvents;
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
    private HywMobilizationGuard() {}

    public static boolean canMobilize(ServerPlayer player, BlockPos pos) {
        if (!BridgeConfig.REQUIRE_OWN_CLAIM_TO_MOBILIZE.get()) {
            return true;
        }
        if (player.getAbilities().instabuild) {
            return true;
        }
        if (ClaimEvents.recruitsClaimManager == null) {
            return false;
        }

        RecruitsClaim claim = ClaimEvents.recruitsClaimManager.getClaim(new ChunkPos(pos));
        if (claim == null || claim.isAdmin || claim.isRemoved) {
            return false;
        }

        RecruitsFaction ownerFaction = claim.getOwnerFaction();
        if (ownerFaction == null) {
            return false;
        }

        return isMemberOfFaction(player.getUUID(), ownerFaction);
    }

    public static void denyMobilization(ServerPlayer player) {
        player.displayClientMessage(
                Component.translatable("message.cannon_territory_bridge.mobilize_denied")
                        .withStyle(ChatFormatting.RED),
                true
        );
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
