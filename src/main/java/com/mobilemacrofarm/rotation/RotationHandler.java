package com.mobilemacrofarm.rotation;

import net.minecraft.client.Minecraft;

public class RotationHandler {
    private float targetYaw = 0.0f;
    private float targetPitch = 0.0f;
    private boolean rotating = false;

    public void startRotation(float yaw, float pitch) {
        // Picks a random static offset on the body instead of vibrating
        this.targetYaw = yaw + (float) ((Math.random() - 0.5) * 1.5);
        this.targetPitch = pitch + (float) ((Math.random() - 0.5) * 1.5);
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

        double sensitivity = client.options.sensitivity().get();
        float f = (float) (sensitivity * 0.6 + 0.2);
        float gcd = f * f * f * 8.0f;
        float stepMult = gcd * 0.15f; 

        if (Math.abs(yawDiff) <= stepMult && Math.abs(pitchDiff) <= stepMult) {
            rotating = false;
            return true;
        }

        int mouseDeltaX = (int) (yawDiff / stepMult);
        int mouseDeltaY = (int) (pitchDiff / stepMult);

        double angularDistance = Math.hypot(yawDiff, pitchDiff);
        int baseSpeed = (int) Math.min(130, Math.max(12, angularDistance * 4.5));
        
        int maxSpeedX = baseSpeed + (int)(Math.random() * 25);
        int maxSpeedY = (int)(baseSpeed * 0.7) + (int)(Math.random() * 15);

        mouseDeltaX = Math.max(-maxSpeedX, Math.min(maxSpeedX, mouseDeltaX));
        mouseDeltaY = Math.max(-maxSpeedY, Math.min(maxSpeedY, mouseDeltaY));

        if (mouseDeltaX == 0 && Math.abs(yawDiff) > stepMult) mouseDeltaX = (int) Math.signum(yawDiff);
        if (mouseDeltaY == 0 && Math.abs(pitchDiff) > stepMult) mouseDeltaY = (int) Math.signum(pitchDiff);

        client.player.setYRot(currentYaw + ((float) mouseDeltaX * stepMult));
        client.player.setXRot(Math.max(-90.0f, Math.min(90.0f, currentPitch + ((float) mouseDeltaY * stepMult))));

        return false;
    }

    public boolean isRotating() { return rotating; }
}
