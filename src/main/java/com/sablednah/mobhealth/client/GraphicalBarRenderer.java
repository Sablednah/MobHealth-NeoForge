package com.sablednah.mobhealth.client;

import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector4f;

import com.sablednah.mobhealth.core.BarStyle;
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
        double scale = policy.scale(MobHealthClientConfig.SCALE.get());
        boolean scaleWithDistance = policy.scaleWithDistance(MobHealthClientConfig.SCALE_WITH_DISTANCE.get());
        boolean fadeWithDistance = policy.fadeWithDistance(MobHealthClientConfig.FADE_WITH_DISTANCE.get());
        int baseWidth = policy.barWidth(MobHealthClientConfig.BAR_WIDTH.get());
        int baseHeight = policy.barHeight(MobHealthClientConfig.BAR_HEIGHT.get());
        BarStyle style = policy.barStyle(MobHealthClientConfig.BAR_STYLE.get());
        int segments = policy.segments(MobHealthClientConfig.GRAPHICAL_SEGMENTS.get());

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
            double distSq = dx * dx + dy * dy + dz * dz;
            if (distSq > maxDistSq) {
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

            // Size: base * scale, optionally shrinking with distance (1.0 up close -> ~0.4 at maxDist).
            double dist = Math.sqrt(distSq);
            float sizeMul = (float) scale;
            if (scaleWithDistance) {
                sizeMul *= (float) (1.0 - 0.6 * Math.min(dist / maxDist, 1.0));
            }
            int width = Math.max(1, Math.round(baseWidth * sizeMul));
            int height = Math.max(1, Math.round(baseHeight * sizeMul));

            // Fade: fully opaque until 70% of maxDist, then fade to 0 at the edge.
            float alpha = 1.0F;
            if (fadeWithDistance) {
                double fadeStart = maxDist * 0.7;
                alpha = (float) clamp01((maxDist - dist) / Math.max(1.0, maxDist - fadeStart));
            }
            if (alpha <= 0.02F) {
                continue;
            }

            int sx = Math.round((ndcX * 0.5F + 0.5F) * screenW);
            int sy = Math.round((1.0F - (ndcY * 0.5F + 0.5F)) * screenH);
            drawBar(graphics, mc, sx, sy, health, max, width, height,
                    policy.showBackground(MobHealthClientConfig.SHOW_BACKGROUND.get()),
                    policy.showText(MobHealthClientConfig.SHOW_TEXT.get()), alpha, style, segments);
        }
    }

    private static void drawBar(GuiGraphics graphics, Minecraft mc, int cx, int cy, float health, float max,
                                int width, int height, boolean showBackground, boolean showText, float alpha,
                                BarStyle style, int segments) {
        int left = cx - width / 2;
        int top = cy - height / 2;
        float fraction = Math.max(0.0F, Math.min(health / max, 1.0F));
        int fillColor = withAlpha(colorFor(fraction), alpha);
        int trackColor = withAlpha(0xFF3A3A3A, alpha);
        int bgColor = withAlpha(0xC0000000, alpha);

        switch (style) {
            case SEGMENTED -> drawSegmented(graphics, left, top, width, height, fraction, showBackground, bgColor, trackColor, fillColor, segments);
            case TAPERED -> drawTapered(graphics, left, top, width, height, fraction, showBackground, bgColor, trackColor, fillColor);
            case ROUNDED -> drawRounded(graphics, left, top, width, height, fraction, showBackground, bgColor, trackColor, fillColor);
            default -> drawSolid(graphics, left, top, width, height, fraction, showBackground, bgColor, trackColor, fillColor);
        }

        if (showText) {
            String text = HealthBarFormatter.value(health, max, ValueStyle.CURRENT_MAX);
            int textX = cx - mc.font.width(text) / 2;
            graphics.drawString(mc.font, text, textX, top - 10, withAlpha(0xFFFFFFFF, alpha));
        }
    }

    private static void drawSolid(GuiGraphics g, int left, int top, int w, int h, float frac,
                                  boolean bg, int bgColor, int track, int fill) {
        if (bg) {
            g.fill(left - 1, top - 1, left + w + 1, top + h + 1, bgColor);
        }
        g.fill(left, top, left + w, top + h, track);
        int fw = Math.round(w * frac);
        if (fw > 0) {
            g.fill(left, top, left + fw, top + h, fill);
        }
    }

    private static void drawSegmented(GuiGraphics g, int left, int top, int w, int h, float frac,
                                      boolean bg, int bgColor, int track, int fill, int segments) {
        int segs = Math.max(2, segments);
        if (bg) {
            g.fill(left - 1, top - 1, left + w + 1, top + h + 1, bgColor);
        }
        int gap = 1;
        int chunkW = Math.max(1, (w - gap * (segs - 1)) / segs);
        int filledSegs = HealthBarFormatter.filledSegments(frac, 1.0, segs);
        int x = left;
        for (int i = 0; i < segs; i++) {
            g.fill(x, top, x + chunkW, top + h, i < filledSegs ? fill : track);
            x += chunkW + gap;
        }
    }

    private static void drawTapered(GuiGraphics g, int left, int top, int w, int h, float frac,
                                    boolean bg, int bgColor, int track, int fill) {
        int fw = Math.round(w * frac);
        float halfW = w / 2.0F;
        for (int i = 0; i < w; i++) {
            float t = (i + 0.5F - halfW) / halfW;                  // -1..1 across the width
            float shape = (float) Math.sqrt(Math.max(0.0, 1.0 - t * t)); // 1 centre -> 0 ends (ellipse)
            int colH = Math.max(1, Math.round(h * (0.25F + 0.75F * shape)));
            int y0 = top + Math.round((h - colH) / 2.0F);
            if (bg) {
                g.fill(left + i, y0 - 1, left + i + 1, y0 + colH + 1, bgColor);
            }
            g.fill(left + i, y0, left + i + 1, y0 + colH, i < fw ? fill : track);
        }
    }

    private static void drawRounded(GuiGraphics g, int left, int top, int w, int h, float frac,
                                    boolean bg, int bgColor, int track, int fill) {
        int r = Math.min(2, Math.min(w, h) / 2);
        if (bg) {
            fillRounded(g, left - 1, top - 1, w + 2, h + 2, r + 1, bgColor);
        }
        fillRounded(g, left, top, w, h, r, track);
        int fw = Math.round(w * frac);
        if (fw > 0) {
            fillRoundedLeft(g, left, top, fw, h, r, fill);
        }
    }

    /** Filled rectangle with all four corners clipped by {@code r} (rounded look). */
    private static void fillRounded(GuiGraphics g, int x, int y, int w, int h, int r, int color) {
        if (r <= 0) {
            g.fill(x, y, x + w, y + h, color);
            return;
        }
        g.fill(x + r, y, x + w - r, y + h, color);          // centre band, full height
        g.fill(x, y + r, x + r, y + h - r, color);          // left edge (corners clipped)
        g.fill(x + w - r, y + r, x + w, y + h - r, color);  // right edge (corners clipped)
    }

    /** Filled rectangle with only the LEFT corners rounded; right edge is a straight cut. */
    private static void fillRoundedLeft(GuiGraphics g, int x, int y, int w, int h, int r, int color) {
        if (r <= 0 || w <= r) {
            g.fill(x, y, x + w, y + h, color);
            return;
        }
        g.fill(x + r, y, x + w, y + h, color);      // body from x+r, full height, straight right
        g.fill(x, y + r, x + r, y + h - r, color);  // left edge (corners clipped)
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

    /** Scale an ARGB colour's alpha channel by {@code factor} (0..1). */
    private static int withAlpha(int argb, float factor) {
        int a = Math.round(((argb >>> 24) & 0xFF) * Math.max(0.0F, Math.min(factor, 1.0F)));
        return (a << 24) | (argb & 0x00FFFFFF);
    }

    private static double clamp01(double v) {
        return v < 0.0 ? 0.0 : Math.min(v, 1.0);
    }
}
