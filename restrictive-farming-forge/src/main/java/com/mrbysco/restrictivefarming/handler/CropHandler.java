package com.mrbysco.restrictivefarming.handler;

import com.mrbysco.restrictivefarming.RestrictiveFarmingMod;
import com.mrbysco.restrictivefarming.config.FarmingConfig;
import com.mrbysco.restrictivefarming.datamap.WhitelistData;
import com.mrbysco.restrictivefarming.loader.CropWhitelistLoader;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = RestrictiveFarmingMod.MOD_ID)
public class CropHandler {

    @SubscribeEvent
    public static void placeBlock(BlockEvent.EntityPlaceEvent event) {
        if (!FarmingConfig.COMMON.restrictPlacement.get()) {
            return;
        }

        LevelAccessor level = event.getLevel();
        BlockState state = event.getPlacedBlock();
        Block block = state.getBlock();
        WhitelistData data = CropWhitelistLoader.INSTANCE.get(block);
        if (data == null) {
            return;
        }

        BlockPos pos = event.getPos();
        Entity entity = event.getEntity();
        Holder<Biome> biome = level.getBiome(pos);
        if (!data.allowsBiome(biome)) {
            event.setCanceled(true);
            if (entity instanceof ServerPlayer player && FarmingConfig.COMMON.showRestrictedMessage.get()) {
                MutableComponent component = data.isCrop()
                        ? Component.translatable("restrictive_farming.restricted_crop_message", block.getName())
                        : Component.translatable("restrictive_farming.restricted_block_message", block.getName());
                player.sendSystemMessage(component.withStyle(ChatFormatting.RED));
            }
        }
    }

    @SubscribeEvent
    public static void beforeCropGrow(BlockEvent.CropGrowEvent.Pre event) {
        if (!FarmingConfig.COMMON.reduceGrowth.get()) {
            return;
        }

        LevelAccessor level = event.getLevel();
        BlockPos pos = event.getPos();
        BlockState state = event.getState();
        Block block = state.getBlock();

        WhitelistData data = CropWhitelistLoader.INSTANCE.get(block);
        if (data == null) {
            return;
        }

        float growthReduction = data.getReductionOrDefault();
        Holder<Biome> biome = level.getBiome(pos);
        if (!data.allowsBiome(biome) && level.getRandom().nextFloat() < growthReduction) {
            event.setResult(Event.Result.DENY);
        }
    }
}
