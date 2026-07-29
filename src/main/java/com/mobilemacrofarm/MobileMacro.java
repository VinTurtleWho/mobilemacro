package com.mobilemacrofarm;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientSendMessageEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
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
                if (isPressed && !wasKeyPressed) { FarmingMacro.getInstance().toggle(client); }
                wasKeyPressed = isPressed;
            }
            FarmingMacro.getInstance().onTick(client);
        });

        // NEW: INCOMING CHAT INTERCEPTOR
        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            String text = message.getString();
            // Checking for exact server message trigger
            if (text.contains("A pest has appeared") || text.contains("A Pest has appeared")) {
                FarmingMacro.getInstance().triggerPestProtocol(Minecraft.getInstance());
            }
        });

        ClientSendMessageEvents.ALLOW_CHAT.register(message -> {
            if (message.startsWith("!macro")) {
                Minecraft client = Minecraft.getInstance();
                if (client.player == null) return false;
                try {
                    String[] parts = message.split(" ");
                    if (parts.length >= 2) {
                        String type = parts[1].toLowerCase();
                        if (type.equals("record")) { FarmingMacro.getInstance().startRecording(client); } 
                        else if (type.equals("stoprecord")) { FarmingMacro.getInstance().stop(client); client.player.sendSystemMessage(Component.literal("§a[MobileMacro] Recording saved!")); } 
                        else if (type.equals("pestonly")) { FarmingMacro.getInstance().setPestOnly(true); client.player.sendSystemMessage(Component.literal("§a[MobileMacro] Mode set to PEST ONLY")); } 
                        else if (type.equals("farmonly")) { FarmingMacro.getInstance().setPestOnly(false); client.player.sendSystemMessage(Component.literal("§a[MobileMacro] Mode set to NORMAL FARMING")); } 
                        else if (type.equals("tool")) { int slot = Integer.parseInt(parts[2]) - 1; FarmingMacro.getInstance().setToolSlot(slot); client.player.sendSystemMessage(Component.literal("§a[MobileMacro] Tool slot set to " + (slot+1))); } 
                        else if (type.equals("vacuum")) { int slot = Integer.parseInt(parts[2]) - 1; FarmingMacro.getInstance().setVacuumSlot(slot); client.player.sendSystemMessage(Component.literal("§a[MobileMacro] Vacuum slot set to " + (slot+1))); } 
                        else if (type.equals("end")) {
                            if (parts.length == 2) { FarmingMacro.getInstance().setEndBlock(client.player.getBlockX(), client.player.getBlockY(), client.player.getBlockZ()); client.player.sendSystemMessage(Component.literal("§a[MobileMacro] End Block set!")); } 
                            else if (parts.length == 3 && parts[2].equalsIgnoreCase("clear")) { FarmingMacro.getInstance().setEndBlock(null, null, null); client.player.sendSystemMessage(Component.literal("§a[MobileMacro] End Block cleared!")); }
                        } else if (parts.length == 3) {
                            String valStr = parts[2].toLowerCase();
                            if (type.equals("mode")) { FarmingMacro.getInstance().setMode(valStr); client.player.sendSystemMessage(Component.literal("§a[MobileMacro] Farming Mode set to " + valStr)); } 
                            else if (type.equals("yaw")) { FarmingMacro.getInstance().setYaw(Float.parseFloat(valStr)); client.player.sendSystemMessage(Component.literal("§a[MobileMacro] Target Yaw set to " + valStr)); } 
                            else if (type.equals("pitch")) { FarmingMacro.getInstance().setPitch(Float.parseFloat(valStr)); client.player.sendSystemMessage(Component.literal("§a[MobileMacro] Target Pitch set to " + valStr)); }
                        }
                    }
                } catch (Exception e) { client.player.sendSystemMessage(Component.literal("§c[MobileMacro] Invalid format!")); }
                return false; 
            }
            return true; 
        });
    }
}
