package com.cannon.economy.deposit;

import com.cannon.economy.config.EconomyConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.Optional;

public class DepositEvents {

    @SubscribeEvent
    public void onBlockBreak(BlockEvent.BreakEvent event) {
        if (event.getLevel().isClientSide()) {
            return;
        }
        ServerLevel level = (ServerLevel) event.getLevel();
        BlockPos pos = event.getPos();
        ResourceKey<Level> dim = level.dimension();

        Optional<ResourceDeposit> deposit = DepositSavedData.get(level).at(pos, dim);
        if (deposit.isEmpty() || deposit.get().type == DepositType.FERTILE) {
            return;
        }

        BlockState state = event.getState();
        if (!matchesDeposit(state, deposit.get().type)) {
            return;
        }

        double mult = EconomyConfig.DEPOSIT_BONUS_MULTIPLIER.get();
        if (mult <= 1.0) {
            return;
        }

        int extra = (int) Math.floor(mult) - 1;
        if (Math.random() < (mult - Math.floor(mult))) {
            extra++;
        }
        for (int i = 0; i < extra; i++) {
            Block.getDrops(state, level, pos, level.getBlockEntity(pos), event.getPlayer(), event.getPlayer().getMainHandItem())
                    .forEach(stack -> Block.popResource(level, pos, stack.copy()));
        }
    }

    private static boolean matchesDeposit(BlockState state, DepositType type) {
        Block block = state.getBlock();
        return switch (type) {
            case GOLD -> state.is(BlockTags.GOLD_ORES) || block.getDescriptionId().contains("gold_ore");
            case IRON -> state.is(BlockTags.IRON_ORES) || block.getDescriptionId().contains("iron_ore");
            case COAL -> state.is(BlockTags.COAL_ORES) || block.getDescriptionId().contains("coal_ore");
            case SILVER -> block.getDescriptionId().contains("silver");
            case GEMS -> state.is(BlockTags.DIAMOND_ORES) || state.is(BlockTags.EMERALD_ORES)
                    || state.is(BlockTags.LAPIS_ORES) || block.getDescriptionId().contains("sapphire")
                    || block.getDescriptionId().contains("amethyst");
            case DRAGONSTEEL -> block.getDescriptionId().contains("dragonsteel") || block.getDescriptionId().contains("dragon_steel");
            case FERTILE -> false;
        };
    }
}
