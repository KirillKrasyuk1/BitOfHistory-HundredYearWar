package com.cannon.territorybridge.server;

import com.cannon.territorybridge.config.BridgeConfig;
import com.talhanation.recruits.FactionEvents;
import com.talhanation.recruits.world.RecruitsClaim;
import com.talhanation.recruits.world.RecruitsDiplomacyManager;
import com.talhanation.recruits.world.RecruitsFaction;
import net.minecraft.world.entity.LivingEntity;
import ydmsama.hundred_years_war.main.entity.entities.BaseCombatEntity;
import ydmsama.hundred_years_war.main.entity.entities.HywHorseEntity;
import ydmsama.hundred_years_war.main.entity.entities.tags.SiegeUnit;

import java.util.List;
import java.util.UUID;

/** Classifies HYW units for Recruits sieges by owner faction, without scoreboard teams. */
public final class HywSiegeClassifier {
    public enum Role {
        ATTACKER,
        DEFENDER,
        NONE
    }

    private HywSiegeClassifier() {}

    public static void supplementHywUnits(
            List<LivingEntity> entities,
            RecruitsClaim claim,
            List<LivingEntity> attackers,
            List<LivingEntity> defenders
    ) {
        if (!BridgeConfig.SYNC_HYW_TEAMS.get() || claim == null || claim.getOwnerFaction() == null) {
            return;
        }
        for (LivingEntity entity : entities) {
            if (attackers.contains(entity) || defenders.contains(entity)) {
                continue;
            }
            Role role = classify(entity, claim);
            if (role == Role.ATTACKER) {
                attackers.add(entity);
            } else if (role == Role.DEFENDER) {
                defenders.add(entity);
            }
        }
    }

    public static Role classify(LivingEntity entity, RecruitsClaim claim) {
        if (!entity.isAlive() || entity.isRemoved()) {
            return Role.NONE;
        }
        if (!(entity instanceof BaseCombatEntity hyw)) {
            return Role.NONE;
        }
        if (hyw instanceof SiegeUnit && !BridgeConfig.COUNT_HYW_SIEGE_WEAPONS.get()) {
            return Role.NONE;
        }
        if (!BridgeConfig.COUNT_MOUNTED_HORSES_FOR_SIEGE.get() && hyw instanceof HywHorseEntity) {
            return Role.NONE;
        }

        UUID ownerId = hyw.getOwnerUUID();
        if (ownerId == null) {
            return Role.NONE;
        }

        RecruitsFaction unitFaction = HywSiegeHelper.findRecruitsFactionForOwner(ownerId);
        if (unitFaction == null) {
            unitFaction = HywSiegeHelper.findRecruitsFactionByHywTeam(ownerId);
        }
        if (unitFaction == null) {
            return Role.NONE;
        }

        String ownerClaimFactionId = claim.getOwnerFactionStringID();
        if (ownerClaimFactionId == null) {
            return Role.NONE;
        }

        String unitFactionId = unitFaction.getStringID();
        if (unitFactionId.equals(ownerClaimFactionId)) {
            return Role.DEFENDER;
        }

        if (FactionEvents.recruitsDiplomacyManager == null) {
            return Role.NONE;
        }

        RecruitsDiplomacyManager.DiplomacyStatus relation =
                FactionEvents.recruitsDiplomacyManager.getRelation(unitFactionId, ownerClaimFactionId);
        if (relation == RecruitsDiplomacyManager.DiplomacyStatus.ALLY) {
            return Role.DEFENDER;
        }
        if (relation == RecruitsDiplomacyManager.DiplomacyStatus.ENEMY) {
            return Role.ATTACKER;
        }
        return Role.NONE;
    }
}
