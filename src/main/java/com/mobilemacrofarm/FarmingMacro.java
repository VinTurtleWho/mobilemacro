package com.mobilemacrofarm;

import com.mobilemacrofarm.input.InputController;
import com.mobilemacrofarm.rotation.RotationHandler;
import com.mobilemacrofarm.state.MacroState;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import java.util.ArrayList;
import java.util.List;
import java.lang.reflect.Field;

public class FarmingMacro {
    private static final FarmingMacro INSTANCE = new FarmingMacro();

    private MacroState state = MacroState.IDLE;
    private final RotationHandler rotationHandler = new RotationHandler();

    private float defaultYaw = 45.0f;
    private float defaultPitch = 0.0f;
    private String mode = "cane"; 
    private boolean pestOnlyMode = false;

    private int toolSlot = 0;
    private int vacuumSlot = 1;

    private boolean isMovingLeft = true;
    private boolean isMovingForward = true; 
    private int stuckTicks = 0;
    private int jumpToggleTicks = 0;
    
    // GUI Randomizer Variables
    private int guiDelayTicks = 0;

    private final List<TickRecord> recordedPath = new ArrayList<>();
    private int replayIndex = 0;

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
    public void setEndBlock(Integer x, Integer y, Integer z) { this.endX = x; this.endY = y; this.endZ = z; }

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
        stuckTicks = 0; restartTicks = 0; preRestartWait = 0; jumpToggleTicks = 0; guiDelayTicks = 0;
        if (client.player != null) client.player.sendSystemMessage(Component.literal("§c[MobileMacro] Stopped!"));
    }

    // THE PEST TRIGGER (Called by our Chat Listener)
    public void triggerPestProtocol(Minecraft client) {
        if (state == MacroState.IDLE) return; // Don't trigger if macro is off
        
        InputController.releaseAll(client);
        if (client.player.connection != null) {
            client.player.connection.sendCommand("setspawnlocation"); // Anchor the exact lane spot!
            client.player.connection.sendCommand("desk");
        }
        state = MacroState.WAITING_FOR_DESK;
        client.player.sendSystemMessage(Component.literal("§e[MobileMacro] Pest detected! Scraping /desk..."));
    }

    private void equipSlot(Minecraft client, int slot) {
        if (client.player == null) return;
        try {
            Field field = net.minecraft.world.entity.player.Inventory.class.getDeclaredField("selected");
            field.setAccessible(true);
            field.set(client.player.getInventory(), slot);
        } catch (Exception e) {
            try {
                Field obfField = net.minecraft.world.entity.player.Inventory.class.getDeclaredField("field_7545");
                obfField.setAccessible(true);
                obfField.set(client.player.getInventory(), slot);
            } catch (Exception ex) { }
        }
        if (client.player.connection != null) client.player.connection.send(new ServerboundSetCarriedItemPacket(slot));
    }

    public void startRecording(Minecraft client) {
        recordedPath.clear(); state = MacroState.RECORDING;
        if (client.player != null) client.player.sendSystemMessage(Component.literal("§c[MobileMacro] Recording started..."));
    }

    public void startReplay(Minecraft client) {
        if (recordedPath.isEmpty()) return;
        InputController.releaseAll(client); 
        replayIndex = 0; state = MacroState.REPLAYING;
    }

    public void onTick(Minecraft client) {
        if (client.player == null || client.level == null) return;
        if (state == MacroState.IDLE) return;

        // GUI SAFETY CHECK (Bypassed if we are actively scraping the desk)
        boolean isGuiState = (state == MacroState.WAITING_FOR_DESK || state == MacroState.DESK_DELAY || state == MacroState.WAITING_FOR_PLOT);
        if (client.screen != null && !isGuiState) {
            InputController.releaseAll(client);
            return; 
        }

        if (state == MacroState.FARMING || state == MacroState.PEST_HUNTING || pestOnlyMode) {
            if (!client.player.getAbilities().flying && !client.player.onGround()) {
                jumpToggleTicks++;
                if (jumpToggleTicks == 1) InputController.setPressed(client.options.keyJump, true);
                else if (jumpToggleTicks == 2) InputController.setPressed(client.options.keyJump, false);
                else if (jumpToggleTicks == 3) InputController.setPressed(client.options.keyJump, true);
                else if (jumpToggleTicks >= 4) { InputController.setPressed(client.options.keyJump, false); if (jumpToggleTicks > 20) jumpToggleTicks = 0; }
            } else {
                if (jumpToggleTicks > 0) { InputController.setPressed(client.options.keyJump, false); jumpToggleTicks = 0; }
            }
        }

        switch (state) {
            case WAITING_FOR_DESK:
                if (client.screen instanceof AbstractContainerScreen) {
                    guiDelayTicks = 10 + (int)(Math.random() * 15); // Randomizer: Wait 0.5s to 1.25s
                    state = MacroState.DESK_DELAY;
                }
                break;
            case DESK_DELAY:
                guiDelayTicks--;
                if (guiDelayTicks <= 0 && client.screen instanceof AbstractContainerScreen) {
                    AbstractContainerScreen<?> screen = (AbstractContainerScreen<?>) client.screen;
                    boolean found = false;
                    for (Slot slot : screen.getMenu().slots) {
                        if (slot.getItem().getHoverName().getString().contains("Configure Plot")) {
                            // Mechanical, server-safe vanilla click
                            client.gameMode.handleInventoryMouseClick(screen.getMenu().containerId, slot.index, 0, ClickType.PICKUP, client.player);
                            found = true; break;
                        }
                    }
                    if (found) {
                        guiDelayTicks = 15 + (int)(Math.random() * 15); // Wait for next menu to load
                        state = MacroState.WAITING_FOR_PLOT;
                    }
                }
                break;
            case WAITING_FOR_PLOT:
                guiDelayTicks--;
                if (guiDelayTicks <= 0 && client.screen instanceof AbstractContainerScreen) {
                    AbstractContainerScreen<?> screen = (AbstractContainerScreen<?>) client.screen;
                    int targetPlot = -1;
                    
                    for (Slot slot : screen.getMenu().slots) {
                        ItemStack stack = slot.getItem();
                        String name = stack.getHoverName().getString();
                        if (name.contains("Plot")) {
                            boolean hasPest = false;
                            // Safe NBT extraction
                            List<Component> lore = stack.getTooltipLines(client.player, TooltipFlag.Default.NORMAL);
                            for (Component comp : lore) {
                                if (comp.getString().contains("Pests:")) { hasPest = true; break; }
                            }
                            if (hasPest) {
                                String cleaned = name.replaceAll("[^0-9]", ""); // Extract number
                                if (!cleaned.isEmpty()) { targetPlot = Integer.parseInt(cleaned); break; }
                            }
                        }
                    }
                    
                    client.player.closeContainer(); // Exit GUI safely
                    if (targetPlot != -1) {
                        if (client.player.connection != null) client.player.connection.sendCommand("tptoplot " + targetPlot);
                        client.player.sendSystemMessage(Component.literal("§a[MobileMacro] Target Plot: " + targetPlot + " found! Teleporting..."));
                        state = MacroState.PEST_FLY_UP; // Phase 2B starts here!
                    } else {
                        client.player.sendSystemMessage(Component.literal("§c[MobileMacro] No pests found in GUI! Resuming..."));
                        if (client.player.connection != null) client.player.connection.sendCommand("warp garden");
                        state = MacroState.ALIGNING;
                        rotationHandler.startRotation(defaultYaw, defaultPitch);
                    }
                }
                break;
            case PEST_FLY_UP:
                // Stub for Part 2B (Aimbot & Flight)
                client.player.sendSystemMessage(Component.literal("§e[MobileMacro] (Waiting for Aimbot Code)"));
                state = MacroState.IDLE;
                break;
            case ALIGNING:
                if (rotationHandler.updateRotation(client)) { equipSlot(client, toolSlot); state = MacroState.FARMING; }
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
            case RECORDING:
                handleRecording(client);
                break;
            case REPLAYING:
                handleReplaying(client);
                break;
            default:
                break;
        }
    }

    private void handleRecording(Minecraft client) { /* Hidden for brevity, same as before */ }
    private void handleReplaying(Minecraft client) { /* Hidden for brevity, same as before */ }
    private void handleFarming(Minecraft client) { /* Hidden for brevity, same as before */ }
    private void handleLaneShift(Minecraft client) { /* Hidden for brevity, same as before */ }
    private void handleRestart(Minecraft client) { /* Hidden for brevity, same as before */ }
    public MacroState getState() { return state; }
}
