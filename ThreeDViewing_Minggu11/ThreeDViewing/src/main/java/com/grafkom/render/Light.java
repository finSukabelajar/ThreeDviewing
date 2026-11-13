package com.grafkom.render;

import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * Cahaya directional: seluruh sinar sejajar, arahnya dari posisi cahaya menuju target.
 * Posisi cahaya hanya dipakai untuk menempatkan "kamera cahaya" pada shadow pass
 * dan untuk penanda visual. Proyeksi shadow map = ortografik.
 */
public class Light {

    private static final float ORTHO_HALF = 12f;
    private static final float NEAR = 2f;
    private static final float FAR = 30f;

    private final Vector3f target = new Vector3f(0f, 0f, 0f);
    private final Vector3f color = new Vector3f(1f, 0.98f, 0.92f);
    private float azimuthDeg = 35f;
    private float elevationDeg = 55f;
    private final float distance = 14f;

    public Vector3f getPosition() {
        float az = (float) Math.toRadians(azimuthDeg);
        float el = (float) Math.toRadians(elevationDeg);
        return new Vector3f(
                target.x + distance * (float) (Math.cos(el) * Math.sin(az)),
                target.y + distance * (float) Math.sin(el),
                target.z + distance * (float) (Math.cos(el) * Math.cos(az)));
    }

    /** Vektor satuan DARI permukaan MENUJU cahaya (L pada rumus N.L). */
    public Vector3f getDirectionToLight() {
        return getPosition().sub(target).normalize();
    }

    public Vector3f getColor() { return new Vector3f(color); }

    public void animate(float dt) {
        azimuthDeg = (azimuthDeg + 25f * dt) % 360f;
    }

    /** lightSpaceMatrix = Projection(ortho) * View(lookAt dari cahaya ke target). */
    public Matrix4f getLightSpaceMatrix() {
        Matrix4f proj = new Matrix4f().ortho(-ORTHO_HALF, ORTHO_HALF, -ORTHO_HALF, ORTHO_HALF, NEAR, FAR);
        Matrix4f view = new Matrix4f().lookAt(getPosition(), target, new Vector3f(0f, 1f, 0f));
        return proj.mul(view);
    }

    /** Selisih kedalaman world-space antara near dan far (untuk konversi bias). */
    public static float getDepthRange() { return FAR - NEAR; }
}
