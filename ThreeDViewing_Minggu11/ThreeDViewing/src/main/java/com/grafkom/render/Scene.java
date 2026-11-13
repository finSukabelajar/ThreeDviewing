package com.grafkom.render;

import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

/**
 * Isi adegan: lantai, bola, kubus, torus, penanda cahaya, dan kuadrat demo z-fighting.
 * Urutan gambar di list `opaque` (lantai dulu) sengaja dipertahankan supaya efek
 * mematikan Z-buffer terlihat jelas.
 */
public class Scene {

    public static final float GROUND_HALF = 8f;

    public final List<SceneObject> opaque = new ArrayList<>();
    public final SceneObject lightMarker;
    public final SceneObject zFightQuad;

    private final List<Texture2D> textures = new ArrayList<>();

    public Scene() {
        Texture2D checker = load("/textures/checker.png");
        Texture2D uvgrid  = load("/textures/uvgrid.png");
        Texture2D brick   = load("/textures/brick.png");

        // Lantai: y = 0, tekstur diulang 8x
        SceneObject ground = new SceneObject(MeshFactory.plane(GROUND_HALF, 8f), checker, new Vector3f(0.6f, 0.6f, 0.6f));
        ground.castsShadow = false;
        opaque.add(ground);

        // Bola low-poly (16x10): beda Flat vs Smooth paling jelas di sini. Bersinggungan dengan lantai.
        SceneObject sphere = new SceneObject(MeshFactory.sphere(1f, 16, 10), uvgrid, new Vector3f(0.85f, 0.45f, 0.35f));
        sphere.model.translate(-3f, 1f, 0f);
        opaque.add(sphere);

        // Kubus: smooth shading dengan rata-rata normal tampak "menggelembung" di sudut
        SceneObject cube = new SceneObject(MeshFactory.cube(1f), brick, new Vector3f(0.7f, 0.35f, 0.25f));
        cube.model.translate(3f, 1f, 0f).rotateY((float) Math.toRadians(20));
        opaque.add(cube);

        // Torus melayang: bayangan terpisah dari objek + self-shadowing
        SceneObject torus = new SceneObject(MeshFactory.torus(1.2f, 0.45f, 28, 14, 6f, 2f), brick, new Vector3f(0.3f, 0.6f, 0.85f));
        torus.model.translate(0f, 2.4f, -3f).rotateX((float) Math.toRadians(25));
        opaque.add(torus);

        // Penanda posisi cahaya (tanpa pencahayaan)
        lightMarker = new SceneObject(MeshFactory.sphere(1f, 8, 6), null, new Vector3f(1f, 0.9f, 0.2f));
        lightMarker.castsShadow = false;
        lightMarker.unlit = true;

        // Kuadrat merah COPLANAR dengan lantai (y = 0) untuk demo z-fighting
        zFightQuad = new SceneObject(MeshFactory.plane(1.2f, 1f), null, new Vector3f(0.9f, 0.1f, 0.1f));
        zFightQuad.castsShadow = false;
        zFightQuad.model.translate(0f, 0f, 4.5f);
    }

    private Texture2D load(String path) {
        Texture2D t = new Texture2D(path);
        textures.add(t);
        return t;
    }

    public void updateLightMarker(Vector3f lightPos) {
        lightMarker.model.identity().translate(lightPos).scale(0.3f);
    }

    public void cleanup() {
        for (SceneObject o : opaque) o.cleanup();
        lightMarker.cleanup();
        zFightQuad.cleanup();
        for (Texture2D t : textures) t.cleanup();
    }
}
