package com.cannon.territorybridge.config;

import net.minecraftforge.common.ForgeConfigSpec;

public final class BridgeConfig {
    public static final ForgeConfigSpec SPEC;

    public static final ForgeConfigSpec.BooleanValue SYNC_HYW_TEAMS;
    public static final ForgeConfigSpec.BooleanValue SYNC_DIPLOMACY_TO_HYW;
    public static final ForgeConfigSpec.BooleanValue BLOCK_RECRUITS_ENTITIES;
    public static final ForgeConfigSpec.BooleanValue BLOCK_RECRUIT_HIRE;
    public static final ForgeConfigSpec.BooleanValue BLOCK_VILLAGER_CLAIM_TAKEOVER;
    public static final ForgeConfigSpec.BooleanValue BLOCK_RECRUIT_COMMAND_UI;
    public static final ForgeConfigSpec.BooleanValue COUNT_MOUNTED_HORSES_FOR_SIEGE;
    public static final ForgeConfigSpec.BooleanValue COUNT_HYW_SIEGE_WEAPONS;
    public static final ForgeConfigSpec.IntValue MIN_CAPTURE_MINUTES;
    public static final ForgeConfigSpec.BooleanValue APPLY_SIEGE_SPEED_TO_DAMAGE;
    public static final ForgeConfigSpec.BooleanValue REQUIRE_ATTACKER_ADVANTAGE;
    public static final ForgeConfigSpec.BooleanValue SHOW_SIEGE_TIMER;
    public static final ForgeConfigSpec.BooleanValue REQUIRE_OWN_CLAIM_TO_MOBILIZE;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

        builder.comment("HYW ↔ Recruits integration for claim sieges.").push("hyw_sync");
        SYNC_HYW_TEAMS = builder
                .comment("Count HYW soldiers for Recruits sieges via the owner's faction team (read-only; does not modify entities).")
                .define("syncHywTeams", true);
        SYNC_DIPLOMACY_TO_HYW = builder
                .comment("Mirror Recruits faction diplomacy into HYW RelationSystem.")
                .define("syncDiplomacyToHyw", true);
        COUNT_MOUNTED_HORSES_FOR_SIEGE = builder
                .comment("If false, HywHorseEntity is not counted for sieges (rider still counts).")
                .define("countMountedHorsesForSiege", false);
        COUNT_HYW_SIEGE_WEAPONS = builder
                .comment("Count HYW cannons/trebuchets (SiegeUnit) for claim siege attacker/defender totals.")
                .define("countHywSiegeWeapons", true);
        MIN_CAPTURE_MINUTES = builder
                .comment("Minimum wall-clock minutes to capture a claim once a siege is active (Recruits ticks every 5 s).")
                .defineInRange("minCaptureMinutes", 3, 1, 60);
        APPLY_SIEGE_SPEED_TO_DAMAGE = builder
                .comment("Apply Recruits attacker/defender ratio to siege damage (defenders slow capture; none = faster).")
                .define("applySiegeSpeedToDamage", true);
        REQUIRE_ATTACKER_ADVANTAGE = builder
                .comment("Claim HP does not drop while defenders >= attackers (garrison holds the line). Empty claims (0 defenders) can still be captured.")
                .define("requireAttackerAdvantage", true);
        SHOW_SIEGE_TIMER = builder
                .comment("Show remaining siege time and claim HP as on-screen text while you are inside a claim under siege.")
                .define("showSiegeTimer", true);
        REQUIRE_OWN_CLAIM_TO_MOBILIZE = builder
                .comment("HYW army mobilization (Conqueror's Staff and scrolls) only inside your own Recruits claim chunks.")
                .define("requireOwnClaimToMobilize", true);
        builder.pop();

        builder.comment("Strip Recruits settlement / army mechanics — keep claims + diplomacy only.").push("recruits_strip");
        BLOCK_RECRUITS_ENTITIES = builder
                .comment("Prevent Recruits mod NPCs (recruits, nobles, patrols) from spawning.")
                .define("blockRecruitsEntities", true);
        BLOCK_RECRUIT_HIRE = builder
                .comment("Cancel hiring Recruits mod villagers.")
                .define("blockRecruitHire", true);
        BLOCK_VILLAGER_CLAIM_TAKEOVER = builder
                .comment("Do not auto-assign vanilla villagers to claim owner faction.")
                .define("blockVillagerClaimTakeover", true);
        BLOCK_RECRUIT_COMMAND_UI = builder
                .comment("Block recruit command / hire screens on the client.")
                .define("blockRecruitCommandUi", true);
        builder.pop();

        SPEC = builder.build();
    }

    private BridgeConfig() {}
}
