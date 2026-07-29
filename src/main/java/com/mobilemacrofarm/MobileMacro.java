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
                    if (parts.length >= 2) {
                        String type = parts[1].toLowerCase();

                        if (type.equals("end")) {
                            if (parts.length == 2) {
                                // Set to current standing block
                                int px = client.player.getBlockX();
                                int py = client.player.getBlockY();
                                int pz = client.player.getBlockZ();
                                FarmingMacro.getInstance().setEndBlock(px, py, pz);
                                client.player.sendSystemMessage(Component.literal("§a[MobileMacro] End Block set to your location: " + px + ", " + py + ", " + pz));
                            } else if (parts.length == 3 && parts[2].equalsIgnoreCase("clear")) {
                                FarmingMacro.getInstance().setEndBlock(null, null, null);
                                client.player.sendSystemMessage(Component.literal("§a[MobileMacro] End Block cleared!"));
                            } else if (parts.length == 5) {
                                // Set to specific XYZ
                                int px = Integer.parseInt(parts[2]);
                                int py = Integer.parseInt(parts[3]);
                                int pz = Integer.parseInt(parts[4]);
                                FarmingMacro.getInstance().setEndBlock(px, py, pz);
                                client.player.sendSystemMessage(Component.literal("§a[MobileMacro] End Block set to: " + px + ", " + py + ", " + pz));
                            } else {
                                client.player.sendSystemMessage(Component.literal("§c[MobileMacro] Usage: !macro end OR !macro end <x> <y> <z> OR !macro end clear"));
                            }
                        } else if (parts.length == 3) {
                            String valStr = parts[2].toLowerCase();
                            if (type.equals("mode")) {
                                if (valStr.equals("cane") || valStr.equals("melon") || valStr.equals("mushroom")) {
                                    FarmingMacro.getInstance().setMode(valStr);
                                    client.player.sendSystemMessage(Component.literal("§a[MobileMacro] Farming Mode set to " + valStr));
                                } else {
                                    client.player.sendSystemMessage(Component.literal("§c[MobileMacro] Invalid mode! Use 'cane', 'melon', or 'mushroom'."));
                                }
                            } else if (type.equals("yaw")) {
                                float val = Float.parseFloat(valStr);
                                FarmingMacro.getInstance().setYaw(val);
                                client.player.sendSystemMessage(Component.literal("§a[MobileMacro] Target Yaw set to " + val));
                            } else if (type.equals("pitch")) {
                                float val = Float.parseFloat(valStr);
                                FarmingMacro.getInstance().setPitch(val);
                                client.player.sendSystemMessage(Component.literal("§a[MobileMacro] Target Pitch set to " + val));
                            }
                        } else {
                            client.player.sendSystemMessage(Component.literal("§c[MobileMacro] Usage: !macro <mode|yaw|pitch|end> <value>"));
                        }
                    } else {
                        client.player.sendSystemMessage(Component.literal("§c[MobileMacro] Usage: !macro <mode|yaw|pitch|end> <value>"));
                    }
                } catch (Exception e) {
                    client.player.sendSystemMessage(Component.literal("§c[MobileMacro] Invalid format!"));
                }
                
                return false; 
            }
            return true; 
        });
    }
}
