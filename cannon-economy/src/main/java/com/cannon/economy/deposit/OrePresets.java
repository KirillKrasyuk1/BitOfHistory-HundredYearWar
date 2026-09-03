package com.cannon.economy.deposit;

import net.minecraft.resources.ResourceLocation;

import java.util.Map;
import java.util.Optional;

public final class OrePresets {
    private static final Map<String, String[]> PRESETS = Map.ofEntries(
            Map.entry("iron", new String[]{"minecraft:iron_ore", "minecraft:deepslate_iron_ore"}),
            Map.entry("gold", new String[]{"minecraft:gold_ore", "minecraft:deepslate_gold_ore"}),
            Map.entry("coal", new String[]{"minecraft:coal_ore", "minecraft:deepslate_coal_ore"}),
            Map.entry("copper", new String[]{"minecraft:copper_ore", "minecraft:deepslate_copper_ore"}),
            Map.entry("diamond", new String[]{"minecraft:diamond_ore", "minecraft:deepslate_diamond_ore"}),
            Map.entry("emerald", new String[]{"minecraft:emerald_ore", "minecraft:deepslate_emerald_ore"}),
            Map.entry("lapis", new String[]{"minecraft:lapis_ore", "minecraft:deepslate_lapis_ore"}),
            Map.entry("redstone", new String[]{"minecraft:redstone_ore", "minecraft:deepslate_redstone_ore"}),
            Map.entry("silver", new String[]{"iceandfire:silver_ore", "iceandfire:deepslate_silver_ore"}),
            Map.entry("sapphire", new String[]{"iceandfire:sapphire_ore", "iceandfire:deepslate_sapphire_ore"})
    );

    private OrePresets() {}

    public static Optional<ResourceLocation[]> resolve(String id) {
        String lower = id.toLowerCase();
        if (PRESETS.containsKey(lower)) {
            String[] pair = PRESETS.get(lower);
            return Optional.of(new ResourceLocation[]{
                    ResourceLocation.tryParse(pair[0]),
                    ResourceLocation.tryParse(pair[1])
            });
        }
        ResourceLocation ore = ResourceLocation.tryParse(id.contains(":") ? id : "minecraft:" + id);
        if (ore == null) {
            return Optional.empty();
        }
        String path = ore.getPath();
        ResourceLocation deepslate;
        if (path.startsWith("deepslate_")) {
            deepslate = ore;
            ore = new ResourceLocation(ore.getNamespace(), path.substring("deepslate_".length()));
        } else {
            deepslate = new ResourceLocation(ore.getNamespace(), "deepslate_" + path);
        }
        return Optional.of(new ResourceLocation[]{ore, deepslate});
    }
}
