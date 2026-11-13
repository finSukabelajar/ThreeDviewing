package com.grafkom.projection;

import org.joml.Matrix4f;

/**
 * Pengelola taksonomi proyeksi.
 *
 * Proyeksi Paralel : Orthographic, Isometric (aksonometrik), Oblique Cavalier
 * Proyeksi Perspektif : 1, 2, dan 3 titik hilang
 *
 * Perspektif 1/2/3 titik memakai matriks yang sama; perbedaannya ada pada
 * orientasi kamera terhadap objek (lihat preset azimuth/elevasi di enum).
 */
public class ProjectionTaxonomyManager {

    public enum ProjectionType {
        //               label                  azimuth  elevasi   perspektif?
        ORTHOGRAPHIC    ("Orthographic (Front)",    0f,   0f,      false),
        ISOMETRIC       ("Isometric",              45f,  35.264f,  false),
        OBLIQUE_CAVALIER("Oblique Cavalier",        0f,   0f,      false),
        PERSPECTIVE_1POINT("Perspective 1-Point",   0f,   0f,      true),
        PERSPECTIVE_2POINT("Perspective 2-Point",  35f,   0f,      true),
        PERSPECTIVE_3POINT("Perspective 3-Point",  35f,  30f,      true);

        private final String label;
        private final float azimuthDeg;
        private final float elevationDeg;
        private final boolean perspective;

        ProjectionType(String label, float azimuthDeg, float elevationDeg, boolean perspective) {
            this.label = label;
            this.azimuthDeg = azimuthDeg;
            this.elevationDeg = elevationDeg;
            this.perspective = perspective;
        }

        public String getLabel()       { return label; }
        public float getAzimuthDeg()   { return azimuthDeg; }
        public float getElevationDeg() { return elevationDeg; }
        public boolean isPerspective() { return perspective; }
    }

    // Parameter proyeksi oblique: panjang sumbu Z (Cavalier L=1, Cabinet L=0.5) dan sudutnya
    private static final float OBLIQUE_L = 1.0f;
    private static final float OBLIQUE_ANGLE_DEG = 45f;

    private final PerspectiveProjection perspective;
    private ProjectionType type = ProjectionType.PERSPECTIVE_3POINT;

    public ProjectionTaxonomyManager(PerspectiveProjection perspective) {
        this.perspective = perspective;
    }

    public ProjectionType getType() { return type; }

    public void setType(ProjectionType type) { this.type = type; }

    /**
     * Matriks proyeksi untuk mode aktif.
     * @param cameraRadius jarak kamera ke target; dipakai agar ukuran tampilan
     *        paralel mengikuti zoom (sama dengan ukuran perspektif di titik target).
     */
    public Matrix4f calculateProjectionMatrix(float cameraRadius) {
        if (type.isPerspective()) {
            return perspective.calculateProjectionMatrix();
        }

        float halfH = cameraRadius * (float) Math.tan(Math.toRadians(perspective.getFovDegree()) / 2.0);
        float halfW = halfH * perspective.getAspectRatio();

        Matrix4f ortho = new Matrix4f().ortho(
                -halfW, halfW, -halfH, halfH,
                perspective.getZNear(), perspective.getZFar());

        if (type == ProjectionType.OBLIQUE_CAVALIER) {
            // Shear relatif terhadap bidang target (z = -radius), bukan terhadap z = 0:
            //   x' = x + a*(z + radius),  y' = y + b*(z + radius)
            // Sehingga titik pada bidang target tidak bergeser (kubus tetap di tengah),
            // sedangkan titik yang lebih jauh bergeser ke kanan-atas.
            float rad = (float) Math.toRadians(OBLIQUE_ANGLE_DEG);
            float a = -OBLIQUE_L * (float) Math.cos(rad);
            float b = -OBLIQUE_L * (float) Math.sin(rad);
            Matrix4f shear = new Matrix4f()
                    .m20(a).m21(b)                                   // shear terhadap z
                    .m30(a * cameraRadius).m31(b * cameraRadius);    // kompensasi ke bidang target
            ortho.mul(shear); // ortho * shear -> shear dikenakan lebih dulu
        }
        return ortho;
    }
}