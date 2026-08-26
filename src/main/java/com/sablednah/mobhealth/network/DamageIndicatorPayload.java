package com.sablednah.mobhealth.network;

import com.sablednah.mobhealth.MobHealth;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Server -> client: pop a floating damage number at a world position.
 *
 * <p>The client cannot work this out for itself. It sees the victim's health drop, but a drop is
 * not a hit — absorption, regeneration and healing all move the same number, and the health it
 * reads is already the post-hit value. The amount that was actually dealt only exists on the
 * server, so it is sent.
 *
 * <p>{@code fatal} marks the blow that took the victim to zero, which the client draws differently.
 */
public record DamageIndicatorPayload(double x, double y, double z, float amount, boolean fatal)
        implements CustomPacketPayload {

    public static final Type<DamageIndicatorPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(MobHealth.MODID, "damage_indicator"));

    public static final StreamCodec<RegistryFriendlyByteBuf, DamageIndicatorPayload> CODEC = StreamCodec.of(
            (buf, p) -> {
                buf.writeDouble(p.x);
                buf.writeDouble(p.y);
                buf.writeDouble(p.z);
                buf.writeFloat(p.amount);
                buf.writeBoolean(p.fatal);
            },
            buf -> new DamageIndicatorPayload(buf.readDouble(), buf.readDouble(), buf.readDouble(),
                    buf.readFloat(), buf.readBoolean()));

    @Override
    public Type<DamageIndicatorPayload> type() {
        return TYPE;
    }
}
