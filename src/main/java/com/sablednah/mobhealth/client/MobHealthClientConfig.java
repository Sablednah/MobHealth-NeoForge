package com.sablednah.mobhealth.client;

import com.sablednah.mobhealth.core.BarStyle;

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
    public static final ModConfigSpec.EnumValue<BarStyle> BAR_STYLE;
    public static final ModConfigSpec.IntValue GRAPHICAL_SEGMENTS;
    public static final ModConfigSpec.DoubleValue SCALE;
    public static final ModConfigSpec.BooleanValue SCALE_WITH_DISTANCE;
    public static final ModConfigSpec.BooleanValue FADE_WITH_DISTANCE;
    public static final ModConfigSpec.BooleanValue SHOW_BACKGROUND;
    public static final ModConfigSpec.BooleanValue SHOW_TEXT;
    public static final ModConfigSpec.BooleanValue SHOW_PLAYERS;
    public static final ModConfigSpec.BooleanValue ONLY_WHEN_DAMAGED;
    public static final ModConfigSpec.BooleanValue REQUIRE_LINE_OF_SIGHT;

    // ------------------------------------------------------------------ floating damage numbers
    public static final ModConfigSpec.BooleanValue INDICATORS_ENABLED;
    public static final ModConfigSpec.DoubleValue INDICATOR_SCALE;
    public static final ModConfigSpec.IntValue INDICATOR_DURATION_MS;
    public static final ModConfigSpec.DoubleValue INDICATOR_DRIFT;
    public static final ModConfigSpec.DoubleValue INDICATOR_VERTICAL_OFFSET;
    public static final ModConfigSpec.DoubleValue INDICATOR_SPREAD;
    public static final ModConfigSpec.DoubleValue INDICATOR_MAX_DISTANCE;
    public static final ModConfigSpec.DoubleValue INDICATOR_MIN_DAMAGE;
    public static final ModConfigSpec.IntValue INDICATOR_MAX_ON_SCREEN;
    public static final ModConfigSpec.BooleanValue INDICATOR_HEARTS;
    public static final ModConfigSpec.ConfigValue<String> INDICATOR_COLOR;
    public static final ModConfigSpec.ConfigValue<String> INDICATOR_FATAL_COLOR;

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
        BAR_STYLE = BUILDER.comment("Bar shape: SOLID, ROUNDED, SEGMENTED, or TAPERED (lens).").defineEnum("barStyle", BarStyle.SOLID);
        GRAPHICAL_SEGMENTS = BUILDER.comment("Number of chunks for the SEGMENTED style.").defineInRange("segments", 10, 2, 50);
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

        BUILDER.comment("Floating damage numbers that pop off a mob when you hit it (client-side).",
                "The server decides whether to send them at all ([display] damageIndicators);",
                "everything here is how THIS client draws the ones it receives.").push("damageIndicators");
        INDICATORS_ENABLED = BUILDER.comment("Master switch for floating damage numbers.").define("enabled", true);
        INDICATOR_SCALE = BUILDER.comment("Size multiplier for the number. 1.0 = normal font size.")
                .defineInRange("scale", 2.0D, 0.25D, 4.0D);
        INDICATOR_DURATION_MS = BUILDER.comment("How long each number lives, in milliseconds.")
                .defineInRange("durationMs", 1200, 200, 5000);
        INDICATOR_DRIFT = BUILDER.comment("How far the number rises over its life, in screen pixels.")
                .defineInRange("drift", 16.0D, 0.0D, 96.0D);
        INDICATOR_VERTICAL_OFFSET = BUILDER
                .comment("Extra height (in blocks) above where the hit landed to start the number.",
                        "The server anchors it at upper-body height, below the floating health bar.")
                .defineInRange("verticalOffset", 0.0D, -2.0D, 4.0D);
        INDICATOR_SPREAD = BUILDER
                .comment("Random scatter (in blocks) applied to each number, so a burst of hits reads",
                        "as several numbers instead of one flickering in place. 0 = no scatter.")
                .defineInRange("spread", 0.6D, 0.0D, 3.0D);
        INDICATOR_MAX_DISTANCE = BUILDER.comment("Only draw numbers within this many blocks of you.")
                .defineInRange("maxDistance", 32.0D, 4.0D, 96.0D);
        INDICATOR_MIN_DAMAGE = BUILDER
                .comment("Ignore hits below this much damage (health points). 0 = show everything.")
                .defineInRange("minDamage", 0.0D, 0.0D, 100.0D);
        INDICATOR_MAX_ON_SCREEN = BUILDER
                .comment("Cap on numbers alive at once; the oldest are dropped first.")
                .defineInRange("maxOnScreen", 40, 4, 200);
        INDICATOR_HEARTS = BUILDER
                .comment("Show damage in HEARTS rather than raw health points (2 points = 1 heart).")
                .define("hearts", false);
        INDICATOR_COLOR = BUILDER
                .comment("Colour of an ordinary damage number, as RRGGBB hex.",
                        "Named numberColor rather than color because the config screen looks options up",
                        "by their last path element alone, and [bossbar] already owns \"color\".")
                .define("numberColor", "FF5555");
        INDICATOR_FATAL_COLOR = BUILDER
                .comment("Colour of the killing blow, which is also drawn bold and larger.")
                .define("fatalColor", "FFAA00");
        BUILDER.pop();
    }

    /** Ordinary damage colour, as 0xRRGGBB. Falls back to the default red if the hex is unusable. */
    public static int indicatorColor() {
        return parseColor(INDICATOR_COLOR.get(), 0xFF5555);
    }

    /** Killing-blow colour, as 0xRRGGBB. Falls back to the default amber if the hex is unusable. */
    public static int indicatorFatalColor() {
        return parseColor(INDICATOR_FATAL_COLOR.get(), 0xFFAA00);
    }

    /**
     * A hand-edited colour is a hand-typed colour, so a leading '#' and stray spaces are accepted
     * and anything unparseable falls back rather than throwing in the middle of a render pass.
     */
    private static int parseColor(String value, int fallback) {
        if (value == null) {
            return fallback;
        }
        String hex = value.trim();
        if (hex.startsWith("#")) {
            hex = hex.substring(1);
        }
        try {
            return Integer.parseInt(hex, 16) & 0x00FFFFFF;
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    public static final ModConfigSpec SPEC = BUILDER.build();

    private MobHealthClientConfig() {}
}
