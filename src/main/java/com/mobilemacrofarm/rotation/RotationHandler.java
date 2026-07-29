package com.mobilemacrofarm.rotation;

import net.minecraft.client.Minecraft;

public class RotationHandler {
    private float targetYaw = 0.0f;
    private float targetPitch = 0.0f;
    private boolean rotating = false;
    
    private int reactionDelay = 0;
    private float previousRealYaw = 0.0f;

    public void startRotation(float yaw, float pitch) {
        this.targetYaw = yaw;
        this.targetPitch = pitch;
        this.rotating = true;
        this.reactionDelay = 0;
    }

    public void updateTarget(float yaw, float pitch) {
        // REACTION TIME SIMULATOR: If the target suddenly changes direction, pause tracking for a few ticks
        float shift = yaw - previousRealYaw;
        while (shift < -180.0f) shift += 360.0f;
        while (shift > 180.0f) shift -= 360.0f;
        
        if (Math.abs(shift) > 8.0f && reactionDelay <= 0) {
            reactionDelay = 3 + (int)(Math.random() * 3); // 3 to 5 ticks of human "lag"
        }
        previousRealYaw = yaw;

        if (reactionDelay > 0) {
            reactionDelay--;
            return; // Freeze the crosshair while the "human" is reacting
        }

        // LAZY TRACKING: Ease into the new target coordinates instead of snapping
        float yDiff = yaw - this.targetYaw;
        while (yDiff < -180.0f) yDiff += 360.0f;
        while (yDiff > 180.0f) yDiff -= 360.0f;
        this.targetYaw += yDiff * 0.35f;

        float pDiff = pitch - this.targetPitch;
        this.targetPitch += pDiff * 0.35f;
        
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

        if (Math.abs(yawDiff) < 1.5 && Math.abs(pitchDiff) < 1.5) {
            rotating = false;
            return true;
        }

        double sensitivity = client.options.sensitivity().get();
        float f = (float) (sensitivity * 0.6 + 0.2);
        float gcd = f * f * f * 8.0f;
        float stepMult = gcd * 0.15f; 

        // SPEED CLAMP: Fixes the SPEED_SIMULATION flags by preventing impossible camera flicks
        double angularDistance = Math.hypot(yawDiff, pitchDiff);
        int baseSpeed = (int) Math.min(60, Math.max(5, angularDistance * 2.5)); // Hard-capped at 60
        
        int maxSpeedX = baseSpeed + (int)(Math.random() * 10);
        int maxSpeedY = (int)(baseSpeed * 0.6) + (int)(Math.random() * 8);

        int mouseDeltaX = (int) (yawDiff / stepMult);
        int mouseDeltaY = (int) (pitchDiff / stepMult);

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
