package com.grafkom;

import com.grafkom.camera.Camera3D;
import com.grafkom.camera.OrbitalController;
import com.grafkom.projection.PerspectiveProjection;
import com.grafkom.projection.ProjectionTaxonomyManager;
import com.grafkom.projection.ProjectionTaxonomyManager.ProjectionType;
import com.grafkom.render.HSRManager;
import com.grafkom.render.Light;
import com.grafkom.render.RenderSettings;
import com.grafkom.render.Scene;
import com.grafkom.render.SceneRenderer;
import com.grafkom.render.StereoAnaglyphRenderer;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.opengl.GL;

import java.util.Locale;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL33.*;
import static org.lwjgl.system.MemoryUtil.NULL;

/**
 * Minggu 11: 3D Visual Realism Suite (OpenGL 3.3 core + GLSL).
 * Menggabungkan viewer Minggu 10 (kamera orbital, proyeksi, stereo anaglyph)
 * dengan shading, HSR, tekstur, dan shadow mapping.
 */
public class ThreeDViewing {

    private static final int WIDTH = 1024;
    private static final int HEIGHT = 768;
    private static final String TITLE = "3D Visual Realism";

    private long window;
    private int fbWidth = WIDTH;
    private int fbHeight = HEIGHT;

    private Camera3D camera;
    private OrbitalController controller;
    private double lastTime;

    private PerspectiveProjection perspective;
    private ProjectionTaxonomyManager projectionManager;
    private StereoAnaglyphRenderer stereo;
    private boolean stereoEnabled = false;

    private RenderSettings settings;
    private Light light;
    private Scene scene;
    private SceneRenderer renderer;

    // FPS
    private int frameCount = 0;
    private double fpsTimer = 0;
    private int fps = 0;

    public void run() {
        init();
        loop();
        cleanup();
    }

    private void init() {
        GLFWErrorCallback.createPrint(System.err).set();
        if (!glfwInit()) throw new IllegalStateException("Gagal inisialisasi GLFW");

        glfwDefaultWindowHints();
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
        glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE);
        // Minggu 11 memakai shader: wajib OpenGL 3.3 core profile
        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3);
        glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);
        glfwWindowHint(GLFW_OPENGL_FORWARD_COMPAT, GLFW_TRUE);   // diperlukan di macOS

        window = glfwCreateWindow(WIDTH, HEIGHT, TITLE, NULL, NULL);
        if (window == NULL) throw new RuntimeException("Gagal membuat jendela GLFW (butuh OpenGL 3.3)");

        glfwSetKeyCallback(window, (win, key, scancode, action, mods) -> onKey(win, key, action));

        glfwSetFramebufferSizeCallback(window, (win, w, h) -> {
            if (h == 0) return;
            fbWidth = w;
            fbHeight = h;
            glViewport(0, 0, w, h);
            perspective.setAspectRatio((float) w / h);
        });

        glfwMakeContextCurrent(window);
        glfwSwapInterval(1);   // VSync. Matikan (0) saat mengukur FPS untuk laporan.
        glfwShowWindow(window);

        GL.createCapabilities();
        System.out.println("OpenGL  : " + glGetString(GL_VERSION));
        System.out.println("Renderer: " + glGetString(GL_RENDERER));

        HSRManager.setupDepthTest(true);
        HSRManager.setupBackFaceCulling(true);
        glClearColor(0.1f, 0.1f, 0.15f, 1.0f);

        int[] fbW = new int[1], fbH = new int[1];
        glfwGetFramebufferSize(window, fbW, fbH);
        fbWidth = fbW[0];
        fbHeight = fbH[0];
        glViewport(0, 0, fbWidth, fbHeight);

        camera = new Camera3D(new Vector3f(8f, 7f, 12f), new Vector3f(0f, 1f, 0f), new Vector3f(0f, 1f, 0f));
        controller = new OrbitalController(window, camera);

        perspective = new PerspectiveProjection(45f, (float) fbWidth / fbHeight, 0.1f, 50f);
        projectionManager = new ProjectionTaxonomyManager(perspective);
        stereo = new StereoAnaglyphRenderer();

        settings = new RenderSettings();
        light = new Light();
        scene = new Scene();
        renderer = new SceneRenderer();

        printHelp();
        updateTitle();
        lastTime = glfwGetTime();
    }

    private void onKey(long win, int key, int action) {
        if (key == GLFW_KEY_ESCAPE && action == GLFW_RELEASE) {
            glfwSetWindowShouldClose(win, true);
        }

        // Tombol yang boleh berulang saat ditahan
        if (action == GLFW_PRESS || action == GLFW_REPEAT) {
            switch (key) {
                case GLFW_KEY_LEFT_BRACKET  -> perspective.setFovDegree(perspective.getFovDegree() - 2f);
                case GLFW_KEY_RIGHT_BRACKET -> perspective.setFovDegree(perspective.getFovDegree() + 2f);
                case GLFW_KEY_EQUAL, GLFW_KEY_KP_ADD       -> stereo.setIpd(stereo.getIpd() + 0.05f);
                case GLFW_KEY_MINUS, GLFW_KEY_KP_SUBTRACT  -> stereo.setIpd(stereo.getIpd() - 0.05f);
                case GLFW_KEY_COMMA  -> settings.changeBias(0.5f);   // bias lebih kecil
                case GLFW_KEY_PERIOD -> settings.changeBias(2.0f);   // bias lebih besar
                default -> { }
            }
        }

        if (action != GLFW_PRESS) {
            updateTitle();
            return;
        }

        if (key >= GLFW_KEY_1 && key <= GLFW_KEY_6) {
            selectProjection(ProjectionType.values()[key - GLFW_KEY_1]);
        }

        switch (key) {
            case GLFW_KEY_S -> stereoEnabled = !stereoEnabled;
            // Indikator 1
            case GLFW_KEY_B -> settings.blinnPhong = !settings.blinnPhong;
            case GLFW_KEY_N -> settings.blinnExponentEqualized = !settings.blinnExponentEqualized;
            // Indikator 2
            case GLFW_KEY_F -> settings.smoothShading = !settings.smoothShading;
            case GLFW_KEY_G -> settings.gouraud = !settings.gouraud;
            case GLFW_KEY_M -> settings.wireframe = !settings.wireframe;
            // Indikator 3
            case GLFW_KEY_Z -> HSRManager.toggleDepthTest();
            case GLFW_KEY_C -> HSRManager.toggleCulling();
            case GLFW_KEY_Y -> settings.zFightMode = (settings.zFightMode + 1) % 3;
            // Indikator 4
            case GLFW_KEY_T -> settings.filterMode = settings.filterMode.next();
            case GLFW_KEY_X -> settings.texturesEnabled = !settings.texturesEnabled;
            // Indikator 5
            case GLFW_KEY_H -> settings.shadowsEnabled = !settings.shadowsEnabled;
            case GLFW_KEY_P -> settings.pcf = !settings.pcf;
            case GLFW_KEY_L -> settings.lightAnimated = !settings.lightAnimated;
            default -> { }
        }
        updateTitle();
    }

    private void selectProjection(ProjectionType type) {
        projectionManager.setType(type);
        controller.lookFrom(type.getAzimuthDeg(), type.getElevationDeg());
    }

    private void updateTitle() {
        StringBuilder t = new StringBuilder(TITLE);
        t.append(" | FPS ").append(fps);

        ProjectionType type = projectionManager.getType();
        t.append(" | [").append(type.ordinal() + 1).append("] ").append(type.getLabel());
        if (type.isPerspective()) t.append(" ").append(Math.round(perspective.getFovDegree())).append("\u00B0");
        if (stereoEnabled) {
            t.append(type.isPerspective() ? " | Stereo IPD " + Math.round(stereo.getIpd() * 100) / 100f
                                          : " | Stereo: hanya mode 4-6");
        }

        t.append(" | ").append(settings.blinnPhong ? "Blinn-Phong" : "Phong");
        if (settings.blinnPhong) t.append(" x").append((int) settings.getBlinnScale());
        t.append(" | ").append(settings.smoothShading ? "Smooth" : "Flat");
        t.append(" | ").append(settings.gouraud ? "Gouraud" : "Per-pixel");
        if (settings.wireframe) t.append(" | WIRE");
        t.append(" | Z:").append(HSRManager.isDepthTestEnabled() ? "ON" : "OFF");
        t.append(" Cull:").append(HSRManager.isCullingEnabled() ? "ON" : "OFF");
        t.append(" | Tex:").append(settings.texturesEnabled ? settings.filterMode.getLabel() : "OFF");
        t.append(" | Shadow:").append(settings.shadowsEnabled ? "ON" : "OFF");
        if (settings.shadowsEnabled) {
            t.append(settings.pcf ? " PCF" : " hard");
            t.append(String.format(Locale.ROOT, " bias=%.5f", settings.biasMax));
        }
        if (settings.zFightMode > 0) t.append(" | ZFight:").append(settings.zFightMode == 1 ? "BUG" : "FIXED");

        glfwSetWindowTitle(window, t.toString());
    }

    private static void printHelp() {
        System.out.println("""

            === KONTROL ===
            Mouse drag/scroll : orbit / zoom         1-6 : mode proyeksi   [ ] : FOV
            S : stereo anaglyph    + / - : IPD
            B : Phong <-> Blinn-Phong     N : eksponen Blinn x4 <-> x1
            F : Flat <-> Smooth           G : Gouraud <-> Phong shading   M : wireframe
            Z : Z-buffer on/off           C : back-face culling on/off    Y : demo z-fighting (off/bug/fixed)
            T : filter tekstur (Nearest/Bilinear/Bilinear+Mipmap)         X : tekstur on/off
            H : bayangan on/off           P : PCF on/off                  , . : bias bayangan -/+
            L : animasi cahaya            ESC : keluar
            """);
    }

    private void loop() {
        while (!glfwWindowShouldClose(window)) {
            double now = glfwGetTime();
            float dt = (float) (now - lastTime);
            lastTime = now;
            controller.update(dt);
            if (settings.lightAnimated) light.animate(dt);
            scene.updateLightMarker(light.getPosition());

            // FPS: perbarui judul tiap 0.5 detik
            frameCount++;
            fpsTimer += dt;
            if (fpsTimer >= 0.5) {
                fps = (int) Math.round(frameCount / fpsTimer);
                frameCount = 0;
                fpsTimer = 0;
                updateTitle();
            }

            // Pass 1: shadow map (sekali per frame, dipakai bersama oleh kedua mata pada stereo)
            renderer.renderShadowPass(scene, light, settings, fbWidth, fbHeight);

            // Pass 2: gambar ke layar
            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
            if (stereoEnabled && projectionManager.getType().isPerspective()) {
                stereo.setFocalLength(camera.getRadius());
                stereo.renderAnaglyph(camera, perspective,
                        (proj, view) -> renderer.renderMainPass(scene, light, settings, proj, view));
            } else {
                Matrix4f proj = projectionManager.calculateProjectionMatrix(camera.getRadius());
                Matrix4f view = camera.calculateLookAtMatrix();
                renderer.renderMainPass(scene, light, settings, proj, view);
            }

            glfwSwapBuffers(window);
            glfwPollEvents();
        }
    }

    private void cleanup() {
        renderer.cleanup();
        scene.cleanup();
        glfwDestroyWindow(window);
        glfwTerminate();
        GLFWErrorCallback cb = glfwSetErrorCallback(null);
        if (cb != null) cb.free();
    }

    public static void main(String[] args) {
        new ThreeDViewing().run();
    }
}
