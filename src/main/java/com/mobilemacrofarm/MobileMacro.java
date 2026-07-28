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

        ClientSendMessageEvents.ALLOW_CHAT.register(message -> {
            if (message.startsWith("!macro")) {
                Minecraft client = Minecraft.getInstance();
                if (client.player == null) return false;

                try {
                    String[] parts = message.split(" ");
                    if (parts.length == 3) {
                        String type = parts[1].toLowerCase();
                        String valStr = parts[2].toLowerCase();

                        if (type.equals("mode")) {
                            if (valStr.equals("cane") || valStr.equals("melon")) {
                                FarmingMacro.getInstance().setMode(valStr);
                                client.player.sendSystemMessage(Component.literal("§a[MobileMacro] Farming Mode set to " + valStr));
                            } else {
                                client.player.sendSystemMessage(Component.literal("§c[MobileMacro] Invalid mode! Use 'cane' or 'melon'."));
                            }
                        } else if (type.equals("yaw")) {
                            float val = Float.parseFloat(valStr);
                            FarmingMacro.getInstance().setYaw(val);
                            client.player.sendSystemMessage(Component.literal("§a[MobileMacro] Target Yaw set to " + val));
                        } else if (type.equals("pitch")) {
                            float val = Float.parseFloat(valStr);
                            FarmingMacro.getInstance().setPitch(val);
                            client.player.sendSystemMessage(Component.literal("§a[MobileMacro] Target Pitch set to " + val));
                        } else {
                            client.player.sendSystemMessage(Component.literal("§c[MobileMacro] Usage: !macro <mode|yaw|pitch> <value>"));
                        }
                    } else {
                        client.player.sendSystemMessage(Component.literal("§c[MobileMacro] Usage: !macro <mode|yaw|pitch> <value>"));
                    }
                } catch (Exception e) {
                    client.player.sendSystemMessage(Component.literal("§c[MobileMacro] Invalid value!"));
                }
                
                return false; 
            }
            return true; 
        });
    }
}
