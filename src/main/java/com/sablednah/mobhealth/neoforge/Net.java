package com.sablednah.mobhealth.neoforge;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * The one clientbound send. Every MobHealth payload goes through here, never a bare
 * {@code PacketDistributor.sendToPlayer}.
 *
 * <p>{@code PayloadRegistrar.optional()} makes the <em>handshake</em> tolerant, so vanilla clients get
 * in; it does NOT make sends droppable. NeoForge's patched {@code send} runs
 * {@code NetworkRegistry.checkPacket}, which throws, synchronously, on the server thread, for a payload
 * the receiver never negotiated. From the login handler that kicks the vanilla player ("Invalid player
 * data"); from the damage event it throws for any vanilla player standing near a hit. Channels are
 * agreed in the configuration phase, so no later event helps: guard permanently. The same helper the
 * rest of Sable's mods carry (Chronicler, LegendQuest, ZombieMod).</p>
 */
public final class Net {

    private Net() {}

    public static void sendIfAble(ServerPlayer player, CustomPacketPayload payload) {
        if (listening(player, payload.type())) {
            PacketDistributor.sendToPlayer(player, payload);
        }
    }

    /**
     * Is this player running MobHealth's client half? Vanilla says no, and so does a fake player. Not
     * just a null check: NeoForge's {@code FakePlayer} HAS a connection whose netty channel is null, and
     * {@code hasChannel} throws on it. {@code isFakePlayer()} rather than an instanceof, so subclasses
     * count; {@code isConnected()} for a mod that hand-rolls a fake ServerPlayer without NeoForge's class.
     */
    public static boolean listening(ServerPlayer player, CustomPacketPayload.Type<?> type) {
        return player.connection != null && !player.isFakePlayer()
                && player.connection.getConnection().isConnected()
                && player.connection.hasChannel(type);
    }
}
