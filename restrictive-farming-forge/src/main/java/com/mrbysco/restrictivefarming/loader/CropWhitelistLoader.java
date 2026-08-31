package com.mrbysco.restrictivefarming.loader;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import com.mrbysco.restrictivefarming.RestrictiveFarmingMod;
import com.mrbysco.restrictivefarming.config.FarmingConfig;
import com.mrbysco.restrictivefarming.datamap.WhitelistData;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.tags.TagKey;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class CropWhitelistLoader extends SimplePreparableReloadListener<Map<ResourceLocation, JsonElement>> {
    private static final Gson GSON = new Gson();
    public static final CropWhitelistLoader INSTANCE = new CropWhitelistLoader();

    private Map<ResourceLocation, WhitelistData> rules = Map.of();

    public WhitelistData get(Block block) {
        ResourceLocation key = ForgeRegistries.BLOCKS.getKey(block);
        return key == null ? null : rules.get(key);
    }

    public boolean isAllowed(Block block, Holder<Biome> biome) {
        WhitelistData data = get(block);
        if (data == null) {
            return true;
        }
        return data.allowsBiome(biome);
    }

    public float growthReduction(Block block) {
        WhitelistData data = get(block);
        return data == null ? 0.0F : data.getReductionOrDefault();
    }

    @Override
    protected Map<ResourceLocation, JsonElement> prepare(ResourceManager manager, ProfilerFiller profiler) {
        Map<ResourceLocation, JsonElement> raw = new HashMap<>();
        for (Map.Entry<ResourceLocation, Resource> entry : manager.listResources("restrictive_farming/crop_whitelist",
                path -> path.getPath().endsWith(".json")).entrySet()) {
            ResourceLocation fileId = entry.getKey();
            if (!FarmingConfig.COMMON.defaultRestrictions.get()
                    && RestrictiveFarmingMod.MOD_ID.equals(fileId.getNamespace())) {
                continue;
            }
            try (var reader = new InputStreamReader(entry.getValue().open(), StandardCharsets.UTF_8)) {
                raw.put(fileId, GSON.fromJson(reader, JsonElement.class));
            } catch (Exception e) {
                RestrictiveFarmingMod.LOGGER.error("Failed reading crop whitelist {}", fileId, e);
            }
        }
        return raw;
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> raw, ResourceManager manager, ProfilerFiller profiler) {
        RegistryAccess access = ServerLifecycleHooks.getCurrentServer().registryAccess();
        RegistryOps<JsonElement> ops = RegistryOps.create(JsonOps.INSTANCE, access);
        Map<ResourceLocation, WhitelistData> loaded = new HashMap<>();

        for (JsonElement json : raw.values()) {
            WhitelistData.WhitelistEntry entry = WhitelistData.FILE_CODEC.parse(ops, json).result().orElse(null);
            if (entry != null) {
                loaded.put(entry.block(), entry.data());
            } else {
                RestrictiveFarmingMod.LOGGER.error("Invalid crop whitelist entry: {}", json);
            }
        }

        if (FarmingConfig.COMMON.defaultRestrictions.get()) {
            applyBuiltInDefaults(loaded, access);
        }

        rules = Map.copyOf(loaded);
        RestrictiveFarmingMod.LOGGER.info("Loaded {} crop whitelist rules", rules.size());
    }

    private void applyBuiltInDefaults(Map<ResourceLocation, WhitelistData> loaded, RegistryAccess access) {
        Registry<Biome> biomes = access.registryOrThrow(Registries.BIOME);
        putDefault(loaded, "minecraft:wheat", "minecraft:is_overworld", biomes);
        putDefault(loaded, "minecraft:carrots", "minecraft:is_overworld", biomes);
        putDefault(loaded, "minecraft:potatoes", "minecraft:is_overworld", biomes);
        putDefault(loaded, "minecraft:beetroots", "minecraft:is_overworld", biomes);
        putDefault(loaded, "minecraft:cocoa", "minecraft:is_overworld", biomes);
        putDefault(loaded, "minecraft:melon_stem", "minecraft:is_overworld", biomes);
        putDefault(loaded, "minecraft:pumpkin_stem", "minecraft:is_overworld", biomes);
        putDefault(loaded, "minecraft:sweet_berry_bush", "minecraft:is_overworld", biomes);
        putDefault(loaded, "minecraft:nether_wart", "minecraft:is_nether", biomes);
    }

    private void putDefault(Map<ResourceLocation, WhitelistData> loaded, String blockId, String tagId,
                            Registry<Biome> biomes) {
        ResourceLocation block = ResourceLocation.tryParse(blockId);
        if (block == null) {
            return;
        }
        if (loaded.containsKey(block)) {
            return;
        }
        TagKey<Biome> tag = TagKey.create(Registries.BIOME, ResourceLocation.tryParse(tagId));
        if (tag == null) {
            return;
        }
        HolderSet<Biome> holders = biomes.getTag(tag)
                .map(named -> (HolderSet<Biome>) named)
                .orElseGet(() -> HolderSet.direct());
        loaded.put(block, new WhitelistData(holders, -1.0F, true));
    }
}
