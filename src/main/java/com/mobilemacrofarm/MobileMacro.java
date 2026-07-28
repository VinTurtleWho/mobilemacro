package com.mobilemacrofarm;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import com.mojang.blaze3d.platform.InputConstants;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MobileMacro implements ModInitializer {
    public static final String MOD_ID = "mobilemacro";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static boolean wasKeyPressed = false;

    @Override
    public void onInitialize() {
        LOGGER.info("Initializing MobileMacro for Mojo Launcher!");

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            // Bypass the KeyMapping registry entirely and read raw keyboard input
            if (client.getWindow() != null) {
                long window = client.getWindow().getWindow();
                boolean isPressed = InputConstants.isKeyDown(window, GLFW.GLFW_KEY_O);
                
                // Toggle exactly once per press (debounce)
                if (isPressed && !wasKeyPressed) {
                    FarmingMacro.getInstance().toggle(client);
                }
                wasKeyPressed = isPressed;
            }
            
            // Keep the farming logic looping
            FarmingMacro.getInstance().onTick(client);
        });
    }
}
