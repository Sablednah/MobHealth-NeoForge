package com.sablednah.mobhealth.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.ToastManager;
import net.minecraft.network.chat.Component;

/**
 * Client-side entry point for showing the MobHealth toast. Called from the network handler when a
 * {@link com.sablednah.mobhealth.network.ToastPayload} arrives. Only ever loaded on the client.
 */
public final class MobHealthToasts {

    private MobHealthToasts() {}

    public static void show(String name, float damage, float current, float max) {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null) {
            return;
        }
        ToastManager manager = mc.getToastManager();
        Component title = Component.literal(name);
        Component message = Component.literal(trim(current) + "/" + trim(max) + "  (-" + trim(damage) + ")");

        MobHealthToast existing = manager.getToast(MobHealthToast.class, MobHealthToast.TOKEN);
        if (existing == null) {
            manager.addToast(new MobHealthToast(title, message));
        } else {
            existing.refresh(title, message);
        }
    }

    private static String trim(float v) {
        if (v == Math.floor(v) && !Float.isInfinite(v)) {
            return Long.toString((long) v);
        }
        return String.valueOf(Math.round(v * 10.0F) / 10.0F);
    }
}
