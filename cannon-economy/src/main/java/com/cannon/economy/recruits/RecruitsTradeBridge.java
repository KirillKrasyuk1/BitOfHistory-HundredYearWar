package com.cannon.economy.recruits;

import com.cannon.economy.trade.TradeRoute;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

/**
 * Optional Recruits integration via reflection so the mod still loads without Recruits on classpath at runtime
 * (Recruits is expected in the modpack).
 */
public final class RecruitsTradeBridge {
    private RecruitsTradeBridge() {}

    public static boolean isRouteBlocked(ServerLevel level, TradeRoute route) {
        if (route.ownerFaction == null) {
            return false;
        }
        return isChunkEmbargoed(level, route.from) || isChunkEmbargoed(level, route.to);
    }

    public static boolean isChunkEmbargoed(ServerLevel level, BlockPos pos) {
        try {
            Class<?> claimManager = Class.forName("com.talhanation.recruits.world.RecruitsClaimManager");
            Object instance = claimManager.getMethod("getInstance").invoke(null);
            Object claim = claimManager.getMethod("getClaimAt", BlockPos.class).invoke(instance, pos);
            if (claim == null) {
                return false;
            }
            // Without faction context we only block when claim marks trade disabled — future hook
            return false;
        } catch (ReflectiveOperationException ignored) {
            return false;
        }
    }
}
