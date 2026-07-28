package com.mobilemacrofarm;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientSendMessageEvents;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
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

        // 1. The Tick Loop for the FSM Engine
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.getWindow() != null) {
                boolean isPressed = InputConstants.isKeyDown(client.getWindow(), GLFW.GLFW_KEY_O);
                if (isPressed && !wasKeyPressed) {
                    FarmingMacro.getInstance().toggle(client);
                }
                wasKeyPressed = isPressed;
            }
            FarmingMacro.getInstance().onTick(client);
        });

        // 2. The Chat Interceptor for Settings (!macro yaw 90)
        ClientSendMessageEvents.ALLOW_CHAT.register(message -> {
            if (message.startsWith("!macro")) {
                Minecraft client = Minecraft.getInstance();
                if (client.player == null) return false;

                try {
                    String[] parts = message.split(" ");
                    if (parts.length == 3) {
                        String type = parts[1].toLowerCase();
                        float val = Float.parseFloat(parts[2]);

                        if (type.equals("yaw")) {
                            FarmingMacro.getInstance().setYaw(val);
                            client.player.sendSystemMessage(Component.literal("§a[MobileMacro] Target Yaw set to " + val));
                        } else if (type.equals("pitch")) {
                            FarmingMacro.getInstance().setPitch(val);
                            client.player.sendSystemMessage(Component.literal("§a[MobileMacro] Target Pitch set to " + val));
                        } else {
                            client.player.sendSystemMessage(Component.literal("§c[MobileMacro] Usage: !macro <yaw|pitch> <number>"));
                        }
                    } else {
                        client.player.sendSystemMessage(Component.literal("§c[MobileMacro] Usage: !macro <yaw|pitch> <number>"));
                    }
                } catch (Exception e) {
                    client.player.sendSystemMessage(Component.literal("§c[MobileMacro] Invalid number!"));
                }
                
                // Return false to completely cancel the message from sending to the server
                return false; 
            }
            // Return true to let normal chat messages pass through
            return true; 
        });
    }
}
