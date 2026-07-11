package com.sablednah.mobhealth.network;

import com.sablednah.mobhealth.neoforge.MobHealthPermissions;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/** MobHealth networking: the server-enforced graphical-bar gate. */
public final class MobHealthNetwork {

    private MobHealthNetwork() {}

    /** Registered on the mod event bus (RegisterPayloadHandlersEvent is a mod-bus event). */
    public static void register(RegisterPayloadHandlersEvent event) {
        // optional(): non-MobHealth (e.g. vanilla) clients may connect without this channel.
        PayloadRegistrar registrar = event.registrar("1").optional();
        registrar.playToClient(GraphicalGatePayload.TYPE, GraphicalGatePayload.CODEC, MobHealthNetwork::handleOnClient);
    }

    /**
     * Handles the gate packet. This runs only on the client (it's a play-to-client payload); it
     * touches only {@link GraphicalGateState} (no client-only classes), so referencing this method
     * during registration on a dedicated server is safe.
     */
    private static void handleOnClient(GraphicalGatePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> GraphicalGateState.serverAllowsGraphical = payload.allowed());
    }

    /** Send the player their current effective graphical-bar permission. */
    public static void sync(ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, new GraphicalGatePayload(MobHealthPermissions.graphicalAllowedFor(player)));
    }
}
