package com.mobilemacrofarm;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.blaze3d.platform.InputConstants;
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

        // --- REGISTER CHAT COMMANDS ---
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(ClientCommandManager.literal("macro")
                .then(ClientCommandManager.literal("yaw")
                    .then(ClientCommandManager.argument("value", FloatArgumentType.floatArg())
                        .executes(context -> {
                            float val = FloatArgumentType.getFloat(context, "value");
                            FarmingMacro.getInstance().setYaw(val);
                            context.getSource().sendFeedback(Component.literal("§a[MobileMacro] Target Yaw set to " + val));
                            return 1;
                        })))
                .then(ClientCommandManager.literal("pitch")
                    .then(ClientCommandManager.argument("value", FloatArgumentType.floatArg())
                        .executes(context -> {
                            float val = FloatArgumentType.getFloat(context, "value");
                            FarmingMacro.getInstance().setPitch(val);
                            context.getSource().sendFeedback(Component.literal("§a[MobileMacro] Target Pitch set to " + val));
                            return 1;
                        }))));
        });
    }
}
