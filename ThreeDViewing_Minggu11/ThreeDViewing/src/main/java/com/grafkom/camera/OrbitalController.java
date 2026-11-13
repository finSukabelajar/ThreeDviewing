package com.grafkom.camera;

import static org.lwjgl.glfw.GLFW.*;

/**
 * Mengontrol Camera3D dengan mouse:
 *  - Drag tombol kiri : memutar kamera (azimuth & elevasi)
 *  - Scroll           : zoom in/out (mengubah radius)
 * Gerakan dihaluskan (smoothing) agar navigasi terasa halus.
 */
public class OrbitalController {

    private static final float ROTATE_SPEED = 0.4f;  // derajat per piksel
    private static final float ZOOM_SPEED = 0.6f;    // satuan per langkah scroll
    private static final float SMOOTHING = 12f;      // makin besar makin responsif

    private final Camera3D camera;

    // Nilai tujuan (diisi oleh input), kamera bergerak menuju nilai ini
    private float targetAzimuth;
    private float targetElevation;
    private float targetRadius;

    private boolean dragging = false;
    private double lastX, lastY;

    public OrbitalController(long window, Camera3D camera) {
        this.camera = camera;
        this.targetAzimuth = camera.getAzimuthDeg();
        this.targetElevation = camera.getElevationDeg();
        this.targetRadius = camera.getRadius();

        glfwSetMouseButtonCallback(window, (win, button, action, mods) -> {
            if (button == GLFW_MOUSE_BUTTON_LEFT) {
                dragging = (action == GLFW_PRESS);
                if (dragging) {
                    double[] x = new double[1];
                    double[] y = new double[1];
                    glfwGetCursorPos(win, x, y);
                    lastX = x[0];
                    lastY = y[0];
                }
            }
        });

        glfwSetCursorPosCallback(window, (win, x, y) -> {
            if (dragging) {
                targetAzimuth -= (float) (x - lastX) * ROTATE_SPEED;
                targetElevation += (float) (y - lastY) * ROTATE_SPEED;
                targetElevation = Math.max(Camera3D.MIN_ELEVATION,
                        Math.min(Camera3D.MAX_ELEVATION, targetElevation));
            }
            lastX = x;
            lastY = y;
        });

        glfwSetScrollCallback(window, (win, dx, dy) -> {
            targetRadius -= (float) dy * ZOOM_SPEED;
            targetRadius = Math.max(Camera3D.MIN_RADIUS,
                    Math.min(Camera3D.MAX_RADIUS, targetRadius));
        });
    }

    /**
     * Pindahkan kamera ke sudut preset (animasi halus lewat smoothing).
     * Azimuth dipilih lewat jalur terpendek agar tidak berputar berlebihan.
     */
    public void lookFrom(float azimuthDeg, float elevationDeg) {
        float current = camera.getAzimuthDeg();
        float delta = ((azimuthDeg - current) % 360f + 540f) % 360f - 180f;
        targetAzimuth = current + delta;
        targetElevation = Math.max(Camera3D.MIN_ELEVATION,
                Math.min(Camera3D.MAX_ELEVATION, elevationDeg));
    }

    /** Dipanggil setiap frame. dt = selisih waktu antar frame (detik). */
    public void update(float dt) {
        // Interpolasi eksponensial: independen dari frame rate
        float k = 1f - (float) Math.exp(-SMOOTHING * dt);

        float az = camera.getAzimuthDeg();
        float el = camera.getElevationDeg();
        float r = camera.getRadius();

        camera.setOrbit(
                r + (targetRadius - r) * k,
                az + (targetAzimuth - az) * k,
                el + (targetElevation - el) * k);
    }
}