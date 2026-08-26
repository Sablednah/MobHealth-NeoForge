package com.sablednah.mobhealth.client;

import java.util.Iterator;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;

import org.joml.Matrix4f;
import org.joml.Vector4f;

import com.sablednah.mobhealth.network.DamageIndicatorPayload;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

/**
 * Floating damage numbers — red digits that pop off a mob when you hit it, drift upward and fade.
 *
 * <p>Same projection recipe as {@link GraphicalBarRenderer}: the 1.21.11 pipeline renders the world
 * several times per frame and exposes no readable projection matrix, so everything happens once per
 * frame in the GUI pass. Rebuild the exact view matrix Minecraft uses, combine it with the
 * projection, and project the number's world position to screen pixels.
 *
 * <p>Unlike the health bars, these are events rather than state: the server sends one packet per
 * hit ({@link DamageIndicatorPayload}) and each number lives out its short life here. The list is
 * copy-on-write because packets arrive on the network thread and are handed over by
 * {@code enqueueWork}, while the render pass walks it every frame.
 */
public final class DamageIndicators {

    private DamageIndicators() {}

    /** One floating number. World position is fixed at birth; the drift is applied in screen space. */
    private record Floater(double x, double y, double z, String text, boolean fatal, long born) {}

    private static final CopyOnWriteArrayList<Floater> ACTIVE = new CopyOnWriteArrayList<>();
    private static final Random RANDOM = new Random();

    /** Called from the network handler (client thread). */
    public static void accept(DamageIndicatorPayload payload) {
        if (!MobHealthClientConfig.INDICATORS_ENABLED.get()) {
            return;
        }
        float amount = payload.amount();
        if (amount < MobHealthClientConfig.INDICATOR_MIN_DAMAGE.get().floatValue()) {
            return;
        }
        // Scatter a little, so a burst of hits reads as several numbers rather than one flickering one.
        double spread = MobHealthClientConfig.INDICATOR_SPREAD.get();
        ACTIVE.add(new Floater(
                payload.x() + (RANDOM.nextDouble() - 0.5) * spread,
                payload.y() + MobHealthClientConfig.INDICATOR_VERTICAL_OFFSET.get()
                        + (RANDOM.nextDouble() - 0.5) * spread * 0.5,
                payload.z() + (RANDOM.nextDouble() - 0.5) * spread,
                format(amount),
                payload.fatal(),
                System.currentTimeMillis()));

        int max = MobHealthClientConfig.INDICATOR_MAX_ON_SCREEN.get();
        while (ACTIVE.size() > max) {
            ACTIVE.removeFirst(); // a swarm fight stays legible, and bounded
        }
    }

    /** Nothing should survive a server change — the numbers belong to the fight you just left. */
    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        ACTIVE.clear();
    }

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        if (ACTIVE.isEmpty()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null || mc.gui.hud.isHidden()) {
            return;
        }

        long now = System.currentTimeMillis();
        long life = MobHealthClientConfig.INDICATOR_DURATION_MS.get();
        float drift = MobHealthClientConfig.INDICATOR_DRIFT.get().floatValue();
        float baseScale = MobHealthClientConfig.INDICATOR_SCALE.get().floatValue();
        double maxDist = MobHealthClientConfig.INDICATOR_MAX_DISTANCE.get();
        double maxDistSq = maxDist * maxDist;
        int color = MobHealthClientConfig.indicatorColor();
        int fatalColor = MobHealthClientConfig.indicatorFatalColor();

        Camera camera = mc.gameRenderer.mainCamera();
        Vec3 camPos = camera.position();
        // 26.1 dropped GameRenderer.getProjectionMatrix(fov) and moved the combined
        // view-rotation-and-projection matrix onto the Camera, which is what vanilla's own
        // projectPointToScreen uses. Building it by hand from the fov option is no longer
        // possible, and no longer necessary.
        //
        // Deliberately NOT switching to projectPointToScreen itself: it returns projected
        // coordinates with no way to tell that a point was behind the camera, and the
        // w <= 0.05 test below is what stops things behind you being drawn mirrored.
        Matrix4f projView = camera.getViewRotationProjectionMatrix(new Matrix4f());

        GuiGraphicsExtractor graphics = event.getGuiGraphics();
        Font font = mc.font;
        int screenW = graphics.guiWidth();
        int screenH = graphics.guiHeight();

        Iterator<Floater> it = ACTIVE.iterator();
        while (it.hasNext()) {
            Floater f = it.next();
            float age = (now - f.born()) / (float) life;
            if (age >= 1.0F) {
                ACTIVE.remove(f); // CopyOnWriteArrayList's iterator cannot remove
                continue;
            }

            double dx = f.x() - camPos.x;
            double dy = f.y() - camPos.y;
            double dz = f.z() - camPos.z;
            if (dx * dx + dy * dy + dz * dz > maxDistSq) {
                continue;
            }

            Vector4f clip = new Vector4f((float) dx, (float) dy, (float) dz, 1.0F);
            projView.transform(clip);
            if (clip.w <= 0.05F) {
                continue; // behind the camera
            }
            float ndcX = clip.x / clip.w;
            float ndcY = clip.y / clip.w;
            if (ndcX < -1.2F || ndcX > 1.2F || ndcY < -1.2F || ndcY > 1.2F) {
                continue; // off screen
            }

            float sx = (ndcX * 0.5F + 0.5F) * screenW;
            float sy = (1.0F - (ndcY * 0.5F + 0.5F)) * screenH;
            // Ease-out drift: a fast pop that slows as it rises and fades.
            float rise = 1.0F - (1.0F - age) * (1.0F - age);
            sy -= rise * drift;
            // Fully opaque for the first 40% of the life, then fade to nothing.
            float alpha = age < 0.4F ? 1.0F : 1.0F - (age - 0.4F) / 0.6F;
            int argb = (Math.max(8, Math.round(alpha * 255)) << 24)
                    | ((f.fatal() ? fatalColor : color) & 0x00FFFFFF);

            float scale = baseScale * (f.fatal() ? 1.4F : 1.0F);
            String text = f.fatal() ? "§l" + f.text() : f.text();

            graphics.pose().pushMatrix();
            graphics.pose().translate(sx, sy);
            graphics.pose().scale(scale, scale);
            graphics.text(font, text, -font.width(text) / 2, -4, argb);
            graphics.pose().popMatrix();
        }
    }

    /**
     * Damage as the player is asked to read it: raw health points, or hearts (two points each).
     * Whole numbers lose the decimal point — "5" rather than "5.0" — because most hits are whole.
     */
    private static String format(float amount) {
        float shown = MobHealthClientConfig.INDICATOR_HEARTS.get() ? amount / 2.0F : amount;
        float rounded = Math.round(shown * 10.0F) / 10.0F;
        if (rounded == Math.floor(rounded)) {
            return Integer.toString((int) rounded);
        }
        return String.valueOf(rounded);
    }
}
