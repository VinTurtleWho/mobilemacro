package net.minecraft.world.entity.player;

public class InventoryHelper {
    public static void setSlot(Inventory inv, int slot) {
        inv.selected = slot; // VIP access to the locked variable
    }
}
