package com.eatingesp;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;

public class EatingEspMod implements ClientModInitializer {

    public static final String MOD_ID = "eating-esp";

    @Override
    public void onInitializeClient() {
        WorldRenderEvents.END.register(EatingEspRenderer::onWorldRender);
    }
}
