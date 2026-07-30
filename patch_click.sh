sed -i '/private void safeClick/,/^\    }/c\
    private void safeClick(Minecraft client, AbstractContainerScreen<?> screen, Slot slot) {\
        if (client.gameMode != null && client.player != null) {\
            client.gameMode.handleInventoryMouseClick(screen.getMenu().containerId, slot.index, 0, net.minecraft.world.inventory.ClickType.QUICK_MOVE, client.player);\
        }\
    }' src/main/java/com/mobilemacrofarm/FarmingMacro.java
