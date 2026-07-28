package com.mobilemacrofarm.input;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;

public class InputController {

    public static void setPressed(KeyBinding keyBinding, boolean pressed) {
        if (keyBinding != null) {
            keyBinding.setPressed(pressed);
        }
    }

    public static void releaseAll(MinecraftClient client) {
        if (client.options == null) return;
        client.options.forwardKey.setPressed(false);
        client.options.backKey.setPressed(false);
        client.options.leftKey.setPressed(false);
        client.options.rightKey.setPressed(false);
        client.options.attackKey.setPressed(false);
        client.options.useKey.setPressed(false);
    }
}
