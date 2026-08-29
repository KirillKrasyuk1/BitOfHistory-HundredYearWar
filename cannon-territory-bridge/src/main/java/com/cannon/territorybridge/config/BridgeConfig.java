package com.cannon.territorybridge.config;

import net.minecraftforge.common.ForgeConfigSpec;

public final class BridgeConfig {
    public static final ForgeConfigSpec SPEC;

    public static final ForgeConfigSpec.BooleanValue SYNC_HYW_TEAMS;
    public static final ForgeConfigSpec.IntValue TEAM_SYNC_INTERVAL_TICKS;
    public static final ForgeConfigSpec.IntValue TEAM_SYNC_RADIUS;
    public static final ForgeConfigSpec.BooleanValue SYNC_DIPLOMACY_TO_HYW;
    public static final ForgeConfigSpec.BooleanValue BLOCK_RECRUITS_ENTITIES;
    public static final ForgeConfigSpec.BooleanValue BLOCK_RECRUIT_HIRE;
    public static final ForgeConfigSpec.BooleanValue BLOCK_VILLAGER_CLAIM_TAKEOVER;
    public static final ForgeConfigSpec.BooleanValue BLOCK_RECRUIT_COMMAND_UI;
    public static final ForgeConfigSpec.BooleanValue COUNT_MOUNTED_HORSES_FOR_SIEGE;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

        builder.comment("HYW unit → Recruits faction team sync (required for sieges with HYW armies).").push("hyw_sync");
        SYNC_HYW_TEAMS = builder
                .comment("Assign HYW combat units to their owner's Recruits scoreboard team.")
                .define("syncHywTeams", true);
        TEAM_SYNC_INTERVAL_TICKS = builder
                .comment("How often (server ticks) to re-sync HYW units near online players.")
                .defineInRange("teamSyncIntervalTicks", 200, 20, 1200);
        TEAM_SYNC_RADIUS = builder
                .comment("Block radius around each player for HYW team re-sync (smaller = less lag).")
                .defineInRange("teamSyncRadius", 128, 32, 512);
        SYNC_DIPLOMACY_TO_HYW = builder
                .comment("Mirror Recruits faction diplomacy into HYW RelationSystem.")
                .define("syncDiplomacyToHyw", true);
        COUNT_MOUNTED_HORSES_FOR_SIEGE = builder
                .comment("If false, HywHorseEntity is not added to the faction team (rider still counts).")
                .define("countMountedHorsesForSiege", false);
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
