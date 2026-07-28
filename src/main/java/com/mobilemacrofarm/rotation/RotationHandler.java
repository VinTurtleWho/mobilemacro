package com.mobilemacrofarm.rotation;

import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.MathHelper;

public class RotationHandler {
    private float targetYaw;
    private float targetPitch;
    private boolean rotating = false;

    public void startRotation(float yaw, float pitch) {
        this.targetYaw = yaw;
        this.targetPitch = pitch;
        this.rotating = true;
    }

    public boolean updateRotation(MinecraftClient client) {
        if (!rotating || client.player == null) return true;

        float currentYaw = client.player.getYaw();
        float currentPitch = client.player.getPitch();

        float yawDiff = MathHelper.wrapDegrees(targetYaw - currentYaw);
        float pitchDiff = targetPitch - currentPitch;

        if (Math.abs(yawDiff) < 0.8f && Math.abs(pitchDiff) < 0.8f) {
            client.player.setYaw(targetYaw + (float)(Math.random() * 0.1 - 0.05));
            client.player.setPitch(targetPitch + (float)(Math.random() * 0.1 - 0.05));
            rotating = false;
            return true;
        }

        float stepYaw = yawDiff * 0.2f + (float)(Math.random() * 0.08 - 0.04);
        float stepPitch = pitchDiff * 0.2f + (float)(Math.random() * 0.08 - 0.04);

        client.player.setYaw(currentYaw + stepYaw);
        client.player.setPitch(currentPitch + stepPitch);

        return false;
    }

    public boolean isRotating() { return rotating; }
}
