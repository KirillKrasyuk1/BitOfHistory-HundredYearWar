package com.cannon.economy.farming;

import com.cannon.economy.CannonEconomy;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;

public final class EconomyBiomeTags {
    public static final TagKey<Biome> ARID = TagKey.create(
            Registries.BIOME, new ResourceLocation(CannonEconomy.MOD_ID, "arid"));
    public static final TagKey<Biome> PLAINS_FARMLAND = TagKey.create(
            Registries.BIOME, new ResourceLocation(CannonEconomy.MOD_ID, "plains_farmland"));

    private EconomyBiomeTags() {}
}
