package com.cannon.economy.deposit;

import net.minecraft.util.StringRepresentable;

public enum DepositType implements StringRepresentable {
    GOLD("gold"),
    IRON("iron"),
    SILVER("silver"),
    GEMS("gems"),
    COAL("coal"),
    DRAGONSTEEL("dragonsteel"),
    FERTILE("fertile");

    private final String id;

    DepositType(String id) {
        this.id = id;
    }

    @Override
    public String getSerializedName() {
        return id;
    }

    public static DepositType byId(String id) {
        for (DepositType type : values()) {
            if (type.id.equalsIgnoreCase(id)) {
                return type;
            }
        }
        return null;
    }
}
