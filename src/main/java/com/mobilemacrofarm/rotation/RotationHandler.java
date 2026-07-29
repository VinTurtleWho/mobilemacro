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

    public void updateTarget(float yaw, float pitch) {
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

        // DEADZONE: Stop exactly when the crosshair is centered (no micro-shaking)
        if (Math.abs(yawDiff) < 1.0f && Math.abs(pitchDiff) < 1.0f) {
            rotating = false;
            return true;
        }

        // GCD (Mouse Pixel) Calculation
        double sensitivity = client.options.sensitivity().get();
        float f = (float) (sensitivity * 0.6 + 0.2);
        float gcd = f * f * f * 8.0f;
        float stepMult = gcd * 0.15f; 

        // SMOOTH LINEAR EASE-OUT: Move exactly 20% of the remaining distance per tick.
        // Cap max turn speed at 25 degrees/tick to prevent SPEED_SIMULATION flags.
        float maxTurn = 25.0f; 
        float stepYaw = Math.max(-maxTurn, Math.min(maxTurn, yawDiff * 0.20f));
        float stepPitch = Math.max(-maxTurn, Math.min(maxTurn, pitchDiff * 0.20f));

        // Quantize the smooth curve to strict physical mouse pixels
        int mouseDeltaX = Math.round(stepYaw / stepMult);
        int mouseDeltaY = Math.round(stepPitch / stepMult);

        // Ensure we always move at least 1 pixel if we haven't reached the deadzone yet
        if (mouseDeltaX == 0 && Math.abs(yawDiff) > stepMult) mouseDeltaX = (int) Math.signum(yawDiff);
        if (mouseDeltaY == 0 && Math.abs(pitchDiff) > stepMult) mouseDeltaY = (int) Math.signum(pitchDiff);

        client.player.setYRot(currentYaw + ((float) mouseDeltaX * stepMult));
        client.player.setXRot(Math.max(-90.0f, Math.min(90.0f, currentPitch + ((float) mouseDeltaY * stepMult))));

        return false;
    }

    public boolean isRotating() { return rotating; }
}
