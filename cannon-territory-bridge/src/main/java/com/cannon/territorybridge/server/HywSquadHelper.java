package com.cannon.territorybridge.server;

import ydmsama.hundred_years_war.main.selection.SelectionSystem;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Checks whether a player has created at least one HYW squad slot. */
public final class HywSquadHelper {
    private HywSquadHelper() {}

    public static boolean hasSquad(UUID playerId) {
        if (playerId == null) {
            return false;
        }
        Map<UUID, List<SelectionSystem.Squad>> squads = SelectionSystem.getSquads();
        List<SelectionSystem.Squad> playerSquads = squads.get(playerId);
        if (playerSquads == null || playerSquads.isEmpty()) {
            return false;
        }
        for (SelectionSystem.Squad squad : playerSquads) {
            if (squad != null) {
                return true;
            }
        }
        return false;
    }
}
