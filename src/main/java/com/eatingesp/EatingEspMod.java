package com.eatingesp;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EatingEspMod implements ClientModInitializer {

    public static final String MOD_ID = "eating-esp";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitializeClient() {
        LOGGER.info("Eating ESP Mod loaded!");
        // Register the world render event for ESP rendering
        WorldRenderEvents.AFTER_ENTITIES.register(EatingEspRenderer::onWorldRender);
    }
}
