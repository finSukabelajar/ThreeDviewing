package com.grafkom.render;

import org.lwjgl.stb.STBImage;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import static org.lwjgl.opengl.GL33.*;

/**
 * Tekstur 2D yang dimuat dari classpath (src/main/resources) lewat STB Image.
 * Konvensi: gambar di-flip saat dimuat sehingga baris atas gambar = v 1.
 * Itu cocok dengan UV pada MeshFactory (v = 0 di bawah).
 */
public class Texture2D {

    public enum FilterMode {
        NEAREST("Nearest"),
        BILINEAR("Bilinear"),
        BILINEAR_MIPMAP("Bilinear+Mipmap");

        private final String label;
        FilterMode(String label) { this.label = label; }
        public String getLabel() { return label; }
        public FilterMode next() { return values()[(ordinal() + 1) % values().length]; }
    }

    private final int id;
    private FilterMode currentMode = null;

    public Texture2D(String resourcePath) {
        id = glGenTextures();
        glActiveTexture(GL_TEXTURE0);      // selalu unit 0: jangan menimpa binding shadow map (unit 1)
        glBindTexture(GL_TEXTURE_2D, id);

        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT);

        ByteBuffer pixels = null;
        ByteBuffer fileData = null;
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer w = stack.mallocInt(1), h = stack.mallocInt(1), ch = stack.mallocInt(1);
            fileData = readResource(resourcePath);
            STBImage.stbi_set_flip_vertically_on_load(true);
            pixels = STBImage.stbi_load_from_memory(fileData, w, h, ch, 4);
            if (pixels == null) {
                throw new IOException("STB gagal decode: " + STBImage.stbi_failure_reason());
            }
            glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8, w.get(0), h.get(0), 0,
                    GL_RGBA, GL_UNSIGNED_BYTE, pixels);
        } catch (IOException e) {
            System.err.println("[Texture2D] " + resourcePath + " gagal dimuat (" + e.getMessage()
                    + "). Memakai tekstur pengganti magenta/hitam.");
            uploadFallback();
        } finally {
            if (pixels != null) STBImage.stbi_image_free(pixels);
            if (fileData != null) MemoryUtil.memFree(fileData);
        }

        glGenerateMipmap(GL_TEXTURE_2D);   // mipmap selalu dibuat; dipakai atau tidak ditentukan filter
        setFilterMode(FilterMode.BILINEAR_MIPMAP);
    }

    private static ByteBuffer readResource(String path) throws IOException {
        try (InputStream in = Texture2D.class.getResourceAsStream(path)) {
            if (in == null) throw new IOException("resource tidak ditemukan");
            byte[] bytes = in.readAllBytes();
            ByteBuffer buf = MemoryUtil.memAlloc(bytes.length); // harus direct buffer
            buf.put(bytes).flip();
            return buf;
        }
    }

    private static void uploadFallback() {
        ByteBuffer px = MemoryUtil.memAlloc(2 * 2 * 4);
        byte[][] c = {{(byte) 255, 0, (byte) 255, (byte) 255}, {0, 0, 0, (byte) 255}};
        for (int y = 0; y < 2; y++) for (int x = 0; x < 2; x++) px.put(c[(x + y) % 2]);
        px.flip();
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8, 2, 2, 0, GL_RGBA, GL_UNSIGNED_BYTE, px);
        MemoryUtil.memFree(px);
    }

    /** Ganti metode filtering (efek terlihat langsung, tanpa memuat ulang gambar). */
    public void setFilterMode(FilterMode mode) {
        if (mode == currentMode) return;
        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, id);
        switch (mode) {
            case NEAREST -> {
                glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
                glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
            }
            case BILINEAR -> {
                glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
                glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
            }
            case BILINEAR_MIPMAP -> {
                glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR_MIPMAP_LINEAR);
                glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
            }
        }
        currentMode = mode;
    }

    public void bind(int unit) {
        glActiveTexture(GL_TEXTURE0 + unit);
        glBindTexture(GL_TEXTURE_2D, id);
    }

    public void cleanup() { glDeleteTextures(id); }
}
