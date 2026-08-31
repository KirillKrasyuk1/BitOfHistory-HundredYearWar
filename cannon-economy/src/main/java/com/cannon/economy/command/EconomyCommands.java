package com.cannon.economy.command;

import com.cannon.economy.config.EconomyConfig;
import com.cannon.economy.deposit.OreDeposit;
import com.cannon.economy.deposit.OreDepositSavedData;
import com.cannon.economy.deposit.OrePresets;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;

import java.util.UUID;

public final class EconomyCommands {
    private EconomyCommands() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        var root = Commands.literal("cannoneconomy").requires(src -> src.hasPermission(2));

        root.then(Commands.literal("deposit")
                .then(Commands.literal("create")
                        .then(Commands.argument("ore", StringArgumentType.word())
                                .executes(ctx -> createDeposit(ctx.getSource(),
                                        StringArgumentType.getString(ctx, "ore"),
                                        EconomyConfig.DEFAULT_CHUNK_RADIUS.get(), ""))
                                .then(Commands.argument("chunkRadius", IntegerArgumentType.integer(1, 32))
                                        .executes(ctx -> createDeposit(ctx.getSource(),
                                                StringArgumentType.getString(ctx, "ore"),
                                                IntegerArgumentType.getInteger(ctx, "chunkRadius"), ""))
                                        .then(Commands.argument("label", StringArgumentType.greedyString())
                                                .executes(ctx -> createDeposit(ctx.getSource(),
                                                        StringArgumentType.getString(ctx, "ore"),
                                                        IntegerArgumentType.getInteger(ctx, "chunkRadius"),
                                                        StringArgumentType.getString(ctx, "label")))))))
                .then(Commands.literal("remove")
                        .then(Commands.argument("id", StringArgumentType.string())
                                .executes(ctx -> removeDeposit(ctx.getSource(),
                                        StringArgumentType.getString(ctx, "id")))))
                .then(Commands.literal("list")
                        .executes(ctx -> listDeposits(ctx.getSource()))));

        root.then(Commands.literal("fertility")
                .executes(ctx -> showFertility(ctx.getSource())));

        dispatcher.register(root);
    }

    private static int createDeposit(CommandSourceStack src, String oreId, int chunkRadius, String label) {
        var pair = OrePresets.resolve(oreId);
        if (pair.isEmpty()) {
            src.sendFailure(Component.translatable("message.cannon_economy.unknown_ore", oreId));
            return 0;
        }
        ServerLevel level = src.getLevel();
        BlockPos pos = BlockPos.containing(src.getPosition());
        ResourceLocation[] blocks = pair.get();
        OreDeposit deposit = new OreDeposit(
                UUID.randomUUID(), pos, level.dimension(), blocks[0], blocks[1],
                chunkRadius, label, false, 0, level.getGameTime());
        OreDepositSavedData.get(level).add(deposit);
        src.sendSuccess(() -> Component.translatable("message.cannon_economy.deposit_created",
                blocks[0], deposit.id, chunkRadius), true);
        return 1;
    }

    private static int removeDeposit(CommandSourceStack src, String idStr) {
        UUID id;
        try {
            id = UUID.fromString(idStr);
        } catch (IllegalArgumentException e) {
            src.sendFailure(Component.literal("Invalid UUID"));
            return 0;
        }
        if (OreDepositSavedData.get(src.getLevel()).remove(id)) {
            src.sendSuccess(() -> Component.translatable("message.cannon_economy.deposit_removed", idStr), true);
            return 1;
        }
        src.sendFailure(Component.translatable("message.cannon_economy.deposit_not_found", idStr));
        return 0;
    }

    private static int listDeposits(CommandSourceStack src) {
        for (OreDeposit d : OreDepositSavedData.get(src.getLevel()).all()) {
            src.sendSuccess(() -> Component.literal(String.format("%s %s r=%d chunks @ %d,%d done=%s %s",
                    d.id, d.oreBlockId, d.chunkRadius, d.center.getX(), d.center.getZ(),
                    d.conversionDone, d.label)), false);
        }
        return 1;
    }

    private static int showFertility(CommandSourceStack src) {
        ServerLevel level = src.getLevel();
        BlockPos pos = BlockPos.containing(src.getPosition());
        int f = com.cannon.economy.farming.FertilitySystem.getFertility(level, pos);
        float mult = com.cannon.economy.farming.FertilitySystem.growthMultiplier(f);
        src.sendSuccess(() -> Component.translatable("message.cannon_economy.fertility", f, String.format("%.2f", mult)), false);
        return 1;
    }
}
