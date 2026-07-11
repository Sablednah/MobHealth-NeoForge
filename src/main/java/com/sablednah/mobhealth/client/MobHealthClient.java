package com.sablednah.mobhealth.client;

import com.sablednah.mobhealth.MobHealth;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.common.NeoForge;

/**
 * Client-only entrypoint. Does not load on dedicated servers, so referencing client code here is safe.
 *
 * <p>This is where the graphical floating-bar renderer (task 4) is registered. Players without this
 * mod still see whatever the server sends (nameplate/boss bar/chat).
 */
@Mod(value = MobHealth.MODID, dist = Dist.CLIENT)
@EventBusSubscriber(modid = MobHealth.MODID, value = Dist.CLIENT)
public class MobHealthClient {

    public MobHealthClient(ModContainer container) {
        // Client-side config for the graphical floating bars.
        container.registerConfig(ModConfig.Type.CLIENT, MobHealthClientConfig.SPEC);

        // Adds an in-game config screen (Mods > MobHealth > Config).
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);

        // Graphical bar renderer listens on the game event bus (level render + GUI passes).
        NeoForge.EVENT_BUS.register(GraphicalBarRenderer.class);
    }

    @SubscribeEvent
    static void onClientSetup(FMLClientSetupEvent event) {
        MobHealth.LOGGER.info("MobHealth client setup complete — graphical bars available.");
    }
}
