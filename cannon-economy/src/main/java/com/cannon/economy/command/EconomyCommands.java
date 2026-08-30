package com.cannon.economy.command;

import com.cannon.economy.config.EconomyConfig;
import com.cannon.economy.deposit.DepositSavedData;
import com.cannon.economy.deposit.DepositType;
import com.cannon.economy.deposit.ResourceDeposit;
import com.cannon.economy.trade.TradePostBlockEntity;
import com.cannon.economy.trade.TradeRoute;
import com.cannon.economy.trade.TradeRouteSavedData;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.item.ItemArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.UUID;

public final class EconomyCommands {
    private EconomyCommands() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext buildContext) {
        var root = Commands.literal("cannoneconomy").requires(src -> src.hasPermission(2));

        root.then(Commands.literal("deposit")
                .then(Commands.literal("add")
                        .then(Commands.argument("type", StringArgumentType.word())
                                .executes(ctx -> addDeposit(ctx.getSource(),
                                        StringArgumentType.getString(ctx, "type"),
                                        EconomyConfig.DEFAULT_DEPOSIT_RADIUS.get(), ""))
                                .then(Commands.argument("radius", IntegerArgumentType.integer(8, 128))
                                        .executes(ctx -> addDeposit(ctx.getSource(),
                                                StringArgumentType.getString(ctx, "type"),
                                                IntegerArgumentType.getInteger(ctx, "radius"), ""))
                                        .then(Commands.argument("label", StringArgumentType.greedyString())
                                                .executes(ctx -> addDeposit(ctx.getSource(),
                                                        StringArgumentType.getString(ctx, "type"),
                                                        IntegerArgumentType.getInteger(ctx, "radius"),
                                                        StringArgumentType.getString(ctx, "label")))))))
                .then(Commands.literal("remove")
                        .then(Commands.argument("id", StringArgumentType.string())
                                .executes(ctx -> removeDeposit(ctx.getSource(),
                                        StringArgumentType.getString(ctx, "id")))))
                .then(Commands.literal("list")
                        .executes(ctx -> listDeposits(ctx.getSource()))));

        var routeAmount = Commands.argument("amount", IntegerArgumentType.integer(1, 64));
        var routeInterval = Commands.argument("interval", IntegerArgumentType.integer(200, 72000));
        var routeTariff = Commands.argument("tariff", IntegerArgumentType.integer(0, 50));

        root.then(Commands.literal("route")
                .then(Commands.literal("create")
                        .then(Commands.argument("name", StringArgumentType.word())
                                .then(Commands.argument("from", StringArgumentType.word())
                                        .then(Commands.argument("to", StringArgumentType.word())
                                                .then(Commands.argument("item", ItemArgument.item(buildContext))
                                                        .then(routeAmount.executes(ctx -> createRoute(ctx.getSource(),
                                                                StringArgumentType.getString(ctx, "name"),
                                                                StringArgumentType.getString(ctx, "from"),
                                                                StringArgumentType.getString(ctx, "to"),
                                                                ItemArgument.getItem(ctx, "item").createItemStack(
                                                                        IntegerArgumentType.getInteger(ctx, "amount"), false),
                                                                EconomyConfig.DEFAULT_ROUTE_INTERVAL_TICKS.get(), 0))
                                                                .then(routeInterval.executes(ctx -> createRoute(ctx.getSource(),
                                                                        StringArgumentType.getString(ctx, "name"),
                                                                        StringArgumentType.getString(ctx, "from"),
                                                                        StringArgumentType.getString(ctx, "to"),
                                                                        ItemArgument.getItem(ctx, "item").createItemStack(
                                                                                IntegerArgumentType.getInteger(ctx, "amount"), false),
                                                                        IntegerArgumentType.getInteger(ctx, "interval"),
                                                                        0))
                                                                        .then(routeTariff.executes(ctx -> createRoute(ctx.getSource(),
                                                                                StringArgumentType.getString(ctx, "name"),
                                                                                StringArgumentType.getString(ctx, "from"),
                                                                                StringArgumentType.getString(ctx, "to"),
                                                                                ItemArgument.getItem(ctx, "item").createItemStack(
                                                                                        IntegerArgumentType.getInteger(ctx, "amount"), false),
                                                                                IntegerArgumentType.getInteger(ctx, "interval"),
                                                                                IntegerArgumentType.getInteger(ctx, "tariff")))))))))))
                .then(Commands.literal("remove")
                        .then(Commands.argument("name", StringArgumentType.word())
                                .executes(ctx -> removeRoute(ctx.getSource(),
                                        StringArgumentType.getString(ctx, "name")))))
                .then(Commands.literal("list")
                        .executes(ctx -> listRoutes(ctx.getSource()))));

        root.then(Commands.literal("post")
                .then(Commands.literal("name")
                        .then(Commands.argument("name", StringArgumentType.word())
                                .executes(ctx -> namePost(ctx.getSource(),
                                        StringArgumentType.getString(ctx, "name"))))));

        dispatcher.register(root);
    }

    private static int addDeposit(CommandSourceStack src, String typeId, int radius, String label) {
        DepositType type = DepositType.byId(typeId);
        if (type == null) {
            src.sendFailure(Component.translatable("message.cannon_economy.unknown_deposit_type", typeId));
            return 0;
        }
        ServerLevel level = src.getLevel();
        BlockPos pos = BlockPos.containing(src.getPosition());
        ResourceDeposit deposit = DepositSavedData.get(level).add(pos, level.dimension(), type, radius, label);
        src.sendSuccess(() -> Component.translatable("message.cannon_economy.deposit_added",
                type.getSerializedName(), deposit.id, radius), true);
        return 1;
    }

    private static int removeDeposit(CommandSourceStack src, String idStr) {
        UUID id;
        try {
            id = UUID.fromString(idStr);
        } catch (IllegalArgumentException e) {
            src.sendFailure(Component.literal("Invalid UUID: " + idStr));
            return 0;
        }
        if (DepositSavedData.get(src.getLevel()).remove(id)) {
            src.sendSuccess(() -> Component.translatable("message.cannon_economy.deposit_removed", idStr), true);
            return 1;
        }
        src.sendFailure(Component.translatable("message.cannon_economy.deposit_not_found", idStr));
        return 0;
    }

    private static int listDeposits(CommandSourceStack src) {
        for (ResourceDeposit d : DepositSavedData.get(src.getLevel()).all()) {
            src.sendSuccess(() -> Component.literal(String.format("%s %s @ %d,%d,%d r=%d %s",
                    d.id, d.type.getSerializedName(), d.center.getX(), d.center.getY(), d.center.getZ(),
                    d.radius, d.label)), false);
        }
        return 1;
    }

    private static int namePost(CommandSourceStack src, String name) {
        ServerPlayer player = src.getPlayer();
        if (player == null) {
            return 0;
        }
        BlockPos pos = player.blockPosition();
        if (src.getLevel().getBlockEntity(pos) instanceof TradePostBlockEntity post) {
            post.setPostName(name);
            src.sendSuccess(() -> Component.translatable("message.cannon_economy.post_named", name), true);
            return 1;
        }
        src.sendFailure(Component.translatable("message.cannon_economy.no_trade_post"));
        return 0;
    }

    private static int createRoute(CommandSourceStack src, String name, String fromName, String toName,
                                   ItemStack cargo, int interval, int tariff) {
        ServerLevel level = src.getLevel();
        ServerPlayer player = src.getPlayer();
        if (player == null) {
            src.sendFailure(Component.literal("Player required"));
            return 0;
        }
        BlockPos from = findPost(level, player.blockPosition(), fromName);
        BlockPos to = findPost(level, player.blockPosition(), toName);
        if (from == null || to == null) {
            src.sendFailure(Component.translatable("message.cannon_economy.post_not_found", from == null ? fromName : toName));
            return 0;
        }
        TradeRoute route = new TradeRoute(UUID.randomUUID(), name, from, to, level.dimension(),
                cargo.copy(), interval, tariff, null, interval);
        TradeRouteSavedData.get(level).add(route);
        src.sendSuccess(() -> Component.translatable("message.cannon_economy.route_created", name), true);
        return 1;
    }

    private static BlockPos findPost(ServerLevel level, BlockPos center, String name) {
        int range = 256;
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-range, -64, -range), center.offset(range, 64, range))) {
            if (level.getBlockEntity(pos) instanceof TradePostBlockEntity post && name.equals(post.getPostName())) {
                return pos.immutable();
            }
        }
        return null;
    }

    private static int removeRoute(CommandSourceStack src, String name) {
        TradeRouteSavedData data = TradeRouteSavedData.get(src.getLevel());
        boolean removed = data.all().stream()
                .filter(r -> r.name.equalsIgnoreCase(name))
                .findFirst()
                .map(r -> data.remove(r.id))
                .orElse(false);
        if (removed) {
            src.sendSuccess(() -> Component.translatable("message.cannon_economy.route_removed", name), true);
            return 1;
        }
        src.sendFailure(Component.translatable("message.cannon_economy.route_not_found", name));
        return 0;
    }

    private static int listRoutes(CommandSourceStack src) {
        for (TradeRoute r : TradeRouteSavedData.get(src.getLevel()).all()) {
            src.sendSuccess(() -> Component.literal(String.format("%s: %s -> %s every %d ticks, tariff %d%%",
                    r.name, r.from, r.to, r.intervalTicks, r.tariffPercent)), false);
        }
        return 1;
    }
}
