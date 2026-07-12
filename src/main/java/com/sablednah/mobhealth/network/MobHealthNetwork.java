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
        registrar.playToClient(ToastPayload.TYPE, ToastPayload.CODEC, MobHealthNetwork::handleToast);
    }

    /**
     * Client-only handler for the toast packet. {@code MobHealthToasts} (which imports client
     * classes) is referenced only inside the enqueued lambda, so it is loaded lazily on first run
     * (client only) and never pulled in on a dedicated server during registration.
     */
    private static void handleToast(ToastPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> com.sablednah.mobhealth.client.MobHealthToasts.show(
                payload.name(), payload.damage(), payload.current(), payload.max()));
    }

    /**
     * Handles the gate packet. This runs only on the client (it's a play-to-client payload); it
     * touches only {@link GraphicalGateState} (no client-only classes), so referencing this method
     * during registration on a dedicated server is safe.
     */
    private static void handleOnClient(GraphicalGatePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> GraphicalGateState.policy = payload.policy());
    }

    /** Send the player their current graphical-bar policy (gate + server-enforced overrides). */
    public static void sync(ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, new GraphicalGatePayload(MobHealthPermissions.graphicalPolicyFor(player)));
    }
}
