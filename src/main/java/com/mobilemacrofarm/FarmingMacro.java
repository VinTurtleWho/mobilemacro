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
    private String mode = "cane"; 

    private boolean isMovingLeft = true;
    private boolean isMovingForward = true; 
    private int stuckTicks = 0;
    private int laneShiftTicks = 0;
    
    // Restart Variables
    private Integer endX = null;
    private Integer endY = null;
    private Integer endZ = null;
    private int preRestartWait = 0;
    private int targetPreRestartWait = 20;
    private int restartTicks = 0;

    public static FarmingMacro getInstance() { return INSTANCE; }

    public void setYaw(float yaw) { this.defaultYaw = yaw; }
    public void setPitch(float pitch) { this.defaultPitch = pitch; }
    public void setMode(String newMode) { this.mode = newMode; }
    public void setEndBlock(Integer x, Integer y, Integer z) {
        this.endX = x;
        this.endY = y;
        this.endZ = z;
    }

    public void toggle(Minecraft client) {
        if (state == MacroState.IDLE) {
            state = MacroState.ALIGNING;
            rotationHandler.startRotation(defaultYaw, defaultPitch);
            if (client.player != null) {
                client.player.sendSystemMessage(Component.literal("§a[MobileMacro] Started! Mode: " + mode));
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
        preRestartWait = 0;
        if (client.player != null) {
            client.player.sendSystemMessage(Component.literal("§c[MobileMacro] Stopped!"));
        }
    }

    public void onTick(Minecraft client) {
        if (client.player == null || client.level == null || state == MacroState.IDLE) {
            return;
        }

        // GUI SAFETY CHECK: If a menu is open, release keys and pause logic
        if (client.screen != null) {
            InputController.releaseAll(client);
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
        // END BLOCK CHECK
        if (endX != null && endY != null && endZ != null) {
            if (client.player.getBlockX() == endX && 
                client.player.getBlockY() == endY && 
                client.player.getBlockZ() == endZ) {
                
                state = MacroState.RESTARTING;
                preRestartWait = 0;
                restartTicks = 0;
                // Randomize reaction time between 15 and 40 ticks (0.75s to 2.0s)
                targetPreRestartWait = 15 + (int)(Math.random() * 25); 
                return;
            }
        }

        InputController.setPressed(client.options.keyAttack, true);

        if (mode.equals("mushroom")) {
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
            InputController.setPressed(client.options.keyDown, false);
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
            isMovingForward = !isMovingForward;
            laneShiftTicks = 0;
            state = MacroState.FARMING;
        } else if (mode.equals("melon")) {
            isMovingLeft = !isMovingLeft;
            laneShiftTicks = 0;
            state = MacroState.FARMING;
        } else {
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
        
        preRestartWait++;
        
        // Wait for human-like reaction time before sending command
        if (preRestartWait == targetPreRestartWait) {
            if (client.player.connection != null) {
                client.player.connection.sendCommand("warp garden");
                client.player.sendSystemMessage(Component.literal("§e[MobileMacro] Restarting farm..."));
            }
        } else if (preRestartWait > targetPreRestartWait) {
            restartTicks++;
            // Wait 100 ticks (5 seconds) for the warp to load before realigning
            if (restartTicks > 100) {
                restartTicks = 0;
                preRestartWait = 0;
                state = MacroState.ALIGNING;
                rotationHandler.startRotation(defaultYaw, defaultPitch);
            }
        }
    }

    public MacroState getState() { return state; }
}
