package com.mobilemacrofarm.input;

import net.minecraft.client.Minecraft;
import net.minecraft.client.KeyMapping;

public class InputController {
    public static void setPressed(KeyMapping key, boolean pressed) {
        KeyMapping.set(key.getKey(), pressed);
    }

    public static void releaseAll(Minecraft client) {
        if (client == null) return;
        KeyMapping.set(client.options.keyUp.getKey(), false);
        KeyMapping.set(client.options.keyDown.getKey(), false);
        KeyMapping.set(client.options.keyLeft.getKey(), false);
        KeyMapping.set(client.options.keyRight.getKey(), false);
        KeyMapping.set(client.options.keyJump.getKey(), false);
        KeyMapping.set(client.options.keyShift.getKey(), false);
        KeyMapping.set(client.options.keyAttack.getKey(), false);
        KeyMapping.set(client.options.keyUse.getKey(), false);
    }
}
