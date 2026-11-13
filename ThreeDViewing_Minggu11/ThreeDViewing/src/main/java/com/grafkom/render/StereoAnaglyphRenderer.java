package com.grafkom.render;

import com.grafkom.camera.Camera3D;
import com.grafkom.projection.PerspectiveProjection;
import org.joml.Matrix4f;

import java.util.function.BiConsumer;

import static org.lwjgl.opengl.GL33.*;

/**
 * Renderer stereoskopik Anaglyph Merah-Cyan dengan metode Off-Axis
 * (asymmetric frustum).
 *
 * Mata kiri  -> hanya kanal MERAH   (kaca merah di mata kiri)
 * Mata kanan -> hanya kanal HIJAU+BIRU = CYAN (kaca cyan di mata kanan)
 *
 * Untuk tiap mata:
 *  1. Kamera digeser sejauh +/- IPD/2 sepanjang sumbu kanan kamera.
 *  2. Frustum digeser berlawanan arah di near plane, sehingga semua mata
 *     "bertemu" di bidang fokus (zero parallax) pada jarak focalLength.
 */
public class StereoAnaglyphRenderer {

    public static final float MIN_IPD = 0.0f;
    public static final float MAX_IPD = 1.5f;

    private float ipd = 0.25f;          // jarak antar mata (satuan dunia)
    private float focalLength = 8.0f;   // jarak ke bidang zero-parallax

    /**
     * @param renderScene callback yang menggambar adegan untuk satu mata. Menerima
     *                    (projection, view) mata tersebut; tidak boleh membersihkan layar.
     */
    public void renderAnaglyph(Camera3D camera, PerspectiveProjection perspective,
                                BiConsumer<Matrix4f, Matrix4f> renderScene) {
        Matrix4f view = camera.calculateLookAtMatrix();

        // 0. Bersihkan SEMUA kanal terlebih dahulu (mencegah ghosting)
        glColorMask(true, true, true, true);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

        // PASS 1: mata kiri -> kanal merah saja
        glColorMask(true, false, false, true);
        renderEye(-ipd / 2f, view, perspective, renderScene);

        // Depth dibersihkan, color TIDAK (agar hasil pass 1 tetap ada)
        glClear(GL_DEPTH_BUFFER_BIT);

        // PASS 2: mata kanan -> kanal hijau + biru (cyan)
        glColorMask(false, true, true, true);
        renderEye(+ipd / 2f, view, perspective, renderScene);

        // Kembalikan ke kondisi normal
        glColorMask(true, true, true, true);
    }

    private void renderEye(float eyeOffset, Matrix4f view,
                           PerspectiveProjection perspective, BiConsumer<Matrix4f, Matrix4f> renderScene) {
        Matrix4f projection = offAxisProjection(perspective, eyeOffset);

        // Mata di +e sepanjang sumbu kanan kamera = dunia tampak bergeser -e di view space
        Matrix4f eyeView = new Matrix4f().translation(-eyeOffset, 0f, 0f).mul(view);

        renderScene.accept(projection, eyeView);
    }

    /**
     * Matriks frustum asimetris (Off-Axis), dihitung eksplisit:
     *
     *  | 2n/(r-l)  0         (r+l)/(r-l)   0            |
     *  | 0         2n/(t-b)  (t+b)/(t-b)   0            |
     *  | 0         0         -(f+n)/(f-n)  -2fn/(f-n)   |
     *  | 0         0         -1            0            |
     */
    private Matrix4f offAxisProjection(PerspectiveProjection p, float eyeOffset) {
        float n = p.getZNear();
        float f = p.getZFar();

        // Jendela simetris pada near plane
        float top = n * (float) Math.tan(Math.toRadians(p.getFovDegree()) / 2.0);
        float bottom = -top;
        float right = top * p.getAspectRatio();
        float left = -right;

        // Geser jendela berlawanan dengan arah geser mata
        float shift = eyeOffset * n / focalLength;
        left -= shift;
        right -= shift;

        Matrix4f m = new Matrix4f();
        m.zero();
        m.m00(2f * n / (right - left));
        m.m11(2f * n / (top - bottom));
        m.m20((right + left) / (right - left));
        m.m21((top + bottom) / (top - bottom));
        m.m22(-(f + n) / (f - n));
        m.m32(-(2f * f * n) / (f - n));
        m.m23(-1f);
        m.m33(0f);
        return m;
    }

    // ---- Getter / Setter ----
    public float getIpd() { return ipd; }

    public void setIpd(float ipd) {
        this.ipd = Math.max(MIN_IPD, Math.min(MAX_IPD, ipd));
    }

    public float getFocalLength() { return focalLength; }

    public void setFocalLength(float focalLength) {
        this.focalLength = Math.max(0.5f, focalLength);
    }
}