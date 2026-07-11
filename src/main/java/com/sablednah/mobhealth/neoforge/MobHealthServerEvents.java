package com.sablednah.mobhealth.neoforge;

import com.sablednah.mobhealth.network.MobHealthNetwork;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.server.permission.events.PermissionGatherEvent;

/**
 * Server-side registrations that live on the NeoForge game event bus: permission nodes and commands.
 * Registered from {@link com.sablednah.mobhealth.MobHealth}.
 */
public final class MobHealthServerEvents {

    private MobHealthServerEvents() {}

    @SubscribeEvent
    public static void onGatherPermissionNodes(PermissionGatherEvent.Nodes event) {
        MobHealthPermissions.onGatherNodes(event);
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        MobHealthCommands.register(event.getDispatcher());
    }

    /** Tell the joining client its effective graphical-bar permission. */
    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            MobHealthNetwork.sync(player);
        }
    }
}
