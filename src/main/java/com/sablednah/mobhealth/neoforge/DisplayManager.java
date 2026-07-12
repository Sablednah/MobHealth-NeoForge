package com.sablednah.mobhealth.neoforge;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.sablednah.mobhealth.MobHealth;
import com.sablednah.mobhealth.MobHealthConfig;
import com.sablednah.mobhealth.network.ToastPayload;
import com.sablednah.mobhealth.core.Audience;
import com.sablednah.mobhealth.core.MobCategory;
import com.sablednah.mobhealth.core.NameplateMode;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * The server-side heart of MobHealth. Listens for player-dealt damage and dispatches to the
 * enabled display modes (chat / nameplate / boss bar), then reverts temporary displays on a timer.
 *
 * <p>Registered on the NeoForge game event bus from {@link MobHealth}. All state lives on the
 * server thread; the maps are keyed by entity id.
 */
public final class DisplayManager {

    /** Persistent-data flag marking an entity whose name tag MobHealth is currently controlling. */
    private static final String CTRL_TAG = "mobhealth_controlled";

    /** Monotonic server tick counter (drives display timeouts). */
    private long serverTick;

    /** Active temporary nameplate augmentations, keyed by victim entity id. */
    private final Map<Integer, NameEntry> nameplates = new ConcurrentHashMap<>();

    /** Active boss bars, keyed by victim entity id. */
    private final Map<Integer, BossEntry> bossBars = new ConcurrentHashMap<>();

    // ================================================================= damage

    @SubscribeEvent
    public void onLivingDamage(LivingDamageEvent.Post event) {
        LivingEntity victim = event.getEntity();
        if (victim.level().isClientSide()) {
            return;
        }

        // Only react to damage dealt by a player (classic "the damage you caused" behaviour).
        Entity source = event.getSource().getEntity();
        if (!(source instanceof ServerPlayer attacker)) {
            return;
        }

        double current = victim.getHealth();
        double max = victim.getMaxHealth();
        if (event.getNewDamage() <= 0.0F) {
            return; // blocked / zero-damage hit carries no useful info
        }
        if (MobHealthConfig.HIDE_UNTIL_DAMAGED.get() && current >= max) {
            return;
        }

        MobCategory category = EntityCategorizer.categorize(victim);
        boolean boss = EntityCategorizer.isBoss(victim);
        if (!shouldDisplay(EntityCategorizer.id(victim), category, boss)) {
            return;
        }

        List<ServerPlayer> viewers = audienceFor(victim, attacker);

        if (MobHealthConfig.CHAT_ENABLED.get()) {
            sendChat(viewers, victim, event.getNewDamage(), current, max);
        }
        if (MobHealthConfig.ACTION_BAR_ENABLED.get()) {
            sendActionBar(viewers, victim, current, max);
        }
        if (MobHealthConfig.TOAST_ENABLED.get()) {
            sendToast(viewers, victim, event.getSource(), attacker, event.getNewDamage(), current, max);
        }
        if (MobHealthConfig.NAMEPLATE_ENABLED.get()) {
            updateNameplate(victim, current, max, category);
        }
        if (MobHealthConfig.BOSS_BAR_ENABLED.get()) {
            updateBossBar(victim, viewers, current, max, category);
        }
    }

    /** Override wins; then bosses use their own toggle; otherwise the group toggle applies. */
    private boolean shouldDisplay(String id, MobCategory category, boolean boss) {
        Boolean override = MobHealthConfig.overrideFor(id);
        if (override != null) {
            return override;
        }
        if (boss) {
            return MobHealthConfig.SHOW_FOR_BOSSES.get();
        }
        return MobHealthConfig.groupEnabled(category);
    }

    private List<ServerPlayer> audienceFor(LivingEntity victim, ServerPlayer attacker) {
        // Only players who may receive displays (not muted, and hold the mobhealth.see permission).
        // Note: this filters chat and boss bar. Nameplates are a shared entity property and cannot
        // be hidden per-player.
        if (MobHealthConfig.AUDIENCE.get() == Audience.ATTACKER || !(victim.level() instanceof ServerLevel level)) {
            return MobHealthPermissions.receivesDisplays(attacker) ? List.of(attacker) : List.of();
        }
        double radius = MobHealthConfig.NEARBY_RADIUS.get();
        double radiusSq = radius * radius;
        List<ServerPlayer> viewers = new ArrayList<>();
        for (ServerPlayer player : level.players()) {
            if (player.distanceToSqr(victim) <= radiusSq && MobHealthPermissions.receivesDisplays(player)) {
                viewers.add(player);
            }
        }
        return viewers;
    }

    /**
     * The mob's real name for labels — never the bar we injected. Prefers a genuine player-given
     * custom name, otherwise the entity's type name ("Zombie", "Camel"). Using
     * {@code getDisplayName()} here would echo our own nameplate bar back into chat/boss bars.
     */
    private Component cleanName(LivingEntity victim) {
        NameEntry entry = nameplates.get(victim.getId());
        if (entry != null) {
            return entry.originalName != null ? entry.originalName : victim.getType().getDescription();
        }
        Component custom = victim.getCustomName();
        if (custom != null && !isControlled(victim)) {
            return custom;
        }
        return victim.getType().getDescription();
    }

    private static boolean isControlled(LivingEntity entity) {
        return entity.getPersistentData().getBoolean(CTRL_TAG).orElse(false);
    }

    // ================================================================= chat

    private void sendChat(List<ServerPlayer> viewers, LivingEntity victim, float damage, double current, double max) {
        MutableComponent line = Component.empty()
                .append(cleanName(victim))
                .append(Component.literal(" "))
                .append(BarText.content(current, max, MobHealthConfig.CHAT_CONTENT.get()))
                .append(Component.literal(" (-" + trim(damage) + ")").withStyle(ChatFormatting.GRAY));
        for (ServerPlayer viewer : viewers) {
            viewer.displayClientMessage(line, false);
        }
    }

    // ================================================================= action bar

    private void sendActionBar(List<ServerPlayer> viewers, LivingEntity victim, double current, double max) {
        MutableComponent line = Component.empty()
                .append(cleanName(victim))
                .append(Component.literal(" "))
                .append(BarText.content(current, max, MobHealthConfig.ACTION_BAR_CONTENT.get()));
        for (ServerPlayer viewer : viewers) {
            viewer.displayClientMessage(line, true); // true = action bar (above hotbar)
        }
    }

    // ================================================================= toast

    private void sendToast(List<ServerPlayer> viewers, LivingEntity victim, DamageSource source,
                           ServerPlayer attacker, float damage, double current, double max) {
        String name = cleanName(victim).getString();
        ItemStack icon = damageIcon(source, attacker);
        for (ServerPlayer viewer : viewers) {
            PacketDistributor.sendToPlayer(viewer, new ToastPayload(name, damage, (float) current, (float) max, icon));
        }
    }

    /** The item that dealt the damage: an arrow for projectiles, else the weapon / main-hand item. */
    private static ItemStack damageIcon(DamageSource source, ServerPlayer attacker) {
        if (source.getDirectEntity() instanceof AbstractArrow) {
            return new ItemStack(Items.ARROW);
        }
        ItemStack weapon = source.getWeaponItem();
        if (weapon != null && !weapon.isEmpty()) {
            return weapon;
        }
        return attacker.getMainHandItem(); // may be empty -> client shows the heart fallback
    }

    // ================================================================= nameplate

    private void updateNameplate(LivingEntity victim, double current, double max, MobCategory category) {
        NameplateMode mode = MobHealthConfig.NAMEPLATE_MODE.get();
        int id = victim.getId();
        NameEntry entry = nameplates.get(id);
        if (entry == null) {
            // If we're already controlling this entity's name (e.g. its window lapsed without a
            // clean revert), the current custom name is OUR bar — treat the real original as absent
            // rather than nesting a bar inside a bar.
            boolean controlled = isControlled(victim);
            Component realName = controlled ? null : victim.getCustomName();
            boolean realVisible = controlled ? false : victim.isCustomNameVisible();
            if (mode == NameplateMode.NAMED_ONLY && realName == null) {
                return; // this mode only decorates already-named mobs
            }
            entry = new NameEntry(victim, realName, realVisible, mode != NameplateMode.ALWAYS);
            nameplates.put(id, entry);
        }

        MutableComponent display = Component.empty();
        if (entry.originalName != null) {
            display.append(entry.originalName).append(Component.literal(" "));
        }
        display.append(BarText.content(current, max, MobHealthConfig.NAMEPLATE_CONTENT.get()));

        victim.setCustomName(display);
        victim.setCustomNameVisible(true);
        victim.getPersistentData().putBoolean(CTRL_TAG, true);
        entry.expiry = serverTick + MobHealthConfig.displayTicks(category);
    }

    // ================================================================= boss bar

    private void updateBossBar(LivingEntity victim, List<ServerPlayer> viewers, double current, double max, MobCategory category) {
        int id = victim.getId();
        BossEntry entry = bossBars.get(id);
        if (entry == null) {
            ServerBossEvent event = new ServerBossEvent(cleanName(victim), bossColor(), BossEvent.BossBarOverlay.PROGRESS);
            entry = new BossEntry(event);
            bossBars.put(id, entry);
        }
        MutableComponent title = Component.empty()
                .append(cleanName(victim))
                .append(Component.literal("  "))
                .append(BarText.value(current, max));
        entry.event.setName(title);
        entry.event.setProgress((float) clamp01(current / (max <= 0 ? 1 : max)));

        // Re-sync the viewer set each hit (cheap, keeps NEARBY audiences correct as players move).
        entry.event.removeAllPlayers();
        for (ServerPlayer viewer : viewers) {
            entry.event.addPlayer(viewer);
        }
        entry.expiry = serverTick + MobHealthConfig.displayTicks(category);
    }

    private static BossEvent.BossBarColor bossColor() {
        try {
            return BossEvent.BossBarColor.valueOf(MobHealthConfig.BOSS_BAR_COLOR.get().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return BossEvent.BossBarColor.RED;
        }
    }

    // ================================================================= death

    /** When a tracked mob dies, cut its remaining display time to the short death timeout. */
    @SubscribeEvent
    public void onLivingDeath(LivingDeathEvent event) {
        int id = event.getEntity().getId();
        long deathExpiry = serverTick + MobHealthConfig.DEATH_TICKS.get();

        BossEntry boss = bossBars.get(id);
        if (boss != null) {
            boss.event.setProgress(0.0F);
            boss.expiry = Math.min(boss.expiry, deathExpiry);
        }
        NameEntry name = nameplates.get(id);
        if (name != null) {
            name.expiry = Math.min(name.expiry, deathExpiry);
        }
    }

    // ================================================================= tick / expiry

    @SubscribeEvent
    public void onServerTick(ServerTickEvent.Post event) {
        serverTick++;

        for (Iterator<Map.Entry<Integer, NameEntry>> it = nameplates.entrySet().iterator(); it.hasNext();) {
            NameEntry entry = it.next().getValue();
            LivingEntity entity = entry.entity;
            if (entity == null || entity.isRemoved()) {
                it.remove();
                continue;
            }
            if (entry.revert && serverTick >= entry.expiry) {
                entity.setCustomName(entry.originalName);
                entity.setCustomNameVisible(entry.originalVisible);
                entity.getPersistentData().putBoolean(CTRL_TAG, false);
                it.remove();
            }
        }

        for (Iterator<Map.Entry<Integer, BossEntry>> it = bossBars.entrySet().iterator(); it.hasNext();) {
            BossEntry entry = it.next().getValue();
            if (serverTick >= entry.expiry) {
                entry.event.removeAllPlayers();
                it.remove();
            }
        }
    }

    // ================================================================= helpers / state

    private static double clamp01(double v) {
        return v < 0 ? 0 : Math.min(v, 1);
    }

    private static String trim(double v) {
        if (v == Math.floor(v) && !Double.isInfinite(v)) {
            return Long.toString((long) v);
        }
        return String.valueOf(Math.round(v * 10.0D) / 10.0D);
    }

    private static final class NameEntry {
        private final LivingEntity entity;
        private final Component originalName;
        private final boolean originalVisible;
        private final boolean revert;
        private long expiry;

        private NameEntry(LivingEntity entity, Component originalName, boolean originalVisible, boolean revert) {
            this.entity = entity;
            this.originalName = originalName;
            this.originalVisible = originalVisible;
            this.revert = revert;
        }
    }

    private static final class BossEntry {
        private final ServerBossEvent event;
        private long expiry;

        private BossEntry(ServerBossEvent event) {
            this.event = event;
        }
    }
}
