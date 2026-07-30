sed -i '/private void safeClick/,/^\    }/c\
    private void safeClick(Minecraft client, AbstractContainerScreen<?> screen, Slot slot) {\
        try {\
            Class<?> clickTypeClass = Class.forName("net.minecraft.world.inventory.ClickType"); Object pickup = null;\
            for (Object ec : clickTypeClass.getEnumConstants()) { if (ec.toString().equals("QUICK_MOVE") || ec.toString().equals("SHIFT_CLICK")) { pickup = ec; break; } }\
            boolean clicked = false;\
            for (Method m : client.gameMode.getClass().getMethods()) {\
                String n = m.getName();\
                if ((n.equals("handleInventoryMouseClick") || n.equals("clickSlot") || n.equals("method_2906")) && m.getParameterCount() == 5) {\
                    m.invoke(client.gameMode, screen.getMenu().containerId, slot.index, 0, pickup, client.player); clicked = true; break;\
                }\
            }\
            if (!clicked && client.player != null) client.player.sendSystemMessage(Component.literal("§c[MobileMacro] Click method not found! (Obfuscation issue)"));\
        } catch (Exception e) { }\
    }' src/main/java/com/mobilemacrofarm/FarmingMacro.java
