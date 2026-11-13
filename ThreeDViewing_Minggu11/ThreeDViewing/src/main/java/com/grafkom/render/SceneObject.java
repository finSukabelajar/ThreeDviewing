package com.grafkom.render;

import org.joml.Matrix4f;
import org.joml.Vector3f;

/** Satu benda di adegan: dua versi mesh (smooth/flat), tekstur, warna, dan model matrix. */
public class SceneObject {

    public final Mesh smooth;
    public final Mesh flat;
    public final Texture2D texture;   // boleh null
    public final Vector3f color;
    public final Matrix4f model = new Matrix4f();
    public boolean castsShadow = true;
    public boolean unlit = false;

    public SceneObject(MeshData data, Texture2D texture, Vector3f color) {
        this.smooth = data.buildSmooth();
        this.flat = data.buildFlat();
        this.texture = texture;
        this.color = color;
    }

    public void cleanup() {
        smooth.cleanup();
        flat.cleanup();
    }
}
