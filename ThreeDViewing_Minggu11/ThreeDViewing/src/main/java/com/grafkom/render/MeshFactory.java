package com.grafkom.render;

/**
 * Pembuat geometri prosedural. Semua segitiga berurutan CCW jika dilihat dari luar
 * (syarat back-face culling). UV memakai konvensi OpenGL: v = 0 di bawah, v = 1 di atas.
 */
public final class MeshFactory {

    private MeshFactory() {}

    /** Bola UV. Lat-long, tanpa segitiga degenerate di kutub. */
    public static MeshData sphere(float radius, int slices, int stacks) {
        int vCount = (stacks + 1) * (slices + 1);
        float[] pos = new float[vCount * 3];
        float[] uv = new float[vCount * 2];
        int vi = 0;
        for (int i = 0; i <= stacks; i++) {
            double phi = Math.PI * i / stacks;                 // 0 (kutub atas) .. PI
            for (int j = 0; j <= slices; j++) {
                double theta = 2.0 * Math.PI * (j % slices) / slices; // j % slices: seam identik
                pos[3 * vi]     = (float) (radius * Math.sin(phi) * Math.cos(theta));
                pos[3 * vi + 1] = (float) (radius * Math.cos(phi));
                pos[3 * vi + 2] = (float) (radius * Math.sin(phi) * Math.sin(theta));
                uv[2 * vi]      = (float) j / slices;
                uv[2 * vi + 1]  = 1f - (float) i / stacks;
                vi++;
            }
        }

        int[] tmp = new int[stacks * slices * 6];
        int n = 0;
        for (int i = 0; i < stacks; i++) {
            for (int j = 0; j < slices; j++) {
                int a = i * (slices + 1) + j;
                int b = a + 1;
                int c = (i + 1) * (slices + 1) + j;
                int d = c + 1;
                if (i != 0)          { tmp[n++] = a; tmp[n++] = b; tmp[n++] = c; }
                if (i != stacks - 1) { tmp[n++] = b; tmp[n++] = d; tmp[n++] = c; }
            }
        }
        return new MeshData(pos, uv, java.util.Arrays.copyOf(tmp, n));
    }

    /** Kubus sisi 2*half, berpusat di (0,0,0); 24 vertex (UV per sisi). */
    public static MeshData cube(float half) {
        float[][] v = {
            {-1, -1,  1}, { 1, -1,  1}, { 1,  1,  1}, {-1,  1,  1},
            {-1, -1, -1}, { 1, -1, -1}, { 1,  1, -1}, {-1,  1, -1}
        };
        int[][] faces = {          // CCW dari luar (sama dengan Mesh3D Minggu 10)
            {0, 1, 2, 3}, {5, 4, 7, 6}, {4, 0, 3, 7},
            {1, 5, 6, 2}, {3, 2, 6, 7}, {4, 5, 1, 0}
        };
        float[][] corner = {{0, 0}, {1, 0}, {1, 1}, {0, 1}};

        float[] pos = new float[24 * 3];
        float[] uv = new float[24 * 2];
        int[] idx = new int[36];
        for (int f = 0; f < 6; f++) {
            for (int k = 0; k < 4; k++) {
                int vi = f * 4 + k;
                float[] p = v[faces[f][k]];
                pos[3 * vi] = p[0] * half; pos[3 * vi + 1] = p[1] * half; pos[3 * vi + 2] = p[2] * half;
                uv[2 * vi] = corner[k][0];  uv[2 * vi + 1] = corner[k][1];
            }
            int b = f * 4;
            int o = f * 6;
            idx[o] = b;     idx[o + 1] = b + 1; idx[o + 2] = b + 2;
            idx[o + 3] = b; idx[o + 4] = b + 2; idx[o + 5] = b + 3;
        }
        return new MeshData(pos, uv, idx);
    }

    /** Torus dengan sumbu Y. Cekung + cembung: bagus untuk uji normal dan self-shadow. */
    public static MeshData torus(float majorR, float minorR, int rings, int sides,
                                 float uRepeat, float vRepeat) {
        int vCount = (rings + 1) * (sides + 1);
        float[] pos = new float[vCount * 3];
        float[] uv = new float[vCount * 2];
        int vi = 0;
        for (int i = 0; i <= rings; i++) {
            double u = 2.0 * Math.PI * (i % rings) / rings;
            for (int j = 0; j <= sides; j++) {
                double v = 2.0 * Math.PI * (j % sides) / sides;
                double ring = majorR + minorR * Math.cos(v);
                pos[3 * vi]     = (float) (ring * Math.cos(u));
                pos[3 * vi + 1] = (float) (minorR * Math.sin(v));
                pos[3 * vi + 2] = (float) (ring * Math.sin(u));
                uv[2 * vi]      = uRepeat * i / rings;
                uv[2 * vi + 1]  = vRepeat * j / sides;
                vi++;
            }
        }
        int[] idx = new int[rings * sides * 6];
        int n = 0;
        for (int i = 0; i < rings; i++) {
            for (int j = 0; j < sides; j++) {
                int a = i * (sides + 1) + j;
                int b = a + 1;
                int c = (i + 1) * (sides + 1) + j;
                int d = c + 1;
                idx[n++] = a; idx[n++] = b; idx[n++] = c;
                idx[n++] = b; idx[n++] = d; idx[n++] = c;
            }
        }
        return new MeshData(pos, uv, idx);
    }

    /** Bidang datar di y = 0, normal +Y, UV diulang (tiling) uvRepeat kali. */
    public static MeshData plane(float half, float uvRepeat) {
        float[] pos = {
            -half, 0,  half,
             half, 0,  half,
             half, 0, -half,
            -half, 0, -half
        };
        float[] uv = { 0, 0,  uvRepeat, 0,  uvRepeat, uvRepeat,  0, uvRepeat };
        int[] idx = { 0, 1, 2,  0, 2, 3 };
        return new MeshData(pos, uv, idx);
    }
}
