package com.mobilemacrofarm.input;

import net.minecraft.client.Minecraft;
import net.minecraft.client.KeyMapping;

public class InputController {

    public static void setPressed(KeyMapping keyMapping, boolean pressed) {
        if (keyMapping != null) {
            keyMapping.setDown(pressed);
        }
    }

    public static void releaseAll(Minecraft client) {
        if (client.options == null) return;
        client.options.keyUp.setDown(false);
        client.options.keyDown.setDown(false);
        client.options.keyLeft.setDown(false);
        client.options.keyRight.setDown(false);
        client.options.keyAttack.setDown(false);
        client.options.keyUse.setDown(false);
    }
}
