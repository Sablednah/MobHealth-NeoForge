package com.sablednah.mobhealth.neoforge;

import com.sablednah.mobhealth.MobHealth;
import com.sablednah.mobhealth.MobHealthConfig;
import com.sablednah.mobhealth.network.GraphicalPolicy;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.server.permission.PermissionAPI;
import net.neoforged.neoforge.server.permission.events.PermissionGatherEvent;
import net.neoforged.neoforge.server.permission.nodes.PermissionNode;
import net.neoforged.neoforge.server.permission.nodes.PermissionTypes;

/**
 * Permission nodes and per-player state for MobHealth.
 *
 * <p>Uses NeoForge's {@link PermissionAPI}. With no permissions manager installed, a node's default
 * resolver is the answer (here: everyone may see). Install a manager such as LuckPerms (which has a
 * NeoForge build) to control {@code mobhealth.see} per group/rank — no extra code required.
 */
public final class MobHealthPermissions {

    private MobHealthPermissions() {}

    /** Whether a player may RECEIVE MobHealth displays at all. Default: everyone. */
    public static final PermissionNode<Boolean> SEE = new PermissionNode<>(
            MobHealth.MODID, "see", PermissionTypes.BOOLEAN,
            (player, playerUUID, context) -> Boolean.TRUE);

    /** NBT key (under the player's persisted tag) for the personal mute toggle. */
    private static final String MUTED_KEY = "mobhealth_muted";

    /** Registered from {@link MobHealthServerEvents} on {@link PermissionGatherEvent.Nodes}. */
    public static void onGatherNodes(PermissionGatherEvent.Nodes event) {
        event.addNodes(SEE);
    }

    /** True if this player is permitted to receive displays (respects LuckPerms if present). */
    public static boolean canSee(ServerPlayer player) {
        return PermissionAPI.getPermission(player, SEE);
    }

    /** True if the player has muted MobHealth's displays for themselves. */
    public static boolean isMuted(ServerPlayer player) {
        return persisted(player).getBoolean(MUTED_KEY).orElse(false);
    }

    /** Set the player's personal mute toggle (persists across sessions and respawns). */
    public static void setMuted(ServerPlayer player, boolean muted) {
        CompoundTag data = player.getPersistentData();
        CompoundTag persisted = data.getCompoundOrEmpty(Player.PERSISTED_NBT_TAG);
        persisted.putBoolean(MUTED_KEY, muted);
        data.put(Player.PERSISTED_NBT_TAG, persisted);
    }

    /** Convenience: this player should receive a targetable display right now. */
    public static boolean receivesDisplays(ServerPlayer player) {
        return !isMuted(player) && canSee(player);
    }

    /**
     * Effective permission for this player to draw GRAPHICAL bars: the server must allow them
     * ({@code graphical} config) and the player must be able to receive displays. Sent to the client
     * so the server can enforce the gate on modded clients.
     */
    public static boolean graphicalAllowedFor(ServerPlayer player) {
        return MobHealthConfig.GRAPHICAL_ALLOWED.get() && receivesDisplays(player);
    }

    /** Build the full graphical policy sent to a client: gate + server-enforced option overrides. */
    public static GraphicalPolicy graphicalPolicyFor(ServerPlayer player) {
        return new GraphicalPolicy(
                graphicalAllowedFor(player),
                MobHealthConfig.ENFORCE_LINE_OF_SIGHT.get().asOverride(),
                MobHealthConfig.ENFORCE_NUMBERS.get().asOverride(),
                MobHealthConfig.ENFORCE_BACKGROUND.get().asOverride(),
                MobHealthConfig.ENFORCE_SHOW_PLAYERS.get().asOverride(),
                MobHealthConfig.ENFORCE_ONLY_WHEN_DAMAGED.get().asOverride(),
                MobHealthConfig.ENFORCE_OFFSET.get() ? MobHealthConfig.ENFORCE_OFFSET_VALUE.get() : null,
                MobHealthConfig.ENFORCE_MAX_DISTANCE.get() ? MobHealthConfig.ENFORCE_MAX_DISTANCE_VALUE.get() : null,
                MobHealthConfig.ENFORCE_BAR_WIDTH.get() ? MobHealthConfig.ENFORCE_BAR_WIDTH_VALUE.get() : null,
                MobHealthConfig.ENFORCE_BAR_HEIGHT.get() ? MobHealthConfig.ENFORCE_BAR_HEIGHT_VALUE.get() : null,
                MobHealthConfig.ENFORCE_SCALE.get() ? MobHealthConfig.ENFORCE_SCALE_VALUE.get() : null,
                MobHealthConfig.ENFORCE_SCALE_WITH_DISTANCE.get().asOverride(),
                MobHealthConfig.ENFORCE_FADE_WITH_DISTANCE.get().asOverride(),
                MobHealthConfig.ENFORCE_BAR_STYLE.get() ? MobHealthConfig.ENFORCE_BAR_STYLE_VALUE.get() : null,
                MobHealthConfig.ENFORCE_SEGMENTS.get() ? MobHealthConfig.ENFORCE_SEGMENTS_VALUE.get() : null);
    }

    private static CompoundTag persisted(ServerPlayer player) {
        return player.getPersistentData().getCompoundOrEmpty(Player.PERSISTED_NBT_TAG);
    }
}
