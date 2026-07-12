package com.sablednah.mobhealth.network;

import com.sablednah.mobhealth.MobHealth;
import com.sablednah.mobhealth.core.BarStyle;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Server -> client: the {@link GraphicalPolicy} for this player (gate + per-option overrides). */
public record GraphicalGatePayload(GraphicalPolicy policy) implements CustomPacketPayload {

    public static final Type<GraphicalGatePayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(MobHealth.MODID, "graphical_gate"));

    public static final StreamCodec<RegistryFriendlyByteBuf, GraphicalGatePayload> CODEC =
            StreamCodec.of(GraphicalGatePayload::encode, GraphicalGatePayload::decode);

    @Override
    public Type<GraphicalGatePayload> type() {
        return TYPE;
    }

    private static void encode(RegistryFriendlyByteBuf buf, GraphicalGatePayload payload) {
        GraphicalPolicy p = payload.policy();
        buf.writeBoolean(p.allowed());
        writeOptBool(buf, p.requireLineOfSight());
        writeOptBool(buf, p.showText());
        writeOptBool(buf, p.showBackground());
        writeOptBool(buf, p.showPlayers());
        writeOptBool(buf, p.onlyWhenDamaged());
        writeOptDouble(buf, p.verticalOffset());
        writeOptDouble(buf, p.maxDistance());
        writeOptInt(buf, p.barWidth());
        writeOptInt(buf, p.barHeight());
        writeOptDouble(buf, p.scale());
        writeOptBool(buf, p.scaleWithDistance());
        writeOptBool(buf, p.fadeWithDistance());
        writeOptInt(buf, p.barStyle() == null ? null : p.barStyle().ordinal());
        writeOptInt(buf, p.segments());
    }

    private static GraphicalGatePayload decode(RegistryFriendlyByteBuf buf) {
        boolean allowed = buf.readBoolean();
        Boolean requireLos = readOptBool(buf);
        Boolean showText = readOptBool(buf);
        Boolean showBackground = readOptBool(buf);
        Boolean showPlayers = readOptBool(buf);
        Boolean onlyWhenDamaged = readOptBool(buf);
        Double verticalOffset = readOptDouble(buf);
        Double maxDistance = readOptDouble(buf);
        Integer barWidth = readOptInt(buf);
        Integer barHeight = readOptInt(buf);
        Double scale = readOptDouble(buf);
        Boolean scaleWithDistance = readOptBool(buf);
        Boolean fadeWithDistance = readOptBool(buf);
        Integer styleOrdinal = readOptInt(buf);
        BarStyle barStyle = styleOrdinal == null ? null : BarStyle.values()[styleOrdinal];
        Integer segments = readOptInt(buf);
        return new GraphicalGatePayload(new GraphicalPolicy(
                allowed, requireLos, showText, showBackground, showPlayers, onlyWhenDamaged,
                verticalOffset, maxDistance, barWidth, barHeight, scale, scaleWithDistance, fadeWithDistance,
                barStyle, segments));
    }

    private static void writeOptBool(RegistryFriendlyByteBuf buf, Boolean value) {
        buf.writeBoolean(value != null);
        if (value != null) {
            buf.writeBoolean(value);
        }
    }

    private static Boolean readOptBool(RegistryFriendlyByteBuf buf) {
        return buf.readBoolean() ? buf.readBoolean() : null;
    }

    private static void writeOptDouble(RegistryFriendlyByteBuf buf, Double value) {
        buf.writeBoolean(value != null);
        if (value != null) {
            buf.writeDouble(value);
        }
    }

    private static Double readOptDouble(RegistryFriendlyByteBuf buf) {
        return buf.readBoolean() ? buf.readDouble() : null;
    }

    private static void writeOptInt(RegistryFriendlyByteBuf buf, Integer value) {
        buf.writeBoolean(value != null);
        if (value != null) {
            buf.writeVarInt(value);
        }
    }

    private static Integer readOptInt(RegistryFriendlyByteBuf buf) {
        return buf.readBoolean() ? buf.readVarInt() : null;
    }
}
