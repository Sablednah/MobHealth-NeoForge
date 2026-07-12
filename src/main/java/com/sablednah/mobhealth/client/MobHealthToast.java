package com.sablednah.mobhealth.client;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.client.gui.components.toasts.ToastManager;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

/**
 * A MobHealth toast popup (the top-right achievement-style notification): a small red heart plus
 * the mob's name and remaining health / damage dealt. Reuses the vanilla system-toast background.
 *
 * <p>All MobHealth toasts share one {@link #TOKEN}, so repeated hits refresh a single toast instead
 * of stacking a new one each time.
 */
public class MobHealthToast implements Toast {

    /** Shared token so {@code getToast(MobHealthToast.class, TOKEN)} finds and refreshes the one toast. */
    public static final Object TOKEN = new Object();

    // The advancement/achievement frame — a clean box with an icon slot on the left (the "system"
    // toast has a gold "!" baked into its corner, so we use this one).
    private static final Identifier BACKGROUND = Identifier.withDefaultNamespace("toast/advancement");
    private static final long DISPLAY_TIME_MS = 5000L;

    // 7x6 heart, 1 = red pixel.
    private static final int[][] HEART = {
        {0, 1, 1, 0, 1, 1, 0},
        {1, 1, 1, 1, 1, 1, 1},
        {1, 1, 1, 1, 1, 1, 1},
        {0, 1, 1, 1, 1, 1, 0},
        {0, 0, 1, 1, 1, 0, 0},
        {0, 0, 0, 1, 0, 0, 0},
    };

    private Component title;
    private Component message;
    private ItemStack icon;
    private long lastChanged;
    private boolean changed = true;
    private Visibility wantedVisibility = Visibility.HIDE;

    public MobHealthToast(Component title, Component message, ItemStack icon) {
        this.title = title;
        this.message = message;
        this.icon = icon;
    }

    /** Update the text/icon and restart the display timer (called when the same mob is hit again). */
    public void refresh(Component title, Component message, ItemStack icon) {
        this.title = title;
        this.message = message;
        this.icon = icon;
        this.changed = true;
    }

    @Override
    public Object getToken() {
        return TOKEN;
    }

    @Override
    public Visibility getWantedVisibility() {
        return wantedVisibility;
    }

    @Override
    public void update(ToastManager manager, long time) {
        if (changed) {
            lastChanged = time;
            changed = false;
        }
        double visibleFor = DISPLAY_TIME_MS * manager.getNotificationDisplayTimeMultiplier();
        wantedVisibility = (time - lastChanged) < visibleFor ? Visibility.SHOW : Visibility.HIDE;
    }

    @Override
    public void render(GuiGraphics graphics, Font font, long time) {
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, BACKGROUND, 0, 0, width(), height());
        if (icon != null && !icon.isEmpty()) {
            graphics.renderItem(icon, 8, 8); // the weapon/arrow that dealt the damage
        } else {
            drawHeart(graphics, 8, 10, 2); // fallback
        }
        graphics.drawString(font, title, 30, 7, 0xFFFFFF00, false);
        graphics.drawString(font, message, 30, 18, 0xFFFFFFFF, false);
    }

    private static void drawHeart(GuiGraphics graphics, int ox, int oy, int scale) {
        for (int row = 0; row < HEART.length; row++) {
            for (int col = 0; col < HEART[row].length; col++) {
                if (HEART[row][col] == 1) {
                    int x = ox + col * scale;
                    int y = oy + row * scale;
                    graphics.fill(x, y, x + scale, y + scale, 0xFFFF3333);
                }
            }
        }
    }
}
