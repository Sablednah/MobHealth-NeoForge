package com.sablednah.mobhealth.neoforge;

import com.sablednah.mobhealth.MobHealthConfig;
import com.sablednah.mobhealth.core.BarContent;
import com.sablednah.mobhealth.core.HealthBarFormatter;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

/**
 * Builds the coloured text bar and readouts shown by the chat and nameplate modes, from the
 * live config values. Bridges the loader-agnostic {@link HealthBarFormatter} to Minecraft's
 * {@link Component} styling.
 */
public final class BarText {

    private BarText() {}

    /** A coloured, bracketed bar such as {@code [||||||||||||||      ]} tinted by health fraction. */
    public static MutableComponent bar(double current, double max) {
        int segments = MobHealthConfig.BAR_SEGMENTS.get();
        char filledChar = firstChar(MobHealthConfig.BAR_FILLED_CHAR.get(), '|');
        char emptyChar = firstChar(MobHealthConfig.BAR_EMPTY_CHAR.get(), ' ');

        int filled = HealthBarFormatter.filledSegments(current, max, segments);
        String filledStr = repeat(filledChar, filled);
        String emptyStr = repeat(emptyChar, Math.max(0, segments - filled));

        ChatFormatting fillColor = colorFor(HealthBarFormatter.fraction(current, max));

        MutableComponent bar = Component.literal("[").withStyle(ChatFormatting.GRAY);
        bar.append(Component.literal(filledStr).withStyle(fillColor));
        if (!emptyStr.isEmpty()) {
            bar.append(Component.literal(emptyStr).withStyle(ChatFormatting.DARK_GRAY));
        }
        bar.append(Component.literal("]").withStyle(ChatFormatting.GRAY));
        return bar;
    }

    /** The numeric readout (e.g. {@code 14/20} or {@code 70%}) per the configured value style, or empty. */
    public static MutableComponent value(double current, double max) {
        String text = HealthBarFormatter.value(current, max, MobHealthConfig.VALUE_STYLE.get());
        return text.isEmpty() ? Component.empty() : Component.literal(text).withStyle(ChatFormatting.WHITE);
    }

    /** {@code [bar] value} — the combined widget used on nameplates and in boss-bar titles. */
    public static MutableComponent barWithValue(double current, double max) {
        MutableComponent out = bar(current, max);
        MutableComponent val = value(current, max);
        if (!val.getString().isEmpty()) {
            out.append(Component.literal(" ")).append(val);
        }
        return out;
    }

    /** Render bar, numbers, or both according to the given content style. */
    public static MutableComponent content(double current, double max, BarContent content) {
        return switch (content) {
            case BAR -> bar(current, max);
            case NUMBERS -> value(current, max);
            case BOTH -> barWithValue(current, max);
        };
    }

    private static ChatFormatting colorFor(double fraction) {
        if (fraction > 0.5D) {
            return ChatFormatting.GREEN;
        }
        if (fraction > 0.25D) {
            return ChatFormatting.YELLOW;
        }
        return ChatFormatting.RED;
    }

    private static char firstChar(String s, char fallback) {
        return (s == null || s.isEmpty()) ? fallback : s.charAt(0);
    }

    private static String repeat(char c, int count) {
        if (count <= 0) {
            return "";
        }
        char[] chars = new char[count];
        java.util.Arrays.fill(chars, c);
        return new String(chars);
    }
}
