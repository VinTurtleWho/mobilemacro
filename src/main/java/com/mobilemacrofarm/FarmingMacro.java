package com.mobilemacrofarm;

import com.mobilemacrofarm.input.InputController;
import com.mobilemacrofarm.rotation.RotationHandler;
import com.mobilemacrofarm.state.MacroState;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import java.util.ArrayList;
import java.util.List;

public class FarmingMacro {
    private static final FarmingMacro INSTANCE = new FarmingMacro();

    private MacroState state = MacroState.IDLE;
    private final RotationHandler rotationHandler = new RotationHandler();

    private float defaultYaw = 45.0f;
    private float defaultPitch = 0.0f;
    private String mode = "cane"; 
    private boolean pestOnlyMode = false;

    // Hotbar Slots (0-8)
    private int toolSlot = 0;
    private int vacuumSlot = 1;

    // Farming Variables
    private boolean isMovingLeft = true;
    private boolean isMovingForward = true; 
    private int stuckTicks = 0;
    private int jumpToggleTicks = 0;
    
    // Recorder Variables
    private final List<TickRecord> recordedPath = new ArrayList<>();
    private int replayIndex = 0;

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
    public void setPestOnly(boolean val) { this.pestOnlyMode = val; }
    public void setToolSlot(int slot) { this.toolSlot = slot; }
    public void setVacuumSlot(int slot) { this.vacuumSlot = slot; }
    public void setEndBlock(Integer x, Integer y, Integer z) {
        this.endX = x; this.endY = y; this.endZ = z;
    }

    public void toggle(Minecraft client) {
        if (state == MacroState.IDLE) {
            if (pestOnlyMode) {
                state = MacroState.PEST_ONLY_MODE;
                if (client.player != null) client.player.sendSystemMessage(Component.literal("§a[MobileMacro] Started PEST ONLY Mode!"));
            } else {
                state = MacroState.ALIGNING;
                rotationHandler.startRotation(defaultYaw, defaultPitch);
                if (client.player != null) client.player.sendSystemMessage(Component.literal("§a[MobileMacro] Started! Mode: " + mode));
            }
        } else {
            stop(client);
        }
    }

    public void stop(Minecraft client) {
        state = MacroState.IDLE;
        InputController.releaseAll(client);
        // Ensure hotbar keys are released
        InputController.setPressed(client.options.keyHotbarSlots[toolSlot], false);
        InputController.setPressed(client.options.keyHotbarSlots[vacuumSlot], false);
        stuckTicks = 0;
        restartTicks = 0;
        preRestartWait = 0;
        jumpToggleTicks = 0;
        if (client.player != null) client.player.sendSystemMessage(Component.literal("§c[MobileMacro] Stopped!"));
    }

    // --- RECORDER LOGIC ---
    public void startRecording(Minecraft client) {
        recordedPath.clear();
        state = MacroState.RECORDING;
        if (client.player != null) client.player.sendSystemMessage(Component.literal("§c[MobileMacro] Recording started... Walk the path!"));
    }

    public void startReplay(Minecraft client) {
        if (recordedPath.isEmpty()) {
            if (client.player != null) client.player.sendSystemMessage(Component.literal("§c[MobileMacro] No path recorded!"));
            return;
        }
        replayIndex = 0;
        state = MacroState.REPLAYING;
        if (client.player != null) client.player.sendSystemMessage(Component.literal("§a[MobileMacro] Replaying path..."));
    }

    public void onTick(Minecraft client) {
        if (client.player == null || client.level == null) return;

        if (state == MacroState.IDLE) return;

        // GUI SAFETY CHECK
        if (client.screen != null && state != MacroState.PEST_GUI_SCAN) {
            InputController.releaseAll(client);
            return; 
        }

        // MECHANICAL FLIGHT ENFORCER (Double tap spacebar if not flying)
        if (state == MacroState.FARMING || state == MacroState.PEST_HUNTING || pestOnlyMode) {
            if (!client.player.getAbilities().flying && !client.player.onGround()) {
                jumpToggleTicks++;
                if (jumpToggleTicks == 1) InputController.setPressed(client.options.keyJump, true);
                else if (jumpToggleTicks == 2) InputController.setPressed(client.options.keyJump, false);
                else if (jumpToggleTicks == 3) InputController.setPressed(client.options.keyJump, true);
                else if (jumpToggleTicks == 4) InputController.setPressed(client.options.keyJump, false);
            } else {
                jumpToggleTicks = 0;
            }
        }

        switch (state) {
            case RECORDING:
                handleRecording(client);
                break;
            case REPLAYING:
                handleReplaying(client);
                break;
            case ALIGNING:
                if (rotationHandler.updateRotation(client)) {
                    // Mechanically press the hotbar key to equip tool (Bypasses private access)
                    InputController.setPressed(client.options.keyHotbarSlots[toolSlot], true);
                    state = MacroState.FARMING;
                }
                break;
            case FARMING:
                // Release the hotbar key instantly so it's not held down forever
                InputController.setPressed(client.options.keyHotbarSlots[toolSlot], false);
                handleFarming(client);
                break;
            case SHIFTING_LANE:
                handleLaneShift(client);
                break;
            case RESTARTING:
                handleRestart(client);
                break;
            case PEST_ONLY_MODE:
                // Stub for Part 2
                break;
        }
    }

    private void handleRecording(Minecraft client) {
        TickRecord record = new TickRecord(
            client.options.keyUp.isDown(), client.options.keyLeft.isDown(),
            client.options.keyDown.isDown(), client.options.keyRight.isDown(),
            client.options.keyJump.isDown(), client.options.keyShift.isDown(),
            client.options.keyAttack.isDown(), client.options.keyUse.isDown(),
            client.player.getYRot(), client.player.getXRot()
        );
        recordedPath.add(record);
    }

    private void handleReplaying(Minecraft client) {
        if (replayIndex >= recordedPath.size()) {
            InputController.releaseAll(client);
            if (client.player.connection != null) client.player.connection.sendCommand("setspawnlocation");
            state = MacroState.ALIGNING;
            rotationHandler.startRotation(defaultYaw, defaultPitch);
            return;
        }

        TickRecord frame = recordedPath.get(replayIndex);
        InputController.setPressed(client.options.keyUp, frame.w);
        InputController.setPressed(client.options.keyLeft, frame.a);
        InputController.setPressed(client.options.keyDown, frame.s);
        InputController.setPressed(client.options.keyRight, frame.d);
        InputController.setPressed(client.options.keyJump, frame.space);
        
        client.player.setYRot(frame.yaw);
        client.player.setXRot(frame.pitch);
        
        replayIndex++;
    }

    private void handleFarming(Minecraft client) {
        if (endX != null && endY != null && endZ != null) {
            if (client.player.getBlockX() == endX && client.player.getBlockY() == endY && client.player.getBlockZ() == endZ) {
                if (!recordedPath.isEmpty()) {
                    startReplay(client);
                } else {
                    state = MacroState.RESTARTING;
                    preRestartWait = 0; restartTicks = 0;
                    targetPreRestartWait = 15 + (int)(Math.random() * 25); 
                }
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
        } else if (mode.equals("cane")) {
            InputController.setPressed(client.options.keyLeft, false);
            InputController.setPressed(client.options.keyDown, false);
            if (isMovingForward) {
                InputController.setPressed(client.options.keyUp, true);
                InputController.setPressed(client.options.keyRight, false);
            } else {
                InputController.setPressed(client.options.keyUp, false);
                InputController.setPressed(client.options.keyRight, true);
            }
        } else if (mode.equals("melon")) {
            InputController.setPressed(client.options.keyDown, false);
            InputController.setPressed(client.options.keyUp, true);
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
            if (stuckTicks > 5) { stuckTicks = 0; state = MacroState.SHIFTING_LANE; }
        } else { stuckTicks = 0; }
    }

    private void handleLaneShift(Minecraft client) {
        InputController.releaseAll(client);
        if (mode.equals("mushroom") || mode.equals("cane")) { isMovingForward = !is movingForward; } 
        else if (mode.equals("melon")) { isMovingLeft = !isMovingLeft; }
        state = MacroState.FARMING;
    }

    private void handleRestart(Minecraft client) {
        InputController.releaseAll(client);
        preRestartWait++;
        if (preRestartWait == targetPreRestartWait) {
            if (client.player.connection != null) {
                client.player.connection.sendCommand("warp garden");
                client.player.sendSystemMessage(Component.literal("§e[MobileMacro] Restarting farm..."));
            }
        } else if (preRestartWait > targetPreRestartWait) {
            restartTicks++;
            if (restartTicks > 100) {
                restartTicks = 0; preRestartWait = 0;
                state = MacroState.ALIGNING;
                rotationHandler.startRotation(defaultYaw, defaultPitch);
            }
        }
    }

    public MacroState getState() { return state; }
}
