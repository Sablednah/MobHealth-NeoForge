package com.sablednah.mobhealth;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import com.sablednah.mobhealth.neoforge.DisplayManager;
import com.sablednah.mobhealth.neoforge.MobHealthServerEvents;
import com.sablednah.mobhealth.network.MobHealthNetwork;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;

/**
 * MobHealth — main mod entrypoint (common: loaded on both client and dedicated server).
 *
 * <p>A modern NeoForge rewrite of the classic MobHealth Bukkit plugin. Displays the damage
 * a player deals to a mob and the mob's remaining health, through any combination of
 * configurable display modes:
 * <ul>
 *     <li>Chat messages</li>
 *     <li>Nameplate health bars (works on vanilla clients)</li>
 *     <li>Boss bars (works on vanilla clients)</li>
 *     <li>Graphical floating bars (requires this mod on the client)</li>
 * </ul>
 *
 * <p>Design note: keep loader-specific glue thin. Reusable, loader-agnostic logic lives under
 * {@code com.sablednah.mobhealth.core} so a future Fabric (or successor) port only needs to
 * re-implement the adapter layer, not the "brains".
 */
@Mod(MobHealth.MODID)
public class MobHealth {

    /** The mod id — must match {@code mod_id} in gradle.properties and the modId in neoforge.mods.toml. */
    public static final String MODID = "mobhealth";

    /** Shared logger. */
    public static final Logger LOGGER = LogUtils.getLogger();

    public MobHealth(IEventBus modEventBus, ModContainer modContainer) {
        // Register the server-side (common) configuration.
        modContainer.registerConfig(ModConfig.Type.COMMON, MobHealthConfig.SPEC);

        // Register the server-side display driver on the game event bus (damage + tick).
        NeoForge.EVENT_BUS.register(new DisplayManager());

        // Register permission nodes and the /mobhealth command.
        NeoForge.EVENT_BUS.register(MobHealthServerEvents.class);

        // Register network payloads (mod event bus) for the server-enforced graphical gate.
        modEventBus.addListener(MobHealthNetwork::register);

        LOGGER.info("MobHealth {} initialising", modContainer.getModInfo().getVersion());
    }
}
