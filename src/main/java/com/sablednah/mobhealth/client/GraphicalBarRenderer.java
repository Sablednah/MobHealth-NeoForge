package com.sablednah.mobhealth.client;

import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector4f;

import com.sablednah.mobhealth.core.HealthBarFormatter;
import com.sablednah.mobhealth.core.HealthBarFormatter.ValueStyle;
import com.sablednah.mobhealth.network.GraphicalGateState;
import com.sablednah.mobhealth.network.GraphicalPolicy;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

/**
 * Draws graphical floating health bars above mobs (client-side).
 *
 * <p>The 1.21.11 render pipeline renders the world several times per frame and no longer exposes a
 * readable projection matrix, so capturing matrices from the level render produced period-N jitter.
 * Instead we do everything once per frame in the GUI pass: rebuild the exact view matrix Minecraft
 * uses ({@code new Matrix4f().rotation(camera.rotation().conjugate())}, see GameRenderer), combine
 * it with the projection, and project each nearby living entity to the screen — then draw a pixel
 * bar with {@link GuiGraphics#fill}. Pixel bars are constant width and immune to the proportional
 * font issue that affects text bars.
 */
public final class GraphicalBarRenderer {

    private GraphicalBarRenderer() {}

    /** Reset the policy when leaving a server, so a server's overrides don't affect the next one. */
    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        GraphicalGateState.policy = GraphicalPolicy.DEFAULT;
    }

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        GraphicalPolicy policy = GraphicalGateState.policy;
        // Client toggle AND server gate (server's graphical config + this player's see/mute).
        if (!MobHealthClientConfig.ENABLED.get() || !policy.allowed()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null || mc.options.hideGui) {
            return;
        }

        Camera camera = mc.gameRenderer.getMainCamera();
        Vec3 camPos = camera.position();

        // Reproduce Minecraft's world view matrix exactly (GameRenderer), then combine with the
        // projection. Computed here (once per frame) rather than captured from the multi-pass level
        // render, so it is stable and correct.
        Quaternionf viewRotation = camera.rotation().conjugate(new Quaternionf());
        Matrix4f projView = mc.gameRenderer.getProjectionMatrix(mc.options.fov().get())
                .mul(new Matrix4f().rotation(viewRotation), new Matrix4f());

        float partialTick = mc.getDeltaTracker().getGameTimeDeltaPartialTick(false);

        GuiGraphics graphics = event.getGuiGraphics();
        int screenW = graphics.guiWidth();
        int screenH = graphics.guiHeight();

        // Each option: the server's enforced value if it set one, otherwise this client's config.
        double offset = policy.verticalOffset(MobHealthClientConfig.VERTICAL_OFFSET.get());
        double maxDist = policy.maxDistance(MobHealthClientConfig.MAX_DISTANCE.get());
        double maxDistSq = maxDist * maxDist;
        boolean showPlayers = policy.showPlayers(MobHealthClientConfig.SHOW_PLAYERS.get());
        boolean onlyDamaged = policy.onlyWhenDamaged(MobHealthClientConfig.ONLY_WHEN_DAMAGED.get());
        boolean requireLos = policy.requireLineOfSight(MobHealthClientConfig.REQUIRE_LINE_OF_SIGHT.get());

        for (Entity entity : mc.level.entitiesForRendering()) {
            if (!(entity instanceof LivingEntity living) || !living.isAlive() || living == mc.player) {
                continue;
            }
            if (living.isInvisible() || living.isSpectator()) {
                continue;
            }
            if (living instanceof Player && !showPlayers) {
                continue;
            }
            float health = living.getHealth();
            float max = living.getMaxHealth();
            if (health <= 0.0F || max <= 0.0F || (onlyDamaged && health >= max)) {
                continue;
            }

            // Interpolated (partial-tick) world position. NOTE: getX/Y/Z(double) are NOT
            // interpolation helpers (they return a point along the bounding box) — use getPosition().
            Vec3 pos = living.getPosition(partialTick);
            double barY = pos.y + living.getBbHeight() + offset;

            double dx = pos.x - camPos.x;
            double dy = barY - camPos.y;
            double dz = pos.z - camPos.z;
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
            if (ndcX < -1.1F || ndcX > 1.1F || ndcY < -1.1F || ndcY > 1.1F) {
                continue; // off screen
            }
            if (requireLos) {
                Vec3 to = new Vec3(pos.x, pos.y + living.getBbHeight() * 0.5, pos.z);
                BlockHitResult hit = mc.level.clip(
                        new ClipContext(camPos, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, living));
                if (hit.getType() != HitResult.Type.MISS) {
                    continue; // occluded by terrain
                }
            }

            int sx = Math.round((ndcX * 0.5F + 0.5F) * screenW);
            int sy = Math.round((1.0F - (ndcY * 0.5F + 0.5F)) * screenH);
            drawBar(graphics, mc, sx, sy, health, max);
        }
    }

    private static void drawBar(GuiGraphics graphics, Minecraft mc, int cx, int cy, float health, float max) {
        GraphicalPolicy policy = GraphicalGateState.policy;
        int width = policy.barWidth(MobHealthClientConfig.BAR_WIDTH.get());
        int height = policy.barHeight(MobHealthClientConfig.BAR_HEIGHT.get());
        int left = cx - width / 2;
        int top = cy - height / 2;
        float fraction = Math.max(0.0F, Math.min(health / max, 1.0F));

        if (policy.showBackground(MobHealthClientConfig.SHOW_BACKGROUND.get())) {
            graphics.fill(left - 1, top - 1, left + width + 1, top + height + 1, 0xC0000000);
        }
        graphics.fill(left, top, left + width, top + height, 0xFF3A3A3A);
        int filledWidth = Math.round(width * fraction);
        if (filledWidth > 0) {
            graphics.fill(left, top, left + filledWidth, top + height, colorFor(fraction));
        }

        if (policy.showText(MobHealthClientConfig.SHOW_TEXT.get())) {
            String text = HealthBarFormatter.value(health, max, ValueStyle.CURRENT_MAX);
            int textX = cx - mc.font.width(text) / 2;
            graphics.drawString(mc.font, text, textX, top - 10, 0xFFFFFFFF);
        }
    }

    private static int colorFor(float fraction) {
        if (fraction > 0.5F) {
            return 0xFF55DD55; // green
        }
        if (fraction > 0.25F) {
            return 0xFFFFD633; // yellow
        }
        return 0xFFFF4444; // red
    }
}
