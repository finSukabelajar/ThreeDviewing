package com.grafkom.render;

import org.joml.Vector3f;

import java.util.HashMap;
import java.util.Map;

/**
 * Geometri mentah di CPU: posisi, UV, dan indeks segitiga (CCW dari luar).
 * Dari data yang sama dibuat dua versi mesh:
 *
 *  - buildSmooth(): vertex dibagi antar segitiga. Normal vertex = rata-rata normal
 *    semua face yang menyentuh titik itu, dibobot sudut interior face pada titik tsb:
 *        N_v = sum(w_i * N_face_i) / ||sum(w_i * N_face_i)||,   w_i = sudut face i di vertex v.
 *    Tanpa bobot (w_i = 1), hasilnya bergantung pada cara triangulasi: pada kubus, vertex
 *    yang berada di diagonal sisi dihitung 2x sehingga normal sudut menjadi tidak simetris.
 *    Vertex dilas berdasarkan POSISI, sehingga duplikat vertex akibat seam UV
 *    (atau kutub bola) tetap mendapat normal yang sama.
 *
 *  - buildFlat(): setiap segitiga memiliki 3 vertex sendiri dengan normal face.
 */
public final class MeshData {

    private final float[] positions; // xyz
    private final float[] uvs;       // uv
    private final int[] indices;

    public MeshData(float[] positions, float[] uvs, int[] indices) {
        this.positions = positions;
        this.uvs = uvs;
        this.indices = indices;
    }

    private record PosKey(long x, long y, long z) {
        static PosKey of(float[] p, int i) {
            return new PosKey(Math.round(p[3 * i] * 10000.0),
                              Math.round(p[3 * i + 1] * 10000.0),
                              Math.round(p[3 * i + 2] * 10000.0));
        }
    }

    private Vector3f faceNormal(int i0, int i1, int i2) {
        Vector3f p0 = new Vector3f(positions[3 * i0], positions[3 * i0 + 1], positions[3 * i0 + 2]);
        Vector3f p1 = new Vector3f(positions[3 * i1], positions[3 * i1 + 1], positions[3 * i1 + 2]);
        Vector3f p2 = new Vector3f(positions[3 * i2], positions[3 * i2 + 1], positions[3 * i2 + 2]);
        Vector3f n = p1.sub(p0, new Vector3f()).cross(p2.sub(p0, new Vector3f()));
        float len = n.length();
        if (len < 1e-8f) return null;      // segitiga degenerate (luas nol)
        return n.div(len);
    }

    /** Sudut interior segitiga (radian) pada vertex ia, di antara sisi ia->ib dan ia->ic. */
    private float cornerAngle(int ia, int ib, int ic) {
        Vector3f a = new Vector3f(positions[3 * ia], positions[3 * ia + 1], positions[3 * ia + 2]);
        Vector3f e1 = new Vector3f(positions[3 * ib], positions[3 * ib + 1], positions[3 * ib + 2]).sub(a).normalize();
        Vector3f e2 = new Vector3f(positions[3 * ic], positions[3 * ic + 1], positions[3 * ic + 2]).sub(a).normalize();
        float d = Math.max(-1f, Math.min(1f, e1.dot(e2)));
        return (float) Math.acos(d);
    }

    public Mesh buildSmooth() {
        int vCount = positions.length / 3;

        // 1) Akumulasi normal face (dibobot sudut) pada tiap posisi unik
        Map<PosKey, Vector3f> sums = new HashMap<>();
        for (int t = 0; t < indices.length; t += 3) {
            Vector3f fn = faceNormal(indices[t], indices[t + 1], indices[t + 2]);
            if (fn == null) continue;
            for (int k = 0; k < 3; k++) {
                int ia = indices[t + k];
                int ib = indices[t + (k + 1) % 3];
                int ic = indices[t + (k + 2) % 3];
                float w = cornerAngle(ia, ib, ic);
                Vector3f weighted = new Vector3f(fn.x * w, fn.y * w, fn.z * w);
                sums.computeIfAbsent(PosKey.of(positions, ia), key -> new Vector3f()).add(weighted);
            }
        }

        // 2) Normalisasi dan susun buffer interleaved
        float[] data = new float[vCount * Mesh.FLOATS_PER_VERTEX];
        for (int i = 0; i < vCount; i++) {
            Vector3f n = sums.get(PosKey.of(positions, i));
            if (n == null || n.lengthSquared() < 1e-12f) n = new Vector3f(0, 1, 0);
            else n = new Vector3f(n).normalize();
            int o = i * Mesh.FLOATS_PER_VERTEX;
            data[o] = positions[3 * i];     data[o + 1] = positions[3 * i + 1]; data[o + 2] = positions[3 * i + 2];
            data[o + 3] = n.x;              data[o + 4] = n.y;                  data[o + 5] = n.z;
            data[o + 6] = uvs[2 * i];       data[o + 7] = uvs[2 * i + 1];
        }
        return new Mesh(data, indices.clone());
    }

    public Mesh buildFlat() {
        int triCount = indices.length / 3;
        float[] data = new float[triCount * 3 * Mesh.FLOATS_PER_VERTEX];
        int[] idx = new int[triCount * 3];
        int out = 0;
        for (int t = 0; t < indices.length; t += 3) {
            Vector3f n = faceNormal(indices[t], indices[t + 1], indices[t + 2]);
            if (n == null) continue;       // buang segitiga degenerate
            for (int k = 0; k < 3; k++) {
                int src = indices[t + k];
                int o = out * Mesh.FLOATS_PER_VERTEX;
                data[o] = positions[3 * src];     data[o + 1] = positions[3 * src + 1]; data[o + 2] = positions[3 * src + 2];
                data[o + 3] = n.x;                data[o + 4] = n.y;                    data[o + 5] = n.z;
                data[o + 6] = uvs[2 * src];       data[o + 7] = uvs[2 * src + 1];
                idx[out] = out;
                out++;
            }
        }
        float[] trimmedData = java.util.Arrays.copyOf(data, out * Mesh.FLOATS_PER_VERTEX);
        int[] trimmedIdx = java.util.Arrays.copyOf(idx, out);
        return new Mesh(trimmedData, trimmedIdx);
    }
}
