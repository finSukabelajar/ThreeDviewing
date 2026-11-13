package com.grafkom.render;

import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import static org.lwjgl.opengl.GL33.*;

/**
 * Renderer dua-pass:
 *   1) renderShadowPass : adegan digambar dari sudut cahaya -> shadow map (depth)
 *   2) renderMainPass   : adegan digambar dari kamera dengan shader Phong atau Gouraud
 * renderMainPass TIDAK membersihkan layar (pemanggil / StereoAnaglyphRenderer yang melakukannya).
 */
public class SceneRenderer {

    private static final int SHADOW_MAP_SIZE = 2048;

    private final ShaderProgram phong;
    private final ShaderProgram gouraud;
    private final ShaderProgram depth;
    private final ShadowMapFBO shadowMap;
    private final Matrix4f lightSpace = new Matrix4f();

    public SceneRenderer() {
        phong = new ShaderProgram("/shaders/phong.vert", "/shaders/phong.frag");
        gouraud = new ShaderProgram("/shaders/gouraud.vert", "/shaders/gouraud.frag");
        depth = new ShaderProgram("/shaders/depth.vert", "/shaders/depth.frag");
        shadowMap = new ShadowMapFBO(SHADOW_MAP_SIZE);
    }

    // ------------------------------------------------------------------ Pass 1
    public void renderShadowPass(Scene scene, Light light, RenderSettings s, int screenW, int screenH) {
        lightSpace.set(light.getLightSpaceMatrix());
        if (!s.shadowsEnabled) return;

        shadowMap.bindForWriting();
        HSRManager.enterShadowPass();                 // depth test wajib ON, terlepas dari tombol Z
        glPolygonMode(GL_FRONT_AND_BACK, GL_FILL);    // wireframe tidak boleh merusak shadow map

        depth.use();
        depth.setUniform("lightSpaceMatrix", lightSpace);
        for (SceneObject o : scene.opaque) {
            if (!o.castsShadow) continue;
            depth.setUniform("model", o.model);
            o.smooth.draw();
        }
        shadowMap.unbind(screenW, screenH);
    }

    // ------------------------------------------------------------------ Pass 2
    public void renderMainPass(Scene scene, Light light, RenderSettings s, Matrix4f projection, Matrix4f view) {
        HSRManager.apply();
        glPolygonMode(GL_FRONT_AND_BACK, s.wireframe ? GL_LINE : GL_FILL);

        ShaderProgram sp = s.gouraud ? gouraud : phong;
        sp.use();

        // Posisi kamera/mata dari invers view matrix (benar juga untuk tiap mata pada stereo)
        Vector3f viewPos = view.invert(new Matrix4f()).getTranslation(new Vector3f());

        sp.setUniform("view", view);
        sp.setUniform("projection", projection);
        sp.setUniform("lightSpaceMatrix", lightSpace);
        sp.setUniform("lightDir", light.getDirectionToLight());
        sp.setUniform("lightColor", light.getColor());
        sp.setUniform("viewPos", viewPos);
        sp.setUniform("useBlinnPhong", s.blinnPhong);
        sp.setUniform("blinnScale", s.getBlinnScale());
        sp.setUniform("shininess", 32f);
        sp.setUniform("ambientStrength", 0.2f);
        sp.setUniform("specularStrength", 0.8f);
        sp.setUniform("useShadow", s.shadowsEnabled);
        sp.setUniform("usePCF", s.pcf);
        sp.setUniform("biasMax", s.biasMax);
        sp.setUniform("biasMin", s.getBiasMin());
        sp.setUniform("textureSampler", 0);
        sp.setUniform("shadowMap", 1);
        shadowMap.bindTexture(1);

        for (SceneObject o : scene.opaque) drawObject(sp, o, s);

        if (s.zFightMode > 0) {
            if (s.zFightMode == 2) {                  // perbaikan: tarik sedikit ke arah kamera
                glEnable(GL_POLYGON_OFFSET_FILL);
                glPolygonOffset(-1f, -1f);
            }
            drawObject(sp, scene.zFightQuad, s);
            glDisable(GL_POLYGON_OFFSET_FILL);
        }

        drawObject(sp, scene.lightMarker, s);
        glPolygonMode(GL_FRONT_AND_BACK, GL_FILL);
    }

    private void drawObject(ShaderProgram sp, SceneObject o, RenderSettings s) {
        sp.setUniform("model", o.model);
        sp.setUniform("normalMatrix", new Matrix3f(o.model).invert().transpose());
        sp.setUniform("objectColor", o.color);
        sp.setUniform("unlit", o.unlit);

        boolean useTex = s.texturesEnabled && o.texture != null;
        sp.setUniform("useTexture", useTex);
        if (useTex) {
            o.texture.setFilterMode(s.filterMode);
            o.texture.bind(0);
        }
        (s.smoothShading ? o.smooth : o.flat).draw();
    }

    public void cleanup() {
        phong.cleanup();
        gouraud.cleanup();
        depth.cleanup();
        shadowMap.cleanup();
    }
}
