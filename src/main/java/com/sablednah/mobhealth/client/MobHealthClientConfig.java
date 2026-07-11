package com.sablednah.mobhealth.client;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * CLIENT-side configuration for the graphical floating bars. These settings are per-player and
 * only affect what THIS client draws; they don't touch the server. (Server-enforced allow/deny of
 * graphical bars will be layered on later via a config-sync packet.)
 */
public final class MobHealthClientConfig {

    public static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.BooleanValue ENABLED;
    public static final ModConfigSpec.DoubleValue VERTICAL_OFFSET;
    public static final ModConfigSpec.DoubleValue MAX_DISTANCE;
    public static final ModConfigSpec.IntValue BAR_WIDTH;
    public static final ModConfigSpec.IntValue BAR_HEIGHT;
    public static final ModConfigSpec.DoubleValue SCALE;
    public static final ModConfigSpec.BooleanValue SCALE_WITH_DISTANCE;
    public static final ModConfigSpec.BooleanValue FADE_WITH_DISTANCE;
    public static final ModConfigSpec.BooleanValue SHOW_BACKGROUND;
    public static final ModConfigSpec.BooleanValue SHOW_TEXT;
    public static final ModConfigSpec.BooleanValue SHOW_PLAYERS;
    public static final ModConfigSpec.BooleanValue ONLY_WHEN_DAMAGED;
    public static final ModConfigSpec.BooleanValue REQUIRE_LINE_OF_SIGHT;

    static {
        BUILDER.comment("Graphical floating health bars drawn above mobs (client-side).").push("graphical");
        ENABLED = BUILDER.comment("Master switch for graphical floating bars.").define("enabled", true);
        VERTICAL_OFFSET = BUILDER
                .comment("Extra height (in blocks) above the mob's head to place the bar.")
                .defineInRange("verticalOffset", 0.5D, -2.0D, 6.0D);
        MAX_DISTANCE = BUILDER
                .comment("Only draw bars for mobs within this many blocks of you.")
                .defineInRange("maxDistance", 24.0D, 4.0D, 96.0D);
        BAR_WIDTH = BUILDER.comment("Bar width in pixels (at scale 1.0).").defineInRange("barWidth", 40, 8, 200);
        BAR_HEIGHT = BUILDER.comment("Bar height in pixels (at scale 1.0).").defineInRange("barHeight", 4, 1, 24);
        SCALE = BUILDER.comment("Overall size multiplier for the bar (and its outline). 1.0 = the width/height above.")
                .defineInRange("scale", 1.0D, 0.25D, 4.0D);
        SCALE_WITH_DISTANCE = BUILDER.comment("Shrink bars as the mob gets further away, so they feel anchored in the world.")
                .define("scaleWithDistance", false);
        FADE_WITH_DISTANCE = BUILDER.comment("Fade bars out as the mob approaches maxDistance.")
                .define("fadeWithDistance", false);
        SHOW_BACKGROUND = BUILDER.comment("Draw a dark outline/background behind the bar.").define("showBackground", true);
        SHOW_TEXT = BUILDER.comment("Draw the numeric health above the bar.").define("showText", true);
        SHOW_PLAYERS = BUILDER.comment("Also draw bars above other players.").define("showPlayers", false);
        ONLY_WHEN_DAMAGED = BUILDER.comment("Only draw a bar once the mob is below full health.").define("onlyWhenDamaged", true);
        REQUIRE_LINE_OF_SIGHT = BUILDER
                .comment("Only draw bars for mobs you can actually see (not through walls/terrain).")
                .define("requireLineOfSight", true);
        BUILDER.pop();
    }

    public static final ModConfigSpec SPEC = BUILDER.build();

    private MobHealthClientConfig() {}
}
