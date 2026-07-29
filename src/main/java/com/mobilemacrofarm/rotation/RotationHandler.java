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

        // Vanilla Minecraft Mouse Sensitivity Math
        double sensitivity = client.options.sensitivity().get();
        float f = (float) (sensitivity * 0.6 + 0.2);
        float gcd = f * f * f * 8.0f;
        float stepMult = gcd * 0.15f; // The exact float delta per 1 pixel of mouse movement

        // CRITICAL FIX: If we are within 1 mouse pixel of the target, STOP. 
        // Do NOT snap to the perfect target. Leave it on the messy decimal.
        if (Math.abs(yawDiff) <= stepMult && Math.abs(pitchDiff) <= stepMult) {
            rotating = false;
            return true;
        }

        // Simulate physical integer mouse movements (how many pixels to move)
        int mouseDeltaX = (int) (yawDiff / stepMult);
        int mouseDeltaY = (int) (pitchDiff / stepMult);

        // Limit speed to look human (max 20 pixels per tick)
        int maxSpeed = 20;
        mouseDeltaX = Math.max(-maxSpeed, Math.min(maxSpeed, mouseDeltaX));
        mouseDeltaY = Math.max(-maxSpeed, Math.min(maxSpeed, mouseDeltaY));

        // Force at least 1 pixel of movement if we haven't reached the threshold
        if (mouseDeltaX == 0 && Math.abs(yawDiff) > stepMult) mouseDeltaX = (int) Math.signum(yawDiff);
        if (mouseDeltaY == 0 && Math.abs(pitchDiff) > stepMult) mouseDeltaY = (int) Math.signum(pitchDiff);

        // Apply exactly like vanilla MouseHandler
        client.player.setYRot(currentYaw + ((float) mouseDeltaX * stepMult));
        client.player.setXRot(Math.max(-90.0f, Math.min(90.0f, currentPitch + ((float) mouseDeltaY * stepMult))));

        return false;
    }

    public boolean isRotating() {
        return rotating;
    }
}
