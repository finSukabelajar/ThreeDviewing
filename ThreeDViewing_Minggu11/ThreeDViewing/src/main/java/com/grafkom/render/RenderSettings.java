package com.grafkom.render;

/** Seluruh status toggle yang diatur lewat keyboard. */
public class RenderSettings {
    // Indikator 1: model pencahayaan
    public boolean blinnPhong = true;
    public boolean blinnExponentEqualized = true;   // true: eksponen Blinn = 4x Phong
    // Indikator 2: shading
    public boolean smoothShading = true;
    public boolean gouraud = false;                 // false: Phong shading (per-pixel)
    public boolean wireframe = false;
    // Indikator 4: tekstur
    public boolean texturesEnabled = true;
    public Texture2D.FilterMode filterMode = Texture2D.FilterMode.BILINEAR_MIPMAP;
    // Indikator 5: bayangan
    public boolean shadowsEnabled = true;
    public boolean pcf = true;
    public float biasMax = 0.0025f;                 // dalam satuan kedalaman [0,1]
    // Demo z-fighting: 0 = mati, 1 = tampil (z-fighting), 2 = tampil + polygon offset
    public int zFightMode = 0;
    // Animasi cahaya
    public boolean lightAnimated = false;

    public float getBiasMin() { return biasMax * 0.16f; }

    public float getBlinnScale() { return blinnExponentEqualized ? 4.0f : 1.0f; }

    public void changeBias(float factor) {
        biasMax = Math.max(0.00001f, Math.min(0.2f, biasMax * factor));
    }
}
