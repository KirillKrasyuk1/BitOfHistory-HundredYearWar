package com.cannon.territorybridge.server;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

/** Detects HYW mobilization items without version-specific mixins. */
public final class HywMobilizationItems {
    private static final String HYW_MOD_ID = "hundred_years_war";
    private static final ResourceLocation CONQUERORS_STAFF =
            ResourceLocation.fromNamespaceAndPath(HYW_MOD_ID, "conquerors_staff");

    private HywMobilizationItems() {}

    public static boolean isConquerorsStaff(ItemStack stack) {
        return matches(stack, CONQUERORS_STAFF);
    }

    public static boolean isMobilizationItem(ItemStack stack) {
        return isConquerorsStaff(stack) || isHywScroll(stack);
    }

    public static boolean isHywScroll(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(stack.getItem());
        if (id == null || !HYW_MOD_ID.equals(id.getNamespace())) {
            return false;
        }
        if (CONQUERORS_STAFF.equals(id)) {
            return false;
        }
        String path = id.getPath();
        return path.startsWith("scroll_")
                || path.contains("cannon")
                || path.contains("trebuchet")
                || path.contains("mangonel")
                || path.contains("bombard");
    }

    private static boolean matches(ItemStack stack, ResourceLocation id) {
        if (stack.isEmpty()) {
            return false;
        }
        Item item = stack.getItem();
        ResourceLocation itemId = ForgeRegistries.ITEMS.getKey(item);
        return id.equals(itemId);
    }
}
