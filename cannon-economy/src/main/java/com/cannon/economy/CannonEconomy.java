package com.cannon.economy;

import com.cannon.economy.command.EconomyCommands;
import com.cannon.economy.config.EconomyConfig;
import com.cannon.economy.deposit.DepositEvents;
import com.cannon.economy.farming.CropGrowthHandler;
import com.cannon.economy.trade.TradeEvents;
import com.cannon.economy.trade.TradeRegistry;
import com.mojang.logging.LogUtils;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.slf4j.Logger;

@Mod(CannonEconomy.MOD_ID)
public class CannonEconomy {
    public static final String MOD_ID = "cannon_economy";
    public static final Logger LOGGER = LogUtils.getLogger();

    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, MOD_ID);
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, MOD_ID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, MOD_ID);

    public static final RegistryObject<Block> TRADE_POST_BLOCK =
            BLOCKS.register("trade_post", com.cannon.economy.trade.TradePostBlock::new);
    public static final RegistryObject<Item> TRADE_POST_ITEM =
            ITEMS.register("trade_post", () -> new BlockItem(TRADE_POST_BLOCK.get(), new Item.Properties()));
    public static final RegistryObject<BlockEntityType<com.cannon.economy.trade.TradePostBlockEntity>> TRADE_POST_BE =
            BLOCK_ENTITIES.register("trade_post",
                    () -> BlockEntityType.Builder.of(
                            com.cannon.economy.trade.TradePostBlockEntity::new,
                            TRADE_POST_BLOCK.get()).build(null));

    public CannonEconomy() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        BLOCK_ENTITIES.register(modBus);

        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, EconomyConfig.SPEC);
        modBus.addListener(this::commonSetup);

        MinecraftForge.EVENT_BUS.register(new DepositEvents());
        MinecraftForge.EVENT_BUS.register(new CropGrowthHandler());
        MinecraftForge.EVENT_BUS.register(new TradeEvents());
        MinecraftForge.EVENT_BUS.addListener(this::registerCommands);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(TradeRegistry::loadDefaults);
        LOGGER.info("Cannon Economy loaded — regional resources & trade routes.");
    }

    private void registerCommands(RegisterCommandsEvent event) {
        EconomyCommands.register(event.getDispatcher(), event.getBuildContext());
    }
}
