package com.cannon.territorybridge.server;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import ydmsama.hundred_years_war.main.item.BaseScrollItem;
import ydmsama.hundred_years_war.main.item.ConquerorsStaffItem;

/** Detects HYW recruitment / summon items by class, not registry path. */
public final class HywMobilizationItems {
    private HywMobilizationItems() {}

    public static boolean isConquerorsStaff(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() instanceof ConquerorsStaffItem;
    }

    public static boolean isMobilizationItem(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        Item item = stack.getItem();
        return item instanceof ConquerorsStaffItem || item instanceof BaseScrollItem;
    }

    public static boolean isHywScroll(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() instanceof BaseScrollItem;
    }
}
