package com.grafkom.render;

import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.system.MemoryStack;

import java.io.IOException;
import java.io.InputStream;
import java.nio.FloatBuffer;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import static org.lwjgl.opengl.GL33.*;

/** Membungkus vertex + fragment shader (GLSL) dan pengaturan uniform. */
public class ShaderProgram {

    private final int programId;
    private final Map<String, Integer> uniformCache = new HashMap<>();

    /** @param vertexPath path resource di classpath, mis. "/shaders/phong.vert" */
    public ShaderProgram(String vertexPath, String fragmentPath) {
        int vs = compile(GL_VERTEX_SHADER, loadResource(vertexPath), vertexPath);
        int fs = compile(GL_FRAGMENT_SHADER, loadResource(fragmentPath), fragmentPath);

        programId = glCreateProgram();
        glAttachShader(programId, vs);
        glAttachShader(programId, fs);
        glLinkProgram(programId);
        if (glGetProgrami(programId, GL_LINK_STATUS) == GL_FALSE) {
            throw new IllegalStateException("Link shader gagal (" + vertexPath + " + " + fragmentPath
                    + "):\n" + glGetProgramInfoLog(programId));
        }
        glDeleteShader(vs);
        glDeleteShader(fs);
    }

    private static int compile(int type, String source, String name) {
        int id = glCreateShader(type);
        glShaderSource(id, source);
        glCompileShader(id);
        if (glGetShaderi(id, GL_COMPILE_STATUS) == GL_FALSE) {
            throw new IllegalStateException("Compile shader gagal (" + name + "):\n" + glGetShaderInfoLog(id));
        }
        return id;
    }

    private static String loadResource(String path) {
        try (InputStream in = ShaderProgram.class.getResourceAsStream(path)) {
            if (in == null) throw new IllegalStateException("Resource shader tidak ditemukan: " + path);
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Gagal membaca shader: " + path, e);
        }
    }

    public void use() { glUseProgram(programId); }

    private int loc(String name) {
        return uniformCache.computeIfAbsent(name, n -> glGetUniformLocation(programId, n));
    }

    // Uniform yang tidak ada / dioptimasi shader -> lokasi -1 -> OpenGL mengabaikannya.
    public void setUniform(String name, int v)     { glUniform1i(loc(name), v); }
    public void setUniform(String name, float v)   { glUniform1f(loc(name), v); }
    public void setUniform(String name, boolean v) { glUniform1i(loc(name), v ? 1 : 0); }
    public void setUniform(String name, Vector3f v) { glUniform3f(loc(name), v.x, v.y, v.z); }

    public void setUniform(String name, Matrix4f m) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            FloatBuffer fb = stack.mallocFloat(16);
            m.get(fb);
            glUniformMatrix4fv(loc(name), false, fb);
        }
    }

    public void setUniform(String name, Matrix3f m) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            FloatBuffer fb = stack.mallocFloat(9);
            m.get(fb);
            glUniformMatrix3fv(loc(name), false, fb);
        }
    }

    public void cleanup() { glDeleteProgram(programId); }
}
