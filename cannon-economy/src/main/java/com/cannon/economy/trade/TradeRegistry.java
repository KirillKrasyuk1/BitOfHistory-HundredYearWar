package com.cannon.economy.trade;

import com.cannon.economy.CannonEconomy;

public final class TradeRegistry {
    private TradeRegistry() {}

    public static void loadDefaults() {
        CannonEconomy.LOGGER.info("Trade route registry ready.");
    }
}
