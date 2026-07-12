package com.sablednah.mobhealth.network;

import com.sablednah.mobhealth.MobHealth;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

/**
 * Server -> client: show/refresh a MobHealth toast popup for a hit (mob name + damage + health,
 * plus the item that dealt the damage as the icon; an empty stack means "use the heart fallback").
 */
public record ToastPayload(String name, float damage, float current, float max, ItemStack icon)
        implements CustomPacketPayload {

    public static final Type<ToastPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(MobHealth.MODID, "toast"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ToastPayload> CODEC = StreamCodec.of(
            (buf, p) -> {
                buf.writeUtf(p.name);
                buf.writeFloat(p.damage);
                buf.writeFloat(p.current);
                buf.writeFloat(p.max);
                ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, p.icon);
            },
            buf -> new ToastPayload(buf.readUtf(), buf.readFloat(), buf.readFloat(), buf.readFloat(),
                    ItemStack.OPTIONAL_STREAM_CODEC.decode(buf)));

    @Override
    public Type<ToastPayload> type() {
        return TYPE;
    }
}
