package com.mobilemacrofarm.rotation;

import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;

public class RotationHandler {
    private float targetYaw;
    private float targetPitch;
    private boolean rotating = false;

    public void startRotation(float yaw, float pitch) {
        this.targetYaw = yaw;
        this.targetPitch = pitch;
        this.rotating = true;
    }

    public boolean updateRotation(Minecraft client) {
        if (!rotating || client.player == null) return true;

        float currentYaw = client.player.getYRot();
        float currentPitch = client.player.getXRot();

        float yawDiff = Mth.wrapDegrees(targetYaw - currentYaw);
        float pitchDiff = targetPitch - currentPitch;

        if (Math.abs(yawDiff) < 0.8f && Math.abs(pitchDiff) < 0.8f) {
            client.player.setYRot(targetYaw + (float)(Math.random() * 0.1 - 0.05));
            client.player.setXRot(targetPitch + (float)(Math.random() * 0.1 - 0.05));
            rotating = false;
            return true;
        }

        float stepYaw = yawDiff * 0.2f + (float)(Math.random() * 0.08 - 0.04);
        float stepPitch = pitchDiff * 0.2f + (float)(Math.random() * 0.08 - 0.04);

        client.player.setYRot(currentYaw + stepYaw);
        client.player.setXRot(currentPitch + stepPitch);

        return false;
    }

    public boolean isRotating() { return rotating; }
}
