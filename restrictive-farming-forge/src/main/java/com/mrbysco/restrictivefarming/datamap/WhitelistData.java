package com.mrbysco.restrictivefarming.datamap;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mrbysco.restrictivefarming.config.FarmingConfig;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;

public record WhitelistData(HolderSet<Biome> whitelist, float growthReduction, boolean isCrop) {
    private static final Codec<Float> REDUCTION_CODEC = Codec.FLOAT.flatXmap(
            value -> (value == -1.0f || (value >= 0.0f && value <= 1.0f))
                    ? DataResult.success(value)
                    : DataResult.error(() -> "growthReduction must be -1 (default) or in [0.0, 1.0], got " + value),
            DataResult::success
    );

    public static final Codec<WhitelistData> CODEC = RecordCodecBuilder.create(in -> in.group(
            RegistryCodecs.homogeneousList(Registries.BIOME).fieldOf("biomes").forGetter(WhitelistData::whitelist),
            REDUCTION_CODEC.optionalFieldOf("growthReduction", -1.0F).forGetter(WhitelistData::growthReduction),
            Codec.BOOL.optionalFieldOf("isCrop", true).forGetter(WhitelistData::isCrop)
    ).apply(in, WhitelistData::new));

    public static final Codec<WhitelistEntry> FILE_CODEC = RecordCodecBuilder.create(in -> in.group(
            ResourceLocation.CODEC.fieldOf("block").forGetter(WhitelistEntry::block),
            RegistryCodecs.homogeneousList(Registries.BIOME).fieldOf("biomes").forGetter(e -> e.data().whitelist()),
            REDUCTION_CODEC.optionalFieldOf("growthReduction", -1.0F).forGetter(e -> e.data().growthReduction()),
            Codec.BOOL.optionalFieldOf("isCrop", true).forGetter(e -> e.data().isCrop())
    ).apply(in, (block, biomes, reduction, isCrop) ->
            new WhitelistEntry(block, new WhitelistData(biomes, reduction, isCrop))));

    public float getReductionOrDefault() {
        return growthReduction == -1.0F ? FarmingConfig.COMMON.growthReduction.get().floatValue() : growthReduction;
    }

    public boolean allowsBiome(Holder<Biome> biome) {
        return whitelist.contains(biome);
    }

    public record WhitelistEntry(ResourceLocation block, WhitelistData data) {}
}
