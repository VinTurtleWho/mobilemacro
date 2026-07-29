package com.mobilemacrofarm.rotation;

import net.minecraft.client.Minecraft;

public class RotationHandler {
    private float targetYaw = 0.0f;
    private float targetPitch = 0.0f;
    private boolean rotating = false;

    private int updateDelay = 0;

    public void startRotation(float yaw, float pitch) {
        this.targetYaw = yaw;
        this.targetPitch = pitch;
        this.rotating = true;
        this.updateDelay = 0;
    }

    public void updateTarget(float yaw, float pitch) {
        float yDiff = yaw - this.targetYaw;
        float pDiff = pitch - this.targetPitch;
        if (Math.hypot(yDiff, pDiff) > 1.0) {
            this.targetYaw = yaw;
            this.targetPitch = pitch;
        }
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

        double angularDistance = Math.hypot(yawDiff, pitchDiff);

        // HITBOX DEADZONE: Stop camera adjustments once crosshair is within ~1.8 degrees (on target)
        if (angularDistance < 1.8) {
            rotating = false;
            return true;
        }

        // HUMAN REACTION PAUSE: Simulate natural micro-pauses in touch/mouse swipes
        if (updateDelay > 0) {
            updateDelay--;
            return false;
        }

        double sensitivity = client.options.sensitivity().get();
        float f = (float) (sensitivity * 0.6 + 0.2);
        float gcd = f * f * f * 8.0f;
        float stepMult = gcd * 0.15f; 

        // UNPREDICTABLE STEP FACTOR: Destroys Spartan's m3-rnn geometric decay detection
        double randomFactor = 0.25 + (Math.random() * 0.35); 
        float stepYaw = (float) (yawDiff * randomFactor);
        float stepPitch = (float) (pitchDiff * randomFactor);

        int mouseDeltaX = (int) (stepYaw / stepMult);
        int mouseDeltaY = (int) (stepPitch / stepMult);

        if (mouseDeltaX == 0 && Math.abs(yawDiff) > stepMult) mouseDeltaX = (int) Math.signum(yawDiff);
        if (mouseDeltaY == 0 && Math.abs(pitchDiff) > stepMult) mouseDeltaY = (int) Math.signum(pitchDiff);

        // 15% chance to pause for 1 tick to mimic human swipe repositioning
        if (Math.random() < 0.15) {
            updateDelay = 1;
        }

        client.player.setYRot(currentYaw + ((float) mouseDeltaX * stepMult));
        client.player.setXRot(Math.max(-90.0f, Math.min(90.0f, currentPitch + ((float) mouseDeltaY * stepMult))));

        return false;
    }

    public boolean isRotating() { return rotating; }
}
