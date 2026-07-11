package com.sablednah.mobhealth.neoforge;

import com.sablednah.mobhealth.core.MobCategory;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;

/**
 * NeoForge/Minecraft-specific adapter that classifies a concrete entity into a loader-agnostic
 * {@link MobCategory}. Kept separate from core so a future Fabric port swaps only this file.
 */
public final class EntityCategorizer {

    private EntityCategorizer() {}

    /**
     * Classify an entity. Order matters: neutral mobs (endermen, wolves, ...) also implement the
     * hostile {@link Enemy} marker, so they must be checked first.
     */
    public static MobCategory categorize(Entity entity) {
        if (entity instanceof Player) {
            return MobCategory.PLAYER;
        }
        if (entity instanceof Mob mob) {
            if (mob instanceof NeutralMob) {
                return MobCategory.NEUTRAL;
            }
            if (mob instanceof Enemy) {
                return MobCategory.HOSTILE;
            }
            // Any other AI-driven creature (cows, villagers, squid, ...) is treated as passive.
            return MobCategory.PASSIVE;
        }
        return MobCategory.OTHER;
    }

    /** Is this entity a vanilla boss? (Modded bosses can be forced on via config overrides.) */
    public static boolean isBoss(Entity entity) {
        return entity instanceof EnderDragon || entity instanceof WitherBoss;
    }

    /** Registry id, e.g. {@code "minecraft:zombie"}. */
    public static String id(Entity entity) {
        return BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString();
    }
}
