package com.mobilemacrofarm;

import com.mobilemacrofarm.input.InputController;
import com.mobilemacrofarm.rotation.RotationHandler;
import com.mobilemacrofarm.state.MacroState;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

public class FarmingMacro {
    private static final FarmingMacro INSTANCE = new FarmingMacro();

    private MacroState state = MacroState.IDLE;
    private final RotationHandler rotationHandler = new RotationHandler();

    private float defaultYaw = 45.0f;
    private float defaultPitch = 0.0f;
    private String mode = "cane"; // Default mode

    private boolean isMovingLeft = true;
    private boolean isMovingForward = true; // Added for mushroom mode
    private int stuckTicks = 0;
    private int laneShiftTicks = 0;
    private int restartTicks = 0;

    public static FarmingMacro getInstance() { return INSTANCE; }

    public void setYaw(float yaw) { this.defaultYaw = yaw; }
    public void setPitch(float pitch) { this.defaultPitch = pitch; }
    public void setMode(String newMode) { this.mode = newMode; }

    public void toggle(Minecraft client) {
        if (state == MacroState.IDLE) {
            state = MacroState.ALIGNING;
            rotationHandler.startRotation(defaultYaw, defaultPitch);
            if (client.player != null) {
                client.player.sendSystemMessage(Component.literal("§a[MobileMacro] Started! Mode: " + mode + " | Target: " + defaultYaw + " / " + defaultPitch));
            }
        } else {
            stop(client);
        }
    }

    public void stop(Minecraft client) {
        state = MacroState.IDLE;
        InputController.releaseAll(client);
        stuckTicks = 0;
        laneShiftTicks = 0;
        restartTicks = 0;
        if (client.player != null) {
            client.player.sendSystemMessage(Component.literal("§c[MobileMacro] Stopped!"));
        }
    }

    public void onTick(Minecraft client) {
        if (client.player == null || client.level == null || state == MacroState.IDLE) {
            return;
        }

        switch (state) {
            case ALIGNING:
                if (rotationHandler.updateRotation(client)) {
                    state = MacroState.FARMING;
                }
                break;
            case FARMING:
                handleFarming(client);
                break;
            case SHIFTING_LANE:
                handleLaneShift(client);
                break;
            case RESTARTING:
                handleRestart(client);
                break;
        }
    }

    private void handleFarming(Minecraft client) {
        InputController.setPressed(client.options.keyAttack, true);

        if (mode.equals("mushroom")) {
            // Mushroom mode strictly uses Forward (W) and Back (S)
            InputController.setPressed(client.options.keyLeft, false);
            InputController.setPressed(client.options.keyRight, false);
            
            if (isMovingForward) {
                InputController.setPressed(client.options.keyUp, true);
                InputController.setPressed(client.options.keyDown, false);
            } else {
                InputController.setPressed(client.options.keyUp, false);
                InputController.setPressed(client.options.keyDown, true);
            }
        } else {
            // Cane and Melon modes
            InputController.setPressed(client.options.keyDown, false); // S is never used here

            if (mode.equals("melon")) {
                InputController.setPressed(client.options.keyUp, true);
            } else {
                InputController.setPressed(client.options.keyUp, false);
            }

            if (isMovingLeft) {
                InputController.setPressed(client.options.keyLeft, true);
                InputController.setPressed(client.options.keyRight, false);
            } else {
                InputController.setPressed(client.options.keyRight, true);
                InputController.setPressed(client.options.keyLeft, false);
            }
        }

        double speed = client.player.getDeltaMovement().horizontalDistanceSqr();
        if (speed < 0.001) {
            stuckTicks++;
            if (stuckTicks > 5) {
                stuckTicks = 0;
                state = MacroState.SHIFTING_LANE;
            }
        } else {
            stuckTicks = 0;
        }
    }

    private void handleLaneShift(Minecraft client) {
        InputController.releaseAll(client);

        if (mode.equals("mushroom")) {
            // Instantly swap from Forward to Back (or Back to Forward)
            isMovingForward = !isMovingForward;
            laneShiftTicks = 0;
            state = MacroState.FARMING;
        } else if (mode.equals("melon")) {
            // Instantly swap from Left to Right
            isMovingLeft = !isMovingLeft;
            laneShiftTicks = 0;
            state = MacroState.FARMING;
        } else {
            // Cane logic: Step forward into next lane, then swap Left to Right
            if (laneShiftTicks < 6) {
                InputController.setPressed(client.options.keyUp, true);
                laneShiftTicks++;
            } else {
                InputController.setPressed(client.options.keyUp, false);
                isMovingLeft = !isMovingLeft;
                laneShiftTicks = 0;
                state = MacroState.FARMING;
            }
        }
    }

    private void handleRestart(Minecraft client) {
        InputController.releaseAll(client);

        if (restartTicks == 0) {
            if (client.player.connection != null) {
                client.player.connection.sendCommand("warp garden");
            }
        }
        restartTicks++;
        if (restartTicks > 100) {
            restartTicks = 0;
            state = MacroState.ALIGNING;
            rotationHandler.startRotation(defaultYaw, defaultPitch);
        }
    }

    public MacroState getState() { return state; }
}
