package com.grafkom.render;

import static org.lwjgl.opengl.GL33.*;

/**
 * Hidden Surface Removal: Z-Buffer (depth test) dan Back-Face Culling.
 * Keduanya bisa dihidupkan/dimatikan saat runtime untuk perbandingan visual.
 */
public final class HSRManager {

    private static boolean depthTestEnabled = true;
    private static boolean cullingEnabled = true;

    private HSRManager() {}

    public static void setupDepthTest(boolean enable) {
        depthTestEnabled = enable;
        if (enable) {
            glEnable(GL_DEPTH_TEST);
            glDepthFunc(GL_LESS);          // fragmen lebih dekat menang
        } else {
            glDisable(GL_DEPTH_TEST);      // urutan gambar menentukan hasil (painter's)
        }
    }

    public static void setupBackFaceCulling(boolean enable) {
        cullingEnabled = enable;
        if (enable) {
            glEnable(GL_CULL_FACE);
            glCullFace(GL_BACK);           // buang sisi belakang
            glFrontFace(GL_CCW);           // CCW = sisi depan
        } else {
            glDisable(GL_CULL_FACE);
        }
    }

    public static void toggleDepthTest()    { setupDepthTest(!depthTestEnabled); }
    public static void toggleCulling()      { setupBackFaceCulling(!cullingEnabled); }

    /** Terapkan ulang status saat ini (dipanggil di awal pass utama). */
    public static void apply() {
        setupDepthTest(depthTestEnabled);
        setupBackFaceCulling(cullingEnabled);
    }

    /** Shadow pass selalu butuh depth test, tidak peduli pilihan pengguna. */
    public static void enterShadowPass() {
        glEnable(GL_DEPTH_TEST);
        glDepthFunc(GL_LESS);
        glDisable(GL_CULL_FACE);
    }

    public static boolean isDepthTestEnabled() { return depthTestEnabled; }
    public static boolean isCullingEnabled()   { return cullingEnabled; }
}
