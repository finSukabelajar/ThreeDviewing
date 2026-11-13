package com.grafkom.camera;

import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * Kamera sintetis 3D (Synthetic Camera Model).
 * Menyimpan eye, target (At), dan up, serta menghitung View Matrix
 * (LookAt) SECARA MANUAL tanpa Matrix4f.lookAt().
 * Mendukung mode orbital: eye berputar mengelilingi target.
 */
public class Camera3D {

    public static final float MIN_ELEVATION = -89f;
    public static final float MAX_ELEVATION = 89f;
    public static final float MIN_RADIUS = 2f;
    public static final float MAX_RADIUS = 30f;

    private final Vector3f eye;
    private final Vector3f target;
    private final Vector3f up;

    // Koordinat sferis (relatif terhadap target)
    private float radius;
    private float azimuthDeg;
    private float elevationDeg;

    public Camera3D(Vector3f eye, Vector3f target, Vector3f up) {
        this.eye = new Vector3f(eye);
        this.target = new Vector3f(target);
        this.up = new Vector3f(up);

        // Turunkan koordinat sferis dari posisi eye awal
        Vector3f offset = new Vector3f(eye).sub(target);
        this.radius = offset.length();
        this.azimuthDeg = (float) Math.toDegrees(Math.atan2(offset.x, offset.z));
        this.elevationDeg = (float) Math.toDegrees(Math.asin(offset.y / radius));
    }

    /**
     * Matriks LookAt (View Matrix), dihitung manual:
     *   n = normalize(E - At)        (View Plane Normal)
     *   u = normalize(Up x n)        (Right vector)
     *   v = n x u                    (True Up vector)
     */
    public Matrix4f calculateLookAtMatrix() {
        Vector3f n = new Vector3f();
        eye.sub(target, n).normalize();

        Vector3f u = new Vector3f();
        up.cross(n, u).normalize();

        Vector3f v = new Vector3f();
        n.cross(u, v);

        // JOML: mXY = kolom X, baris Y. Jadi m30/m31/m32 = kolom translasi.
        Matrix4f view = new Matrix4f();
        view.m00(u.x).m10(u.y).m20(u.z).m30(-u.dot(eye));
        view.m01(v.x).m11(v.y).m21(v.z).m31(-v.dot(eye));
        view.m02(n.x).m12(n.y).m22(n.z).m32(-n.dot(eye));
        view.m03(0f).m13(0f).m23(0f).m33(1f);
        return view;
    }

    /** Hitung ulang posisi eye dari koordinat sferis, relatif terhadap target. */
    public void updateOrbitalPosition() {
        float azRad = (float) Math.toRadians(azimuthDeg);
        float elRad = (float) Math.toRadians(elevationDeg);

        eye.x = target.x + radius * (float) (Math.cos(elRad) * Math.sin(azRad));
        eye.y = target.y + radius * (float) Math.sin(elRad);
        eye.z = target.z + radius * (float) (Math.cos(elRad) * Math.cos(azRad));
    }

    /** Set parameter orbital sekaligus (dengan pembatasan nilai) lalu perbarui eye. */
    public void setOrbit(float radius, float azimuthDeg, float elevationDeg) {
        this.radius = Math.max(MIN_RADIUS, Math.min(MAX_RADIUS, radius));
        this.azimuthDeg = azimuthDeg;
        // Elevasi dibatasi agar Up tidak sejajar arah pandang (gimbal flip)
        this.elevationDeg = Math.max(MIN_ELEVATION, Math.min(MAX_ELEVATION, elevationDeg));
        updateOrbitalPosition();
    }

    // ---- Getter ----
    public Vector3f getEye()    { return new Vector3f(eye); }
    public Vector3f getTarget() { return new Vector3f(target); }
    public Vector3f getUp()     { return new Vector3f(up); }
    public float getRadius()       { return radius; }
    public float getAzimuthDeg()   { return azimuthDeg; }
    public float getElevationDeg() { return elevationDeg; }
}