package com.grafkom.projection;

import org.joml.Matrix4f;

/**
 * Proyeksi perspektif berbasis Viewing Frustum.
 * Parameter: FOV vertikal, aspect ratio, near plane, far plane.
 * Matriks dihitung eksplisit (tanpa Matrix4f.perspective()).
 */
public class PerspectiveProjection {

    public static final float MIN_FOV = 20f;
    public static final float MAX_FOV = 120f;

    private float fovDegree;
    private float aspectRatio;
    private float zNear;
    private float zFar;

    public PerspectiveProjection(float fovDegree, float aspectRatio, float zNear, float zFar) {
        setFovDegree(fovDegree);
        this.aspectRatio = aspectRatio;
        this.zNear = zNear;
        this.zFar = zFar;
    }

    /**
     * P = | f/aspect  0   0                    0                     |
     *     | 0         f   0                    0                     |
     *     | 0         0   -(far+near)/(far-near)  -2*far*near/(far-near) |
     *     | 0         0   -1                   0                     |
     * dengan f = 1 / tan(FOV/2)
     */
    public Matrix4f calculateProjectionMatrix() {
        float fovRad = (float) Math.toRadians(fovDegree);
        float f = 1.0f / (float) Math.tan(fovRad / 2.0f);

        Matrix4f proj = new Matrix4f();
        proj.zero();
        proj.m00(f / aspectRatio);
        proj.m11(f);
        proj.m22(-(zFar + zNear) / (zFar - zNear));
        proj.m32(-(2.0f * zFar * zNear) / (zFar - zNear));
        proj.m23(-1.0f);
        proj.m33(0.0f);
        return proj;
    }

    // ---- Getter / Setter ----
    public float getFovDegree() { return fovDegree; }

    public void setFovDegree(float fovDegree) {
        this.fovDegree = Math.max(MIN_FOV, Math.min(MAX_FOV, fovDegree));
    }

    public float getAspectRatio() { return aspectRatio; }

    public void setAspectRatio(float aspectRatio) { this.aspectRatio = aspectRatio; }

    public float getZNear() { return zNear; }

    public float getZFar() { return zFar; }
}