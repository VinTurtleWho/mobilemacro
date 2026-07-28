package com.mobilemacrofarm;

import com.mobilemacrofarm.input.InputController;
import com.mobilemacrofarm.rotation.RotationHandler;
import com.mobilemacrofarm.state.MacroState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

public class FarmingMacro {
    private static final FarmingMacro INSTANCE = new FarmingMacro();

    private MacroState state = MacroState.IDLE;
    private final RotationHandler rotationHandler = new RotationHandler();

    private float defaultYaw = 45.0f;
    private float defaultPitch = 0.0f;

    private boolean isMovingLeft = true;
    private int stuckTicks = 0;
    private int laneShiftTicks = 0;
    private int restartTicks = 0;

    public static FarmingMacro getInstance() {
        return INSTANCE;
    }

    public void toggle(MinecraftClient client) {
        if (state == MacroState.IDLE) {
            state = MacroState.ALIGNING;
            rotationHandler.startRotation(defaultYaw, defaultPitch);
            if (client.player != null) {
                client.player.sendMessage(Text.literal("§a[MobileMacro] Started!"), false);
            }
        } else {
            stop(client);
        }
    }

    public void stop(MinecraftClient client) {
        state = MacroState.IDLE;
        InputController.releaseAll(client);
        stuckTicks = 0;
        laneShiftTicks = 0;
        restartTicks = 0;
        if (client.player != null) {
            client.player.sendMessage(Text.literal("§c[MobileMacro] Stopped!"), false);
        }
    }

    public void onTick(MinecraftClient client) {
        if (client.player == null || client.world == null || state == MacroState.IDLE) {
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

    private void handleFarming(MinecraftClient client) {
        InputController.setPressed(client.options.attackKey, true);

        if (isMovingLeft) {
            InputController.setPressed(client.options.leftKey, true);
            InputController.setPressed(client.options.rightKey, false);
        } else {
            InputController.setPressed(client.options.rightKey, true);
            InputController.setPressed(client.options.leftKey, false);
        }

        double speed = client.player.getVelocity().horizontalLengthSquared();
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

    private void handleLaneShift(MinecraftClient client) {
        InputController.releaseAll(client);

        if (laneShiftTicks < 6) {
            InputController.setPressed(client.options.forwardKey, true);
            laneShiftTicks++;
        } else {
            InputController.setPressed(client.options.forwardKey, false);
            isMovingLeft = !isMovingLeft;
            laneShiftTicks = 0;
            state = MacroState.FARMING;
        }
    }

    private void handleRestart(MinecraftClient client) {
        InputController.releaseAll(client);

        if (restartTicks == 0) {
            if (client.player.networkHandler != null) {
                client.player.networkHandler.sendCommand("warp garden");
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
