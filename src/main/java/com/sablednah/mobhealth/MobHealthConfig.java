package com.sablednah.mobhealth;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.sablednah.mobhealth.core.Audience;
import com.sablednah.mobhealth.core.BarContent;
import com.sablednah.mobhealth.core.Enforce;
import com.sablednah.mobhealth.core.MobCategory;
import com.sablednah.mobhealth.core.NameplateMode;
import com.sablednah.mobhealth.core.HealthBarFormatter.ValueStyle;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Server/modpack-facing configuration (COMMON: dedicated server + singleplayer).
 *
 * <p>Philosophy: lots of options, sensible defaults. Every display mode and every target group can
 * be toggled independently, with per-entity overrides on top. All modes except {@code graphical}
 * work on unmodified vanilla clients.
 */
public final class MobHealthConfig {

    public static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    // ------------------------------------------------------------------ display modes
    public static final ModConfigSpec.BooleanValue CHAT_ENABLED;
    public static final ModConfigSpec.BooleanValue NAMEPLATE_ENABLED;
    public static final ModConfigSpec.BooleanValue BOSS_BAR_ENABLED;
    public static final ModConfigSpec.BooleanValue GRAPHICAL_ALLOWED;
    public static final ModConfigSpec.EnumValue<Audience> AUDIENCE;
    public static final ModConfigSpec.IntValue NEARBY_RADIUS;

    // ------------------------------------------------------------------ target groups
    public static final ModConfigSpec.BooleanValue SHOW_FOR_HOSTILE;
    public static final ModConfigSpec.BooleanValue SHOW_FOR_NEUTRAL;
    public static final ModConfigSpec.BooleanValue SHOW_FOR_PASSIVE;
    public static final ModConfigSpec.BooleanValue SHOW_FOR_PLAYERS;
    public static final ModConfigSpec.BooleanValue SHOW_FOR_BOSSES;
    public static final ModConfigSpec.BooleanValue HIDE_UNTIL_DAMAGED;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> OVERRIDES;

    // ------------------------------------------------------------------ bar appearance
    public static final ModConfigSpec.IntValue BAR_SEGMENTS;
    public static final ModConfigSpec.ConfigValue<String> BAR_FILLED_CHAR;
    public static final ModConfigSpec.ConfigValue<String> BAR_EMPTY_CHAR;
    public static final ModConfigSpec.EnumValue<ValueStyle> VALUE_STYLE;

    // ------------------------------------------------------------------ boss bar / nameplate
    public static final ModConfigSpec.ConfigValue<String> BOSS_BAR_COLOR;
    public static final ModConfigSpec.EnumValue<NameplateMode> NAMEPLATE_MODE;
    public static final ModConfigSpec.EnumValue<BarContent> NAMEPLATE_CONTENT;
    public static final ModConfigSpec.EnumValue<BarContent> CHAT_CONTENT;

    // ------------------------------------------------------------------ timing (ticks; 20 = 1s)
    public static final ModConfigSpec.IntValue DISPLAY_TICKS;
    public static final ModConfigSpec.IntValue DISPLAY_TICKS_HOSTILE;
    public static final ModConfigSpec.IntValue DISPLAY_TICKS_NEUTRAL;
    public static final ModConfigSpec.IntValue DISPLAY_TICKS_PASSIVE;
    public static final ModConfigSpec.IntValue DEATH_TICKS;

    // ---------------------------------- graphical enforcement (server overrides modded clients)
    public static final ModConfigSpec.EnumValue<Enforce> ENFORCE_LINE_OF_SIGHT;
    public static final ModConfigSpec.EnumValue<Enforce> ENFORCE_NUMBERS;
    public static final ModConfigSpec.EnumValue<Enforce> ENFORCE_BACKGROUND;
    public static final ModConfigSpec.EnumValue<Enforce> ENFORCE_SHOW_PLAYERS;
    public static final ModConfigSpec.EnumValue<Enforce> ENFORCE_ONLY_WHEN_DAMAGED;
    public static final ModConfigSpec.BooleanValue ENFORCE_OFFSET;
    public static final ModConfigSpec.DoubleValue ENFORCE_OFFSET_VALUE;
    public static final ModConfigSpec.BooleanValue ENFORCE_MAX_DISTANCE;
    public static final ModConfigSpec.DoubleValue ENFORCE_MAX_DISTANCE_VALUE;
    public static final ModConfigSpec.BooleanValue ENFORCE_BAR_WIDTH;
    public static final ModConfigSpec.IntValue ENFORCE_BAR_WIDTH_VALUE;
    public static final ModConfigSpec.BooleanValue ENFORCE_BAR_HEIGHT;
    public static final ModConfigSpec.IntValue ENFORCE_BAR_HEIGHT_VALUE;

    static {
        BUILDER.comment("MobHealth — display modes. Enable any combination.").push("display");
        CHAT_ENABLED = BUILDER.comment("Message the viewer with damage dealt and health remaining.").define("chat", true);
        NAMEPLATE_ENABLED = BUILDER.comment("Show a health bar on the mob's name tag (works on vanilla clients).").define("nameplate", true);
        BOSS_BAR_ENABLED = BUILDER.comment("Show a boss-bar widget at the top of the screen (works on vanilla clients).").define("bossBar", false);
        GRAPHICAL_ALLOWED = BUILDER.comment("Allow clients that have MobHealth installed to draw graphical floating bars above mobs.").define("graphical", true);
        AUDIENCE = BUILDER.comment("Who sees the chat / boss bar / graphical displays. NAMEPLATE is a shared name tag and is always visible to everyone nearby.").defineEnum("audience", Audience.ATTACKER);
        NEARBY_RADIUS = BUILDER.comment("When audience = NEARBY, how many blocks away players still receive the display.").defineInRange("nearbyRadius", 32, 4, 128);
        CHAT_CONTENT = BUILDER.comment("What the chat message shows: BAR, NUMBERS, or BOTH.").defineEnum("chatContent", BarContent.BOTH);
        BUILDER.pop();

        BUILDER.comment("Which entities MobHealth reacts to. Per-entity overrides below beat these groups.").push("targets");
        SHOW_FOR_HOSTILE = BUILDER.comment("Hostile monsters (zombies, skeletons, creepers, ...).").define("hostile", true);
        SHOW_FOR_NEUTRAL = BUILDER.comment("Neutral mobs — passive until provoked (endermen, wolves, bees, ...).").define("neutral", true);
        SHOW_FOR_PASSIVE = BUILDER.comment("Passive animals (cows, sheep, chickens, ...).").define("passive", true);
        SHOW_FOR_PLAYERS = BUILDER.comment("Other players (PvP).").define("players", false);
        SHOW_FOR_BOSSES = BUILDER.comment("Boss entities (Ender Dragon, Wither, ...).").define("bosses", true);
        HIDE_UNTIL_DAMAGED = BUILDER.comment("Only show once the mob is below full health (suppresses on a full-health first hit for always-on modes).").define("hideUntilDamaged", true);
        OVERRIDES = BUILDER
                .comment("Per-entity overrides, force on/off regardless of group. Format: \"namespace:id=true|false\".",
                        "Example: [\"minecraft:villager=false\", \"minecraft:ender_dragon=true\"]")
                .defineListAllowEmpty("overrides",
                        List.of("minecraft:armor_stand=false"),
                        () -> "minecraft:example=false",
                        o -> o instanceof String s && s.contains("="));
        BUILDER.pop();

        BUILDER.comment("Appearance of the text bar used by chat and nameplate modes.").push("bar");
        BAR_SEGMENTS = BUILDER.comment("Number of segments in the text bar.").defineInRange("segments", 20, 1, 100);
        BAR_FILLED_CHAR = BUILDER.comment("Glyph for a filled segment.").define("filledChar", "|");
        BAR_EMPTY_CHAR = BUILDER
                .comment("Glyph for a depleted segment.",
                        "Minecraft's font is proportional (a space is wider than '|'), so using a DIFFERENT",
                        "glyph here (e.g. a space) makes the bar change width as health changes.",
                        "Keep this the SAME as filledChar (the default) for a constant-width bar — depleted",
                        "segments are simply shown dimmed.")
                .define("emptyChar", "|");
        VALUE_STYLE = BUILDER.comment("How to render the numeric health readout.").defineEnum("valueStyle", ValueStyle.CURRENT_MAX);
        BUILDER.pop();

        BUILDER.comment("Boss-bar specific options.").push("bossbar");
        BOSS_BAR_COLOR = BUILDER.comment("Boss-bar colour: PINK, BLUE, RED, GREEN, YELLOW, PURPLE, WHITE.").define("color", "RED");
        BUILDER.pop();

        BUILDER.comment("Nameplate specific options.").push("nameplate");
        NAMEPLATE_MODE = BUILDER
                .comment("ON_DAMAGE: show a bar briefly after each hit, then revert.",
                        "NAMED_ONLY: only augment mobs that already have a name tag.",
                        "ALWAYS: show a bar after a hit and keep it (no revert).")
                .defineEnum("mode", NameplateMode.ON_DAMAGE);
        NAMEPLATE_CONTENT = BUILDER
                .comment("What the nameplate shows: BAR (bar only), NUMBERS (health only), or BOTH.")
                .defineEnum("content", BarContent.BOTH);
        BUILDER.pop();

        BUILDER.comment("How long (in ticks, 20 = 1s) nameplate/boss bars stay visible after the last hit.",
                "Per-group values of -1 fall back to the default.").push("timing");
        DISPLAY_TICKS = BUILDER.comment("Default display duration.").defineInRange("displayTicks", 100, 20, 1200);
        DISPLAY_TICKS_HOSTILE = BUILDER.comment("Override for hostile mobs (-1 = use default).").defineInRange("displayTicksHostile", -1, -1, 1200);
        DISPLAY_TICKS_NEUTRAL = BUILDER.comment("Override for neutral mobs (-1 = use default).").defineInRange("displayTicksNeutral", -1, -1, 1200);
        DISPLAY_TICKS_PASSIVE = BUILDER.comment("Override for passive mobs (-1 = use default).").defineInRange("displayTicksPassive", -1, -1, 1200);
        DEATH_TICKS = BUILDER
                .comment("How long a boss bar / nameplate lingers after the mob DIES (0 = remove next tick).",
                        "Kept short so an empty boss bar doesn't hang around after a kill.")
                .defineInRange("deathTicks", 20, 0, 600);
        BUILDER.pop();

        BUILDER.comment("Server enforcement of the CLIENT-side graphical bar options.",
                "These let the server override what modded clients draw. Boolean options use:",
                "  CLIENT = let each client decide (their client config wins)",
                "  ON     = force the option on for everyone",
                "  OFF    = force the option off for everyone",
                "Numeric options: set enforce<X> = true to force <x>Value on all clients.").push("graphicalEnforce");
        ENFORCE_LINE_OF_SIGHT = BUILDER.comment("Only draw bars for mobs the player can see.").defineEnum("lineOfSight", Enforce.CLIENT);
        ENFORCE_NUMBERS = BUILDER.comment("Show the numeric health above graphical bars.").defineEnum("numbers", Enforce.CLIENT);
        ENFORCE_BACKGROUND = BUILDER.comment("Dark outline/background behind graphical bars.").defineEnum("background", Enforce.CLIENT);
        ENFORCE_SHOW_PLAYERS = BUILDER.comment("Draw graphical bars above other players.").defineEnum("showPlayers", Enforce.CLIENT);
        ENFORCE_ONLY_WHEN_DAMAGED = BUILDER.comment("Only draw a graphical bar once the mob is hurt.").defineEnum("onlyWhenDamaged", Enforce.CLIENT);
        ENFORCE_OFFSET = BUILDER.comment("Force the graphical bar's vertical offset (blocks above head).").define("enforceVerticalOffset", false);
        ENFORCE_OFFSET_VALUE = BUILDER.comment("Forced vertical offset when enforceVerticalOffset = true.").defineInRange("verticalOffsetValue", 0.5D, -2.0D, 6.0D);
        ENFORCE_MAX_DISTANCE = BUILDER.comment("Force the graphical bar draw distance (blocks).").define("enforceMaxDistance", false);
        ENFORCE_MAX_DISTANCE_VALUE = BUILDER.comment("Forced max distance when enforceMaxDistance = true.").defineInRange("maxDistanceValue", 24.0D, 4.0D, 96.0D);
        ENFORCE_BAR_WIDTH = BUILDER.comment("Force the graphical bar width (pixels).").define("enforceBarWidth", false);
        ENFORCE_BAR_WIDTH_VALUE = BUILDER.comment("Forced bar width when enforceBarWidth = true.").defineInRange("barWidthValue", 40, 8, 200);
        ENFORCE_BAR_HEIGHT = BUILDER.comment("Force the graphical bar height (pixels).").define("enforceBarHeight", false);
        ENFORCE_BAR_HEIGHT_VALUE = BUILDER.comment("Forced bar height when enforceBarHeight = true.").defineInRange("barHeightValue", 4, 1, 24);
        BUILDER.pop();
    }

    public static final ModConfigSpec SPEC = BUILDER.build();

    private MobHealthConfig() {}

    // ------------------------------------------------------------------ derived helpers

    /** Is the given group currently enabled? */
    public static boolean groupEnabled(MobCategory category) {
        return switch (category) {
            case HOSTILE -> SHOW_FOR_HOSTILE.get();
            case NEUTRAL -> SHOW_FOR_NEUTRAL.get();
            case PASSIVE -> SHOW_FOR_PASSIVE.get();
            case PLAYER -> SHOW_FOR_PLAYERS.get();
            case OTHER -> false;
        };
    }

    /** Display duration in ticks for the given group, honouring per-group overrides. */
    public static int displayTicks(MobCategory category) {
        int override = switch (category) {
            case HOSTILE -> DISPLAY_TICKS_HOSTILE.get();
            case NEUTRAL -> DISPLAY_TICKS_NEUTRAL.get();
            case PASSIVE -> DISPLAY_TICKS_PASSIVE.get();
            default -> -1;
        };
        return override >= 0 ? override : DISPLAY_TICKS.get();
    }

    /**
     * Resolve a per-entity override for the given registry id (e.g. {@code "minecraft:zombie"}).
     *
     * @return {@code Boolean.TRUE}/{@code FALSE} if an override exists, or {@code null} if none.
     */
    public static Boolean overrideFor(String entityId) {
        return overrideMap().get(entityId.toLowerCase(Locale.ROOT));
    }

    private static Map<String, Boolean> overrideMap() {
        Map<String, Boolean> map = new HashMap<>();
        for (String entry : OVERRIDES.get()) {
            int eq = entry.indexOf('=');
            if (eq <= 0) {
                continue;
            }
            String id = entry.substring(0, eq).trim().toLowerCase(Locale.ROOT);
            boolean value = Boolean.parseBoolean(entry.substring(eq + 1).trim());
            if (!id.isEmpty()) {
                map.put(id, value);
            }
        }
        return map;
    }
}
