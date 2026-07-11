package com.sablednah.mobhealth.network;

import com.sablednah.mobhealth.MobHealth;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Server -> client: whether this client is allowed to draw graphical floating bars. */
public record GraphicalGatePayload(boolean allowed) implements CustomPacketPayload {

    public static final Type<GraphicalGatePayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(MobHealth.MODID, "graphical_gate"));

    public static final StreamCodec<ByteBuf, GraphicalGatePayload> CODEC =
            ByteBufCodecs.BOOL.map(GraphicalGatePayload::new, GraphicalGatePayload::allowed);

    @Override
    public Type<GraphicalGatePayload> type() {
        return TYPE;
    }
}
