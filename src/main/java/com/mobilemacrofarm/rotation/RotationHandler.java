package com.mobilemacrofarm.rotation;

import net.minecraft.client.Minecraft;

public class RotationHandler {
    private float targetYaw = 0.0f;
    private float targetPitch = 0.0f;
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

        float yawDiff = targetYaw - currentYaw;
        float pitchDiff = targetPitch - currentPitch;

        while (yawDiff < -180.0f) yawDiff += 360.0f;
        while (yawDiff > 180.0f) yawDiff -= 360.0f;

        if (Math.abs(yawDiff) < 0.5f && Math.abs(pitchDiff) < 0.5f) {
            client.player.setYRot(targetYaw);
            client.player.setXRot(targetPitch);
            rotating = false;
            return true;
        }

        // GCD Calculation to bypass AimModulo360 checks in Grim AC
        double sensitivity = client.options.sensitivity().get();
        float f = (float) (sensitivity * 0.6 + 0.2);
        float gcd = f * f * f * 1.2f;

        float stepYaw = Math.max(0.5f, Math.min(Math.abs(yawDiff), 4.0f)) * Math.signum(yawDiff);
        float stepPitch = Math.max(0.5f, Math.min(Math.abs(pitchDiff), 2.0f)) * Math.signum(pitchDiff);

        stepYaw = Math.round(stepYaw / gcd) * gcd;
        stepPitch = Math.round(stepPitch / gcd) * gcd;

        client.player.setYRot(currentYaw + stepYaw);
        client.player.setXRot(Math.max(-90.0f, Math.min(90.0f, currentPitch + stepPitch)));

        return false;
    }

    public boolean isRotating() {
        return rotating;
    }
}
