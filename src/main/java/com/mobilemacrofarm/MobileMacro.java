package com.mobilemacrofarm;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MobileMacro implements ModInitializer {
    public static final String MOD_ID = "mobilemacro";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static KeyMapping toggleKey;

    @Override
    public void onInitialize() {
        LOGGER.info("Initializing MobileMacro for Mojo Launcher!");

        toggleKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.mobilemacro.toggle",
                GLFW.GLFW_KEY_O,
                "key.categories.misc"
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (toggleKey.consumeClick()) {
                FarmingMacro.getInstance().toggle(client);
            }
            FarmingMacro.getInstance().onTick(client);
        });
    }
}
