package com.cannon.economy.hyw;

import com.cannon.economy.CannonEconomy;
import com.cannon.economy.config.EconomyConfig;
import net.minecraftforge.fml.ModList;

/**
 * Applies Cannon economy balance to HYW global modifiers after both mods load.
 */
public final class HywIntegration {
    private HywIntegration() {}

    public static void applySupplyMultiplier() {
        if (!ModList.get().isLoaded("hundred_years_war")) {
            return;
        }
        try {
            Class<?> cfg = Class.forName("ydmsama.hundred_years_war.main.config.HywGlobalModifierConfig");
            Object instance = cfg.getField("INSTANCE").get(null);
            double mult = EconomyConfig.SUPPLY_CONSUMPTION_MULTIPLIER.get();
            cfg.getField("globalSupplyConsumptionMultiplier").set(instance, mult);
            cfg.getMethod("save").invoke(null);
            CannonEconomy.LOGGER.info("HYW globalSupplyConsumptionMultiplier set to {}", mult);
        } catch (ReflectiveOperationException e) {
            CannonEconomy.LOGGER.warn("Could not apply HYW supply multiplier: {}", e.getMessage());
        }
    }
}
